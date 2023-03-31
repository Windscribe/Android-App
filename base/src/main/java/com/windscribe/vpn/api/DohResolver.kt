package com.windscribe.vpn.api

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.windscribe.vpn.api.response.DOHTxtRecord
import com.windscribe.vpn.api.response.TxtAnswer
import com.windscribe.vpn.constants.NetworkKeyConstants
import io.reactivex.Single
import kotlinx.coroutines.rx2.await

class DohResolver(private val apiFactory: EchApiFactory) {

    private val cache: HashMap<String, TxtAnswer> = HashMap()
    fun getTxtAnswerAsync(hostname: String, googleAsBackupResolver: Boolean): Single<TxtAnswer> {
        if (cache.containsKey(hostname)) {
            return Single.just(cache[hostname])
        }
        val queryMap = mutableMapOf<String, String>()
        queryMap["name"] = hostname
        queryMap["type"] = "TXT"
        return apiFactory.createApi(NetworkKeyConstants.CLOUDFLARE_DOH)
            .getCloudflareTxtRecord(queryMap).onErrorResumeNext {
            if (googleAsBackupResolver) {
                return@onErrorResumeNext apiFactory.createApi(NetworkKeyConstants.GOOGLE_DOH)
                    .getGoogleDOHTxtRecord(queryMap)
            } else {
                throw it
            }
        }.flatMap {
            try {
                val response = it.string()
                return@flatMap Single.fromCallable {
                    val answer = Gson().fromJson(response, DOHTxtRecord::class.java).answer.first()
                    cache[hostname] = answer
                    return@fromCallable answer
                }
            } catch (e: JsonSyntaxException) {
                throw e
            }
        }
    }

    suspend fun getTxtAnswer(
        hostname: String,
        googleAsBackupResolver: Boolean = false
    ): TxtAnswer? {
        return try {
            getTxtAnswerAsync(hostname, googleAsBackupResolver).await()
        } catch (e: Exception) {
            null
        }
    }
}