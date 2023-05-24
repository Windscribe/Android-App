/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services.firebasecloud

import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.backend.utils.WindVpnController
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject

class WindscribeCloudMessaging : FirebaseMessagingService() {

    @Inject
    lateinit var vpnController: WindVpnController
    private val logger: Logger = LoggerFactory.getLogger("fcm")
    override fun onCreate() {
        super.onCreate()
        appContext.applicationComponent.inject(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        logger.info("Received cloud message from: " + remoteMessage.from)
        if (remoteMessage.notification != null) {
            logger.info("Message Notification Body: " + remoteMessage.notification?.body)
        }
        val payload = remoteMessage.data
        if (payload.containsKey("type")) {
            when (payload["type"]) {
                ACCOUNT_DOWNGRADE -> {
                    logger.info("Received Account downgrade notification, scheduling service task...")
                    appContext.workManager.updateSession()
                }
                ACCOUNT_EXPIRED -> {
                    logger.info("Received Account expired notification, scheduling service task...")
                    appContext.workManager.updateSession()
                }
                FORCE_DISCONNECT -> {
                    logger.info("Received Force disconnect notification , stopping VPN Services.")
                    with(vpnController) {
                        scope.launch { disconnectAsync() }
                    }
                }
                PROMO -> {
                    logger.info("Received Promo notification , Launching upgrade Activity.")
                    val pushNotificationAction = payloadToPushNotificationAction(payload)
                    if (pushNotificationAction != null) {
                        val launchIntent = appContext.applicationInterface.upgradeIntent
                        launchIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                        appContext.appLifeCycleObserver.pushNotificationAction = pushNotificationAction
                        startActivity(launchIntent)
                    }
                }
            }
        }
    }

    private fun payloadToPushNotificationAction(payload: Map<String, String>): PushNotificationAction? {
        return payload["pcpid"]?.let {
            payload["type"]?.let { it1 ->
                payload["promo_code"]?.let { it2 ->
                    PushNotificationAction(
                            pcpID = it,
                            type = it1,
                            promoCode = it2
                    )
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        logger.info("Received new FCM Token = $token")
    }

    companion object {

        const val ACCOUNT_DOWNGRADE = "account_downgrade"
        const val ACCOUNT_EXPIRED = "account_expired"
        const val FORCE_DISCONNECT = "force_disconnect"
        const val PROMO = "promo"
    }
}
