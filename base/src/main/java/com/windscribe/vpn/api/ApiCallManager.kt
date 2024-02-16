/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.AccessIpResponse
import com.windscribe.vpn.api.response.AddEmailResponse
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.BestLocationResponse
import com.windscribe.vpn.api.response.BillingPlanResponse
import com.windscribe.vpn.api.response.ClaimAccountResponse
import com.windscribe.vpn.api.response.DOHTxtRecord
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.GetMyIpResponse
import com.windscribe.vpn.api.response.Latency
import com.windscribe.vpn.api.response.NewsFeedNotification
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.RegToken
import com.windscribe.vpn.api.response.RobertFilterResponse
import com.windscribe.vpn.api.response.RobertSettingsResponse
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.api.response.StaticIPResponse
import com.windscribe.vpn.api.response.TicketResponse
import com.windscribe.vpn.api.response.TxtAnswer
import com.windscribe.vpn.api.response.UserLoginResponse
import com.windscribe.vpn.api.response.UserRegistrationResponse
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.VerifyExpressLoginResponse
import com.windscribe.vpn.api.response.WebSession
import com.windscribe.vpn.api.response.WgConnectResponse
import com.windscribe.vpn.api.response.WgInitResponse
import com.windscribe.vpn.api.response.XPressLoginCodeResponse
import com.windscribe.vpn.api.response.XPressLoginVerifyResponse
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.ApiConstants.APP_VERSION
import com.windscribe.vpn.constants.ApiConstants.PLATFORM
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkKeyConstants
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
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
open class ApiCallManager @Inject constructor(private val apiFactory: WindApiFactory, private val customApiFactory: WindCustomApiFactory, private val hashedDomain: String, private val authorizationGenerator: AuthorizationGenerator, private var accessIps: List<String>?, private var primaryApiEndpoints: Map<HostType, String>, private var secondaryApiEndpoints: Map<HostType, String>, private val domainFailOverManager: DomainFailOverManager) : IApiCallManager {

    private val logger = LoggerFactory.getLogger("api_call")

