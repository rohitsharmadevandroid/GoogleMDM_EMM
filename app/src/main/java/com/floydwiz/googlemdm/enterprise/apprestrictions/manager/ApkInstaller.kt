package com.floydwiz.googlemdm.enterprise.apprestrictions.manager

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.floydwiz.googlemdm.core.logger.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads a backend-supplied APK, verifies it against the expected
 * SHA-256 before touching PackageInstaller, then installs it with no user
 * prompt. USER_ACTION_NOT_REQUIRED only works for Device Owner (and
 * Profile Owner) apps - a regular app committing a session still forces
 * the system install confirmation UI.
 */
@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageInstaller = context.packageManager.packageInstaller

    suspend fun downloadAndInstall(packageName: String, apkUrl: String, apkSha256: String): Boolean {
        val tempFile = File(context.cacheDir, "install-$packageName.apk")
        return try {
            if (!download(apkUrl, tempFile, apkSha256)) {
                false
            } else {
                installSilently(packageName, tempFile)
            }
        } catch (e: Exception) {
            Logger.e("Failed to download/install $packageName from $apkUrl: ${e.message}")
            false
        } finally {
            tempFile.delete()
        }
    }

    private fun download(apkUrl: String, destination: File, expectedSha256: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = URL(apkUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.connect()
            if (connection.responseCode !in 200..299) {
                Logger.e("APK download for $apkUrl failed: HTTP ${connection.responseCode}")
                return false
            }
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
        val matches = actualSha256.equals(expectedSha256, ignoreCase = true)
        if (!matches) {
            Logger.e("APK from $apkUrl failed checksum verification (expected $expectedSha256, got $actualSha256) - not installing")
        }
        return matches
    }

    private suspend fun installSilently(packageName: String, apkFile: File): Boolean {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setInstallReason(PackageManager.INSTALL_REASON_POLICY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {
            session.openWrite(packageName, 0, apkFile.length()).use { out ->
                apkFile.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
        } catch (e: Exception) {
            session.abandon()
            Logger.e("Failed to write install session for $packageName: ${e.message}")
            return false
        }

        return awaitCommitResult(session, sessionId, packageName)
    }

    private suspend fun awaitCommitResult(
        session: PackageInstaller.Session,
        sessionId: Int,
        packageName: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val action = "com.floydwiz.googlemdm.APK_INSTALL_RESULT.$sessionId"
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                receiverContext.unregisterReceiver(this)
                val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                val success = status == PackageInstaller.STATUS_SUCCESS
                Logger.i("Silent install result for $packageName: status=$status ($message), success=$success")
                if (continuation.isActive) continuation.resume(success) { }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(action),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        continuation.invokeOnCancellation {
            runCatching { context.unregisterReceiver(receiver) }
        }

        // FLAG_MUTABLE is required - the system fills in EXTRA_STATUS/EXTRA_STATUS_MESSAGE
        // on this exact PendingIntent's extras before delivering it.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId,
            Intent(action).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        session.commit(pendingIntent.intentSender)
        session.close()
    }
}
