/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.constants.NetworkKeyConstants
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient.Builder
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@Singleton
class WindApiFactory @Inject constructor(retrofitBuilder: Retrofit.Builder, okHttpClient: Builder, val protectedApiFactory: ProtectedApiFactory, windscribeDnsResolver: WindscribeDnsResolver) {

    private val mRetrofit: Retrofit
    fun createApi(url: String, protect: Boolean = false): ApiService {
        val activeBackend = Windscribe.appContext.vpnController.vpnBackendHolder.activeBackend
        if (protect && activeBackend is WireguardBackend && activeBackend.active) {
            return protectedApiFactory.createApi(url)
        }
        return mRetrofit.newBuilder().baseUrl(url)
                .build().create(ApiService::class.java)
    }

    init {
        okHttpClient.connectTimeout(NetworkKeyConstants.NETWORK_REQUEST_CONNECTION_TIMEOUT, SECONDS)
        okHttpClient.readTimeout(5, SECONDS)
        okHttpClient.writeTimeout(5, SECONDS)
        val connectionPool = ConnectionPool(0, 5, MINUTES)
        okHttpClient.connectionPool(connectionPool)
        okHttpClient.dns(windscribeDnsResolver)
        mRetrofit = retrofitBuilder.baseUrl(NetworkKeyConstants.API_ENDPOINT)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient.build())
                .build()
    }
}
