package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@HiltWorker
class StaticIpWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val staticIpRepository: StaticIpRepository,
    private val userRepository: UserRepository
) : CoroutineWorker(context, params) {
    val logger: Logger = LoggerFactory.getLogger("worker")

    override suspend fun doWork(): Result {
        if(!userRepository.loggedIn())return Result.failure()
        try {
            staticIpRepository.updateFromApi()
            staticIpRepository.load()
            logger.debug("Successfully updated static ip list.")
            return Result.success()
        } catch (e: Exception) {
            logger.debug("Failed to update static ip list.: $e")
            return Result.failure()
        }
    }
}