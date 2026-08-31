package com.floydwiz.googlemdm.di

import com.floydwiz.googlemdm.BuildConfig
import com.floydwiz.googlemdm.data.local.AndroidDeviceInfoProvider
import com.floydwiz.googlemdm.data.local.DeviceCredentialsStore
import com.floydwiz.googlemdm.data.local.DeviceInfoProvider
import com.floydwiz.googlemdm.data.local.SecureDeviceCredentials
import com.floydwiz.googlemdm.data.remote.ApiClient
import com.floydwiz.googlemdm.data.remote.AuthInterceptor
import com.floydwiz.googlemdm.data.remote.DevHostnameOverride
import com.floydwiz.googlemdm.data.remote.EmmApiService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {

    @Binds
    @Singleton
    abstract fun bindDeviceCredentialsStore(impl: SecureDeviceCredentials): DeviceCredentialsStore

    @Binds
    @Singleton
    abstract fun bindDeviceInfoProvider(impl: AndroidDeviceInfoProvider): DeviceInfoProvider
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(credentialsStore: DeviceCredentialsStore): AuthInterceptor {
        return AuthInterceptor(credentialsStore)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return ApiClient.buildOkHttpClient(
            authInterceptor = authInterceptor,
            enableLogging = BuildConfig.DEBUG,
            devHostnameOverride = if (BuildConfig.DEBUG) {
                // The local dev backend's self-signed cert has no SAN, only a CN,
                // so standard hostname verification correctly rejects it. We can't
                // change the backend's cert, so debug builds pin to this exact
                // known cert instead of disabling verification generally.
                // Regenerate this pin (see docs/emm-backend-integration-test-plan.md)
                // if the backend's dev cert is ever regenerated.
                DevHostnameOverride(
                    hosts = setOf("localhost", "10.0.2.2"),
                    certificateSha256Pin = "sha256/ZBDrNath2tZT00LoGtGXDxKSYKK1JTuD33jSYDaU14U="
                )
            } else {
                null
            }
        )
    }

    @Provides
    @Singleton
    fun provideEmmApiService(okHttpClient: OkHttpClient): EmmApiService {
        return ApiClient.buildEmmApiService(BuildConfig.EMM_BASE_URL, okHttpClient)
    }
}
