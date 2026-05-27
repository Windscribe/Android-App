package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.repository.LatencyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class LatencyWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val latencyRepository: LatencyRepository,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result {
            return withContext(Dispatchers.IO) {
                runCatching {
                    latencyRepository.updateAllServerLatencies()
                }
                return@withContext Result.success()
            }
        }
    }
