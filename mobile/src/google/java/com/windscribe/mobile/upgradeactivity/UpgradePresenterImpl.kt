/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.upgradeactivity

import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.gson.Gson
import com.windscribe.mobile.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.BillingPlanResponse
import com.windscribe.vpn.api.response.BillingPlanResponse.BillingPlans
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.WebSession
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.billing.AmazonProducts
import com.windscribe.vpn.billing.AmazonPurchase
import com.windscribe.vpn.billing.GoogleProducts
import com.windscribe.vpn.billing.PurchaseState
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.RegionLocator
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.constants.BillingConstants.AMAZON_PURCHASED_ITEM
import com.windscribe.vpn.constants.BillingConstants.PLAY_STORE_UPDATE
import com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM
import com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM_NULL
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.exceptions.GenericApiException
import com.windscribe.vpn.exceptions.InvalidSessionException
import com.windscribe.vpn.exceptions.UnknownException
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.ReceiptValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import javax.inject.Inject

class UpgradePresenterImpl @Inject constructor(
    private var upgradeView: UpgradeView?,
    private val activityScope: CoroutineScope,
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    private val userRepository: UserRepository,
    private val receiptValidator: ReceiptValidator,
    private val connectionDataRepository: ConnectionDataRepository,
    private val serverListRepository: ServerListRepository
) : UpgradePresenter {
    private var mPurchase: Purchase? = null
    private var mPushNotificationAction: PushNotificationAction? = null
    private var mobileBillingPlans: List<BillingPlans> = ArrayList()
    private var overriddenPlans: BillingPlanResponse.OverriddenPlans? = null
    private val presenterLog = LoggerFactory.getLogger("billing")

    private suspend fun getUserSessionData(): UserSessionResponse {
        val result = result<UserSessionResponse> {
            apiCallManager.getSessionGeneric(null)
        }
        when (result) {
            is CallResult.Error -> {
                when (result.code) {
                    NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA -> {
                        throw UnknownException("Unknown exception")
                    }

                    NetworkErrorCodes.ERROR_RESPONSE_SESSION_INVALID -> {
                        throw InvalidSessionException("Session request Success: Invalid session.")
                    }

                    else -> {
                        throw GenericApiException(result.errorMessage)
                    }
                }
            }

            is CallResult.Success -> return result.data
        }
    }

    override fun onDestroy() {
        presenterLog.info("Stopping billing connection...")
        if (mPurchase != null) {
            presenterLog.info("Starting purchase verification service...")
            receiptValidator.checkPendingAccountUpgrades()
        }
        upgradeView = null
    }

    override fun checkBillingProcessStatus() {
        if (upgradeView?.isBillingProcessFinished() == true) {
            upgradeView?.setBillingProcessStatus(false)
        }
    }

    private fun handleAmazonReceipt(receipt: Receipt, userData: UserData) {
        receipt.productType?.let {
            when (it) {
                ProductType.ENTITLED -> {}
                ProductType.CONSUMABLE, ProductType.SUBSCRIPTION -> handleAmazonPurchase(
                    receipt,
                    userData
                )
            }
        }
    }

    private fun handleAmazonPurchase(receipt: Receipt, userData: UserData) {
        upgradeView?.showProgressBar("Verifying purchase.")
        if (!receipt.isCanceled) {
            val amazonPurchase = AmazonPurchase(receipt.receiptId, userData.userId)
            saveAmazonSubscriptionRecord(amazonPurchase)
            try {
                verifyAmazonReceipt(amazonPurchase)
            } catch (_: Exception) {
                presenterLog.debug("Error saving fulfilling amazon order.")
                upgradeView?.showBillingError("Error saving fulfilling amazon order.")
            }
        } else {
            presenterLog.debug("Subscription/Consumable with receipt is already cancelled.")
            upgradeView?.showBillingError("Receipt cancelled already.")
        }
    }

    private fun launchPurchaseFlowWithAccountID(productDetailsParams: List<BillingFlowParams.ProductDetailsParams>) {
        activityScope.launch(Dispatchers.IO) {
            try {
                val userSessionResponse = getUserSessionData()
                val userID = userSessionResponse.userID.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(userID)
                val accountID = String(digest)
                withContext(Dispatchers.Main) {
                    presenterLog.info("Generated encrypted account ID.")
                    upgradeView?.startPurchaseFlow(productDetailsParams, accountID)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    presenterLog.info("Failed to generate encrypted account ID.")
                    upgradeView?.startPurchaseFlow(productDetailsParams, null)
                }
            }
        }
    }

    override fun onAmazonPurchaseHistoryError(error: String) {
        upgradeView?.hideProgressBar()
        upgradeView?.showBillingError(error)
    }

    override fun onAmazonPurchaseHistorySuccess(amazonPurchases: List<AmazonPurchase>) {
        verifyAmazonReceipt(amazonPurchases[0])
    }

    override fun onBillingSetupFailed(errorCode: Int) {
        val errorMessage = getBillingErrorMessage(errorCode)
        upgradeView?.showBillingError(errorMessage)
    }

    override fun onBillingSetupSuccessful() {
        presenterLog.info("Getting billing plans...")
        activityScope.launch(Dispatchers.IO) {
            try {
                val promoCode = mPushNotificationAction?.promoCode ?: ""
                val result = result<BillingPlanResponse> {
                    apiCallManager.getBillingPlans(promoCode)
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Error -> {
                            val log = String.format("Billing response error: %s", result.errorMessage)
                            presenterLog.debug(log)
                            upgradeView?.showBillingError(result.errorMessage)
                        }

                        is CallResult.Success -> {
                            onBillingResponse(result.data)
                        }
                    }
                }
            } catch (throwable: Throwable) {
                withContext(Dispatchers.Main) {
                    presenterLog.debug(
                        "Failed to get the billing plans... proceeding with default plans" + instance
                            .convertThrowableToString(throwable)
                    )
                    upgradeView?.showBillingError("Failed to get billing plans check your network connection.")
                }
            }
        }
    }

    override fun onConsumeFailed(responseCode: Int, purchase: Purchase) {
        presenterLog.debug(
            "Failed to consume the purchased product. If product token is [null] then play billing did not return the purchased item. " +
                    "User will be asked to contact support. [Product Token]: " + purchase.packageName + "-" +
                    purchase.purchaseToken
        )
        presenterLog.info("Saving purchased product for later update...")
        preferencesHelper.purchasedItem = purchase.originalJson
        onBillingSetupFailed(responseCode)
    }

    override fun onContinuePlanClick(selectedSku: Product) {
        upgradeView?.startPurchaseFlow(selectedSku)
    }

    override fun buyGoogleProduct(productDetailsParams: List<BillingFlowParams.ProductDetailsParams>?) {
        if (productDetailsParams != null) {
            presenterLog.info("Starting purchase flow...")
            launchPurchaseFlowWithAccountID(productDetailsParams)
        } else {
            presenterLog.debug("sku returned null! This should not happen... Notify user to retry...")
            upgradeView?.showToast(
                appContext.resources.getString(com.windscribe.vpn.R.string.unable_to_process_request)
            )
        }
    }

    override fun onProductDataResponse(products: Map<String, Product>) {
        upgradeView?.hideProgressBar()
        upgradeView?.setupPlans(AmazonProducts(products, mobileBillingPlans, mPushNotificationAction))
    }

    override fun onProductResponseFailure() {
        presenterLog.debug("Unable query product for your account.")
    }

    override fun onPurchaseConsumed(itemPurchased: Purchase) {
        mPurchase = itemPurchased
        presenterLog.info("Saving purchased item to process later...")
        upgradeView?.showProgressBar("#Verifying purchase...")
        preferencesHelper.purchasedItem = itemPurchased.originalJson
        presenterLog.info("Verifying payment for purchased item: " + itemPurchased.originalJson)

        activityScope.launch(Dispatchers.IO) {
            try {
                val result = result<GenericSuccess> {
                    apiCallManager.verifyPurchaseReceipt(
                        itemPurchased.purchaseToken,
                        itemPurchased.packageName,
                        itemPurchased.products[0],
                        "",
                        ""
                    )
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Error -> showBillingError(
                            result.code,
                            result.errorMessage
                        )

                        is CallResult.Success -> {
                            presenterLog.info("Payment verification successful. ")
                            preferencesHelper.purchasedItem = null
                            presenterLog.info("Setting item purchased to null & upgrading user account")
                            mPurchase = null
                            upgradeUserAccount()
                            setPurchaseFlowState(PurchaseState.FINISHED)
                        }
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    presenterLog.debug(
                        "Payment verification failed. " + instance.convertThrowableToString(e)
                    )
                    upgradeView?.showBillingError("Payment verification failed!")
                }
            }
        }
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        val requestId = response.requestId.toString()
        val userId = response.userData.userId
        val status = response.requestStatus
        presenterLog.debug(
            String.format(
                "OnPurchaseResponse: requestId:%s userId:%s status:%s",
                requestId,
                userId,
                status
            )
        )
        upgradeView?.showProgressBar("Purchase successful")
        val receipt = response.receipt
        presenterLog.debug(receipt.toJSON().toString())
        handleAmazonReceipt(receipt, response.userData)
    }

    override fun onPurchaseResponseFailure(requestStatus: PurchaseResponse.RequestStatus) {
        when (requestStatus) {
            PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                presenterLog.debug("onPurchaseResponse: already purchased, running verify service.")
                receiptValidator.checkPendingAccountUpgrades()
                upgradeView?.goBackToMainActivity()
            }

            PurchaseResponse.RequestStatus.INVALID_SKU -> {
                presenterLog.debug("onPurchaseResponse: invalid SKU!.")
                upgradeView?.goBackToMainActivity()
            }

            PurchaseResponse.RequestStatus.FAILED, PurchaseResponse.RequestStatus.NOT_SUPPORTED -> {
                presenterLog.debug("onPurchaseResponse: failed to complete purchase")
                upgradeView?.goBackToMainActivity()
            }

            else -> {}
        }
    }

    override fun onPurchaseUpdated(responseCode: Int, purchases: List<Purchase>?) {
        when (responseCode) {
            BillingResponseCode.USER_CANCELED -> {
                setPurchaseFlowState(PurchaseState.FINISHED)
                presenterLog.info("User cancelled the purchase...")
                upgradeView?.showToast(
                    appContext.resources.getString(com.windscribe.vpn.R.string.purchase_cancelled)
                )
                upgradeView?.onPurchaseCancelled()
            }

            BillingResponseCode.OK -> {
                presenterLog.info("Purchase successful...Need to consume the product...")
                presenterLog.info(purchases?.toString() ?: "Purchase not available")
                upgradeView?.onPurchaseSuccessful(purchases)
            }

            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                presenterLog.debug("Item already owned by user: Running verify Purchase service.")
                receiptValidator.checkPendingAccountUpgrades()
            }

            else -> {
                setPurchaseFlowState(PurchaseState.FINISHED)
                presenterLog.debug(
                    "Showing dialog for error. Purchase failed with response code: " + responseCode +
                            " Error Message: " + getBillingErrorMessage(responseCode)
                )
                onBillingSetupFailed(responseCode)
            }
        }
    }

    override fun onSkuDetailsReceived(responseCode: Int, productDetails: List<ProductDetails>) {
        if (upgradeView == null) {
            return
        }
        if (responseCode == BillingResponseCode.OK && productDetails.isNotEmpty()) {
            upgradeView?.hideProgressBar()
            upgradeView?.setupPlans(GoogleProducts(productDetails, mobileBillingPlans, mPushNotificationAction))
        } else if (productDetails.isEmpty()) {
            presenterLog.debug("Failed to find requested products from the store.")
            upgradeView?.showBillingError("Promo is not valid anymore.")
        } else {
            val errorMessage = getBillingErrorMessage(responseCode)
            presenterLog.debug(
                "Error while retrieving sku details from play billing. Error Code: " + responseCode +
                        " Message: " + errorMessage
            )
            upgradeView?.showBillingError(errorMessage)
        }
    }

    override fun restorePurchase() {
        upgradeView?.showProgressBar("Loading user data...")
        upgradeView?.restorePurchase()
    }

    override fun setPurchaseFlowState(state: PurchaseState) {
        preferencesHelper.savePurchaseFlowState(state.name)
        presenterLog.debug(
            "Purchase flow: state changed To: " + preferencesHelper.purchaseFlowState
        )
    }

    override fun setPushNotificationAction(pushNotificationAction: PushNotificationAction) {
        presenterLog.debug(pushNotificationAction.toString())
        mPushNotificationAction = pushNotificationAction
    }

    private fun billingResponseToSkuList(billingPlanResponse: BillingPlanResponse): List<String> {
        val inAppSkuList: MutableList<String> = ArrayList()
        if (billingPlanResponse.plansList.isNotEmpty()) {
            mobileBillingPlans = billingPlanResponse.plansList
            overriddenPlans = billingPlanResponse.overriddenPlans
            presenterLog.debug("Getting in app skus from billing plan...")
            for (billingPlan in mobileBillingPlans) {
                presenterLog.debug("Billing plan: {}", billingPlan)
                inAppSkuList.add(billingPlan.extId)
            }
        }
        return inAppSkuList
    }

    private fun getBillingErrorMessage(responseCode: Int): String {
        when (responseCode) {
            BillingResponseCode.BILLING_UNAVAILABLE -> {
                presenterLog.debug("Billing unavailable for the device. Response code: $responseCode")
                return appContext
                    .resources.getString(com.windscribe.vpn.R.string.billing_unavailable)
            }

            BillingResponseCode.ITEM_UNAVAILABLE -> {
                presenterLog.debug("Item user requested is not available. Response code: $responseCode")
                return appContext
                    .resources.getString(com.windscribe.vpn.R.string.item_unavailable)
            }

            BillingResponseCode.SERVICE_UNAVAILABLE -> {
                presenterLog.debug(
                    "Billing service unavailable, user may not be connected to a network. Response Code: " +
                            responseCode
                )
                return appContext
                    .resources.getString(com.windscribe.vpn.R.string.billing_service_unavailable)
            }

            BillingResponseCode.ERROR -> {
                presenterLog.info(
                    "Fatal error during api call, user most likely lost network connection during the process or pressed the " +
                            "button while not connected to internet. Response Code: " + responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.play_store_generic_api_error)
            }

            BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                presenterLog.debug(
                    "Requested feature is not supported by Play Store on the current device." +
                            "Response Code: " + responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.fatal_error)
            }

            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                presenterLog.debug(
                    "Item already owned. Unknown error will be shown to user... Response code: " +
                            responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
            }

            BillingResponseCode.ITEM_NOT_OWNED -> {
                presenterLog.debug(
                    "Item not owned. Unknown error will be shown to user... Response code: " +
                            responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
            }

            BillingResponseCode.DEVELOPER_ERROR -> {
                presenterLog.debug(
                    "Developer error. We probably failed to provide valid data to the api... Response code: " +
                            responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
            }

            PLAY_STORE_UPDATE -> {
                presenterLog.debug(
                    "Play store is updating in the background. Need to try later... Response code: " +
                            responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.play_store_updating)
            }

            PURCHASED_ITEM_NULL -> {
                presenterLog.debug(
                    """User purchased the item but purchase list returned null.
 User will be shown unknown error. Support please look for the token in the log. Response code: $responseCode"""
                )
                appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
            }
        }
        return appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
    }

    private fun onBillingResponse(billingPlanResponse: BillingPlanResponse) {
        presenterLog.debug("Billing plan received. ")
        val skuList = billingResponseToSkuList(billingPlanResponse)
        if (skuList.isNotEmpty()) {
            if (upgradeView?.billingType == UpgradeActivity.BillingType.Amazon) {
                presenterLog.debug("Querying amazon products")
                upgradeView?.getProducts(skuList)
            } else {
                presenterLog.debug("Querying google products")
                val products: MutableList<QueryProductDetailsParams.Product> = mutableListOf()
                for (sku in skuList) {
                    val planType =
                        mobileBillingPlans.stream().filter { billingPlans: BillingPlans ->
                            billingPlans.extId == sku
                        }.findFirst()
                            .map { billingPlans -> if (billingPlans.isReBill) "subs" else "inapp" }
                            .orElse(ProductType.SUBSCRIPTION.name)
                    products.add(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductType(planType)
                            .setProductId(sku).build()
                    )
                }
                upgradeView?.querySkuDetails(products)
            }
        } else if (mPushNotificationAction != null) {
            upgradeView?.showBillingError("Promo is not valid anymore.")
        } else {
            upgradeView?.showBillingError("Failed to get billing plans check your network connection.")
        }
    }

    private suspend fun postPromoPaymentConfirmation() {
        try {
            val result = result<GenericSuccess> {
                apiCallManager.postPromoPaymentConfirmation(mPushNotificationAction?.pcpID ?: "")
            }
            when (result) {
                is CallResult.Error -> {
                    presenterLog.debug(
                        String.format(
                            "Error posting promo payment confirmation : %s",
                            result.errorMessage
                        )
                    )
                    return
                }

                is CallResult.Success -> {
                    presenterLog.debug("Successfully posted promo payment confirmation.")
                    return
                }
            }
        } catch (e: Exception) {
            presenterLog.debug(String.format("Error posting promo payment confirmation : %s", e.message))
        }
    }

    private fun saveAmazonSubscriptionRecord(amazonPurchase: AmazonPurchase) {
        presenterLog.debug("Saving amazon purchase:{}", amazonPurchase)
        val purchaseJson = Gson().toJson(amazonPurchase)
        preferencesHelper.amazonPurchasedItem = purchaseJson
    }

    private fun showBillingError(errorCode: Int, error: String) {
        presenterLog.info(error)
        upgradeView?.showBillingError(error)
        if (errorCode == 4005) {
            presenterLog.debug("Purchase flow: Token was already verified once. Ignore")
            preferencesHelper.savePurchaseFlowState(PurchaseState.FINISHED.name)
        }
    }

    private suspend fun updateUserStatus() {
        try {
            val userSessionResponse = getUserSessionData()
            userRepository.reload(userSessionResponse, null)
            serverListRepository.load()
        } catch (throwable: Throwable) {
            presenterLog.debug(
                "Error updating user status table. " +
                        instance.convertThrowableToString(throwable)
            )
        }
    }

    private fun upgradeUserAccount() {
        presenterLog.info("Updating server locations,credentials, server config and port map...")
        upgradeView?.showProgressBar("#Upgrading to pro...")
        activityScope.launch(Dispatchers.IO) {
            try {
                if (mPushNotificationAction != null) {
                    postPromoPaymentConfirmation()
                }

                connectionDataRepository.update()
                serverListRepository.update()
                updateUserStatus()

                withContext(Dispatchers.Main) {
                    setPurchaseFlowState(PurchaseState.FINISHED)
                    upgradeView?.hideProgressBar()
                    presenterLog.info("User status before going to Home: ${preferencesHelper.userStatus}")
                    val ghostMode = preferencesHelper.userIsInGhostMode()
                    upgradeView?.goToSuccessfulUpgrade(ghostMode)
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    presenterLog.debug(
                        "Could not modify the server list data..." +
                                instance.convertThrowableToString(e)
                    )
                    upgradeView?.hideProgressBar()
                    val ghostMode = preferencesHelper.userIsInGhostMode()
                    upgradeView?.goToSuccessfulUpgrade(ghostMode)
                }
            }
        }
    }

    private fun verifyAmazonReceipt(amazonPurchase: AmazonPurchase) {
        presenterLog.debug("Verifying amazon receipt.")
        upgradeView?.showProgressBar("#Verifying purchase...")
        activityScope.launch(Dispatchers.IO) {
            try {
                val result = result<GenericSuccess> {
                    apiCallManager.verifyPurchaseReceipt(
                        amazonPurchase.receiptId,
                        "",
                        "",
                        BillingConstants.AMAZON_PURCHASE_TYPE,
                        amazonPurchase.userId
                    )
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Error -> showBillingError(
                            result.code,
                            result.errorMessage
                        )

                        is CallResult.Success -> {
                            presenterLog.info("Payment verification successful.")
                            preferencesHelper.amazonPurchasedItem = null
                            presenterLog.info("Setting item purchased to null & upgrading user account")
                            mPurchase = null
                            PurchasingService.notifyFulfillment(
                                amazonPurchase.receiptId, FulfillmentResult.FULFILLED
                            )
                            upgradeUserAccount()
                            setPurchaseFlowState(PurchaseState.FINISHED)
                        }
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    presenterLog.debug(
                        "Payment verification failed. " + instance.convertThrowableToString(e)
                    )
                    upgradeView?.showBillingError("Payment verification failed!")
                }
            }
        }
    }

    override fun regionalPlanIfAvailable(sku: String): String? {
        if (overriddenPlans != null) {
            if (overriddenPlans?.russianPlan != null && RegionLocator.matchesCountryCode("ru")) {
                return when (sku) {
                    "pro_monthly" -> overriddenPlans?.russianPlan?.proMonthly
                    "pro_yearly" -> overriddenPlans?.russianPlan?.proYearly
                    else -> null
                }
            }
        }
        return null
    }

    override fun onRegionalPlanSelected(url: String) {
        upgradeView?.showProgressBar("Getting Web Session")
        presenterLog.info("Requesting web session...")
        activityScope.launch(Dispatchers.IO) {
            try {
                val result = result<WebSession> {
                    apiCallManager.getWebSession()
                }
                withContext(Dispatchers.Main) {
                    upgradeView?.hideProgressBar()
                    when (result) {
                        is CallResult.Error -> {
                            upgradeView?.showBillingError(result.errorMessage)
                        }

                        is CallResult.Success -> {
                            val urlWithSession = url + "&temp_session=" + result.data.tempSession
                            presenterLog.debug("Url: $urlWithSession")
                            upgradeView?.openUrlInBrowser(urlWithSession)
                        }
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    upgradeView?.hideProgressBar()
                    upgradeView?.showBillingError("Unable to generate web session. Check your network connection.")
                }
            }
        }
    }
}
