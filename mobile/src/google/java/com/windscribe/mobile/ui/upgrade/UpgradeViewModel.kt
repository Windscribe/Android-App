/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.ui.upgrade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.windscribe.mobile.utils.UiUtil
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.BillingPlanResponse
import com.windscribe.vpn.api.response.BillingPlanResponse.BillingPlans
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.api.response.WebSession
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.billing.AmazonBillingManager
import com.windscribe.vpn.billing.AmazonProducts
import com.windscribe.vpn.billing.AmazonPurchase
import com.windscribe.vpn.billing.GoogleBillingManager
import com.windscribe.vpn.billing.GoogleProducts
import com.windscribe.vpn.billing.PurchaseManager
import com.windscribe.vpn.billing.PurchaseState
import com.windscribe.vpn.billing.ReceiptParams
import com.windscribe.vpn.billing.WindscribeInAppProduct
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.RegionLocator
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.constants.BillingConstants.PLAY_STORE_UPDATE
import com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM_NULL
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserDataState
import com.windscribe.vpn.services.ReceiptValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.Locale
import javax.inject.Inject

/**
 * MVVM replacement for the old `UpgradePresenter`/`UpgradeView`/`UpgradeActivity` trio. Owns the
 * Google/Amazon billing lifecycle (the managers are [androidx.lifecycle.DefaultLifecycleObserver]s
 * registered on the hosting activity by [UpgradeScreen]) and exposes the screen's render state as
 * [UpgradeState], plus one-shot [UpgradeEvent]s for navigation/toasts.
 *
 * Logic here is a faithful port of `UpgradePresenterImpl`; only the delivery mechanism changed
 * (collected flows + StateFlow instead of a view interface). Billing verification still runs through
 * the application-scoped [PurchaseManager], so it survives this view model.
 */
