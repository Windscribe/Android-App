/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.constants.NetworkKeyConstants
import okhttp3.OkHttpClient.Builder
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WindApiFactory @Inject constructor(
    retrofitBuilder: Retrofit.Builder,
    okHttpClient: Builder,
    val protectedApiFactory: ProtectedApiFactory
) {

    private val mRetrofit: Retrofit
    fun createApi(url: String, protect: Boolean = false): ApiService {
        val activeBackend = Windscribe.appContext.vpnController.vpnBackendHolder.activeBackend
        if (protect && activeBackend is WireguardBackend && activeBackend.active) {
            return protectedApiFactory.createApi(url)
        }
        return mRetrofit.newBuilder().baseUrl(url).build().create(ApiService::class.java)
    }

    init {
        mRetrofit = retrofitBuilder.baseUrl(NetworkKeyConstants.API_ENDPOINT)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create()).client(okHttpClient.build()).build()
    }
}
