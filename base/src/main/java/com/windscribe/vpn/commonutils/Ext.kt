package com.windscribe.vpn.commonutils

import androidx.work.ListenableWorker
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.decoytraffic.FakeTrafficVolume
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.repository.CallResult
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.coroutines.rx2.await

object Ext {
    suspend fun Completable.result(callback: (successful: Boolean, error: String?) -> Unit): ListenableWorker.Result {
        return try {
            await()
            callback(true, null)
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            callback(false, WindError.instance.rxErrorToString(e))
            ListenableWorker.Result.failure()
        }
    }

    suspend fun <T> Single<*>.result(): CallResult<T> {
        val response = await() as GenericResponseClass<*, *>
        return response.callResult()
    }

    fun getFakeTrafficVolumeOptions(): Array<String> {
        return FakeTrafficVolume.values().map {
            it.name
        }.toTypedArray()
    }
}
