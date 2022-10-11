/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.upgrade

import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.common.collect.ImmutableList
import com.google.gson.Gson
import com.windscribe.tv.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.*
import com.windscribe.vpn.api.response.BillingPlanResponse.BillingPlans
import com.windscribe.vpn.billing.AmazonProducts
import com.windscribe.vpn.billing.AmazonPurchase
import com.windscribe.vpn.billing.GoogleProducts
import com.windscribe.vpn.billing.PurchaseState
import com.windscribe.vpn.constants.ApiConstants.PAY_ID
import com.windscribe.vpn.constants.ApiConstants.PROMO_CODE
import com.windscribe.vpn.constants.BillingConstants.AMAZON_PURCHASED_ITEM
import com.windscribe.vpn.constants.BillingConstants.AMAZON_PURCHASE_TYPE
import com.windscribe.vpn.constants.BillingConstants.AMAZON_USER_ID
import com.windscribe.vpn.constants.BillingConstants.GP_PACKAGE_NAME
import com.windscribe.vpn.constants.BillingConstants.GP_PRODUCT_ID
import com.windscribe.vpn.constants.BillingConstants.PLAY_STORE_UPDATE
import com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM
import com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM_NULL
import com.windscribe.vpn.constants.BillingConstants.PURCHASE_TOKEN
import com.windscribe.vpn.constants.BillingConstants.PURCHASE_TYPE
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.constants.UserStatusConstants.ACCOUNT_STATUS_OK
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.exceptions.GenericApiException
import com.windscribe.vpn.exceptions.InvalidSessionException
import com.windscribe.vpn.exceptions.UnknownException
import com.windscribe.vpn.localdatabase.tables.UserStatusTable
import com.windscribe.vpn.model.User
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import javax.inject.Inject

