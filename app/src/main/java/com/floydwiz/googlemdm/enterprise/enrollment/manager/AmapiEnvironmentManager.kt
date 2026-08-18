package com.floydwiz.googlemdm.enterprise.enrollment.manager

import android.content.ComponentName
import android.content.Context
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.receiver.MyDeviceAdminReceiver
import com.google.android.managementapi.common.model.Role
import com.google.android.managementapi.environment.EnvironmentClient
import com.google.android.managementapi.environment.EnvironmentClientFactory
import com.google.android.managementapi.environment.model.GetEnvironmentRequest
import com.google.android.managementapi.environment.model.PrepareEnvironmentRequest
import com.google.android.managementapi.environment.model.PrepareEnvironmentResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmapiEnvironmentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val environmentClient: EnvironmentClient = EnvironmentClientFactory.create(context)

    suspend fun getEnvironment(): Boolean {
        return try {
            val role = Role.builder()
                .setRoleType(
                    Role.RoleType.DEVICE_POLICY_CONTROLLER
                )
                .build()

            val request = GetEnvironmentRequest.builder()
                .setRoles(listOf(role))
                .build()

            val environment = environmentClient.getEnvironment(request)

            Logger.d("AMAPI Environment received: $environment")
            true
        } catch (e: Exception) {
            Logger.e("Failed to get AMAPI Environment: ${e.message}")
            false
        }
    }

    suspend fun prepareEnvironment(): PrepareEnvironmentResponse? {
        return try {
            val adminComponent = ComponentName(
                context,
                MyDeviceAdminReceiver::class.java
            )

            val role = Role.builder()
                .setRoleType(
                    Role.RoleType.DEVICE_POLICY_CONTROLLER
                )
                .build()

            val request = PrepareEnvironmentRequest.builder()
                .setRoles(listOf(role))
                .setAdmin(adminComponent)
                .build()

            Logger.d(
                "Starting AMAPI Environment preparation"
            )

            val response = environmentClient.prepareEnvironment(
                request,
                null
            )
            val environment =
                response.environment

            val adpEnvironment =
                environment.androidDevicePolicyEnvironment

            Logger.d(
                "AMAPI Prepare Environment State = ${adpEnvironment.state}"
            )
            Logger.d(
                "AMAPI Prepare Environment Version = ${adpEnvironment.version}"
            )
            response
        } catch (e: Exception) {
            Logger.e("Failed to prepare AMAPI Environment: $e.message")
            null
        }
    }
}