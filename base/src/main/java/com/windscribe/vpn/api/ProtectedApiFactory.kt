package com.windscribe.vpn.api

import com.windscribe.vpn.constants.NetworkKeyConstants
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

class ProtectedApiFactory @Inject constructor(
        private val retrofitBuilder: Retrofit.Builder,
        okHttpClient: OkHttpClient.Builder
) {
    private val mRetrofit: Retrofit
    fun createApi(url: String): ApiService {
        return mRetrofit.newBuilder().baseUrl(url)
            .build().create(ApiService::class.java)
    }

    init {
        okHttpClient.connectTimeout(NetworkKeyConstants.NETWORK_REQUEST_CONNECTION_TIMEOUT, SECONDS)
        okHttpClient.readTimeout(5, SECONDS)
        okHttpClient.writeTimeout(5, SECONDS)
        val connectionPool = ConnectionPool(0, 5, MINUTES)
        okHttpClient.connectionPool(connectionPool)
        mRetrofit = retrofitBuilder.baseUrl("https://api.windscribe.com")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient.build())
            .build()
    }
}