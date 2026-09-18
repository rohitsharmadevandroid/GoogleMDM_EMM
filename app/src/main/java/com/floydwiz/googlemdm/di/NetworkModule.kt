package com.floydwiz.googlemdm.di

import com.floydwiz.googlemdm.BuildConfig
import com.floydwiz.googlemdm.data.local.AndroidDeviceInfoProvider
import com.floydwiz.googlemdm.data.local.DeviceCredentialsStore
import com.floydwiz.googlemdm.data.local.DeviceInfoProvider
import com.floydwiz.googlemdm.data.local.SecureDeviceCredentials
import com.floydwiz.googlemdm.data.remote.ApiClient
import com.floydwiz.googlemdm.data.remote.AuthInterceptor
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
        // Standard TLS verification (network_security_config.xml scopes CA
        // trust to the dev backend's hosts in debug builds) - the backend's
        // dev cert has a real SAN now, so no hostname-verifier/pinning
        // workaround is needed.
        return ApiClient.buildOkHttpClient(
            authInterceptor = authInterceptor,
            enableLogging = BuildConfig.DEBUG
        )
    }

    @Provides
    @Singleton
    fun provideEmmApiService(okHttpClient: OkHttpClient): EmmApiService {
        return ApiClient.buildEmmApiService(BuildConfig.EMM_BASE_URL, okHttpClient)
    }
}
