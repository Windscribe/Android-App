/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.repository.NotificationRepository
import com.windscribe.vpn.repository.UserRepository
import javax.inject.Inject
import org.slf4j.LoggerFactory

class NotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val logger = LoggerFactory.getLogger("worker")
    @Inject
    lateinit var notificationRepository: NotificationRepository
    @Inject
    lateinit var userRepository: UserRepository

    init {
        appContext.applicationComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        if(!userRepository.loggedIn())return Result.failure()
        return notificationRepository.update().result{ success, error ->
            if(success){
                logger.debug("Successful updated notification data.")
            }else{
                logger.debug("Failed to update notification data: $error")
            }
        }
    }
}