@HiltViewModel
class UpgradeViewModel
    @Inject
    constructor(
        private val preferencesHelper: PreferencesHelper,
        private val apiCallManager: IApiCallManager,
        private val purchaseManager: PurchaseManager,
        private val receiptValidator: ReceiptValidator,
        val googleBillingManager: GoogleBillingManager,
        val amazonBillingManager: AmazonBillingManager,
    ) : ViewModel() {
        private val logger = LoggerFactory.getLogger("billing")

        private val _state = MutableStateFlow(UpgradeState())
        val state: StateFlow<UpgradeState> = _state.asStateFlow()

        private val _events = MutableSharedFlow<UpgradeEvent>(replay = 0, extraBufferCapacity = 4)
        val events: SharedFlow<UpgradeEvent> = _events.asSharedFlow()

        enum class BillingType { Google, Amazon }

        var billingType: BillingType = BillingType.Google
            private set

        private var plans: WindscribeInAppProduct? = null
        private var selectedProductDetails: ProductDetails? = null
        private var mPurchase: Purchase? = null
        private var pushNotificationAction: PushNotificationAction? = null
        private var mobileBillingPlans: List<BillingPlans> = emptyList()
        private var overriddenPlans: BillingPlanResponse.OverriddenPlans? = null
        private var paymentToken: String? = null
        private var billingProcessFinished = false
        private var upgradingFromWebsite = false
        private var started = false

        /**
         * Purchase tokens we have already started handling. Google's PurchasesUpdatedListener and the
         * getRecentPurchases() query can both deliver the same purchase more than once (we have seen
         * three OK callbacks for one purchase within a few ms). Without this guard each delivery fires
         * a fresh acknowledgePurchase() on the same token; the duplicates race the first ack, come back
         * with an error response, and surface a false "billing error" after a successful purchase.
         */
        private val handledPurchaseTokens = mutableSetOf<String>()

        /**
         * Called once by the screen after it has resolved the install source (Amazon vs Google) and
         * registered the matching billing manager on the activity lifecycle. Idempotent.
         */
        fun start(
            type: BillingType,
            promo: PushNotificationAction?,
        ) {
            if (started) return
            started = true
            billingType = type
            promo?.let { pushNotificationAction = it }
            _state.value = _state.value.copy(showRestore = type == BillingType.Amazon)
            if (type == BillingType.Amazon) {
                collectAmazon()
            } else {
                collectGoogle()
            }
        }

        // region UI intents -------------------------------------------------------------------

        fun selectMonthly() {
            _state.value = _state.value.copy(monthlySelected = true)
        }

        fun selectYearly() {
            _state.value = _state.value.copy(monthlySelected = false)
        }

        fun onTermsClick() {
            emit(UpgradeEvent.OpenUrl(websiteLink(TERMS_PATH)))
        }

        fun onPrivacyClick() {
            emit(UpgradeEvent.OpenUrl(websiteLink(PRIVACY_PATH)))
        }

        fun restorePurchase() {
            showProgress("Loading user data...")
            amazonBillingManager.getPurchaseHistory()
        }

        /**
         * The user tapped Subscribe. Resolves the selected SKU to a store product and starts the
         * platform purchase flow. [launchBillingFlow] is supplied by the screen because Play billing
         * needs the concrete [androidx.appcompat.app.AppCompatActivity].
         */
        fun subscribe(launchBillingFlow: (BillingFlowParams) -> Unit) {
            val currentPlans = plans ?: return
            val sku = _state.value.selectedSku ?: return
            when (currentPlans) {
                is GoogleProducts -> buyGoogleProduct(currentPlans.getSkuDetails(sku), launchBillingFlow)
                is AmazonProducts -> amazonBillingManager.launchPurchaseFlow(currentPlans.getProduct(sku))
                else -> {}
            }
        }

        /** Re-checks the cached billing process flag when the screen resumes (matches old onResume). */
        fun onResume(): Boolean {
            if (upgradingFromWebsite) return true
            if (billingType == BillingType.Google && billingProcessFinished) {
                billingProcessFinished = false
            }
            return false
        }

        // endregion

        private fun buyGoogleProduct(
            productDetails: ProductDetails,
            launchBillingFlow: (BillingFlowParams) -> Unit,
        ) {
            logger.info("User clicked on plan item...")
            val regionPlanUrl = regionalPlanIfAvailable(productDetails.productId)
            if (regionPlanUrl != null) {
                onRegionalPlanSelected(regionPlanUrl)
                return
            }
            selectedProductDetails = productDetails
            val builder =
                BillingFlowParams.ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(productDetails)
            productDetails.subscriptionOfferDetails?.firstOrNull()?.let {
                builder.setOfferToken(it.offerToken)
            }
            val params = listOf(builder.build())
            logger.info("Starting purchase flow...")
            val flowBuilder = BillingFlowParams.newBuilder().setProductDetailsParamsList(params)
            val obfuscatedId = paymentToken ?: preferencesHelper.deviceUuid
            if (!obfuscatedId.isNullOrEmpty()) {
                flowBuilder.setObfuscatedAccountId(obfuscatedId)
            }
            launchBillingFlow(flowBuilder.build())
        }

        // region Billing manager flow collection ----------------------------------------------

        private fun collectGoogle() {
            collect(googleBillingManager.onBillingSetUpSuccess) {
                logger.info("Billing client connected successfully...")
                onBillingSetupSuccessful()
            }
            collect(googleBillingManager.onBillingSetupFailure) { code ->
                logger.info("Billing client set up failure...")
                showBillingError(getBillingErrorMessage(code))
            }
            collect(googleBillingManager.onProductConsumeSuccess) { purchase ->
                logger.info("Product consumption successful...")
                emit(UpgradeEvent.Toast(string(com.windscribe.vpn.R.string.purchase_successful)))
                onPurchaseConsumed(purchase)
            }
            collect(googleBillingManager.onProductConsumeFailure) { custom ->
                logger.debug("Product consumption failed...")
                onConsumeFailed(custom.responseCode, custom.purchase)
            }
            collect(googleBillingManager.purchaseUpdateEvent) { custom ->
                logger.info("Purchase updated...")
                onPurchaseUpdated(custom.responseCode, custom.purchase)
            }
            collect(googleBillingManager.querySkuDetailEvent) { custom ->
                onSkuDetailsReceived(custom.billingResult.responseCode, custom.productDetails)
            }
        }

        private fun collectAmazon() {
            // onBillingSetUpSuccess is a StateFlow seeded with false; only react once it flips true.
            collect(amazonBillingManager.onBillingSetUpSuccess) { setUp ->
                if (setUp) {
                    logger.info("Billing client connected successfully...")
                    onBillingSetupSuccessful()
                }
            }
            collect(amazonBillingManager.onProductsResponseSuccess) { products ->
                hideProgress()
                setupPlans(AmazonProducts(products, mobileBillingPlans, pushNotificationAction))
            }
            collect(amazonBillingManager.onProductsResponseFailure) {
                logger.debug("Unable query product for your account.")
            }
            collect(amazonBillingManager.onPurchaseResponseSuccess) { response ->
                onAmazonPurchaseResponse(response)
            }
            collect(amazonBillingManager.onPurchaseResponseFailure) { status ->
                onAmazonPurchaseResponseFailure(status)
            }
            collect(amazonBillingManager.onAmazonPurchaseHistorySuccess) { purchases ->
                if (purchases.isNotEmpty()) verifyAmazonReceipt(purchases[0])
            }
            collect(amazonBillingManager.onAmazonPurchaseHistoryError) { error ->
                hideProgress()
                showBillingError(error)
            }
        }

        private fun <T> collect(
            flow: kotlinx.coroutines.flow.Flow<T>,
            block: suspend (T) -> Unit,
        ) {
            viewModelScope.launch { flow.collect { block(it) } }
        }

        // endregion

        private fun onBillingSetupSuccessful() {
            logger.info("Getting billing plans...")
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val promoCode = pushNotificationAction?.promoCode ?: ""
                    val result = result<BillingPlanResponse> { apiCallManager.getBillingPlans(promoCode) }
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is CallResult.Error -> {
                                logger.debug("Billing response error: ${result.errorMessage}")
                                showBillingError(result.errorMessage)
                            }
                            is CallResult.Success -> onBillingResponse(result.data)
                        }
                    }
                } catch (throwable: Throwable) {
                    withContext(Dispatchers.Main) {
                        logger.debug(
                            "Failed to get the billing plans... proceeding with default plans" +
                                instance.convertThrowableToString(throwable),
                        )
                        showBillingError("Failed to get billing plans check your network connection.")
                    }
                }
            }
        }

        private fun onBillingResponse(billingPlanResponse: BillingPlanResponse) {
            logger.debug("Billing plan received. ")
            val skuList = billingResponseToSkuList(billingPlanResponse)
            when {
                skuList.isNotEmpty() && billingType == BillingType.Amazon -> {
                    logger.debug("Querying amazon products")
                    amazonBillingManager.getProducts(skuList)
                }
                skuList.isNotEmpty() -> {
                    logger.debug("Querying google products")
                    val products =
                        skuList.map { sku ->
                            val planType =
                                mobileBillingPlans
                                    .firstOrNull { it.extId == sku }
                                    ?.let { if (it.isReBill) "subs" else "inapp" }
                                    ?: ProductType.SUBSCRIPTION.name
                            QueryProductDetailsParams.Product
                                .newBuilder()
                                .setProductType(planType)
                                .setProductId(sku)
                                .build()
                        }
                    logger.info("Querying sku details...")
                    googleBillingManager.querySkuDetailsAsync(products)
                }
                pushNotificationAction != null -> showBillingError("Promo is not valid anymore.")
                else -> showBillingError("Failed to get billing plans check your network connection.")
            }
        }

        private fun billingResponseToSkuList(billingPlanResponse: BillingPlanResponse): List<String> {
            if (billingPlanResponse.plansList.isNullOrEmpty()) return emptyList()
            mobileBillingPlans = billingPlanResponse.plansList ?: emptyList()
            overriddenPlans = billingPlanResponse.overriddenPlans
            paymentToken = billingPlanResponse.paymentToken
            return mobileBillingPlans.map { it.extId ?: "" }
        }

        private fun onSkuDetailsReceived(
            responseCode: Int,
            productDetails: List<ProductDetails>,
        ) {
            when {
                responseCode == BillingResponseCode.OK && productDetails.isNotEmpty() -> {
                    hideProgress()
                    setupPlans(GoogleProducts(productDetails, mobileBillingPlans, pushNotificationAction))
                }
                productDetails.isEmpty() -> {
                    logger.debug("Failed to find requested products from the store.")
                    showBillingError("Promo is not valid anymore.")
                }
                else -> showBillingError(getBillingErrorMessage(responseCode))
            }
        }

        private fun onPurchaseUpdated(
            responseCode: Int,
            purchases: List<Purchase>?,
        ) {
            when (responseCode) {
                BillingResponseCode.USER_CANCELED -> {
                    logger.info("User cancelled the purchase...")
                    emit(UpgradeEvent.Toast(string(com.windscribe.vpn.R.string.purchase_cancelled)))
                    billingProcessFinished = true
                }
                BillingResponseCode.OK -> {
                    val purchase = purchases?.firstOrNull() ?: return
                    // Only act on a fully completed purchase, and only once per token. A duplicate
                    // delivery (second listener callback or getRecentPurchases re-query) is ignored
                    // so we never acknowledge the same token twice.
                    if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
                    if (purchase.isAcknowledged) return
                    if (!handledPurchaseTokens.add(purchase.purchaseToken)) {
                        logger.debug("Ignoring duplicate purchase update for already-handled token.")
                        return
                    }
                    logger.info("Purchase successful...Need to consume the product...")
                    if (selectedProductDetails?.oneTimePurchaseOfferDetails != null) {
                        googleBillingManager.InAppConsume(purchase)
                    } else {
                        googleBillingManager.subscriptionConsume(purchase)
                    }
                }
                BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    logger.debug("Item already owned by user: Running verify Purchase service.")
                    receiptValidator.checkPendingAccountUpgrades()
                }
                else -> {
                    showBillingError(getBillingErrorMessage(responseCode))
                }
            }
        }

        private fun onPurchaseConsumed(purchase: Purchase) {
            mPurchase = purchase
            logger.info("Saving purchased item to process later...")
            showProgress("#Verifying purchase...")
            preferencesHelper.purchasedItem = purchase.originalJson
            val receipt =
                ReceiptParams(
                    purchaseToken = purchase.purchaseToken,
                    gpPackageName = purchase.packageName,
                    gpProductId = purchase.products[0],
                )
            completePurchase(receipt) {
                // PurchaseManager now handles clearing purchasedItem
                mPurchase = null
            }
        }

        private fun onConsumeFailed(
            responseCode: Int,
            purchase: Purchase,
        ) {
            // A purchase that Google already considers acknowledged/owned is not a real failure: the
            // entitlement exists, so a redundant ack returning these codes must not show an error.
            // Persist the receipt and let the durable verifier finish the upgrade instead.
            if (purchase.isAcknowledged ||
                responseCode == BillingResponseCode.ITEM_ALREADY_OWNED ||
                responseCode == BillingResponseCode.ITEM_NOT_OWNED
            ) {
                logger.debug("Consume reported a redundant ack (code=$responseCode); verifying purchase instead of erroring.")
                preferencesHelper.purchasedItem = purchase.originalJson
                receiptValidator.checkPendingAccountUpgrades()
                return
            }
            logger.debug(
                "Failed to consume the purchased product. Saving purchased product for later update. " +
                    "[Product Token]: ${purchase.packageName}-${purchase.purchaseToken}",
            )
            preferencesHelper.purchasedItem = purchase.originalJson
            showBillingError(getBillingErrorMessage(responseCode))
        }

        // region Amazon purchase handling ------------------------------------------------------

        private fun onAmazonPurchaseResponse(response: PurchaseResponse) {
            logger.debug("OnPurchaseResponse status:${response.requestStatus}")
            showProgress("Purchase successful")
            handleAmazonReceipt(response.receipt, response.userData)
        }

        private fun onAmazonPurchaseResponseFailure(requestStatus: PurchaseResponse.RequestStatus) {
            when (requestStatus) {
                PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                    logger.debug("onPurchaseResponse: already purchased, running verify service.")
                    receiptValidator.checkPendingAccountUpgrades()
                    emit(UpgradeEvent.Close)
                }
                PurchaseResponse.RequestStatus.INVALID_SKU,
                PurchaseResponse.RequestStatus.FAILED,
                PurchaseResponse.RequestStatus.NOT_SUPPORTED,
                -> emit(UpgradeEvent.Close)
                else -> {}
            }
        }

        private fun handleAmazonReceipt(
            receipt: Receipt,
            userData: UserData,
        ) {
            when (receipt.productType) {
                ProductType.CONSUMABLE, ProductType.SUBSCRIPTION -> handleAmazonPurchase(receipt, userData)
                else -> {}
            }
        }

        private fun handleAmazonPurchase(
            receipt: Receipt,
            userData: UserData,
        ) {
            showProgress("Verifying purchase.")
            if (receipt.isCanceled) {
                logger.debug("Subscription/Consumable with receipt is already cancelled.")
                showBillingError("Receipt cancelled already.")
                return
            }
            val amazonPurchase = AmazonPurchase(receipt.receiptId, userData.userId)
            preferencesHelper.amazonPurchasedItem = Gson().toJson(amazonPurchase)
            try {
                verifyAmazonReceipt(amazonPurchase)
            } catch (_: Exception) {
                logger.debug("Error saving fulfilling amazon order.")
                showBillingError("Error saving fulfilling amazon order.")
            }
        }

        private fun verifyAmazonReceipt(amazonPurchase: AmazonPurchase) {
            logger.debug("Verifying amazon receipt.")
            showProgress("#Verifying purchase...")
            val receipt =
                ReceiptParams(
                    purchaseToken = amazonPurchase.receiptId,
                    type = BillingConstants.AMAZON_PURCHASE_TYPE,
                    amazonUserId = amazonPurchase.userId,
                )
            completePurchase(receipt) {
                // PurchaseManager now handles clearing amazonPurchasedItem
                mPurchase = null
                PurchasingService.notifyFulfillment(amazonPurchase.receiptId, FulfillmentResult.FULFILLED)
            }
        }

        // endregion

        /**
         * Drives the durable verify -> promo -> account-refresh pipeline. [PurchaseManager] runs it on
         * the application scope so it survives this view model; we only collect to update UI.
         */
        private fun completePurchase(
            receipt: ReceiptParams,
            onVerified: () -> Unit,
        ) {
            viewModelScope.launch {
                purchaseManager
                    .completePurchase(receipt, promoPcpId = pushNotificationAction?.pcpID)
                    .collect { state ->
                        when (state) {
                            is UserDataState.Loading -> {
                                // Show loading state from PurchaseManager
                                showProgress(state.status)
                            }
                            is UserDataState.Success -> {
                                logger.info("Payment verification + account upgrade successful.")
                                onVerified()
                                hideProgress()
                                emit(UpgradeEvent.Success(preferencesHelper.userIsInGhostMode()))
                            }
                            is UserDataState.Error -> {
                                logger.debug("Purchase completion failed: ${state.error}")
                                showBillingError("Payment verification failed!")
                            }
                        }
                    }
            }
        }

        private fun onRegionalPlanSelected(url: String) {
            showProgress("Getting Web Session")
            logger.info("Requesting web session...")
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = result<WebSession> { apiCallManager.getWebSession() }
                    withContext(Dispatchers.Main) {
                        hideProgress()
                        when (result) {
                            is CallResult.Error -> showBillingError(result.errorMessage)
                            is CallResult.Success -> {
                                val urlWithSession = "$url&temp_session=${result.data.tempSession}"
                                upgradingFromWebsite = true
                                emit(UpgradeEvent.OpenUrl(urlWithSession))
                            }
                        }
                    }
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) {
                        hideProgress()
                        showBillingError("Unable to generate web session. Check your network connection.")
                    }
                }
            }
        }

        private fun regionalPlanIfAvailable(sku: String): String? {
            val russianPlan = overriddenPlans?.russianPlan ?: return null
            if (!RegionLocator.matchesCountryCode("ru")) return null
            return when (sku) {
                "pro_monthly" -> russianPlan.proMonthly
                "pro_yearly" -> russianPlan.proYearly
                else -> null
            }
        }

        // region Plan -> UI model mapping ------------------------------------------------------

        private fun setupPlans(product: WindscribeInAppProduct) {
            if (product.getSkus().isEmpty()) return
            plans = product
            val monthly = product.getMonthlyPlan()
            val yearly = product.getYearlyPlan()
            val promo = product.getPromoPlan()
            if (promo != null) {
                when {
                    monthly != null ->
                        _state.value =
                            _state.value.copy(
                                isPromo = true,
                                monthly = monthlyTile(product, monthly, isPromo = true),
                                yearly = null,
                                monthlySelected = true,
                            )
                    yearly != null ->
                        _state.value =
                            _state.value.copy(
                                isPromo = true,
                                yearly = yearlyTile(product, yearly, null, isPromo = true),
                                monthly = null,
                                monthlySelected = false,
                            )
                }
            } else {
                _state.value =
                    _state.value.copy(
                        isPromo = false,
                        monthly = monthly?.let { monthlyTile(product, it, isPromo = false) },
                        yearly = yearly?.let { yearlyTile(product, it, monthly, isPromo = false) },
                        monthlySelected = true,
                    )
            }
        }

        private fun monthlyTile(
            product: WindscribeInAppProduct,
            sku: String,
            isPromo: Boolean,
        ): PlanTile {
            val price = product.getPrice(sku).orEmpty()
            val billed: PlanBilledLabel
            var promoLabel: String? = null
            if (isPromo) {
                promoLabel = product.getDiscountLabel(sku)
                billed = promoBilledLabel(product, sku, price, string(com.windscribe.vpn.R.string.charged_every_month))
            } else {
                billed = PlanBilledLabel(text = string(com.windscribe.vpn.R.string.billed_monthly))
            }
            return PlanTile(
                sku = sku,
                title = string(com.windscribe.vpn.R.string.plan_monthly),
                price = price,
                billedLabel = billed,
                promoLabel = promoLabel,
            )
        }

        private fun yearlyTile(
            product: WindscribeInAppProduct,
            yearlySku: String,
            monthlySku: String?,
            isPromo: Boolean,
        ): PlanTile {
            val yearlyPrice = product.getPrice(yearlySku).orEmpty()
            if (isPromo) {
                return PlanTile(
                    sku = yearlySku,
                    title = string(com.windscribe.vpn.R.string.plan_yearly),
                    price = yearlyPrice,
                    billedLabel =
                        promoBilledLabel(
                            product,
                            yearlySku,
                            yearlyPrice,
                            string(com.windscribe.vpn.R.string.charged_every_12_months),
                        ),
                    promoLabel = product.getDiscountLabel(yearlySku),
                )
            }
            // Non-promo: derive a per-month equivalent + discount vs the monthly plan.
            var billedText = string(com.windscribe.vpn.R.string.yearly_billed)
            var discountLabel: String? = null
            val yearlyWithCurrency = UiUtil.getPriceWithCurrency(yearlyPrice)
            if (yearlyWithCurrency != null) {
                val perMonth = yearlyWithCurrency.second / 12
                val monthlyStr = String.format(Locale.getDefault(), "%s %.2f", yearlyWithCurrency.first, perMonth)
                billedText = string(com.windscribe.vpn.R.string.monthly_billed, monthlyStr)
                if (monthlySku != null) {
                    UiUtil.getPriceWithCurrency(product.getPrice(monthlySku))?.let { monthlyWithCurrency ->
                        val full = monthlyWithCurrency.second
                        if (full > 0) {
                            val discount = ((full - perMonth) / full) * 100
                            discountLabel = String.format(Locale.getDefault(), "-%d%%", Math.round(discount))
                        }
                    }
                }
            }
            return PlanTile(
                sku = yearlySku,
                title = string(com.windscribe.vpn.R.string.plan_yearly),
                price = yearlyPrice,
                billedLabel = PlanBilledLabel(text = billedText),
                discountLabel = discountLabel,
            )
        }

        private fun promoBilledLabel(
            product: WindscribeInAppProduct,
            sku: String,
            price: String,
            cadence: String,
        ): PlanBilledLabel {
            val exchange = exchangeRate(product, sku)
            val originalPriceStr = product.getOriginalPrice(sku)
            if (exchange != null && originalPriceStr != null) {
                val originalPrice = originalPriceStr.toDoubleOrNull()
                if (originalPrice != null) {
                    val localFullPrice = exchange.second * originalPrice
                    val formatted = String.format(Locale.US, "%.2f", localFullPrice)
                    return PlanBilledLabel(
                        strikeThroughPrice = "${exchange.first} $formatted",
                        text = "$price $cadence",
                    )
                }
            }
            return PlanBilledLabel(text = "$price $cadence")
        }

        private fun exchangeRate(
            product: WindscribeInAppProduct,
            sku: String,
        ): Pair<String, Double>? {
            val local = UiUtil.getPriceWithCurrency(product.getPrice(sku)) ?: return null
            val usd = UiUtil.getPriceWithCurrency(product.getUsdPrice(sku)) ?: return null
            if (usd.second <= 0) return null
            return Pair(local.first, local.second / usd.second)
        }

        // endregion

        private fun getBillingErrorMessage(responseCode: Int): String =
            when (responseCode) {
                BillingResponseCode.BILLING_UNAVAILABLE -> string(com.windscribe.vpn.R.string.billing_unavailable)
                BillingResponseCode.ITEM_UNAVAILABLE -> string(com.windscribe.vpn.R.string.item_unavailable)
                BillingResponseCode.SERVICE_UNAVAILABLE -> string(com.windscribe.vpn.R.string.billing_service_unavailable)
                BillingResponseCode.ERROR -> string(com.windscribe.vpn.R.string.play_store_generic_api_error)
                BillingResponseCode.FEATURE_NOT_SUPPORTED -> string(com.windscribe.vpn.R.string.fatal_error)
                PLAY_STORE_UPDATE -> string(com.windscribe.vpn.R.string.play_store_updating)
                BillingResponseCode.ITEM_ALREADY_OWNED,
                BillingResponseCode.ITEM_NOT_OWNED,
                BillingResponseCode.DEVELOPER_ERROR,
                PURCHASED_ITEM_NULL,
                -> string(com.windscribe.vpn.R.string.unknown_billing_error)
                else -> string(com.windscribe.vpn.R.string.unknown_billing_error)
            }

        // region small helpers ----------------------------------------------------------------

        private fun showProgress(message: String) {
            _state.value = _state.value.copy(loadingMessage = message, errorMessage = null)
        }

        private fun hideProgress() {
            _state.value = _state.value.copy(loadingMessage = null)
        }

        private fun showBillingError(message: String) {
            _state.value = _state.value.copy(loadingMessage = null, errorMessage = message)
        }

        fun dismissError() {
            _state.value = _state.value.copy(errorMessage = null)
        }

        private fun emit(event: UpgradeEvent) {
            viewModelScope.launch { _events.emit(event) }
        }

        private fun string(
            resId: Int,
            vararg args: Any,
        ): String = appContext.resources.getString(resId, *args)

        private fun websiteLink(path: String): String =
            com.windscribe.vpn.constants.NetworkKeyConstants
                .getWebsiteLink(path)

        override fun onCleared() {
            super.onCleared()
            if (mPurchase != null) {
                logger.info("Starting purchase verification service...")
                receiptValidator.checkPendingAccountUpgrades()
            }
        }

        // endregion

        companion object {
            private val TERMS_PATH = com.windscribe.vpn.constants.NetworkKeyConstants.URL_TERMS
            private val PRIVACY_PATH = com.windscribe.vpn.constants.NetworkKeyConstants.URL_PRIVACY
        }
    }
