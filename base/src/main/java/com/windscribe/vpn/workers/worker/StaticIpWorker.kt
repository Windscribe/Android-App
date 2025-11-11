package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject

class StaticIpWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @Inject
    lateinit var staticIpRepository: StaticIpRepository
    @Inject
    lateinit var userRepository: UserRepository

    val logger: Logger = LoggerFactory.getLogger("worker")

    init {
        appContext.applicationComponent.inject(this)
    }

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