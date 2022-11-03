/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @PUT("/Users")
    fun claimAccount(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/")
    fun connectivityTestAndIp(): Single<ResponseBody>

    @GET("/checkip")
    fun connectivityTestAndIpDirectIp(): Single<ResponseBody>

    @GET("/ApiAccessIps")
    fun getAccessIps(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/BestLocation")
    fun getBestLocation(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/MobileBillingPlans?mobile_plan_type=google&version=3")
    fun getBillingPlans(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/MyIp")
    fun getMyIP(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/Notifications")
    fun getNotifications(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    // @GET("/PortMap?version=5&country_code=RU")
    @GET("/PortMap?version=5")
    fun getPortMaps(
        @QueryMap params: Map<String, String>?,
        @Query("force_protocols[]") forceProtocols: Array<String>
    ): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/RegToken")
    fun getReg(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/ServerConfigs?cipher=gcm")
    fun getServerConfig(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/ServerCredentials?type=openvpn")
    fun getServerCredentials(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/ServerCredentials?type=ikev2")
    fun getServerCredentialsForIKev2(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/serverlist/mob-v2/{plan}/{loc_rev}")
    fun getServerList(
            @Path("plan") billing_plan: String?,
            @Path("loc_rev") locRev: String?,
            @Query("alc") alcList: String?,
            @Query("country_override") country_override: String?
    ): Single<ResponseBody>

    @GET("/assets/serverlist/mob-v2/{plan}/{loc_rev}")
    fun getServerListDirectIp(
            @Path("plan") billing_plan: String?,
            @Path("loc_rev") locRev: String?,
            @Query("alc") alcList: String?,
            @Query("country_override") country_override: String?
    ): Single<ResponseBody>

    // Deprecated, exception with direct IP
    @GET("/ServerLocations")
    fun getServerLocations(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/Session")
    fun getSession(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/StaticIps?os=android")
    fun getStaticIPList(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/Report/applog?type=android")
    fun postAppLog(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @PUT("/Users")
    fun postUserEmailAddress(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    // Record new install
    @FormUrlEncoded
    @POST("/RecordInstall/mobile/android")
    fun recordAppInstall(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @PUT("/Users?resend_confirmation=1")
    fun resendUserEmailAddress(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/SupportTicket")
    fun sendTicket(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/Session")
    fun userLogin(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/Users")
    fun userRegistration(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @POST("/AndroidIPN")
    @FormUrlEncoded
    fun verifyPayment(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @PUT("/XpressLogin")
    fun verifyExpressLoginCode(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/XpressLogin")
    fun generateXPressLoginCode(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/XpressLogin")
    fun verifyXPressLoginCode(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/WebSession")
    fun getWebSession(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/BillingCpid")
    fun postPromoPaymentConfirmation(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/Robert/settings")
    fun getRobertSettings(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @GET("/Robert/filters")
    fun getRobertFilters(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @PUT("/Robert/filter")
    fun updateRobertSettings(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/Robert/syncrobert")
    fun syncRobert(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @DELETE("/Session")
    fun deleteSession(@QueryMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/WgConfigs/connect")
    fun wgConnect(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @FormUrlEncoded
    @POST("/WgConfigs/init")
    fun wgInit(@FieldMap params: Map<String, String>?): Single<ResponseBody>

    @Streaming
    @FormUrlEncoded
    @POST("/")
    fun sendDecoyTraffic(@FieldMap params: Map<String, String>?, @Header("Content-Type") contentType: String, @Header("X-DECOY-RESPONSE") xDecoyResponse: String ): Single<ResponseBody>

    @Streaming
    @FormUrlEncoded
    @POST("/")
    fun sendDecoyTraffic(@FieldMap params: Map<String, String>?, @Header("Content-Type") contentType: String): Single<ResponseBody>
}
