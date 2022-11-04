/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.*
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.ApiConstants.APP_VERSION
import com.windscribe.vpn.constants.ApiConstants.PLATFORM
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkKeyConstants.API_HOST_ASSET
import com.windscribe.vpn.constants.NetworkKeyConstants.API_HOST_CHECK_IP
import com.windscribe.vpn.constants.NetworkKeyConstants.API_HOST_GENERIC
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.exceptions.WindScribeException
import io.reactivex.Single
import okhttp3.ResponseBody
import org.json.JSONObject
import org.slf4j.LoggerFactory
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCallManager @Inject constructor(
    private val apiFactory: WindApiFactory,
    private val customApiFactory: WindCustomApiFactory,
    private val hashedDomains: List<String>,
    private val authorizationGenerator: AuthorizationGenerator,
    private var accessIps: List<String>?,
    private var primaryApiEndpoints: Map<HostType, String>,
    private var secondaryApiEndpoints: Map<HostType, String>,
    private val domainFailOverManager: DomainFailOverManager
) : IApiCallManager {

    private val logger = LoggerFactory.getLogger("api_call")

    /**
     * Combines authorization params with extra params if auth required
     * @param extraParams extra parameters for http call
     * @param authRequired if true adds authentication params
     * @return params to attach to http call
     */
    private fun createQueryMap(
            extraParams: Map<String, String>? = null,
            authRequired: Boolean = true
    ): Map<String, String> {
        val paramMap = mutableMapOf<String, String>()
        paramMap[PLATFORM] = "android"
        paramMap[APP_VERSION] = WindUtilities.getVersionName()
        if (authRequired) {
            val authMap = authorizationGenerator.create()
            paramMap.putAll(authMap)
        }
        extraParams?.let {
            paramMap.putAll(it)
        }
        return paramMap
    }

    /**
     * Gets access ips to be used as api endpoint as last resort
     * @return Access Ip response , contains List of hosts.
     */
    private fun getAccessIp(accessIpMap: Map<String, String>?): Single<GenericResponseClass<AccessIpResponse?, ApiErrorResponse?>> {
        val params = createQueryMap(accessIpMap, true)
        return (customApiFactory.createCustomCertApi(BuildConfig.API_STATIC_IP_1)
                .getAccessIps(params))
                .onErrorResumeNext {
                    return@onErrorResumeNext (
                            customApiFactory.createCustomCertApi(BuildConfig.API_STATIC_IP_2)
                                    .getAccessIps(params)
                            )
                }
                .flatMap {
                    responseToModel(it, AccessIpResponse::class.java)
                }
    }

    /**
     * Gets ApiServices from custom Api factory
     * @see WindCustomApiFactory
     */
    private fun getApiServicesWithAccessIp(params: Map<String, String>): Single<List<ApiService>> {
        return Single.fromCallable {
            return@fromCallable accessIps?.let {
                listOf(
                        customApiFactory.createCustomCertApi("https://${it[0]}"),
                        customApiFactory.createCustomCertApi("https://${it[1]}")
                )
            } ?: run {
                throw Exception("No Previously Saved access ip available. Now trying Api.")
            }
        }.onErrorResumeNext {
            return@onErrorResumeNext getAccessIp(params)
                    .flatMap { access ->
                        access.dataClass?.let {
                            return@flatMap Single.fromCallable {
                                accessIps = mutableListOf(it.hosts[0],it.hosts[1])
                                appContext.preference.setStaticAccessIp(PreferencesKeyConstants.ACCESS_API_IP_1, it.hosts[0])
                                appContext.preference.setStaticAccessIp(PreferencesKeyConstants.ACCESS_API_IP_2, it.hosts[1])
                                listOf(
                                        customApiFactory.createCustomCertApi("https://${it.hosts[0]}"),
                                        customApiFactory.createCustomCertApi("https://${it.hosts[1]}")
                                )
                            }
                        } ?: kotlin.run {
                            throw Exception("Api returned no access ip.")
                        }
                    }
        }
    }

    /**
     * Maps response body to generic api response
     * @param responseBody Response body
     * @param modelType Class type for data if String is provided raw response is returned .
     * @return Optional Generic Class with either data or ApiErrorResponse
     */
    private fun <T> responseToModel(
            responseBody: ResponseBody,
            modelType: Class<T>
    ): Single<GenericResponseClass<T?, ApiErrorResponse?>> {
        val responseDataString = responseBody.string()
        responseBody.close()
        return Single.fromCallable<GenericResponseClass<T?, ApiErrorResponse?>> {
            if (modelType.simpleName.equals("String")) {
                return@fromCallable (GenericResponseClass(responseDataString as T, null))
            } else {
                val dataObject = JsonResponseConverter.getResponseClass(
                        JSONObject(responseDataString), modelType
                )
                return@fromCallable (GenericResponseClass(dataObject, null))
            }
        }.onErrorResumeNext {
            return@onErrorResumeNext Single.fromCallable {
                val errorObject =
                        JsonResponseConverter.getErrorClass(JSONObject(responseDataString))
                return@fromCallable GenericResponseClass(null, errorObject)
            }
        }
    }

    /**
     * Executes Api calls with multiple urls and returns optional successful
     * response. Based on host type a Main endpoint is selected. Hashed domains
     * list is modified with host type if needed. if any domains throws non-http
     * error next domain is tried. As a last resort app tries to get direct access ips
     * from hardcoded ips.
     * @param extraParams Any extra params except authentication
     * @param authRequired if true authentication params are sent with request
     * @param hostType host type to select endpoint.
     * @param modelType model type for successful data object.
     *@param service suspend function which returns Response body from Api service Interface.
     * @return optional GenericResponseClass<T, ApiErrorResponse>
     */
    fun <T> call(
        extraParams: Map<String, String>? = null,
        authRequired: Boolean = true,
        hostType: HostType = HostType.API,
        modelType: Class<T>,
        protect: Boolean = false,
        apiCallType: ApiCallType = ApiCallType.Other,
        service: (ApiService, Map<String, String>, Boolean) -> Single<ResponseBody>
    ): Single<GenericResponseClass<T?, ApiErrorResponse?>> {
        try {
            val params = createQueryMap(extraParams, authRequired)
            val primaryDomain = primaryApiEndpoints[hostType]
            val secondaryDomain = secondaryApiEndpoints[hostType]
            val backupApiEndPoint = mutableListOf<String>()
            hashedDomains.forEach {
                when (hostType) {
                    HostType.ASSET -> {
                        backupApiEndPoint.add(it.replace(API_HOST_GENERIC, API_HOST_ASSET))
                    }
                    HostType.CHECK_IP -> {
                        backupApiEndPoint.add(it.replace(API_HOST_GENERIC, API_HOST_CHECK_IP))
                    }
                    else -> {
                        backupApiEndPoint.add(it)
                    }
                }
            }
            return callOrSkip(
                apiCallType,
                service,
                DomainType.Primary,
                primaryDomain!!,
                protect,
                params
            )
                .onErrorResumeNext {
                    if (it is HttpException && isErrorBodyValid(it)) {
                        return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                    } else {
                        domainFailOverManager.setDomainBlocked(DomainType.Primary, apiCallType)
                        return@onErrorResumeNext (callOrSkip(
                            apiCallType,
                            service,
                            DomainType.Secondary,
                            secondaryDomain!!,
                            protect,
                            params
                        ))
                    }
                }.onErrorResumeNext {
                    if (it is HttpException && isErrorBodyValid(it)) {
                        return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                        } else {
                            return@onErrorResumeNext (
                                    if (BuildConfig.DEV || BuildConfig.BACKUP_API_ENDPOINT_STRING.isEmpty()) {
                                        throw WindScribeException("Hash domains are disabled.")
                                    } else {
                                        domainFailOverManager.setDomainBlocked(
                                            DomainType.Secondary,
                                            apiCallType
                                        )
                                        callOrSkip(
                                            apiCallType,
                                            service,
                                            DomainType.Hashed1,
                                            backupApiEndPoint[0],
                                            protect,
                                            params
                                        )
                                    }
                                    )
                        }
                    }.onErrorResumeNext {
                        if (it is HttpException && isErrorBodyValid(it)) {
                            return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                        } else {
                            return@onErrorResumeNext (
                                    if (BuildConfig.DEV || BuildConfig.BACKUP_API_ENDPOINT_STRING.isEmpty()) {
                                        throw WindScribeException("Hash domains are disabled.")
                                    } else {
                                        domainFailOverManager.setDomainBlocked(
                                            DomainType.Hashed1,
                                            apiCallType
                                        )
                                        callOrSkip(
                                            apiCallType,
                                            service,
                                            DomainType.Hashed2,
                                            backupApiEndPoint[1],
                                            protect,
                                            params
                                        )
                                    }
                                    )
                        }
                    }.onErrorResumeNext {
                        if (it is HttpException && isErrorBodyValid(it)) {
                            return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                        } else {
                            return@onErrorResumeNext (
                                    if (BuildConfig.DEV || BuildConfig.BACKUP_API_ENDPOINT_STRING.isEmpty()) {
                                        throw WindScribeException("Hash domains are disabled.")
                                    } else {
                                        domainFailOverManager.setDomainBlocked(
                                            DomainType.Hashed2,
                                            apiCallType
                                        )
                                        callOrSkip(
                                            apiCallType,
                                            service,
                                            DomainType.Hashed3,
                                            backupApiEndPoint[2],
                                            protect,
                                            params
                                        )
                                    })
                        }
                    }
                    .onErrorResumeNext {
                        if (it is HttpException && isErrorBodyValid(it)) {
                            return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                        } else {
                            if (BuildConfig.DEV || BuildConfig.API_STATIC_CERT.isEmpty()) {
                                throw WindScribeException("Unsafe http client disabled.")
                            } else {
                                if (domainFailOverManager.isAccessible(
                                        DomainType.DirectIp1,
                                        apiCallType
                                    )
                                ) {
                                    domainFailOverManager.setDomainBlocked(
                                        DomainType.Hashed3,
                                        apiCallType
                                    )
                                    val services = getApiServicesWithAccessIp(params)
                                    return@onErrorResumeNext services.flatMap { apiService ->
                                        return@flatMap service.invoke(apiService[0], params, true)
                                    }
                                } else {
                                    throw WindScribeException("Direct ip domain 1 blocked.")
                                }
                            }
                        }
                    }.onErrorResumeNext {
                        if (it is HttpException && isErrorBodyValid(it)) {
                            return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                        } else {
                            if (BuildConfig.DEV || BuildConfig.API_STATIC_CERT.isEmpty()) {
                                throw WindScribeException("Unsafe http client disabled.")
                            } else {
                                if (domainFailOverManager.isAccessible(
                                        DomainType.DirectIp2,
                                        apiCallType
                                    )
                                ) {
                                    domainFailOverManager.setDomainBlocked(
                                        DomainType.DirectIp1,
                                        apiCallType
                                    )
                                    val services = getApiServicesWithAccessIp(params)
                                    return@onErrorResumeNext services.flatMap { apiService ->
                                        return@flatMap service.invoke(apiService[1], params, true)
                                    }
                                } else {
                                    throw WindScribeException("Direct ip domain 2 blocked.")
                                }
                            }
                        }
                    }.onErrorResumeNext {
                        if (it is HttpException && isErrorBodyValid(it)) {
                            return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                        } else {
                            domainFailOverManager.reset(apiCallType)
                            throw WindScribeException("No more endpoints left to try. Giving up.")
                        }
                    }.flatMap {
                        responseToModel(it, modelType)
                    }
        } catch (e: Exception) {
            val apiErrorResponse = ApiErrorResponse()
            apiErrorResponse.errorCode = NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API
            apiErrorResponse.errorMessage = WindError.instance.rxErrorToString(e)
            return Single.just(GenericResponseClass(null, apiErrorResponse))
        }
    }

    private fun isErrorBodyValid(httpException: HttpException): Boolean {
        return httpException.response()?.errorBody()?.let {
            return httpException.code() < 500
        } ?: false
    }

    private fun callOrSkip(
        apiCallType: ApiCallType,
        service: (ApiService, Map<String, String>, Boolean) -> Single<ResponseBody>,
        domainType: DomainType,
        domain: String,
        protect: Boolean,
        params: Map<String, String>
    ): Single<ResponseBody> {
        return if (domainFailOverManager.isAccessible(domainType, apiCallType)) {
            service.invoke(apiFactory.createApi(domain, protect), params, false)
        } else {
            return Single.error(Throwable())
        }
    }

    override fun getWebSession(extraParams: Map<String, String>?): Single<GenericResponseClass<WebSession?, ApiErrorResponse?>> {
        return call(extraParams, modelType = WebSession::class.java) { apiService, params, _ ->
            apiService.getWebSession(params)
        }
    }

    override fun addUserEmailAddress(extraParams: Map<String, String>?): Single<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = AddEmailResponse::class.java
        ) { apiService, params, _ ->
            apiService.postUserEmailAddress(params)
        }
    }

    override fun checkConnectivityAndIpAddress(extraParams: Map<String, String>?): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return call(
                authRequired = true,
                hostType = HostType.CHECK_IP,
                modelType = String::class.java
        ) { apiService, params,  directIp ->
            if(directIp){
                apiService.connectivityTestAndIpDirectIp()
            }else{
                apiService.connectivityTestAndIp()
            }
        }
    }

    override fun claimAccount(extraParams: Map<String, String>?): Single<GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = ClaimAccountResponse::class.java
        ) { apiService, params, _ ->
            apiService.claimAccount(params)
        }
    }

    override fun getBestLocation(extraParams: Map<String, String>?): Single<GenericResponseClass<BestLocationResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = BestLocationResponse::class.java
        ) { apiService, params, _ ->
            apiService.getBestLocation(params)
        }
    }

    override fun getBillingPlans(extraParams: Map<String, String>?): Single<GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = BillingPlanResponse::class.java
        ) { apiService, params, _ ->
            apiService.getBillingPlans(params)
        }
    }

    override fun getMyIp(extraParams: Map<String, String>?): Single<GenericResponseClass<GetMyIpResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = GetMyIpResponse::class.java
        ) { apiService, params, _ ->
            apiService.getMyIP(params)
        }
    }

    override fun getNotifications(extraParams: Map<String, String>?): Single<GenericResponseClass<NewsFeedNotification?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = NewsFeedNotification::class.java
        ) { apiService, params, _ ->
            apiService.getNotifications(params)
        }
    }

    override fun getPortMap(extraParams: Map<String, String>?): Single<GenericResponseClass<PortMapResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = PortMapResponse::class.java
        ) { apiService, params, _ ->
            apiService.getPortMaps(params, arrayOf("wstunnel"))
        }
    }

    override fun getReg(extraParams: Map<String, String>?): Single<GenericResponseClass<RegToken?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = RegToken::class.java
        ) { apiService, params, _ ->
            apiService.getReg(params)
        }
    }

    override fun getServerConfig(extraParams: Map<String, String>?): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = String::class.java
        ) { apiService, params, _ ->
            apiService.getServerConfig(params)
        }
    }

    override fun getServerCredentials(extraParams: Map<String, String>?): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = ServerCredentialsResponse::class.java
        ) { apiService, params, _ ->
            apiService.getServerCredentials(params)
        }
    }

    override fun getServerCredentialsForIKev2(extraParams: Map<String, String>?): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = ServerCredentialsResponse::class.java
        ) { apiService, params, _ ->
            apiService.getServerCredentialsForIKev2(params)
        }
    }

    override fun getServerList(
            extraParams: Map<String, String>?,
            billingPlan: String?,
            locHash: String?,
            alcList: String?,
            overriddenCountryCode: String?
    ): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return call(
                hostType = HostType.ASSET,
                modelType = String::class.java
        ) { apiService, params, directIp ->
            if (directIp){
                apiService.getServerListDirectIp(billingPlan, locHash, alcList, overriddenCountryCode)
            } else {
                apiService.getServerList(billingPlan, locHash, alcList, overriddenCountryCode)
            }
        }
    }

    override fun getSessionGeneric(extraParams: Map<String, String>?, protect: Boolean): Single<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = UserSessionResponse::class.java,
                protect = protect
        ) { apiService, params, _ ->
            apiService.getSession(params)
        }
    }

    override fun getSessionGeneric(extraParams: Map<String, String>?): Single<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = UserSessionResponse::class.java,
        ) { apiService, params, _ ->
            apiService.getSession(params)
        }
    }

    override fun getSessionGenericInConnectedState(extraParams: Map<String, String>?): Single<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = UserSessionResponse::class.java
        ) { apiService, params, _ ->
            apiService.getSession(params)
        }
    }

    override fun getStaticIpList(extraParams: Map<String, String>?): Single<GenericResponseClass<StaticIPResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = StaticIPResponse::class.java
        ) { apiService, params, _ ->
            apiService.getStaticIPList(params)
        }
    }

    override fun logUserIn(extraParams: Map<String, String>?): Single<GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = UserLoginResponse::class.java
        ) { apiService, params, _ ->
            apiService.userLogin(params)
        }
    }

    override fun recordAppInstall(extraParams: Map<String, String>?): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = String::class.java
        ) { apiService, params, _ ->
            apiService.recordAppInstall(params)
        }
    }

    override fun resendUserEmailAddress(extraParams: Map<String, String>?): Single<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = AddEmailResponse::class.java
        ) { apiService, params, _ ->
            apiService.resendUserEmailAddress(params)
        }
    }

    override fun sendTicket(map: Map<String, String>?): Single<GenericResponseClass<TicketResponse?, ApiErrorResponse?>> {
        return call(
                map,
                modelType = TicketResponse::class.java
        ) { apiService, params, _ ->
            apiService.sendTicket(params)
        }
    }

    override fun signUserIn(extraParams: Map<String, String>?): Single<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = UserRegistrationResponse::class.java
        ) { apiService, params, _ ->
            apiService.userRegistration(params)
        }
    }

    override fun verifyPurchaseReceipt(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = GenericSuccess::class.java
        ) { apiService, params, _ ->
            apiService.verifyPayment(params)
        }
    }

    override fun verifyExpressLoginCode(extraParams: Map<String, String>?): Single<GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = VerifyExpressLoginResponse::class.java
        ) { apiService, params, _ ->
            apiService.verifyExpressLoginCode(params)
        }
    }

    override fun generateXPressLoginCode(
            extraParams: Map<String, String>?
    ): Single<GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = XPressLoginCodeResponse::class.java
        ) { apiService, params, _ ->
            apiService.generateXPressLoginCode(params)
        }
    }

    override fun verifyXPressLoginCode(
            extraParams: Map<String, String>?
    ): Single<GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = XPressLoginVerifyResponse::class.java
        ) { apiService, params, _ ->
            apiService.verifyXPressLoginCode(params)
        }
    }

    override fun postDebugLog(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = GenericSuccess::class.java
        ) { apiService, params, _ ->
            apiService.postAppLog(params)
        }
    }

    override fun postPromoPaymentConfirmation(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = GenericSuccess::class.java
        ) { apiService, params, _ ->
            apiService.postPromoPaymentConfirmation(params)
        }
    }

    override fun updateRobertSettings(
            extraParams: Map<String, String>?
    ): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = GenericSuccess::class.java
        ) { apiService, params, _ ->
            apiService.updateRobertSettings(params)
        }
    }

    override fun syncRobert(
            extraParams: Map<String, String>?
    ): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = GenericSuccess::class.java
        ) { apiService, params, _ ->
            apiService.syncRobert(params)
        }
    }

    override fun getRobertSettings(
            extraParams: Map<String, String>?
    ): Single<GenericResponseClass<RobertSettingsResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = RobertSettingsResponse::class.java
        ) { apiService, params, _ ->
            apiService.getRobertSettings(params)
        }
    }

    override fun getRobertFilters(extraParams: Map<String, String>?): Single<GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = RobertFilterResponse::class.java
        ) { apiService, params, _ ->
            apiService.getRobertFilters(params)
        }
    }

    override fun deleteSession(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(
                extraParams,
                modelType = GenericSuccess::class.java
        ) { apiService, params, _ ->
            apiService.deleteSession(params)
        }
    }

    override fun wgConnect(extraParams: Map<String, String>?, protect: Boolean): Single<GenericResponseClass<WgConnectResponse?, ApiErrorResponse?>> {
        return call(
            extraParams,
            modelType = WgConnectResponse::class.java,
            protect = protect,
            apiCallType = ApiCallType.WgConnect
        ) { apiService, params, _ ->
            apiService.wgConnect(params)
        }
    }

    override fun wgInit(extraParams: Map<String, String>?, protect: Boolean): Single<GenericResponseClass<WgInitResponse?, ApiErrorResponse?>> {
        return call(
            extraParams,
            modelType = WgInitResponse::class.java,
            protect = protect,
            apiCallType = ApiCallType.WgConnect
        ) { apiService, params, _ ->
            apiService.wgInit(params)
        }
    }

    override fun sendDecoyTraffic(url: String, data: String, sizeToReceive: String?): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        try {
            return sizeToReceive?.let {
                return apiFactory.createApi(url).sendDecoyTraffic(hashMapOf(Pair("data", data)), "text/plain", sizeToReceive)
                        .flatMap {
                            responseToModel(it, String::class.java)
                        }
            }
                    ?: apiFactory.createApi(url).sendDecoyTraffic(hashMapOf(Pair("data", data)), "text/plain")
                        .flatMap {
                            responseToModel(it, String::class.java)
                        }
        } catch (e: Exception) {
            val apiErrorResponse = ApiErrorResponse()
            apiErrorResponse.errorCode = NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API
            apiErrorResponse.errorMessage = WindError.instance.rxErrorToString(e)
            return Single.just(GenericResponseClass(null, apiErrorResponse))
        }
    }

    override fun getConnectedIp(): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return apiFactory.createApi("https://checkip.windscribe.com/").connectivityTestAndIp()
            .flatMap {
                 responseToModel(it, String::class.java)
            }
    }
}