class UpgradePresenterImpl @Inject constructor(
    private var upgradeView: UpgradeView,
    private var interactor: ActivityInteractor
) : UpgradePresenter {
    private var mPurchase: Purchase? = null
    private var notificationAction: PushNotificationAction? = null
    private var mobileBillingPlans: List<BillingPlans> = ArrayList()
    private val logger = LoggerFactory.getLogger("upgrade_p")
    override fun onDestroy() {
        logger.info("Stopping billing connection...")
        // Start the background service to verify purchase before destroying
        if (mPurchase != null) {
            logger.info("Starting purchase verification service...")
            appContext.workManager.checkPendingAccountUpgrades()
        }
        interactor.getCompositeDisposable().clear()
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
                ProductType.CONSUMABLE, ProductType.SUBSCRIPTION -> handleAmazonPurchase(receipt, userData)
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
            } catch (ignored: Exception) {
                logger.debug("Error saving fulfilling amazon order.")
                upgradeView.showBillingErrorDialog("Error saving fulfilling amazon order.")
            }
        } else {
            logger.debug("Receipt with receipt is already cancelled.")
            upgradeView.showBillingErrorDialog("Subscription cancelled already.")
        }
    }

    private fun launchPurchaseFlowWithAccountID(productDetailsParams: ImmutableList<BillingFlowParams.ProductDetailsParams>) {
        interactor.getCompositeDisposable().add(
            interactor.getUserSessionData()
                .flatMap { userSessionResponse: UserSessionResponse ->
                    Single.fromCallable {
                        val userID = userSessionResponse.userID.toByteArray()
                        val md = MessageDigest.getInstance("SHA-256")
                        val digest = md.digest(userID)
                        String(digest)
                    }
                }
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<String?>() {
                    override fun onError(e: Throwable) {
                        logger.info("Failed to generate encrypted account ID.")
                        upgradeView.startPurchaseFlow(productDetailsParams, null)
                    }

                    override fun onSuccess(accountID: String) {
                        logger.info("Generated encrypted account ID.")
                        upgradeView.startPurchaseFlow(productDetailsParams, accountID)
                    }
                })
        )
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
        // Get Billing Plans
        logger.info("Getting billing plans...")
        val billingPlanMap: MutableMap<String, String> = HashMap()
        notificationAction?.let { billingPlanMap[PROMO_CODE] = it.promoCode }
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager()
                .getBillingPlans(billingPlanMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ billingPlanResponse: GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?> ->
                    onBillingResponse(
                        billingPlanResponse
                    )
                }) { throwable: Throwable -> onBillingResponseError(throwable) }
        )
    }

    override fun onConsumeFailed(responseCode: Int, purchase: Purchase) {
        logger
            .debug(
                "Failed to consume the purchased product. If product token is [null] then play billing did not return the purchased item. " +
                    "User will be asked to contact support. [Product Token]: " + purchase.packageName + "-" +
                    purchase.purchaseToken
            )
        logger.info("Saving purchased product for later update...")
        interactor.getAppPreferenceInterface()
            .saveResponseStringData(PURCHASED_ITEM, purchase.originalJson)
        onBillingSetupFailed(responseCode)
    }

    override fun onContinueFreeClick() {
        interactor.getUserRepository().user.value?.let {
            val userLoggedIn = interactor.getAppPreferenceInterface().sessionHash != null
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

    override fun onMonthlyItemClicked(productDetailsParams: ImmutableList<BillingFlowParams.ProductDetailsParams>) {
        logger.info("Starting purchase flow...")
        launchPurchaseFlowWithAccountID(productDetailsParams)
    }

    override fun onProductDataResponse(products: Map<String, Product>) {
        interactor.getCompositeDisposable().add(
            interactor.getUserSessionData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<UserSessionResponse?>() {
                    override fun onError(e: Throwable) {
                        logger.debug(
                            "Error reading user session response..." + e
                                .localizedMessage
                        )
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

                    override fun onSuccess(userSessionResponse: UserSessionResponse) {
                        logger.info("Showing upgrade dialog to the user...")
                        upgradeView.hideProgressBar()
                        upgradeView.showBillingDialog(
                            AmazonProducts(
                                products,
                                mobileBillingPlans,
                                notificationAction
                            ),
                            userSessionResponse.userEmail != null,
                            userSessionResponse.emailStatus
                                == UserStatusConstants.EMAIL_STATUS_CONFIRMED
                        )
                    }
                })
        )
    }

    override fun onProductResponseFailure() {
        logger.debug("Unable query product for your account.")
    }

    override fun onPurchaseConsumed(purchase: Purchase) {
        // Set the purchase item
        mPurchase = purchase
        logger.info("Saving purchased item to process later...")
        upgradeView.showProgressBar("#Verifying purchase...")
        interactor.getAppPreferenceInterface().saveResponseStringData(PURCHASED_ITEM, purchase.originalJson)
        logger.info("Verifying payment for purchased item: " + purchase.originalJson)
        val purchaseMap: MutableMap<String, String> = HashMap()
        // Add purchase maps
        purchaseMap[GP_PACKAGE_NAME] = purchase.packageName
        purchaseMap[GP_PRODUCT_ID] = purchase.products[0]
        purchaseMap[PURCHASE_TOKEN] = purchase.purchaseToken
        logger.info(purchaseMap.toString())
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager()
                .verifyPurchaseReceipt(purchaseMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    object :
                        DisposableSingleObserver<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>?>() {
                        override fun onError(e: Throwable) {
                            logger.debug("Payment verification failed. " + instance.convertThrowableToString(e))
                            upgradeView.showBillingErrorDialog("Payment verification failed!")
                        }

                        override fun onSuccess(paymentVerificationResponse: GenericResponseClass<GenericSuccess?, ApiErrorResponse?>) {
                            when {
                                paymentVerificationResponse.dataClass != null -> {
                                    logger.info("Payment verification successful. ")
                                    interactor.getAppPreferenceInterface().removeResponseData(PURCHASED_ITEM)
                                    // Item purchased and verified
                                    logger.info("Setting item purchased to null & upgrading user account")
                                    mPurchase = null
                                    upgradeUserAccount()
                                    setPurchaseFlowState(PurchaseState.FINISHED)
                                }
                                paymentVerificationResponse.errorClass != null -> {
                                    showBillingError(paymentVerificationResponse.errorClass)
                                }
                                else -> {
                                    showBillingError(ApiErrorResponse())
                                }
                            }
                        }
                    })
        )
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
                interactor.getWorkManager().checkPendingAccountUpgrades()
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
                    appContext.resources.getString(R.string.purchase_cancelled)
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
                appContext.workManager.checkPendingAccountUpgrades()
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
            interactor.getCompositeDisposable().add(
                interactor.getUserSessionData()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { userSession: UserSessionResponse ->
                            onUserSessionResponse(
                                productDetailsList,
                                userSession
                            )
                        }
                    ) { throwable: Throwable ->
                        onUserSessionResponseError(
                            productDetailsList,
                            throwable
                        )
                    }
            )
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
        val sessionMap: Map<String, String> = HashMap()
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager()
                .getSessionGeneric(sessionMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    object :
                        DisposableSingleObserver<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>?>() {
                        override fun onError(e: Throwable) {
                            // Error in API Call
                            logger.debug(
                                "Error while making get session call:" +
                                    instance.convertThrowableToString(e)
                            )
                        }

                        override fun onSuccess(
                            userSessionResponse: GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>
                        ) {
                            if (userSessionResponse.dataClass != null) {
                                interactor.getAppPreferenceInterface()
                                    .saveResponseStringData(
                                        PreferencesKeyConstants.GET_SESSION,
                                        Gson().toJson(userSessionResponse.dataClass)
                                    )
                                upgradeView
                                    .setEmailStatus(
                                        userSessionResponse.dataClass!!.userEmail != null,
                                        userSessionResponse.dataClass!!.emailStatus
                                            == UserStatusConstants.EMAIL_STATUS_CONFIRMED
                                    )
                            } else if (userSessionResponse.errorClass != null) {
                                // Server responded with error!
                                logger.debug(
                                    "Server returned error during get session call." +
                                        userSessionResponse.errorClass.toString()
                                )
                            }
                        }
                    })
        )
    }

    override fun setPurchaseFlowState(state: PurchaseState) {
        interactor.getAppPreferenceInterface().savePurchaseFlowState(state.name)
        logger.debug(
            "Purchase flow: state changed To: " + interactor.getAppPreferenceInterface()
                .purchaseFlowState
        )
    }

    override fun setPushNotificationAction(pushNotificationAction: PushNotificationAction) {
        logger.debug(pushNotificationAction.toString())
        notificationAction = pushNotificationAction
    }

    private fun billingResponseToSkuList(billingPlanResponse: BillingPlanResponse?): List<String> {
        val inAppSkuList: MutableList<String> = ArrayList()
        if (billingPlanResponse!!.plansList.size > 0) {
            mobileBillingPlans = billingPlanResponse.plansList
            logger.debug("Getting in app skus from billing plan...")
            for (billingPlan in mobileBillingPlans) {
                logger.debug("Billing plan: $billingPlan")
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
                    .resources.getString(R.string.billing_unavailable)
            }
            BillingResponseCode.ITEM_UNAVAILABLE -> {
                logger.debug("Item user requested is not available. Response code: $responseCode")
                return appContext
                    .resources.getString(R.string.item_unavailable)
            }
            BillingResponseCode.SERVICE_UNAVAILABLE -> {
                logger
                    .debug(
                        "Billing service unavailable, user may not be connected to a network. Response Code: " +
                            responseCode
                    )
                return appContext
                    .resources.getString(R.string.billing_service_unavailable)
            }
            BillingResponseCode.ERROR -> {
                logger
                    .info(
                        "Fatal error during api call, user most likely lost network connection during the process or pressed the " +
                            "button while not connected to internet. Response Code: " + responseCode
                    )
                return appContext.resources.getString(R.string.fatal_error)
            }
            BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                logger.debug(
                    "Requested feature is not supported by Play Store on the current device." +
                        "Response Code: " + responseCode
                )
                return appContext.resources.getString(R.string.fatal_error)
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                logger.debug(
                    "Item already owned. Unknown error will be shown to user... Response code: " +
                        responseCode
                )
                return appContext.resources.getString(R.string.unknown_billing_error)
            }
            BillingResponseCode.ITEM_NOT_OWNED -> {
                logger.debug(
                    "Item not owned. Unknown error will be shown to user... Response code: " +
                        responseCode
                )
                return appContext.resources.getString(R.string.unknown_billing_error)
            }
            BillingResponseCode.DEVELOPER_ERROR -> {
                logger
                    .debug(
                        "Developer error. We probably failed to provide valid data to the api... Response code: " +
                            responseCode
                    )
                return appContext.resources.getString(R.string.unknown_billing_error)
            }
            PLAY_STORE_UPDATE -> {
                logger.debug(
                    "Play store is updating in the background. Need to try later... Response code: " +
                        responseCode
                )
                return appContext.resources.getString(R.string.play_store_updating)
            }
            PURCHASED_ITEM_NULL -> {
                logger
                    .debug(
                        """User purchased the item but purchase list returned null.
 User will be shown unknown error. Support please look for the token in the log. Response code: $responseCode"""
                    )
                appContext.resources.getString(R.string.unknown_billing_error)
            }
        }
        return appContext.resources.getString(R.string.unknown_billing_error)
    }

    private val userSession: Single<UserSessionResponse>
        get() = interactor.getApiCallManager().getSessionGeneric(null)
            .flatMap(
                Function<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>, SingleSource<UserSessionResponse>> label@{ genericSessionResponse: GenericResponseClass<UserSessionResponse?, ApiErrorResponse?> ->
                    if (genericSessionResponse.dataClass != null) {
                        return@label Single.fromCallable { genericSessionResponse.dataClass }
                    } else if (genericSessionResponse.errorClass != null) {
                        if (genericSessionResponse.errorClass!!.errorCode
                            == NetworkErrorCodes.ERROR_RESPONSE_SESSION_INVALID
                        ) {
                            throw InvalidSessionException("Session request Success: Invalid session.")
                        } else {
                            throw GenericApiException(genericSessionResponse.errorClass)
                        }
                    } else {
                        throw UnknownException("Unknown exception")
                    }
                }
            )

    private fun onBillingResponse(billingPlanResponse: GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?>) {
        if (billingPlanResponse.dataClass != null) {
            logger.debug("Billing plan received. ")
            val skuList = billingResponseToSkuList(billingPlanResponse.dataClass)
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
                                .setProductId(sku)
                                .build()
                        )
                    }
                    upgradeView.querySkuDetails(products)
                }
            } else if (notificationAction != null) {
                upgradeView.showBillingErrorDialog("Promo is not valid anymore.")
            } else {
                upgradeView.showBillingErrorDialog("Failed to get billing plans check your network connection.")
            }
        } else if (billingPlanResponse.errorClass != null) {
            logger.debug(
                String.format(
                    "Billing response error: %s",
                    billingPlanResponse.errorClass!!
                        .errorMessage
                )
            )
            upgradeView.showBillingErrorDialog(billingPlanResponse.errorClass!!.errorMessage)
        }
    }

    private fun onBillingResponseError(throwable: Throwable) {
        logger
            .debug(
                "Failed to get the billing plans... proceeding with default plans" + instance
                    .convertThrowableToString(throwable)
            )
        upgradeView.showBillingErrorDialog("Failed to get billing plans check your network connection.")
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

    private fun postPromoPaymentConfirmation(): Completable {
        val paymentPromoConfirmationMap: MutableMap<String, String> = HashMap()
        paymentPromoConfirmationMap[PAY_ID] = notificationAction!!.pcpID
        return interactor.getApiCallManager()
            .postPromoPaymentConfirmation(paymentPromoConfirmationMap)
            .onErrorReturn { GenericResponseClass(null, null) }
            .flatMapCompletable { response: GenericResponseClass<GenericSuccess?, ApiErrorResponse?> ->
                Completable.fromAction {
                    if (response.errorClass != null) {
                        logger.debug(
                            String.format(
                                "Error posting promo payment confirmation : %s",
                                response.errorClass!!.errorMessage
                            )
                        )
                    }
                    if (response.dataClass != null) {
                        logger.debug("Successfully posted promo payment confirmation.")
                    }
                }
            }
    }

    private fun saveAmazonSubscriptionRecord(amazonPurchase: AmazonPurchase) {
        logger.debug("Saving amazon purchase:$amazonPurchase")
        val purchaseJson = Gson().toJson(amazonPurchase)
        interactor.getAppPreferenceInterface()
            .saveResponseStringData(AMAZON_PURCHASED_ITEM, purchaseJson)
    }

    private fun showBillingError(errorResponse: ApiErrorResponse?) {
        logger.info(errorResponse.toString())
        upgradeView.showBillingErrorDialog(errorResponse!!.errorMessage)
        if (errorResponse.errorCode == 4005) {
            logger.debug("Purchase flow: Token was already verified once. Ignore")
            interactor.getAppPreferenceInterface()
                .savePurchaseFlowState(PurchaseState.FINISHED.name)
        }
    }

    private fun updateUserStatus(): Completable {
        return userSession.flatMapCompletable { userSessionResponse: UserSessionResponse ->
            interactor
                .insertOrUpdateUserStatus(
                    UserStatusTable(
                        userSessionResponse.userName,
                        userSessionResponse.isPremium,
                        userSessionResponse.userAccountStatus
                    )
                )
                .doOnError {
                    if (userSessionResponse.userAccountStatus != ACCOUNT_STATUS_OK) {
                        interactor.getAppPreferenceInterface().globalUserConnectionPreference =
                            false
                    }
                }.doOnError { throwable: Throwable? ->
                    logger.debug(
                        "Error updating user status table. " +
                            instance.convertThrowableToString(throwable)
                    )
                }
        }
    }

    private fun upgradeUserAccount() {
        logger.info("Updating server locations,credentials, server config and port map...")
        upgradeView.showProgressBar("#Upgrading to pro...")
        interactor.getCompositeDisposable()
            .add(
                (if (notificationAction != null) postPromoPaymentConfirmation() else Completable.fromAction {})
                    .andThen(interactor.getConnectionDataUpdater().update())
                    .andThen(interactor.getServerListUpdater().update())
                    .andThen(updateUserStatus())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableCompletableObserver() {
                        override fun onComplete() {
                            setPurchaseFlowState(PurchaseState.FINISHED)
                            upgradeView.hideProgressBar()
                            logger
                                .info(
                                    "User status before going to Home: " + interactor
                                        .getAppPreferenceInterface()
                                        .userStatus
                                )
                            val ghostMode = interactor.getAppPreferenceInterface()
                                .userIsInGhostMode()
                            if (ghostMode) {
                                upgradeView.startSignUpActivity()
                            } else {
                                upgradeView.startWindscribeActivity()
                            }
                        }

                        override fun onError(e: Throwable) {
                            logger.debug(
                                "Could not modify the server list data..." +
                                    instance.convertThrowableToString(e)
                            )
                            upgradeView.hideProgressBar()
                            upgradeView.startWindscribeActivity()
                        }
                    })
            )
    }

    private fun verifyAmazonReceipt(amazonPurchase: AmazonPurchase) {
        logger.debug("Verifying amazon receipt.")
        upgradeView.showProgressBar("#Verifying purchase...")
        val purchaseMap: MutableMap<String, String> = HashMap()
        purchaseMap[PURCHASE_TOKEN] = amazonPurchase.receiptId
        purchaseMap[PURCHASE_TYPE] = AMAZON_PURCHASE_TYPE
        purchaseMap[AMAZON_USER_ID] = amazonPurchase.userId
        logger.info(purchaseMap.toString())
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager()
                .verifyPurchaseReceipt(purchaseMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    object : DisposableSingleObserver<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>?>() {
                        override fun onError(e: Throwable) {
                            logger.debug("Payment verification failed. " + instance.convertThrowableToString(e))
                            upgradeView.showBillingErrorDialog("Payment verification failed!")
                        }

                        override fun onSuccess(paymentVerificationResponse: GenericResponseClass<GenericSuccess?, ApiErrorResponse?>) {
                            when {
                                paymentVerificationResponse.dataClass != null -> {
                                    logger.info("Payment verification successful.")
                                    interactor.getAppPreferenceInterface()
                                            .removeResponseData(AMAZON_PURCHASED_ITEM)
                                    // Item purchased and verified
                                    logger.info("Setting item purchased to null & upgrading user account")
                                    mPurchase = null
                                    PurchasingService.notifyFulfillment(
                                            amazonPurchase.receiptId,
                                            FulfillmentResult.FULFILLED
                                    )
                                    upgradeUserAccount()
                                    setPurchaseFlowState(PurchaseState.FINISHED)
                                }
                                paymentVerificationResponse.errorClass != null -> {
                                    showBillingError(paymentVerificationResponse.errorClass)
                                }
                                else -> {
                                    showBillingError(ApiErrorResponse())
                                }
                            }
                        }
                    })
        )
    }
}
