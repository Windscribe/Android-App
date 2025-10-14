/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.upgrade

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
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.BillingPlanResponse
import com.windscribe.vpn.api.response.BillingPlanResponse.BillingPlans
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.billing.AmazonProducts
import com.windscribe.vpn.billing.AmazonPurchase
import com.windscribe.vpn.billing.GoogleProducts
import com.windscribe.vpn.billing.PurchaseState
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.constants.BillingConstants.AMAZON_PURCHASED_ITEM
import com.windscribe.vpn.constants.BillingConstants.PLAY_STORE_UPDATE
import com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM
import com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM_NULL
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.exceptions.GenericApiException
import com.windscribe.vpn.exceptions.InvalidSessionException
import com.windscribe.vpn.exceptions.UnknownException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.UserStatusTable
import com.windscribe.vpn.model.User
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
    private var upgradeView: UpgradeView,
    private val activityScope: CoroutineScope,
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface,
    private val userRepository: UserRepository,
    private val receiptValidator: ReceiptValidator,
    private val connectionDataRepository: ConnectionDataRepository,
    private val serverListRepository: ServerListRepository
) : UpgradePresenter {
    private var mPurchase: Purchase? = null
    private var notificationAction: PushNotificationAction? = null
    private var mobileBillingPlans: List<BillingPlans> = ArrayList()
    private val logger = LoggerFactory.getLogger("basic")

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
        logger.info("Stopping billing connection...")
        if (mPurchase != null) {
            logger.info("Starting purchase verification service...")
            receiptValidator.checkPendingAccountUpgrades()
        }
    }

    override fun checkBillingProcessStatus() {
        // If the billing process status is true then go back to main activity
        if (upgradeView.isBillingProcessFinished) {
            upgradeView.setBillingProcessStatus(false)
            upgradeView.goBackToMainActivity()
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
        upgradeView.showProgressBar("Verifying purchase.")
        if (!receipt.isCanceled) {
            val amazonPurchase = AmazonPurchase(receipt.receiptId, userData.userId)
            saveAmazonSubscriptionRecord(amazonPurchase)
            try {
                verifyAmazonReceipt(amazonPurchase)
            } catch (_: Exception) {
                logger.debug("Error saving fulfilling amazon order.")
                upgradeView.showBillingErrorDialog("Error saving fulfilling amazon order.")
            }
        } else {
            logger.debug("Receipt with receipt is already cancelled.")
            upgradeView.showBillingErrorDialog("Subscription cancelled already.")
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
                    logger.info("Generated encrypted account ID.")
                    upgradeView.startPurchaseFlow(productDetailsParams, accountID)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    logger.info("Failed to generate encrypted account ID.")
                    upgradeView.startPurchaseFlow(productDetailsParams, null)
                }
            }
        }
    }

    override fun onAmazonPurchaseHistoryError(error: String) {
        upgradeView.hideProgressBar()
        upgradeView.showBillingErrorDialog(error)
    }

    override fun onAmazonPurchaseHistorySuccess(amazonPurchases: List<AmazonPurchase>) {
        verifyAmazonReceipt(amazonPurchases[0])
    }

    override fun onBillingSetupFailed(errorCode: Int) {
        val errorMessage = getBillingErrorMessage(errorCode)
        upgradeView.showBillingErrorDialog(errorMessage)
    }

    override fun onBillingSetupSuccessful() {
        logger.info("Getting billing plans...")
        activityScope.launch(Dispatchers.IO) {
            try {
                val result = result<BillingPlanResponse> {
                    apiCallManager.getBillingPlans(notificationAction?.promoCode)
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Error -> {
                            val log =
                                String.format("Billing response error: %s", result.errorMessage)
                            logger.debug(log)
                            upgradeView.showBillingErrorDialog(result.errorMessage)
                        }

                        is CallResult.Success -> {
                            onBillingResponse(result.data)
                        }
                    }
                }
            } catch (throwable: Throwable) {
                withContext(Dispatchers.Main) {
                    logger.debug(
                        "Failed to get the billing plans... proceeding with default plans" + instance
                            .convertThrowableToString(throwable)
                    )
                    upgradeView.showBillingErrorDialog("Failed to get billing plans check your network connection.")
                }
            }
        }
    }

    override fun onConsumeFailed(responseCode: Int, purchase: Purchase) {
        logger
            .debug(
                "Failed to consume the purchased product. If product token is [null] then play billing did not return the purchased item. " +
                        "User will be asked to contact support. [Product Token]: " + purchase.packageName + "-" +
                        purchase.purchaseToken
            )
        logger.info("Saving purchased product for later update...")
        preferencesHelper.saveResponseStringData(PURCHASED_ITEM, purchase.originalJson)
        onBillingSetupFailed(responseCode)
    }

    override fun onContinueFreeClick() {
        userRepository.user.value?.let {
            val userLoggedIn = preferencesHelper.sessionHash != null
            if (it.isGhost) {
                upgradeView.gotToClaimAccount()
            } else if (userLoggedIn && it.emailStatus == User.EmailStatus.NoEmail) {
                upgradeView.goToAddEmail()
            } else if (userLoggedIn && it.emailStatus == User.EmailStatus.EmailProvided) {
                upgradeView.goToConfirmEmail()
            }
        }
    }

    override fun onContinuePlanClick(selectedSku: Product) {
        upgradeView.startPurchaseFlow(selectedSku)
    }

    override fun onMonthlyItemClicked(productDetailsParams: List<BillingFlowParams.ProductDetailsParams>) {
        logger.info("Starting purchase flow...")
        launchPurchaseFlowWithAccountID(productDetailsParams)
    }

    override fun onProductDataResponse(products: Map<String, Product>) {
        activityScope.launch(Dispatchers.IO) {
            try {
                val userSessionResponse = getUserSessionData()
                withContext(Dispatchers.Main) {
                    logger.info("Showing upgrade dialog to the user...")
                    upgradeView.hideProgressBar()
                    upgradeView.showBillingDialog(
                        AmazonProducts(
                            products,
                            mobileBillingPlans,
                            notificationAction
                        ),
                        userSessionResponse.userEmail != null,
                        userSessionResponse.emailStatus == UserStatusConstants.EMAIL_STATUS_CONFIRMED
                    )
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    logger.debug("Error reading user session response..." + e.localizedMessage)
                    upgradeView.hideProgressBar()
                    upgradeView.showBillingDialog(
                        AmazonProducts(
                            products,
                            mobileBillingPlans,
                            notificationAction
                        ),
                        isEmailAdded = true, isEmailConfirmed = true
                    )
                }
            }
        }
    }

    override fun onProductResponseFailure() {
        logger.debug("Unable query product for your account.")
    }

    override fun onPurchaseConsumed(purchase: Purchase) {
        // Set the purchase item
        mPurchase = purchase
        logger.info("Saving purchased item to process later...")
        upgradeView.showProgressBar("#Verifying purchase...")
        preferencesHelper.saveResponseStringData(PURCHASED_ITEM, purchase.originalJson)
        logger.info("Verifying payment for purchased item: " + purchase.originalJson)
        activityScope.launch(Dispatchers.IO) {
            try {
                val result = result<GenericSuccess> {
                    apiCallManager.verifyPurchaseReceipt(
                        purchase.purchaseToken,
                        purchase.packageName,
                        purchase.products[0],
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
                            logger.info("Payment verification successful. ")
                            preferencesHelper.removeResponseData(PURCHASED_ITEM)
                            // Item purchased and verified
                            logger.info("Setting item purchased to null & upgrading user account")
                            mPurchase = null
                            upgradeUserAccount()
                            setPurchaseFlowState(PurchaseState.FINISHED)
                        }
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    logger.debug(
                        "Payment verification failed. " + instance.convertThrowableToString(
                            e
                        )
                    )
                    upgradeView.showBillingErrorDialog("Payment verification failed!")
                }
            }
        }
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        val requestId = response.requestId.toString()
        val userId = response.userData.userId
        val status = response.requestStatus
        logger.debug(
            String.format(
                "OnPurchaseResponse: requestId:%s userId:%s status:%s",
                requestId,
                userId,
                status
            )
        )
        upgradeView.showProgressBar("Purchase successful")
        val receipt = response.receipt
        logger.debug(receipt.toJSON().toString())
        handleAmazonReceipt(receipt, response.userData)
    }

    override fun onPurchaseResponseFailure(requestStatus: PurchaseResponse.RequestStatus) {
        when (requestStatus) {
            PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                logger
                    .debug("onPurchaseResponse: already purchased, running verify service.")
                receiptValidator.checkPendingAccountUpgrades()
                upgradeView.goBackToMainActivity()
            }

            PurchaseResponse.RequestStatus.INVALID_SKU -> {
                logger.debug("onPurchaseResponse: invalid SKU!.")
                upgradeView.goBackToMainActivity()
            }

            PurchaseResponse.RequestStatus.FAILED, PurchaseResponse.RequestStatus.NOT_SUPPORTED -> {
                logger.debug("onPurchaseResponse: failed to complete purchase")
                upgradeView.goBackToMainActivity()
            }

            else -> {}
        }
    }

    override fun onPurchaseUpdated(responseCode: Int, purchases: List<Purchase>) {
        when (responseCode) {
            BillingResponseCode.USER_CANCELED -> {
                setPurchaseFlowState(PurchaseState.FINISHED)
                logger.info("User cancelled the purchase...")
                upgradeView.showToast(
                    appContext.resources.getString(com.windscribe.vpn.R.string.purchase_cancelled)
                )
                upgradeView.onPurchaseCancelled()
            }

            BillingResponseCode.OK -> {
                logger.info("Purchase successful...Need to consume the product...")
                logger.info(purchases.toString())
                upgradeView.onPurchaseSuccessful(purchases)
            }

            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                logger.debug("Item already owned by user: Running verify Purchase service.")
                receiptValidator.checkPendingAccountUpgrades()
            }

            else -> {
                setPurchaseFlowState(PurchaseState.FINISHED)
                logger.debug(
                    "Showing dialog for error. Purchase failed with response code: " + responseCode +
                            " Error Message: " + getBillingErrorMessage(responseCode)
                )
                onBillingSetupFailed(responseCode)
            }
        }
    }

    override fun onSkuDetailsReceived(responseCode: Int, productDetailsList: List<ProductDetails>) {
        if (responseCode == BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
            activityScope.launch(Dispatchers.IO) {
                try {
                    val userSession = getUserSessionData()
                    withContext(Dispatchers.Main) {
                        onUserSessionResponse(productDetailsList, userSession)
                    }
                } catch (throwable: Throwable) {
                    withContext(Dispatchers.Main) {
                        onUserSessionResponseError(productDetailsList, throwable)
                    }
                }
            }
        } else if (productDetailsList.isEmpty()) {
            logger.debug("Failed to find requested products from the store.")
            upgradeView.showBillingErrorDialog("Promo is not valid anymore.")
        } else {
            val errorMessage = getBillingErrorMessage(responseCode)
            logger.debug(
                "Error while retrieving sku details from play billing. Error Code: " + responseCode +
                        " Message: " + errorMessage
            )
            upgradeView.showBillingErrorDialog(errorMessage)
        }
    }

    override fun restorePurchase() {
        upgradeView.showProgressBar("Loading user data...")
        upgradeView.restorePurchase()
    }

    override fun setLayoutFromApiSession() {
        activityScope.launch(Dispatchers.IO) {
            try {
                val result = result<UserSessionResponse> {
                    apiCallManager.getSessionGeneric(null)
                }
                when (result) {
                    is CallResult.Error -> {
                        logger.debug("Server returned error during get session call. ${result.errorMessage}")
                    }

                    is CallResult.Success -> {
                        withContext(Dispatchers.Main) {
                            preferencesHelper.saveResponseStringData(
                                PreferencesKeyConstants.GET_SESSION, Gson().toJson(result.data)
                            )
                            upgradeView.setEmailStatus(
                                result.data.userEmail != null,
                                result.data.emailStatus == UserStatusConstants.EMAIL_STATUS_CONFIRMED
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                logger.debug("Error while making get session call:" + e.message)
            }
        }
    }

    override fun setPurchaseFlowState(state: PurchaseState) {
        preferencesHelper.savePurchaseFlowState(state.name)
        logger.debug(
            "Purchase flow: state changed To: " + preferencesHelper.purchaseFlowState
        )
    }

    override fun setPushNotificationAction(pushNotificationAction: PushNotificationAction) {
        logger.debug(pushNotificationAction.toString())
        notificationAction = pushNotificationAction
    }

    private fun billingResponseToSkuList(billingPlanResponse: BillingPlanResponse): List<String> {
        val inAppSkuList: MutableList<String> = ArrayList()
        if (billingPlanResponse.plansList.isNotEmpty()) {
            mobileBillingPlans = billingPlanResponse.plansList
            logger.debug("Getting in app skus from billing plan...")
            for (billingPlan in mobileBillingPlans) {
                logger.debug("Billing plan: {}", billingPlan)
                inAppSkuList.add(billingPlan.extId)
            }
        }
        return inAppSkuList
    }

    private fun getBillingErrorMessage(responseCode: Int): String {
        when (responseCode) {
            BillingResponseCode.BILLING_UNAVAILABLE -> {
                logger.debug("Billing unavailable for the device. Response code: $responseCode")
                return appContext
                    .resources.getString(com.windscribe.vpn.R.string.billing_unavailable)
            }

            BillingResponseCode.ITEM_UNAVAILABLE -> {
                logger.debug("Item user requested is not available. Response code: $responseCode")
                return appContext
                    .resources.getString(com.windscribe.vpn.R.string.item_unavailable)
            }

            BillingResponseCode.SERVICE_UNAVAILABLE -> {
                logger
                    .debug(
                        "Billing service unavailable, user may not be connected to a network. Response Code: " +
                                responseCode
                    )
                return appContext
                    .resources.getString(com.windscribe.vpn.R.string.billing_service_unavailable)
            }

            BillingResponseCode.ERROR -> {
                logger
                    .info(
                        "Fatal error during api call, user most likely lost network connection during the process or pressed the " +
                                "button while not connected to internet. Response Code: " + responseCode
                    )
                return appContext.resources.getString(com.windscribe.vpn.R.string.fatal_error)
            }

            BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                logger.debug(
                    "Requested feature is not supported by Play Store on the current device." +
                            "Response Code: " + responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.fatal_error)
            }

            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                logger.debug(
                    "Item already owned. Unknown error will be shown to user... Response code: " +
                            responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
            }

            BillingResponseCode.ITEM_NOT_OWNED -> {
                logger.debug(
                    "Item not owned. Unknown error will be shown to user... Response code: " +
                            responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
            }

            BillingResponseCode.DEVELOPER_ERROR -> {
                logger
                    .debug(
                        "Developer error. We probably failed to provide valid data to the api... Response code: " +
                                responseCode
                    )
                return appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
            }

            PLAY_STORE_UPDATE -> {
                logger.debug(
                    "Play store is updating in the background. Need to try later... Response code: " +
                            responseCode
                )
                return appContext.resources.getString(com.windscribe.vpn.R.string.play_store_updating)
            }

            PURCHASED_ITEM_NULL -> {
                logger.debug(
                    """User purchased the item but purchase list returned null.
 User will be shown unknown error. Support please look for the token in the log. Response code: $responseCode"""
                )
                appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
            }
        }
        return appContext.resources.getString(com.windscribe.vpn.R.string.unknown_billing_error)
    }

    private fun onBillingResponse(billingPlanResponse: BillingPlanResponse) {
        logger.debug("Billing plan received. ")
        val skuList = billingResponseToSkuList(billingPlanResponse)
        if (skuList.isNotEmpty()) {
            if (upgradeView.billingType === UpgradeActivity.BillingType.Amazon) {
                logger.debug("Querying amazon products")
                upgradeView.getProducts(skuList)
            } else {
                logger.debug("Querying google products")
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
                upgradeView.querySkuDetails(products)
            }
        } else if (notificationAction != null) {
            upgradeView.showBillingErrorDialog("Promo is not valid anymore.")
        } else {
            upgradeView.showBillingErrorDialog("Failed to get billing plans check your network connection.")
        }
    }

    private fun onUserSessionResponse(
        skuDetailsList: List<ProductDetails>,
        userSessionResponse: UserSessionResponse
    ) {
        logger.info("Showing upgrade dialog to the user...")
        upgradeView.hideProgressBar()
        upgradeView.showBillingDialog(
            GoogleProducts(skuDetailsList, mobileBillingPlans, notificationAction),
            userSessionResponse.userEmail != null,
            userSessionResponse.emailStatus
                    == UserStatusConstants.EMAIL_STATUS_CONFIRMED
        )
    }

    private fun onUserSessionResponseError(
        skuDetailsList: List<ProductDetails>,
        throwable: Throwable
    ) {
        // We failed to get the data remaining
        logger.debug("Error reading user session response..." + throwable.localizedMessage)
        upgradeView.hideProgressBar()
        upgradeView.showBillingDialog(
            GoogleProducts(skuDetailsList, mobileBillingPlans, notificationAction),
            isEmailAdded = true,
            isEmailConfirmed = true
        )
    }

    private suspend fun postPromoPaymentConfirmation() {
        try {
            val result = result<GenericSuccess> {
                apiCallManager.postPromoPaymentConfirmation(notificationAction?.pcpID ?: "")
            }
            when (result) {
                is CallResult.Error -> {
                    logger.debug(
                        String.format(
                            "Error posting promo payment confirmation : %s",
                            result.errorMessage
                        )
                    )
                    return
                }

                is CallResult.Success -> {
                    logger.debug("Successfully posted promo payment confirmation.")
                    return
                }
            }
        } catch (e: Exception) {
            logger.debug(String.format("Error posting promo payment confirmation : %s", e.message))
        }
    }

    private fun saveAmazonSubscriptionRecord(amazonPurchase: AmazonPurchase) {
        logger.debug("Saving amazon purchase:{}", amazonPurchase)
        val purchaseJson = Gson().toJson(amazonPurchase)
        preferencesHelper.saveResponseStringData(AMAZON_PURCHASED_ITEM, purchaseJson)
    }

    private fun showBillingError(errorCode: Int, error: String) {
        logger.info(error)
        upgradeView.showBillingErrorDialog(error)
        if (errorCode == 4005) {
            logger.debug("Purchase flow: Token was already verified once. Ignore")
            preferencesHelper.savePurchaseFlowState(PurchaseState.FINISHED.name)
        }
    }

    private suspend fun updateUserStatus() {
        try {
            val userSessionResponse = getUserSessionData()
            val userStatusTable = UserStatusTable(
                userSessionResponse.userName,
                userSessionResponse.isPremium,
                userSessionResponse.userAccountStatus
            )
            localDbInterface.updateUserStatus(userStatusTable)
        } catch (throwable: Throwable) {
            logger.debug(
                "Error updating user status table. " +
                        instance.convertThrowableToString(throwable)
            )
            throw throwable
        }
    }

    private fun upgradeUserAccount() {
        logger.info("Updating server locations,credentials, server config and port map...")
        upgradeView.showProgressBar("#Upgrading to pro...")
        activityScope.launch(Dispatchers.IO) {
            try {
                // Post promo payment confirmation if needed
                if (notificationAction != null) {
                    postPromoPaymentConfirmation()
                }

                // Update connection data
                connectionDataRepository.update()

                // Update server list
                serverListRepository.update()

                // Update user status
                updateUserStatus()

                withContext(Dispatchers.Main) {
                    setPurchaseFlowState(PurchaseState.FINISHED)
                    upgradeView.hideProgressBar()
                    logger.info("User status before going to Home: ${preferencesHelper.userStatus}")
                    val ghostMode = preferencesHelper.userIsInGhostMode()
                    if (ghostMode) {
                        upgradeView.startSignUpActivity()
                    } else {
                        upgradeView.startWindscribeActivity()
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    logger.debug(
                        "Could not modify the server list data..." +
                                instance.convertThrowableToString(e)
                    )
                    upgradeView.hideProgressBar()
                    upgradeView.startWindscribeActivity()
                }
            }
        }
    }

    private fun verifyAmazonReceipt(amazonPurchase: AmazonPurchase) {
        logger.debug("Verifying amazon receipt.")
        upgradeView.showProgressBar("#Verifying purchase...")
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
                            logger.info("Payment verification successful.")
                            preferencesHelper.removeResponseData(AMAZON_PURCHASED_ITEM)
                            // Item purchased and verified
                            logger.info("Setting item purchased to null & upgrading user account")
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
                    logger.debug(
                        "Payment verification failed. " + instance.convertThrowableToString(
                            e
                        )
                    )
                    upgradeView.showBillingErrorDialog("Payment verification failed!")
                }
            }
        }
    }
}
