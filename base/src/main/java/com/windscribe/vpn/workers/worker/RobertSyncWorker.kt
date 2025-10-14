/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.ApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.repository.CallResult
import javax.inject.Inject

class RobertSyncWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var apiManager: ApiCallManager

    init {
        appContext.applicationComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        return try {
            val result = result<GenericSuccess> {
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
