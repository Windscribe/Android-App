/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import okhttp3.ResponseBody
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ApiService {
    @Streaming
    @FormUrlEncoded
    @POST("/")
    suspend fun sendDecoyTraffic(
            @FieldMap params: Map<String, String>?,
            @Header("Content-Type") contentType: String,
            @Header("X-DECOY-RESPONSE") xDecoyResponse: String
    ): ResponseBody

    @Streaming
    @FormUrlEncoded
    @POST("/")
    suspend fun sendDecoyTraffic(
            @FieldMap params: Map<String, String>?, @Header("Content-Type") contentType: String
    ): ResponseBody
}