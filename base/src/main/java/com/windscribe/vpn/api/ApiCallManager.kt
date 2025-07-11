/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import com.windscribe.vpn.api.response.AddEmailResponse
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.AuthToken
import com.windscribe.vpn.api.response.BillingPlanResponse
import com.windscribe.vpn.api.response.ClaimAccountResponse
import com.windscribe.vpn.api.response.ClaimVoucherCodeResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.GetMyIpResponse
import com.windscribe.vpn.api.response.NewsFeedNotification
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.RegToken
import com.windscribe.vpn.api.response.RobertFilterResponse
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.api.response.SsoResponse
import com.windscribe.vpn.api.response.StaticIPResponse
import com.windscribe.vpn.api.response.TicketResponse
import com.windscribe.vpn.api.response.UserLoginResponse
import com.windscribe.vpn.api.response.UserRegistrationResponse
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.VerifyExpressLoginResponse
import com.windscribe.vpn.api.response.WebSession
import com.windscribe.vpn.api.response.WgConnectResponse
import com.windscribe.vpn.api.response.WgInitResponse
import com.windscribe.vpn.api.response.XPressLoginCodeResponse
import com.windscribe.vpn.api.response.XPressLoginVerifyResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.exceptions.WSNetException
import com.windscribe.vpn.exceptions.WindScribeException
import com.wsnet.lib.WSNetServerAPI
import io.reactivex.Single
import io.reactivex.SingleEmitter
import okhttp3.ResponseBody
import org.json.JSONObject
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
open class ApiCallManager @Inject constructor(
    private val apiFactory: ProtectedApiFactory,
    val wsNetServerAPI: WSNetServerAPI,
    val preferencesHelper: PreferencesHelper
) : IApiCallManager {

    private val logger = LoggerFactory.getLogger("basic")
    override fun getWebSession(): Single<GenericResponseClass<WebSession?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.webSession(preferencesHelper.sessionHash) { code, json ->
                buildResponse(sub, code, json, WebSession::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun addUserEmailAddress(email: String): Single<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback =
                wsNetServerAPI.addEmail(preferencesHelper.sessionHash, email) { code, json ->
                    buildResponse(sub, code, json, AddEmailResponse::class.java)
                }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun checkConnectivityAndIpAddress(): Single<GenericResponseClass<GetMyIpResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.myIP { code, json ->
                buildResponse(sub, code, json, GetMyIpResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun claimAccount(
        username: String,
        password: String,
        email: String,
        voucherCode: String?
    ): Single<GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.claimAccount(
                preferencesHelper.sessionHash,
                username,
                password,
                email,
                voucherCode ?: "",
                "1"
            ) { code, json ->
                buildResponse(sub, code, json, ClaimAccountResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getNotifications(pcpID: String?): Single<GenericResponseClass<NewsFeedNotification?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.notifications(
                preferencesHelper.sessionHash, pcpID
                    ?: ""
            ) { code, json ->
                buildResponse(sub, code, json, NewsFeedNotification::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getPortMap(): Single<GenericResponseClass<PortMapResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.portMap(
                preferencesHelper.sessionHash,
                5,
                arrayOf("wstunnel")
            ) { code, json ->
                buildResponse(sub, code, json, PortMapResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getServerConfig(): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback =
                wsNetServerAPI.serverConfigs(preferencesHelper.sessionHash) { code, json ->
                    buildResponse(sub, code, json, String::class.java)
                }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getServerCredentials(extraParams: Map<String, String>?): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.serverCredentials(
                preferencesHelper.sessionHash,
                true
            ) { code, json ->
                buildResponse(sub, code, json, ServerCredentialsResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getServerCredentialsForIKev2(extraParams: Map<String, String>?): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.serverCredentials(
                preferencesHelper.sessionHash,
                false
            ) { code, json ->
                buildResponse(sub, code, json, ServerCredentialsResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getServerList(
        isPro: Boolean,
        locHash: String,
        alcList: Array<String>,
        overriddenCountryCode: String?
    ): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.serverLocations(
                overriddenCountryCode
                    ?: "", locHash, isPro, alcList
            ) { code, json ->
                buildResponse(sub, code, json, String::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getSessionGeneric(firebaseToken: String?): Single<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.session(
                preferencesHelper.sessionHash,
                "",
                firebaseToken ?: ""
            ) { code, json ->
                buildResponse(sub, code, json, UserSessionResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getStaticIpList(deviceID: String?): Single<GenericResponseClass<StaticIPResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback =
                wsNetServerAPI.staticIps(preferencesHelper.sessionHash, 2) { code, json ->
                    buildResponse(sub, code, json, StaticIPResponse::class.java)
                }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun logUserIn(
        username: String,
        password: String,
        twoFa: String?,
        secureToken: String?,
        captchaSolution: String?,
        captchaTrailX: FloatArray,
        captchaTrailY: FloatArray
    ): Single<GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.login(username, password, twoFa ?: "", secureToken ?: "", captchaSolution ?: "", captchaTrailX, captchaTrailY) { code, json ->
                buildResponse(sub, code, json, UserLoginResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun recordAppInstall(): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.recordInstall(false) { code, json ->
                buildResponse(sub, code, json, String::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun sendTicket(
        supportEmail: String,
        supportName: String,
        supportSubject: String,
        supportMessage: String,
        supportCategory: String,
        type: String,
        channel: String
    ): Single<GenericResponseClass<TicketResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.sendSupportTicket(
                supportEmail,
                supportName,
                supportSubject,
                supportMessage,
                supportCategory,
                type,
                channel
            ) { code, json ->
                buildResponse(sub, code, json, TicketResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun signUserIn(
        username: String,
        password: String,
        referringUsername: String?,
        email: String?,
        voucherCode: String?,
        secureToken: String?,
        captchaSolution: String?,
        captchaTrailX: FloatArray,
        captchaTrailY: FloatArray
    ): Single<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.signup(
                username, password, referringUsername ?: "", email
                    ?: "", voucherCode ?: "", secureToken ?: "", captchaSolution ?: "", captchaTrailX, captchaTrailY
            ) { code, json ->
                buildResponse(sub, code, json, UserRegistrationResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun claimVoucherCode(voucherCode: String): Single<GenericResponseClass<ClaimVoucherCodeResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.claimVoucherCode(
                preferencesHelper.sessionHash,
                voucherCode
            ) { code, json ->
                buildResponse(sub, code, json, ClaimVoucherCodeResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun signUpUsingToken(token: String): Single<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.signupUsingToken(token) { code, json ->
                buildResponse(sub, code, json, UserRegistrationResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun verifyPurchaseReceipt(
        purchaseToken: String,
        gpPackageName: String,
        gpProductId: String,
        type: String,
        amazonUserId: String
    ): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.verifyPayment(
                preferencesHelper.sessionHash,
                purchaseToken,
                gpPackageName,
                gpProductId,
                type,
                amazonUserId
            ) { code, json ->
                buildResponse(sub, code, json, GenericSuccess::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun verifyExpressLoginCode(loginCode: String): Single<GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.verifyTvLoginCode(
                preferencesHelper.sessionHash,
                loginCode
            ) { code, json ->
                buildResponse(sub, code, json, VerifyExpressLoginResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun generateXPressLoginCode(): Single<GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.getXpressLoginCode { code, json ->
                buildResponse(sub, code, json, XPressLoginCodeResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun verifyXPressLoginCode(
        loginCode: String,
        signature: String
    ): Single<GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback =
                wsNetServerAPI.verifyXpressLoginCode(loginCode, signature) { code, json ->
                    buildResponse(sub, code, json, XPressLoginVerifyResponse::class.java)
                }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun postDebugLog(
        username: String,
        log: String
    ): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.debugLog(username, log) { code, json ->
                buildResponse(sub, code, json, GenericSuccess::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun postPromoPaymentConfirmation(pcpID: String): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback =
                wsNetServerAPI.postBillingCpid(preferencesHelper.sessionHash, pcpID) { code, json ->
                    buildResponse(sub, code, json, GenericSuccess::class.java)
                }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun updateRobertSettings(
        id: String,
        status: Int
    ): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.setRobertFilter(
                preferencesHelper.sessionHash,
                id,
                status
            ) { code, json ->
                buildResponse(sub, code, json, GenericSuccess::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun syncRobert(): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.syncRobert(preferencesHelper.sessionHash) { code, json ->
                buildResponse(sub, code, json, GenericSuccess::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getRobertFilters(): Single<GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback =
                wsNetServerAPI.getRobertFilters(preferencesHelper.sessionHash) { code, json ->
                    buildResponse(sub, code, json, RobertFilterResponse::class.java)
                }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun deleteSession(): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback =
                wsNetServerAPI.deleteSession(preferencesHelper.sessionHash) { code, json ->
                    buildResponse(sub, code, json, GenericSuccess::class.java)
                }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun wgConnect(
        clientPublicKey: String,
        hostname: String,
        deviceId: String
    ): Single<GenericResponseClass<WgConnectResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.wgConfigsConnect(
                preferencesHelper.sessionHash,
                clientPublicKey,
                hostname,
                deviceId,
                "3600"
            ) { code, json ->
                buildResponse(sub, code, json, WgConnectResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun wgInit(
        clientPublicKey: String,
        deleteOldestKey: Boolean
    ): Single<GenericResponseClass<WgInitResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.wgConfigsInit(
                preferencesHelper.sessionHash,
                clientPublicKey,
                deleteOldestKey
            ) { code, json ->
                buildResponse(sub, code, json, WgInitResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun resendUserEmailAddress(extraParams: Map<String, String>?): Single<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback =
                wsNetServerAPI.confirmEmail(preferencesHelper.sessionHash) { code, json ->
                    buildResponse(sub, code, json, AddEmailResponse::class.java)
                }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun getBillingPlans(promo: String?): Single<GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            if (checkSession(sub)) return@create
            val callback = wsNetServerAPI.mobileBillingPlans(
                preferencesHelper.sessionHash, "google", promo
                    ?: "", 3
            ) { code, json ->
                buildResponse(sub, code, json, BillingPlanResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun sso(
        provider: String,
        token: String
    ): Single<GenericResponseClass<SsoResponse?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.sso(provider, token) { code, json ->
                buildResponse(sub, code, json, SsoResponse::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun authTokenLogin(useAsciiCaptcha: Boolean): Single<GenericResponseClass<AuthToken?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.authTokenLogin(useAsciiCaptcha) { code, json ->
                buildResponse(sub, code, json, AuthToken::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    override fun authTokenSignup(useAsciiCaptcha: Boolean): Single<GenericResponseClass<AuthToken?, ApiErrorResponse?>> {
        return Single.create { sub ->
            val callback = wsNetServerAPI.authTokenSignup(useAsciiCaptcha) { code, json ->
                buildResponse(sub, code, json, AuthToken::class.java)
            }
            sub.setCancellable { callback.cancel() }
        }
    }

    private fun <T> checkSession(sub: SingleEmitter<GenericResponseClass<T?, ApiErrorResponse?>>): Boolean {
        return if (preferencesHelper.sessionHash == null) {
            sub.onError(WindScribeException("User is not logged In."))
            true
        } else {
            false
        }
    }

    private fun <T> buildResponse(
        sub: SingleEmitter<GenericResponseClass<T?, ApiErrorResponse?>>,
        code: Int,
        responseDataString: String,
        modelType: Class<T>
    ) {
        when (code) {
            1 -> sub.onError(WSNetException("WSNet: Network failed to connect to server.", 1))
            2 -> sub.onError(WSNetException("WSNet: No network available to reach API.", 2))
            3 -> sub.onError(
                WSNetException(
                    "WSNet: Server returned incorrect json response. Unable to parse it. Response: $responseDataString",
                    3
                )
            )

            4 -> sub.onError(WSNetException("WSNet: All fallback domains have failed.", 4))
            else -> {
                try {
                    if (modelType.simpleName.equals("String")) {
                        sub.onSuccess(GenericResponseClass(responseDataString as T, null))
                    } else {
                        val dataObject = JsonResponseConverter.getResponseClass(
                            JSONObject(responseDataString),
                            modelType
                        )
                        sub.onSuccess(GenericResponseClass(dataObject, null))
                    }
                } catch (e: Exception) {
                    try {
                        val errorObject =
                            JsonResponseConverter.getErrorClass(JSONObject(responseDataString))
                        sub.onSuccess(GenericResponseClass(null, errorObject))
                    } catch (e: Exception) {
                        sub.onError(
                            WSNetException(
                                "App: Unable to parse [ $responseDataString ] to ${modelType.simpleName}. ) ",
                                3
                            )
                        )
                    }
                }
            }
        }
    }

    override fun sendDecoyTraffic(
        url: String,
        data: String,
        sizeToReceive: String?
    ): Single<GenericResponseClass<String?, ApiErrorResponse?>> {
        try {
            return sizeToReceive?.let {
                return apiFactory.createApi(url)
                    .sendDecoyTraffic(hashMapOf(Pair("data", data)), "text/plain", sizeToReceive)
                    .flatMap {
                        responseToModel(it, String::class.java)
                    }
            }
                ?: apiFactory.createApi(url)
                    .sendDecoyTraffic(hashMapOf(Pair("data", data)), "text/plain").flatMap {
                    responseToModel(it, String::class.java)
                }
        } catch (e: Exception) {
            val apiErrorResponse = ApiErrorResponse()
            apiErrorResponse.errorCode = NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API
            apiErrorResponse.errorMessage = WindError.instance.rxErrorToString(e)
            return Single.just(GenericResponseClass(null, apiErrorResponse))
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
                    JSONObject(responseDataString),
                    modelType
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
}
