package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.repository.UnblockWgParamsRepository
import com.windscribe.vpn.repository.UserRepository
import javax.inject.Inject

class UnblockWgParamsWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    @Inject
    lateinit var unblockWgParamsRepository: UnblockWgParamsRepository
    @Inject
    lateinit var userRepository: UserRepository

    init {
        Windscribe.appContext.applicationComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        if (!userRepository.loggedIn()) return Result.success()
        val success = unblockWgParamsRepository.update()
        return if (success) {
            Result.success()
        } else {
            Result.failure()
        }
    }
}