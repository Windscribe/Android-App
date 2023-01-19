package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.repository.LatencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LatencyWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    @Inject
    lateinit var latencyRepository: LatencyRepository

    init {
        Windscribe.appContext.applicationComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            return@withContext if (latencyRepository.updateAllServerLatencies()) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }
}