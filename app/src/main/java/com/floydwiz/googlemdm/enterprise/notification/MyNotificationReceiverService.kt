package com.floydwiz.googlemdm.enterprise.notification

import com.floydwiz.googlemdm.core.logger.Logger
import com.google.android.managementapi.environment.EnvironmentListener
import com.google.android.managementapi.environment.model.EnvironmentEvent
import com.google.android.managementapi.notification.NotificationReceiverService

class MyNotificationReceiverService : NotificationReceiverService() {

    override fun getPrepareEnvironmentListener(): EnvironmentListener {
        return object : EnvironmentListener{
            override fun onEnvironmentEvent(event: EnvironmentEvent) {
                Logger.d("AMAPI Environment Event received: $event")
            }
        }
    }
}