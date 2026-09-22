package com.floydwiz.googlemdm.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the OkHttp/Retrofit stack for the EMM backend. Kept as plain factory
 * functions (rather than baked into the Hilt module) so tests can point the
 * same construction logic at a MockWebServer instance.
 */
object ApiClient {

    fun buildOkHttpClient(
        authInterceptor: AuthInterceptor,
        enableLogging: Boolean
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

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
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
