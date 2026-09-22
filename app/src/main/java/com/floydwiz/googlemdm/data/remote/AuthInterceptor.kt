package com.floydwiz.googlemdm.data.remote

import com.floydwiz.googlemdm.data.local.DeviceCredentialsStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches the device's bearer credential to every DPC API request except
 * enrollment, which has no credential yet.
 */
class AuthInterceptor @Inject constructor(
    private val credentialsStore: DeviceCredentialsStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.url.encodedPath.endsWith("/api/dpc/enroll")) {
            return chain.proceed(request)
        }

        val apiKey = credentialsStore.getDeviceApiKey()
            ?: return chain.proceed(request)

        val authorizedRequest = request.newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .build()

        return chain.proceed(authorizedRequest)
    }
}