    /**
     * Combines authorization params with extra params if auth required
     * @param extraParams extra parameters for http call
     * @param authRequired if true adds authentication params
     * @return params to attach to http call
     */
    private fun createQueryMap(extraParams: Map<String, String>? = null, authRequired: Boolean = true): Map<String, String> {
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

    private fun getEchDomain(type: HostType): String {
        return when (type) {
            HostType.API -> "$API_HOST_GENERIC${BuildConfig.ECH_ENDPOINT}"
            HostType.ASSET -> "$API_HOST_ASSET${BuildConfig.ECH_ENDPOINT}"
            HostType.CHECK_IP -> "$API_HOST_CHECK_IP${BuildConfig.ECH_ENDPOINT}"
        }
    }

    /**
     * Gets access ips to be used as api endpoint as last resort
     * @return Access Ip response , contains List of hosts.
     */
    private fun getAccessIp(accessIpMap: Map<String, String>?): Single<GenericResponseClass<AccessIpResponse?, ApiErrorResponse?>> {
        val params = createQueryMap(accessIpMap, true)
        return (customApiFactory.createCustomCertApi(BuildConfig.API_STATIC_IP_1).getAccessIps(params)).onErrorResumeNext {
            return@onErrorResumeNext (customApiFactory.createCustomCertApi(BuildConfig.API_STATIC_IP_2).getAccessIps(params))
        }.flatMap {
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
                listOf(customApiFactory.createCustomCertApi("https://${it[0]}"), customApiFactory.createCustomCertApi("https://${it[1]}"))
            } ?: run {
                throw Exception("No Previously Saved access ip available. Now trying Api.")
            }
        }.onErrorResumeNext {
            return@onErrorResumeNext getAccessIp(params).flatMap { access ->
                access.dataClass?.let {
                    return@flatMap Single.fromCallable {
                        accessIps = mutableListOf(it.hosts[0], it.hosts[1])
                        appContext.preference.setStaticAccessIp(PreferencesKeyConstants.ACCESS_API_IP_1, it.hosts[0])
                        appContext.preference.setStaticAccessIp(PreferencesKeyConstants.ACCESS_API_IP_2, it.hosts[1])
                        listOf(customApiFactory.createCustomCertApi("https://${it.hosts[0]}"), customApiFactory.createCustomCertApi("https://${it.hosts[1]}"))
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
    private fun <T> responseToModel(responseBody: ResponseBody, modelType: Class<T>): Single<GenericResponseClass<T?, ApiErrorResponse?>> {
        val responseDataString = responseBody.string()
        responseBody.close()
        return Single.fromCallable<GenericResponseClass<T?, ApiErrorResponse?>> {
            if (modelType.simpleName.equals("String")) {
                return@fromCallable (GenericResponseClass(responseDataString as T, null))
            } else {
                val dataObject = JsonResponseConverter.getResponseClass(JSONObject(responseDataString), modelType)
                return@fromCallable (GenericResponseClass(dataObject, null))
            }
        }.onErrorResumeNext {
            return@onErrorResumeNext Single.fromCallable {
                val errorObject = JsonResponseConverter.getErrorClass(JSONObject(responseDataString))
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
    fun <T> call(extraParams: Map<String, String>? = null, authRequired: Boolean = true, hostType: HostType = HostType.API, modelType: Class<T>, protect: Boolean = false, apiCallType: ApiCallType = ApiCallType.Other, service: (ApiService, Map<String, String>, Boolean) -> Single<ResponseBody>): Single<GenericResponseClass<T?, ApiErrorResponse?>> {
        try {
            val params = createQueryMap(extraParams, authRequired)
            val primaryDomain = primaryApiEndpoints[hostType]
            val secondaryDomain = secondaryApiEndpoints[hostType]
            val randomHashedDomain = when (hostType) {
                HostType.ASSET -> {
                    hashedDomain.replace(API_HOST_GENERIC, API_HOST_ASSET)
                }

                HostType.CHECK_IP -> {
                    hashedDomain.replace(API_HOST_GENERIC, API_HOST_CHECK_IP)
                }

                else -> {
                    hashedDomain
                }
            }
            return callOrSkip(apiCallType, service, DomainType.Primary, primaryDomain!!, protect, params).onErrorResumeNext {
                if (it is HttpException && isErrorBodyValid(it)) {
                    return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                } else {
                    domainFailOverManager.setDomainBlocked(DomainType.Primary, apiCallType)
                    if (BuildConfig.DEV) {
                        throw WindScribeException("Secondary domains are disabled.")
                    } else {
                        return@onErrorResumeNext (callOrSkip(apiCallType, service, DomainType.Secondary, secondaryDomain!!, protect, params))
                    }
                }
            }.onErrorResumeNext {
                if (it is HttpException && isErrorBodyValid(it)) {
                    return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                } else {
                    lastUsedDynamicEndpoint = null
                    domainFailOverManager.setDomainBlocked(DomainType.Secondary, apiCallType)
                    return@onErrorResumeNext (if (BuildConfig.DEV || BuildConfig.BACKUP_API_ENDPOINT_STRING.isEmpty()) {
                        throw WindScribeException("Hash domains are disabled.")
                    } else {
                        callOrSkip(apiCallType, service, DomainType.Hashed, randomHashedDomain, protect, params)
                    })
                }
            }.onErrorResumeNext {
                if (it is HttpException && isErrorBodyValid(it)) {
                    return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                } else {
                    domainFailOverManager.setDomainBlocked(DomainType.Hashed, apiCallType)
                    return@onErrorResumeNext (if (BuildConfig.DEV || BuildConfig.DYNAMIC_DNS.isEmpty()) {
                        throw WindScribeException("Dynamic doh disabled.")
                    } else {
                        getDynamicDohEndpoint(hostType).flatMap { dynamicEndpoint ->
                            callOrSkip(apiCallType, service, DomainType.DYNAMIC_DOH, dynamicEndpoint, protect, params)
                        }
                    })
                }
            }.onErrorResumeNext {
                if (it is HttpException && isErrorBodyValid(it)) {
                    return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                } else {
                    domainFailOverManager.setDomainBlocked(DomainType.DYNAMIC_DOH, apiCallType)
                    return@onErrorResumeNext (if (BuildConfig.DEV || BuildConfig.ECH_DOMAIN.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        throw WindScribeException("Ech domain disabled.")
                    } else {
                        callOrSkip(apiCallType, service, DomainType.Ech, getEchDomain(hostType), protect, params)
                    })
                }
            }.onErrorResumeNext {
                if (it is HttpException && isErrorBodyValid(it)) {
                    return@onErrorResumeNext Single.fromCallable { it.response()?.errorBody() }
                } else {
                    domainFailOverManager.setDomainBlocked(DomainType.Ech, apiCallType)
                    if (BuildConfig.DEV || BuildConfig.API_STATIC_CERT.isEmpty()) {
                        throw WindScribeException("Unsafe http client disabled.")
                    } else {
                        if (domainFailOverManager.isAccessible(DomainType.DirectIp1, apiCallType)) {
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
                        domainFailOverManager.setDomainBlocked(DomainType.DirectIp1, apiCallType)
                        if (domainFailOverManager.isAccessible(DomainType.DirectIp2, apiCallType)) {
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

    private fun callOrSkip(apiCallType: ApiCallType, service: (ApiService, Map<String, String>, Boolean) -> Single<ResponseBody>, domainType: DomainType, domain: String, protect: Boolean, params: Map<String, String>): Single<ResponseBody> {
        return if (domainFailOverManager.isAccessible(domainType, apiCallType) && !ignoreDomainForRestrictedRegion(domainType, arrayOf(DomainType.Primary, DomainType.Secondary))) {
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
        return call(extraParams, modelType = AddEmailResponse::class.java) { apiService, params, _ ->
            apiService.postUserEmailAddress(params)
        }
    }

    override fun checkConnectivityAndIpAddress(extraParams: Map<String, String>?): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return call(authRequired = true, hostType = HostType.CHECK_IP, modelType = String::class.java) { apiService, _, directIp ->
            if (directIp) {
                apiService.connectivityTestAndIpDirectIp()
            } else {
                apiService.connectivityTestAndIp()
            }
        }
    }

    override fun claimAccount(extraParams: Map<String, String>?): Single<GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = ClaimAccountResponse::class.java) { apiService, params, _ ->
            apiService.claimAccount(params)
        }
    }

    override fun getBestLocation(extraParams: Map<String, String>?): Single<GenericResponseClass<BestLocationResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = BestLocationResponse::class.java) { apiService, params, _ ->
            apiService.getBestLocation(params)
        }
    }

    override fun getBillingPlans(extraParams: Map<String, String>?): Single<GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = BillingPlanResponse::class.java) { apiService, params, _ ->
            apiService.getBillingPlans(params)
        }
    }

    override fun getMyIp(extraParams: Map<String, String>?): Single<GenericResponseClass<GetMyIpResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = GetMyIpResponse::class.java) { apiService, params, _ ->
            apiService.getMyIP(params)
        }
    }

    override fun getNotifications(extraParams: Map<String, String>?): Single<GenericResponseClass<NewsFeedNotification?, ApiErrorResponse?>> {
        return call(extraParams, modelType = NewsFeedNotification::class.java) { apiService, params, _ ->
            apiService.getNotifications(params)
        }
    }

    override fun getPortMap(extraParams: Map<String, String>?): Single<GenericResponseClass<PortMapResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = PortMapResponse::class.java) { apiService, params, _ ->
            apiService.getPortMaps(params, arrayOf("wstunnel"))
        }
    }

    override fun getReg(extraParams: Map<String, String>?): Single<GenericResponseClass<RegToken?, ApiErrorResponse?>> {
        return call(extraParams, modelType = RegToken::class.java) { apiService, params, _ ->
            apiService.getReg(params)
        }
    }

    override fun getServerConfig(extraParams: Map<String, String>?): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return call(extraParams, modelType = String::class.java) { apiService, params, _ ->
            apiService.getServerConfig(params)
        }
    }

    override fun getServerCredentials(extraParams: Map<String, String>?): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = ServerCredentialsResponse::class.java) { apiService, params, _ ->
            apiService.getServerCredentials(params)
        }
    }

    override fun getServerCredentialsForIKev2(extraParams: Map<String, String>?): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = ServerCredentialsResponse::class.java) { apiService, params, _ ->
            apiService.getServerCredentialsForIKev2(params)
        }
    }

