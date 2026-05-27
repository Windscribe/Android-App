/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.api.ApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.repository.CallResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RobertSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParameters: WorkerParameters,
        private val apiManager: ApiCallManager,
    ) : CoroutineWorker(context, workerParameters) {
        override suspend fun doWork(): Result {
            return try {
                val result =
                    result<GenericSuccess> {
                        apiManager.syncRobert()
                    }
                return when (result) {
                    is CallResult.Error -> Result.failure()
                    is CallResult.Success<*> -> Result.success()
                }
            } catch (_: Exception) {
                Result.failure()
            }
        }
    }
