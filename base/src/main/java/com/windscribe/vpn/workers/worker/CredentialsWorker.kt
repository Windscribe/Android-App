package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.slf4j.LoggerFactory

@HiltWorker
class CredentialsWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val connectionDataRepository: ConnectionDataRepository,
        private val userRepository: UserRepository,
    ) : CoroutineWorker(appContext, params) {
        private val logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result =
            if (userRepository.loggedIn() && userRepository.accountStatusOkay()) {
                try {
                    connectionDataRepository.update()
                    logger.debug("Successful updated credentials data.")
                    Result.success()
                } catch (_: Exception) {
                    Result.failure()
                }
            } else {
                Result.failure()
            }
    }
