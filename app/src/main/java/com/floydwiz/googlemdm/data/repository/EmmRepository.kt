package com.floydwiz.googlemdm.data.repository

import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.data.local.DeviceCredentialsStore
import com.floydwiz.googlemdm.data.local.DeviceInfoProvider
import com.floydwiz.googlemdm.data.model.CheckInResult
import com.floydwiz.googlemdm.data.model.EnrollmentResult
import com.floydwiz.googlemdm.data.remote.CheckInRequestDto
import com.floydwiz.googlemdm.data.remote.CommandAckRequestDto
import com.floydwiz.googlemdm.data.remote.CommandDto
import com.floydwiz.googlemdm.data.remote.EmmApiService
import com.floydwiz.googlemdm.data.remote.EnrollRequestDto
import com.floydwiz.googlemdm.enterprise.command.CommandExecutor
import com.floydwiz.googlemdm.enterprise.command.model.CommandOutcome
import com.floydwiz.googlemdm.enterprise.command.model.CommandType
import com.floydwiz.googlemdm.enterprise.policy.PolicyApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point of contact between the DPC/UI layers and the EMM backend.
 * DevicePolicyManager code and Compose screens never call Retrofit directly.
 */
@Singleton
class EmmRepository @Inject constructor(
    private val apiService: EmmApiService,
    private val credentialsStore: DeviceCredentialsStore,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val policyApplier: PolicyApplier,
    private val commandExecutor: CommandExecutor
) {

    val isEnrolled: Boolean get() = credentialsStore.isEnrolled()

    val deviceId: String? get() = credentialsStore.getDeviceId()

    val checkInIntervalSeconds: Long get() = credentialsStore.getCheckInIntervalSeconds()

    val lastSuccessfulCheckInAtMillis: Long? get() = credentialsStore.getLastSuccessfulCheckInAtMillis()

    /** Clears local enrollment state only - the backend contract has no DPC unenroll endpoint to call. */
    suspend fun clearEnrollment() = withContext(Dispatchers.IO) {
        credentialsStore.clear()
        Logger.i("Local enrollment state cleared")
    }

    suspend fun enroll(enrollmentToken: String): EnrollmentResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.enroll(EnrollRequestDto(enrollmentToken))

            if (!response.isSuccessful) {
                Logger.e("Enrollment failed: HTTP ${response.code()}")
                return@withContext EnrollmentResult.Error("Enrollment failed (HTTP ${response.code()})")
            }

            val body = response.body()
                ?: return@withContext EnrollmentResult.Error("Enrollment succeeded but response body was empty")

            val interval = body.checkInIntervalSeconds ?: credentialsStore.getCheckInIntervalSeconds()
            credentialsStore.saveEnrollment(body.deviceId, body.deviceApiKey, interval)

            Logger.i("Enrollment succeeded, deviceId=${body.deviceId}")
            EnrollmentResult.Success(deviceId = body.deviceId, checkInIntervalSeconds = interval)
        } catch (e: IOException) {
            Logger.e("Enrollment network error: ${e.message}")
            EnrollmentResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Logger.e("Enrollment unexpected error: ${e.message}")
            EnrollmentResult.Error(e.message ?: "Unknown enrollment error")
        }
    }

    suspend fun checkIn(): CheckInResult = withContext(Dispatchers.IO) {
        if (!credentialsStore.isEnrolled()) {
            Logger.w("Check-in skipped: device is not enrolled")
            return@withContext CheckInResult.MissingCredentials
        }

        try {
            val request = CheckInRequestDto(
                osVersion = deviceInfoProvider.osVersion(),
                model = deviceInfoProvider.model(),
                manufacturer = deviceInfoProvider.manufacturer(),
                lastPolicyVersionApplied = credentialsStore.getLastPolicyVersionApplied()
            )

            val response = apiService.checkIn(request)

            if (response.code() == 401) {
                Logger.w("Check-in unauthorized: stored deviceApiKey was rejected - clearing local enrollment state")
                credentialsStore.clear()
                return@withContext CheckInResult.Unauthorized
            }

            if (!response.isSuccessful) {
                Logger.e("Check-in failed: HTTP ${response.code()}")
                return@withContext CheckInResult.Error("Check-in failed (HTTP ${response.code()})")
            }

            val body = response.body()
                ?: return@withContext CheckInResult.Error("Check-in succeeded but response body was empty")

            val interval = body.checkInIntervalSeconds ?: credentialsStore.getCheckInIntervalSeconds()
            credentialsStore.setCheckInIntervalSeconds(interval)
            credentialsStore.setLastSuccessfulCheckInAtMillis(System.currentTimeMillis())

            // body.policy == null means "no change since lastPolicyVersionApplied,"
            // not "no policy assigned" - only touch device state when it's present.
            body.policy?.let { policy ->
                policyApplier.apply(policy)
                credentialsStore.setLastPolicyVersionApplied(body.policyVersion)
                Logger.i("Applied policy version ${body.policyVersion}")
            }

            val commands = body.pendingCommands.orEmpty()
            val (terminalCommands, nonTerminalCommands) = commands.partition {
                it.type == CommandType.WIPE.name || it.type == CommandType.REBOOT.name
            }

            nonTerminalCommands.forEach { processCommand(it) }

            // At most one terminal command is expected per check-in - the
            // device won't survive to process a second one, so only the
            // first is actioned; any others stay SENT until next check-in.
            terminalCommands.firstOrNull()?.let { processTerminalCommand(it) }

            Logger.i(
                "Check-in succeeded: policyVersion=${body.policyVersion}, " +
                    "policyUpdateApplied=${body.policy != null}, " +
                    "commandsProcessed=${commands.size}, " +
                    "intervalSeconds=$interval"
            )

            CheckInResult.Success(
                checkInIntervalSeconds = interval,
                hasPolicy = body.policy != null,
                policyVersion = body.policyVersion,
                pendingCommandCount = commands.size
            )
        } catch (e: IOException) {
            Logger.e("Check-in network error: ${e.message}")
            CheckInResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Logger.e("Check-in unexpected error: ${e.message}")
            CheckInResult.Error(e.message ?: "Unknown check-in error")
        }
    }

    private suspend fun processCommand(command: CommandDto) {
        val outcome = commandExecutor.execute(command)
        ackCommand(command.commandId, outcome)
    }

    /**
     * WIPE/REBOOT tear the device down essentially immediately once
     * executed, leaving no chance to ack afterward - so ack first (once
     * Device Owner is confirmed), then invoke the destructive call.
     */
    private suspend fun processTerminalCommand(command: CommandDto) {
        if (!commandExecutor.isReadyForDestructiveCommand()) {
            ackCommand(command.commandId, CommandOutcome.Failed("App is not Device Owner"))
            return
        }

        ackCommand(command.commandId, CommandOutcome.Completed())
        commandExecutor.execute(command)
    }

    private suspend fun ackCommand(commandId: String, outcome: CommandOutcome) {
        val ackRequest = when (outcome) {
            is CommandOutcome.Completed -> CommandAckRequestDto(status = "COMPLETED", resultData = outcome.resultData)
            is CommandOutcome.Failed -> CommandAckRequestDto(status = "FAILED", errorMessage = outcome.message)
        }

        try {
            apiService.acknowledgeCommand(commandId, ackRequest)
        } catch (e: Exception) {
            Logger.e("Failed to ack command $commandId: ${e.message}")
        }
    }
}
