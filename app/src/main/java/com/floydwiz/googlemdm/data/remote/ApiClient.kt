package com.floydwiz.googlemdm.data.remote

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier

/**
 * Narrow, dev-only workaround for the local backend's self-signed cert
 * having no Subject Alternative Name - only a CN. Modern hostname
 * verification (correctly) ignores CN and rejects that, so we scope an
 * exception to exactly [hosts] and require the connection to still present
 * the exact certificate pinned by [certificateSha256Pin]. This is NOT a
 * trust-all workaround: an unrelated/rogue cert for these hosts still fails.
 */
data class DevHostnameOverride(
    val hosts: Set<String>,
    val certificateSha256Pin: String
)

/**
 * Builds the OkHttp/Retrofit stack for the EMM backend. Kept as plain factory
 * functions (rather than baked into the Hilt module) so tests can point the
 * same construction logic at a MockWebServer instance.
 */
object ApiClient {

    fun buildOkHttpClient(
        authInterceptor: AuthInterceptor,
        enableLogging: Boolean,
        devHostnameOverride: DevHostnameOverride? = null
    ): OkHttpClient {
        // BASIC only: method/URL/status/size. Never BODY - the enroll response
        // carries the deviceApiKey and must not be dumped to logcat.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (enableLogging) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }

        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)

        if (devHostnameOverride != null) {
            builder
                .hostnameVerifier(
                    HostnameVerifier { hostname, _ ->
                        devHostnameOverride.hosts.any { it.equals(hostname, ignoreCase = true) }
                    }
                )
                .certificatePinner(
                    CertificatePinner.Builder().apply {
                        devHostnameOverride.hosts.forEach { host ->
                            add(host, devHostnameOverride.certificateSha256Pin)
                        }
                    }.build()
                )
        }

        return builder.build()
    }

    fun buildRetrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun buildEmmApiService(baseUrl: String, okHttpClient: OkHttpClient): EmmApiService {
        return buildRetrofit(baseUrl, okHttpClient).create(EmmApiService::class.java)
    }
}
