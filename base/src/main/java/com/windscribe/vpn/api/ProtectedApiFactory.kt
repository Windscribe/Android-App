package com.windscribe.vpn.api

import java.net.Proxy
import javax.inject.Inject
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class ProtectedApiFactory @Inject constructor(var retrofitBuilder: Retrofit.Builder, windscribeDnsResolver: WindscribeDnsResolver) {
    private var protectedHttpClient: OkHttpClient? = null

    init {
        protectedHttpClient = OkHttpClient.Builder()
                .dns(windscribeDnsResolver)
                .proxy(Proxy.NO_PROXY)
                .socketFactory(CustomSocketFactory())
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