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
import com.windscribe.vpn.constants.VpnPreferenceConstants.WG_CONNECT_DEFAULT_TTL
import com.windscribe.vpn.exceptions.WindScribeException
import com.wsnet.lib.WSNetBridgeAPI
import com.wsnet.lib.WSNetServerAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume


@Singleton
open class ApiCallManager @Inject constructor(
    val wsNetServerAPI: WSNetServerAPI,
    val preferencesHelper: PreferencesHelper,
    val bridgeAPI: WSNetBridgeAPI
) : IApiCallManager {

    private val logger = LoggerFactory.getLogger("basic")

    override suspend fun getWebSession(): GenericResponseClass<WebSession?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.webSession(preferencesHelper.sessionHash) { code, json ->
                buildResponse(continuation, code, json, WebSession::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun addUserEmailAddress(email: String): GenericResponseClass<AddEmailResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback =
                wsNetServerAPI.addEmail(preferencesHelper.sessionHash, email) { code, json ->
                    buildResponse(continuation, code, json, AddEmailResponse::class.java)
                }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getIp(): GenericResponseClass<String?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback =wsNetServerAPI.pingTest(5000) { code, json ->
                buildResponse(continuation, code, json, String::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun claimAccount(
        username: String,
        password: String,
        email: String,
        voucherCode: String?
    ): GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.claimAccount(
                preferencesHelper.sessionHash,
                username,
                password,
                email,
                voucherCode ?: "",
                "1"
            ) { code, json ->
                buildResponse(continuation, code, json, ClaimAccountResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getNotifications(pcpID: String?): GenericResponseClass<NewsFeedNotification?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.notifications(
                preferencesHelper.sessionHash, pcpID
                    ?: ""
            ) { code, json ->
                buildResponse(continuation, code, json, NewsFeedNotification::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getPortMap(): GenericResponseClass<PortMapResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.portMap(
                preferencesHelper.sessionHash,
                5,
                arrayOf("wstunnel")
            ) { code, json ->
                buildResponse(continuation, code, json, PortMapResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getServerConfig(): GenericResponseClass<String?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback =
                wsNetServerAPI.serverConfigs(preferencesHelper.sessionHash) { code, json ->
                    buildResponse(continuation, code, json, String::class.java)
                }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getServerCredentials(extraParams: Map<String, String>?): GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.serverCredentials(
                preferencesHelper.sessionHash,
                true
            ) { code, json ->
                buildResponse(continuation, code, json, ServerCredentialsResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getServerCredentialsForIKev2(extraParams: Map<String, String>?): GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.serverCredentials(
                preferencesHelper.sessionHash,
                false
            ) { code, json ->
                buildResponse(continuation, code, json, ServerCredentialsResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getServerList(
        isPro: Boolean,
        locHash: String,
        alcList: Array<String>,
        overriddenCountryCode: String?
    ): GenericResponseClass<String?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.serverLocations(
                overriddenCountryCode
                    ?: "", locHash, isPro, alcList
            ) { code, json ->
                buildResponse(continuation, code, json, String::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getSessionGeneric(firebaseToken: String?): GenericResponseClass<UserSessionResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.session(
                preferencesHelper.sessionHash,
                "",
                firebaseToken ?: ""
            ) { code, json ->
                buildResponse(continuation, code, json, UserSessionResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getStaticIpList(deviceID: String?): GenericResponseClass<StaticIPResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback =
                wsNetServerAPI.staticIps(preferencesHelper.sessionHash, 2) { code, json ->
                    buildResponse(continuation, code, json, StaticIPResponse::class.java)
                }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun logUserIn(
        username: String,
        password: String,
        twoFa: String?,
        secureToken: String?,
        captchaSolution: String?,
        captchaTrailX: FloatArray,
        captchaTrailY: FloatArray
    ): GenericResponseClass<UserLoginResponse?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.login(
                username,
                password,
                twoFa ?: "",
                secureToken ?: "",
                captchaSolution ?: "",
                captchaTrailX,
                captchaTrailY
            ) { code, json ->
                buildResponse(continuation, code, json, UserLoginResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun recordAppInstall(): GenericResponseClass<String?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.recordInstall(false) { code, json ->
                buildResponse(continuation, code, json, String::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun sendTicket(
        supportEmail: String,
        supportName: String,
        supportSubject: String,
        supportMessage: String,
        supportCategory: String,
        type: String,
        channel: String
    ): GenericResponseClass<TicketResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.sendSupportTicket(
                supportEmail,
                supportName,
                supportSubject,
                supportMessage,
                supportCategory,
                type,
                channel
            ) { code, json ->
                buildResponse(continuation, code, json, TicketResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun signUserIn(
        username: String,
        password: String,
        referringUsername: String?,
        email: String?,
        voucherCode: String?,
        secureToken: String?,
        captchaSolution: String?,
        captchaTrailX: FloatArray,
        captchaTrailY: FloatArray
    ): GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.signup(
                username,
                password,
                referringUsername ?: "",
                email
                    ?: "",
                voucherCode ?: "",
                secureToken ?: "",
                captchaSolution ?: "",
                captchaTrailX,
                captchaTrailY
            ) { code, json ->
                buildResponse(continuation, code, json, UserRegistrationResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun claimVoucherCode(voucherCode: String): GenericResponseClass<ClaimVoucherCodeResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.claimVoucherCode(
                preferencesHelper.sessionHash,
                voucherCode
            ) { code, json ->
                buildResponse(continuation, code, json, ClaimVoucherCodeResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun signUpUsingToken(token: String): GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.signupUsingToken(token) { code, json ->
                buildResponse(continuation, code, json, UserRegistrationResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun verifyPurchaseReceipt(
        purchaseToken: String,
        gpPackageName: String,
        gpProductId: String,
        type: String,
        amazonUserId: String
    ): GenericResponseClass<GenericSuccess?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.verifyPayment(
                preferencesHelper.sessionHash,
                purchaseToken,
                gpPackageName,
                gpProductId,
                type,
                amazonUserId
            ) { code, json ->
                buildResponse(continuation, code, json, GenericSuccess::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun verifyExpressLoginCode(loginCode: String): GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.verifyTvLoginCode(
                preferencesHelper.sessionHash,
                loginCode
            ) { code, json ->
                buildResponse(continuation, code, json, VerifyExpressLoginResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun generateXPressLoginCode(): GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.getXpressLoginCode { code, json ->
                buildResponse(continuation, code, json, XPressLoginCodeResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun verifyXPressLoginCode(
        loginCode: String,
        signature: String
    ): GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback =
                wsNetServerAPI.verifyXpressLoginCode(loginCode, signature) { code, json ->
                    buildResponse(continuation, code, json, XPressLoginVerifyResponse::class.java)
                }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun postDebugLog(
        username: String,
        log: String
    ): GenericResponseClass<GenericSuccess?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.debugLog(username, log) { code, json ->
                buildResponse(continuation, code, json, GenericSuccess::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun postPromoPaymentConfirmation(pcpID: String): GenericResponseClass<GenericSuccess?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback =
                wsNetServerAPI.postBillingCpid(preferencesHelper.sessionHash, pcpID) { code, json ->
                    buildResponse(continuation, code, json, GenericSuccess::class.java)
                }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun updateRobertSettings(
        id: String,
        status: Int
    ): GenericResponseClass<GenericSuccess?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.setRobertFilter(
                preferencesHelper.sessionHash,
                id,
                status
            ) { code, json ->
                buildResponse(continuation, code, json, GenericSuccess::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun syncRobert(): GenericResponseClass<GenericSuccess?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.syncRobert(preferencesHelper.sessionHash) { code, json ->
                buildResponse(continuation, code, json, GenericSuccess::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getRobertFilters(): GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback =
                wsNetServerAPI.getRobertFilters(preferencesHelper.sessionHash) { code, json ->
                    buildResponse(continuation, code, json, RobertFilterResponse::class.java)
                }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun deleteSession(): GenericResponseClass<GenericSuccess?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback =
                wsNetServerAPI.deleteSession(preferencesHelper.sessionHash) { code, json ->
                    buildResponse(continuation, code, json, GenericSuccess::class.java)
                }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun wgInit(
        clientPublicKey: String,
        deleteOldestKey: Boolean
    ): GenericResponseClass<WgInitResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.wgConfigsInit(
                preferencesHelper.sessionHash,
                clientPublicKey,
                deleteOldestKey
            ) { code, json ->
                buildResponse(continuation, code, json, WgInitResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun resendUserEmailAddress(extraParams: Map<String, String>?): GenericResponseClass<AddEmailResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback =
                wsNetServerAPI.confirmEmail(preferencesHelper.sessionHash) { code, json ->
                    buildResponse(continuation, code, json, AddEmailResponse::class.java)
                }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun getBillingPlans(promo: String?): GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?> {
        checkSession()
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.mobileBillingPlans(
                preferencesHelper.sessionHash, "google", promo
                    ?: "", 3
            ) { code, json ->
                buildResponse(continuation, code, json, BillingPlanResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun sso(
        provider: String,
        token: String
    ): GenericResponseClass<SsoResponse?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.sso(provider, token) { code, json ->
                buildResponse(continuation, code, json, SsoResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun authTokenLogin(useAsciiCaptcha: Boolean): GenericResponseClass<AuthToken?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.authTokenLogin(useAsciiCaptcha) { code, json ->
                buildResponse(continuation, code, json, AuthToken::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun authTokenSignup(useAsciiCaptcha: Boolean): GenericResponseClass<AuthToken?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.authTokenSignup(useAsciiCaptcha) { code, json ->
                buildResponse(continuation, code, json, AuthToken::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun rotateIp(): GenericResponseClass<String?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = bridgeAPI.rotateIp() { code, json ->
                buildResponse(continuation, code, json, String::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun pinIp(ip: String?): GenericResponseClass<String?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = bridgeAPI.pinIp(ip ?: "") { code, json ->
                buildResponse(continuation, code, json, String::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    override suspend fun passwordRecovery(email: String?): GenericResponseClass<GenericSuccess?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.passwordRecovery(email) { code, json ->
                buildResponse(continuation, code, json, GenericSuccess::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }

    private fun checkSession() {
        if (preferencesHelper.sessionHash == null) {
            throw WindScribeException("User is not logged In.")
        }
    }

    private fun <T> buildResponse(
        continuation: kotlin.coroutines.Continuation<GenericResponseClass<T?, ApiErrorResponse?>>,
        code: Int,
        responseDataString: String,
        modelType: Class<T>
    ) {
        when (code) {
            1 -> {
                val apiErrorResponse = ApiErrorResponse()
                apiErrorResponse.errorCode = 1
                apiErrorResponse.errorMessage = "WSNet: Network failed to connect to server."
                continuation.resume(GenericResponseClass(null, apiErrorResponse))
            }

            2 -> {
                val apiErrorResponse = ApiErrorResponse()
                apiErrorResponse.errorCode = 2
                apiErrorResponse.errorMessage = "WSNet: No network available to reach API."
                continuation.resume(GenericResponseClass(null, apiErrorResponse))
            }

            3 -> {
                val apiErrorResponse = ApiErrorResponse()
                apiErrorResponse.errorCode = 3
                apiErrorResponse.errorMessage = "WSNet: Server returned incorrect json response. Unable to parse it. Response: $responseDataString"
                continuation.resume(GenericResponseClass(null, apiErrorResponse))
            }

            4 -> {
                val apiErrorResponse = ApiErrorResponse()
                apiErrorResponse.errorCode = 4
                apiErrorResponse.errorMessage = "WSNet: All fallback domains have failed."
                continuation.resume(GenericResponseClass(null, apiErrorResponse))
            }
            5 -> {
                val apiErrorResponse = ApiErrorResponse()
                apiErrorResponse.errorCode = 5
                apiErrorResponse.errorMessage = "WSNet: Bridge Api failed."
                continuation.resume(GenericResponseClass(null, apiErrorResponse))
            }

            else -> {
                try {
                    if (modelType.simpleName.equals("String")) {
                        continuation.resume(GenericResponseClass(responseDataString as T, null))
                    } else {
                        val dataObject = JsonResponseConverter.getResponseClass(
                            JSONObject(responseDataString),
                            modelType
                        )
                        continuation.resume(GenericResponseClass(dataObject, null))
                    }
                } catch (e: Exception) {
                    try {
                        val errorObject =
                            JsonResponseConverter.getErrorClass(JSONObject(responseDataString))
                        continuation.resume(GenericResponseClass(null, errorObject))
                    } catch (e: Exception) {
                        val apiErrorResponse = ApiErrorResponse()
                        apiErrorResponse.errorCode = 3
                        apiErrorResponse.errorMessage = "App: Unable to parse [ $responseDataString ] to ${modelType.simpleName}."
                        continuation.resume(GenericResponseClass(null, apiErrorResponse))
                    }
                }
            }
        }
    }

    override suspend fun sendDecoyTraffic(
        url: String,
        data: String,
        sizeToReceive: String?
    ): GenericResponseClass<String?, ApiErrorResponse?> {
        return try {
            withContext(Dispatchers.IO) {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val formBody = okhttp3.FormBody.Builder()
                    .add("data", data)
                    .build()

                val requestBuilder = okhttp3.Request.Builder()
                    .url(url)
                    .post(formBody)
                    .header("Content-Type", "text/plain")

                sizeToReceive?.let {
                    requestBuilder.header("X-DECOY-RESPONSE", it)
                }

                val request = requestBuilder.build()
                val response = client.newCall(request).execute()
                val responseBody = response.body

                if (response.isSuccessful && responseBody != null) {
                    val responseString = responseBody.string()
                    responseBody.close()
                    GenericResponseClass(responseString, null)
                } else {
                    val apiErrorResponse = ApiErrorResponse()
                    apiErrorResponse.errorCode = NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API
                    apiErrorResponse.errorMessage = "HTTP ${response.code}: ${response.message}"
                    GenericResponseClass(null, apiErrorResponse)
                }
            }
        } catch (e: Exception) {
            val apiErrorResponse = ApiErrorResponse()
            apiErrorResponse.errorCode = NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API
            apiErrorResponse.errorMessage = e.message ?: ""
            GenericResponseClass(null, apiErrorResponse)
        }
    }
}
