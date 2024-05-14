package com.windscribe.vpn.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Proxy
import javax.inject.Inject

class ProtectedApiFactory @Inject constructor(
        private var retrofitBuilder: Retrofit.Builder,
        okHttpClientBuilder: OkHttpClient.Builder
) {
    private var protectedHttpClient: OkHttpClient? = null

    init {
        protectedHttpClient =
            okHttpClientBuilder.proxy(Proxy.NO_PROXY).socketFactory(VPNBypassSocketFactory())
                .build()
    }

    fun createApi(url: String): ApiService {
        protectedHttpClient?.connectionPool?.evictAll()
        protectedHttpClient?.socketFactory?.createSocket()
        return retrofitBuilder
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(protectedHttpClient!!).baseUrl(url)
                .build().create(ApiService::class.java)
    }
}