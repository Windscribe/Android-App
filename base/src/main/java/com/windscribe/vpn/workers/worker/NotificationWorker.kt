/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.repository.NotificationRepository
import com.windscribe.vpn.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.slf4j.LoggerFactory

@HiltWorker
class NotificationWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val notificationRepository: NotificationRepository,
        private val userRepository: UserRepository,
    ) : CoroutineWorker(context, workerParams) {
        private val logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result {
            if (!userRepository.loggedIn()) return Result.failure()
            return try {
                notificationRepository.update()
                Result.success()
            } catch (e: Exception) {
                logger.debug("Failed to update notifications: $e")
                Result.failure()
            }
        }
    }