    override fun getServerList(extraParams: Map<String, String>?, billingPlan: String?, locHash: String?, alcList: String?, overriddenCountryCode: String?): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return call(hostType = HostType.ASSET, modelType = String::class.java) { apiService, _, directIp ->
            if (directIp) {
                apiService.getServerListDirectIp(billingPlan, locHash, alcList, overriddenCountryCode)
            } else {
                apiService.getServerList(billingPlan, locHash, alcList, overriddenCountryCode)
            }
        }
    }

    override fun getSessionGeneric(extraParams: Map<String, String>?, protect: Boolean): Single<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = UserSessionResponse::class.java, protect = protect) { apiService, params, _ ->
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
        return call(extraParams, modelType = UserSessionResponse::class.java) { apiService, params, _ ->
            apiService.getSession(params)
        }
    }

    override fun getStaticIpList(extraParams: Map<String, String>?): Single<GenericResponseClass<StaticIPResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = StaticIPResponse::class.java) { apiService, params, _ ->
            apiService.getStaticIPList(params)
        }
    }

    override fun logUserIn(extraParams: Map<String, String>?): Single<GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = UserLoginResponse::class.java) { apiService, params, _ ->
            apiService.userLogin(params)
        }
    }

    override fun recordAppInstall(extraParams: Map<String, String>?): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return call(extraParams, modelType = String::class.java) { apiService, params, _ ->
            apiService.recordAppInstall(params)
        }
    }

    override fun resendUserEmailAddress(extraParams: Map<String, String>?): Single<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = AddEmailResponse::class.java) { apiService, params, _ ->
            apiService.resendUserEmailAddress(params)
        }
    }

    override fun sendTicket(map: Map<String, String>?): Single<GenericResponseClass<TicketResponse?, ApiErrorResponse?>> {
        return call(map, modelType = TicketResponse::class.java) { apiService, params, _ ->
            apiService.sendTicket(params)
        }
    }

    override fun signUserIn(extraParams: Map<String, String>?): Single<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = UserRegistrationResponse::class.java) { apiService, params, _ ->
            apiService.userRegistration(params)
        }
    }

    override fun verifyPurchaseReceipt(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(extraParams, modelType = GenericSuccess::class.java) { apiService, params, _ ->
            apiService.verifyPayment(params)
        }
    }

    override fun verifyExpressLoginCode(extraParams: Map<String, String>?): Single<GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = VerifyExpressLoginResponse::class.java) { apiService, params, _ ->
            apiService.verifyExpressLoginCode(params)
        }
    }

    override fun generateXPressLoginCode(extraParams: Map<String, String>?): Single<GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = XPressLoginCodeResponse::class.java) { apiService, params, _ ->
            apiService.generateXPressLoginCode(params)
        }
    }

    override fun verifyXPressLoginCode(extraParams: Map<String, String>?): Single<GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = XPressLoginVerifyResponse::class.java) { apiService, params, _ ->
            apiService.verifyXPressLoginCode(params)
        }
    }

    override fun postDebugLog(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(extraParams, modelType = GenericSuccess::class.java) { apiService, params, _ ->
            apiService.postAppLog(params)
        }
    }

    override fun postPromoPaymentConfirmation(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(extraParams, modelType = GenericSuccess::class.java) { apiService, params, _ ->
            apiService.postPromoPaymentConfirmation(params)
        }
    }

    override fun updateRobertSettings(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(extraParams, modelType = GenericSuccess::class.java) { apiService, params, _ ->
            apiService.updateRobertSettings(params)
        }
    }

    override fun syncRobert(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(extraParams, modelType = GenericSuccess::class.java) { apiService, params, _ ->
            apiService.syncRobert(params)
        }
    }

    override fun getRobertSettings(extraParams: Map<String, String>?): Single<GenericResponseClass<RobertSettingsResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = RobertSettingsResponse::class.java) { apiService, params, _ ->
            apiService.getRobertSettings(params)
        }
    }

    override fun getRobertFilters(extraParams: Map<String, String>?): Single<GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = RobertFilterResponse::class.java) { apiService, params, _ ->
            apiService.getRobertFilters(params)
        }
    }

    override fun deleteSession(extraParams: Map<String, String>?): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return call(extraParams, modelType = GenericSuccess::class.java) { apiService, params, _ ->
            apiService.deleteSession(params)
        }
    }

    override fun wgConnect(extraParams: Map<String, String>?, protect: Boolean): Single<GenericResponseClass<WgConnectResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = WgConnectResponse::class.java, protect = protect, apiCallType = ApiCallType.WgConnect) { apiService, params, _ ->
            apiService.wgConnect(params)
        }
    }

    override fun wgInit(extraParams: Map<String, String>?, protect: Boolean): Single<GenericResponseClass<WgInitResponse?, ApiErrorResponse?>> {
        return call(extraParams, modelType = WgInitResponse::class.java, protect = protect, apiCallType = ApiCallType.WgConnect) { apiService, params, _ ->
            apiService.wgInit(params)
        }
    }

    override fun sendDecoyTraffic(url: String, data: String, sizeToReceive: String?): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        try {
            return sizeToReceive?.let {
                return apiFactory.createApi(url).sendDecoyTraffic(hashMapOf(Pair("data", data)), "text/plain", sizeToReceive).flatMap {
                    responseToModel(it, String::class.java)
                }
            }
                    ?: apiFactory.createApi(url).sendDecoyTraffic(hashMapOf(Pair("data", data)), "text/plain").flatMap {
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
        return apiFactory.createApi(BuildConfig.CHECK_IP_URL).connectivityTestAndIp().flatMap {
            responseToModel(it, String::class.java)
        }
    }

    private var lastUsedDynamicEndpoint: String? = null
    private fun getDynamicDohEndpoint(hostType: HostType): Single<String> {
        if (lastUsedDynamicEndpoint != null) {
            return Single.fromCallable { "${hostType.text}$lastUsedDynamicEndpoint" }
        }
        val queryMap = mutableMapOf<String, String>()
        queryMap["name"] = BuildConfig.DYNAMIC_DNS
        queryMap["type"] = "TXT"
        return apiFactory.createApi(NetworkKeyConstants.CLOUDFLARE_DOH).getCloudflareTxtRecord(queryMap).onErrorResumeNext {
            logger.info("Using google doh resolver")
            return@onErrorResumeNext apiFactory.createApi(NetworkKeyConstants.GOOGLE_DOH).getGoogleDOHTxtRecord(queryMap)
        }.flatMap {
            try {
                val response = it.string()
                return@flatMap Single.fromCallable {
                    return@fromCallable Gson().fromJson<DOHTxtRecord?>(response, DOHTxtRecord::class.java).answer.first<TxtAnswer>()
                }
            } catch (e: JsonSyntaxException) {
                throw e
            }
        }.flatMap {
            try {
                val endpoint = it.data.replace("\"", "")
                lastUsedDynamicEndpoint = endpoint
                return@flatMap Single.fromCallable { "${hostType.text}$endpoint" }
            } catch (e: JsonSyntaxException) {
                throw WindScribeException("Doh endpoint returned unknown data.")
            }
        }
    }

    override suspend fun getLatency(url: String, ip: String): Result<GenericResponseClass<Latency?, ApiErrorResponse?>> {
        return kotlin.runCatching {
            return@runCatching apiFactory.createApi("$url/", ip = ip).getLatency().map()
        }
    }

    fun <T> Response<T>.map(): GenericResponseClass<T?, ApiErrorResponse?> {
        body()?.let {
            return GenericResponseClass(it, null)
        } ?: errorBody()?.let {
            return GenericResponseClass(null, Gson().fromJson(it.string(), ApiErrorResponse::class.java))
        } ?: kotlin.run {
            return GenericResponseClass(null, null)
        }
    }

    /**
     * returns true if given domain matchesTheIngore list
     */
    private fun ignoreDomainForRestrictedRegion(domainToTry: DomainType, domainTypeToIgnore: Array<DomainType>): Boolean {
        return appContext.isRegionRestricted && domainTypeToIgnore.contains(domainToTry)
    }
}
