package com.floydwiz.googlemdm.di

import android.content.Context
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {


    @Singleton
    fun provideDeviceAdminManager(
        @ApplicationContext context: Context
    ): DeviceAdminManager {
        return DeviceAdminManager(context)
    }
}