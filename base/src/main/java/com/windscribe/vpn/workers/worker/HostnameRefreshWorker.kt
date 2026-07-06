/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.backend.utils.ExcludedIpHolder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.slf4j.LoggerFactory

@HiltWorker
class HostnameRefreshWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val excludedIpHolder: ExcludedIpHolder,
    ) : CoroutineWorker(context, workerParams) {
        private val logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result =
            try {
                excludedIpHolder.forceRefreshAll()
                Result.success()
            } catch (e: Exception) {
                logger.error("Failed to refresh hostnames: ${e.message}", e)
                Result.retry()
            }
    }
