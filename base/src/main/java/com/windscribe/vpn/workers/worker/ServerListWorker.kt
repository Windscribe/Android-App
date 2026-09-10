package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.workers.ReconnectAfterRefresh
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

@HiltWorker
class ServerListWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val serverListRepository: ServerListRepository,
        private val userRepository: UserRepository,
        private val reconnectAfterRefresh: ReconnectAfterRefresh,
        private val mainScope: CoroutineScope,
    ) : CoroutineWorker(context, workerParams) {
        private val logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result {
            if (!userRepository.loggedIn()) return Result.failure()

            return runCatching {
                serverListRepository.update()
                logger.debug("Successfully updated server list.")
                mainScope.launch {
                    reconnectAfterRefresh.run()
                }
                Result.success()
            }.getOrElse { error ->
                logger.debug("Failed to update server list: $error")
                Result.failure()
            }
        }
    }
