package com.windscribe.vpn.commonutils

import androidx.work.ListenableWorker
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.constants.NetworkErrorCodes
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
        return try {
            val response = await() as GenericResponseClass<*, *>
            response.callResult()
        } catch (e: Exception){
            CallResult.Error(NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API, WindError.instance.rxErrorToString(e))
        }
    }

    fun getFakeTrafficVolumeOptions(): Array<String> {
        return FakeTrafficVolume.values().map {
            it.name
        }.toTypedArray()
    }
}
