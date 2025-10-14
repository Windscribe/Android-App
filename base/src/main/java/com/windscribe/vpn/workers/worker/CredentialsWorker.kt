package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.UserRepository
import org.slf4j.LoggerFactory
import javax.inject.Inject

class CredentialsWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    private val logger = LoggerFactory.getLogger("worker")

    @Inject
    lateinit var connectionDataRepository: ConnectionDataRepository

    @Inject
    lateinit var userRepository: UserRepository

    init {
        Windscribe.appContext.applicationComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        return if (userRepository.loggedIn() && userRepository.accountStatusOkay()) {
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
}