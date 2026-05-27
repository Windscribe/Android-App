/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.amazon.device.iap.model.Product
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.windscribe.mobile.R
import com.windscribe.mobile.databinding.ActivityUpgradeBinding
import com.windscribe.mobile.utils.UiUtil
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.billing.AmazonBillingManager
import com.windscribe.vpn.billing.AmazonProducts
import com.windscribe.vpn.billing.GoogleBillingManager
import com.windscribe.vpn.billing.GoogleProducts
import com.windscribe.vpn.billing.PurchaseState
import com.windscribe.vpn.billing.WindscribeInAppProduct
import com.windscribe.vpn.constants.ExtraConstants.PROMO_EXTRA
import com.windscribe.vpn.constants.NetworkKeyConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class UpgradeActivity : BaseActivity(), UpgradeView {

    private val logger = LoggerFactory.getLogger(TAG)

    @Inject
    lateinit var presenter: UpgradePresenter

    @Inject
    lateinit var amazonBillingManager: AmazonBillingManager

    @Inject
    lateinit var googleBillingManager: GoogleBillingManager

    private lateinit var binding: ActivityUpgradeBinding
    private var billingProcessFinished = false
    private var billingTypeValue = BillingType.Google
    private var selectedProductDetails: ProductDetails? = null
    private var upgradingFromWebsite = false
    private var plans: WindscribeInAppProduct? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.bind(this, lifecycleScope)
        try {
            binding = ActivityUpgradeBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            logger.error("Failed to inflate upgrade activity layout: " + e.message, e)
            finish()
            return
        }
        setContentLayout(false)
        addClickListeners()
        deactivatePlans()
        setTermAndPolicyText()
        logger.info("OnCreate: Upgrade Activity")
        showProgressBar("Loading Billing Plans...")
        // In app notification promo
        if (intent.hasExtra(PROMO_EXTRA)) {
            val pushNotificationAction = intent.getSerializableExtra(PROMO_EXTRA) as? PushNotificationAction
            if (pushNotificationAction != null) {
                presenter.setPushNotificationAction(pushNotificationAction)
            }
        } else {
            // Push notification promo
            appContext.appLifeCycleObserver.pushNotificationAction?.let {
                presenter.setPushNotificationAction(it)
            }
        }
        setBillingType()
        if (billingTypeValue == BillingType.Amazon) {
            lifecycle.addObserver(amazonBillingManager)
            initAmazonBillingLifecycleListeners()
        } else {
            lifecycle.addObserver(googleBillingManager)
            initBillingLifecycleListeners()
            binding.restoreBtn.visibility = View.GONE
        }
    }

    private fun addClickListeners() {
        binding.monthlyPlanContainer.setOnClickListener { setActivePlan(true) }
        binding.yearlyPlanContainer.setOnClickListener { setActivePlan(false) }
        binding.restoreBtn.setOnClickListener { presenter.restorePurchase() }
        binding.closeBtn.setOnClickListener { finish() }
        binding.subscribe.setOnClickListener { v ->
            val currentPlans = plans ?: return@setOnClickListener
            val tag = v.tag ?: return@setOnClickListener
            val sku = tag.toString()
            if (currentPlans is GoogleProducts) {
                val productDetails = currentPlans.getSkuDetails(sku)
                buyGoogleProduct(productDetails)
            } else if (currentPlans is AmazonProducts) {
                val productDetails = currentPlans.getProduct(sku)
                presenter.onContinuePlanClick(productDetails)
            }
        }
    }

    private fun buyGoogleProduct(productDetails: ProductDetails) {
        logger.info("User clicked on plan item...")
        val regionPlanUrl = presenter.regionalPlanIfAvailable(productDetails.productId)
        if (regionPlanUrl != null) {
            presenter.onRegionalPlanSelected(regionPlanUrl)
        } else {
            this.selectedProductDetails = productDetails
            val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
            builder.setProductDetails(productDetails)
            val offerDetails = productDetails.subscriptionOfferDetails
            if (offerDetails != null && offerDetails.isNotEmpty()) {
                val offerToken = offerDetails[0].offerToken
                builder.setOfferToken(offerToken)
            }
            val productDetailsParamsList = listOf(builder.build())
            presenter.buyGoogleProduct(productDetailsParamsList)
        }
    }

    private fun setActivePlan(isMonthly: Boolean) {
        if (isMonthly) {
            binding.yearlyPlanContainer.active = false
            binding.monthlyPlanContainer.active = true
            binding.monthlyPlanSelection.isChecked = true
            binding.yearlyPlanSelection.isChecked = false
            binding.subscribe.tag = binding.monthlyPlanContainer.tag
            binding.subscribe.isEnabled = true
        } else {
            binding.monthlyPlanContainer.active = false
            binding.yearlyPlanContainer.active = true
            binding.monthlyPlanSelection.isChecked = false
            binding.yearlyPlanSelection.isChecked = true
            binding.subscribe.tag = binding.yearlyPlanContainer.tag
            binding.subscribe.isEnabled = true
        }
    }

    private fun hideMonthlyView() {
        binding.monthlyPlanContainer.visibility = View.GONE
        binding.monthlyPlanSelection.visibility = View.GONE
        binding.monthlyBilled.visibility = View.GONE
        binding.monthlyPlanTitle.visibility = View.GONE
        binding.monthlyPlanPrice.visibility = View.GONE
    }

    private fun hideYearlyView() {
        binding.yearlyPlanContainer.visibility = View.GONE
        binding.yearlyPlanSelection.visibility = View.GONE
        binding.yearlyBilled.visibility = View.GONE
        binding.yearlyPlanTitle.visibility = View.GONE
        binding.yearlyPlanPrice.visibility = View.GONE
        binding.yearlyPlanDiscount.visibility = View.GONE
    }

    private fun deactivatePlans() {
        binding.monthlyPlanContainer.active = false
        binding.monthlyPlanContainer.isEnabled = false
        binding.yearlyPlanContainer.active = false
        binding.yearlyPlanSelection.isChecked = false
        binding.monthlyPlanPrice.text = "--"
        binding.yearlyPlanPrice.text = "--"
        binding.yearlyPlanDiscount.visibility = View.GONE
        binding.subscribe.isEnabled = false
    }

    private fun setYearlyPlan(yearlySku: String, monthlySku: String?, isPromo: Boolean) {
        val currentPlans = plans ?: return
        val yearlyPrice = currentPlans.getPrice(yearlySku)
        binding.yearlyPlanPrice.text = yearlyPrice
        binding.yearlyPlanContainer.isEnabled = true
        binding.yearlyPlanContainer.tag = yearlySku

        if (isPromo) {
            val discount = currentPlans.getDiscountLabel(yearlySku)
            binding.yearlyPromoDiscount.text = discount
            binding.yearlyPromoDiscount.visibility = View.VISIBLE
            binding.yearlyPlanDiscount.visibility = View.GONE

            val exchangeRateWithCurrency = getExchangeRate(yearlySku)
            val originalPriceStr = currentPlans.getOriginalPrice(yearlySku)

            if (exchangeRateWithCurrency != null && originalPriceStr != null) {
                try {
                    val originalPrice = originalPriceStr.toDouble()
                    val localFullPrice = exchangeRateWithCurrency.second * originalPrice
                    val formattedPrice = String.format(Locale.US, "%.2f", localFullPrice)
                    val currency = exchangeRateWithCurrency.first
                    val originalPriceSpan = SpannableString("$currency $formattedPrice")
                    originalPriceSpan.setSpan(StrikethroughSpan(), 0, originalPriceSpan.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    binding.yearlyBilled.text = TextUtils.concat(originalPriceSpan, " ", yearlyPrice, " ", getString(com.windscribe.vpn.R.string.charged_every_12_months))
                } catch (ignored: NumberFormatException) {
                }
            }
        } else {
            setMonthlyBilledAmount(yearlySku, monthlySku)
        }
    }

    private fun getExchangeRate(sku: String): Pair<String, Double>? {
        val currentPlans = plans ?: return null
        val localPricePair = UiUtil.getPriceWithCurrency(currentPlans.getPrice(sku))
        val usdPricePair = UiUtil.getPriceWithCurrency(currentPlans.getUsdPrice(sku))
        if (localPricePair != null && usdPricePair != null) {
            val localPrice = localPricePair.second
            val usdPrice = usdPricePair.second
            if (usdPrice > 0) {
                return Pair(localPricePair.first, localPrice / usdPrice)
            }
        }
        return null
    }

    private fun setMonthlyPlan(sku: String, isPromo: Boolean) {
        val currentPlans = plans ?: return
        val monthlyPrice = currentPlans.getPrice(sku)
        binding.monthlyPlanPrice.text = monthlyPrice
        binding.monthlyPlanContainer.isEnabled = true
        binding.monthlyPlanContainer.tag = sku

        if (isPromo) {
            val discount = currentPlans.getDiscountLabel(sku)
            binding.monthlyPromoDiscount.text = discount
            binding.monthlyPromoDiscount.visibility = View.VISIBLE

            val exchangeRateWithCurrency = getExchangeRate(sku)
            val originalPriceStr = currentPlans.getOriginalPrice(sku)

            if (exchangeRateWithCurrency != null && originalPriceStr != null) {
                try {
                    val originalPrice = originalPriceStr.toDouble()
                    val localFullPrice = exchangeRateWithCurrency.second * originalPrice
                    val formattedPrice = String.format(Locale.US, "%.2f", localFullPrice)
                    val currency = exchangeRateWithCurrency.first
                    val originalPriceSpan = SpannableString("$currency $formattedPrice")
                    originalPriceSpan.setSpan(StrikethroughSpan(), 0, originalPriceSpan.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    binding.monthlyBilled.text = TextUtils.concat(originalPriceSpan, " ", monthlyPrice, " ", getString(com.windscribe.vpn.R.string.charged_every_month))
                } catch (ignored: NumberFormatException) {
                }
            }
        } else {
            binding.monthlyBilled.text = getString(com.windscribe.vpn.R.string.billed_monthly)
        }
    }

    private fun setMonthlyBilledAmount(yearlySku: String, monthlySku: String?) {
        val currentPlans = plans ?: return
        val yearlyPriceWithCurrency = UiUtil.getPriceWithCurrency(currentPlans.getPrice(yearlySku))
        if (yearlyPriceWithCurrency != null) {
            val monthlyPrice = yearlyPriceWithCurrency.second / 12
            val monthly = String.format(Locale.getDefault(), "%s %.2f", yearlyPriceWithCurrency.first, monthlyPrice)
            binding.yearlyBilled.text = getString(com.windscribe.vpn.R.string.monthly_billed, monthly)

            if (monthlySku != null) {
                val monthlyPriceWithCurrency = UiUtil.getPriceWithCurrency(currentPlans.getPrice(monthlySku))
                if (monthlyPriceWithCurrency != null) {
                    val monthlyPriceWithDiscount = monthlyPriceWithCurrency.second
                    val discount = ((monthlyPriceWithDiscount - monthlyPrice) / monthlyPriceWithDiscount) * 100
                    binding.yearlyPlanDiscount.visibility = View.VISIBLE
                    binding.yearlyPlanDiscount.text = String.format(Locale.getDefault(), "-%d%%", Math.round(discount))
                }
            }
        } else {
            binding.yearlyBilled.text = getString(com.windscribe.vpn.R.string.yearly_billed)
        }
    }

    private fun setTermAndPolicyText() {
        val appName = getString(com.windscribe.vpn.R.string.app_name)
        val termAndPolicyText = getString(com.windscribe.vpn.R.string.terms_policy_en)
        val fullText = "$appName $termAndPolicyText"
        val spannable: Spannable = SpannableString(fullText)
        val spanStart = fullText.length - termAndPolicyText.length
        spannable.setSpan(
            ForegroundColorSpan(Color.WHITE),
            spanStart,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val termsSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                openURLInBrowser(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_TERMS))
            }

            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.color = textPaint.linkColor
                textPaint.isUnderlineText = true
            }
        }

        // Clickable span for Policy
        val policySpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                openURLInBrowser(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_PRIVACY))
            }

            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.color = textPaint.linkColor
                textPaint.isUnderlineText = true
            }
        }
        val andIndex = fullText.indexOf("&")
        if (andIndex != -1) {
            spannable.setSpan(
                termsSpan, spanStart, andIndex - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                policySpan, andIndex + 1, fullText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.terms.movementMethod = LinkMovementMethod.getInstance()
            binding.terms.setText(spannable, TextView.BufferType.SPANNABLE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (upgradingFromWebsite) {
            finish()
        } else {
            if (billingTypeValue == BillingType.Google) {
                presenter.checkBillingProcessStatus()
            }
        }
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override val billingType: BillingType
        get() = billingTypeValue

    override fun getProducts(skuList: List<String>) {
        amazonBillingManager.getProducts(skuList)
    }

    override fun goBackToMainActivity() {
        finish()
    }

    override fun hideProgressBar() {
        ProgressDialog.hide(this)
    }

    override fun isBillingProcessFinished(): Boolean {
        return billingProcessFinished
    }

    override fun openUrlInBrowser(url: String) {
        upgradingFromWebsite = true
        openURLInBrowser(url)
    }

    override fun onPurchaseCancelled() {
        logger.info("Setting billing process finished...")
        billingProcessFinished = true
    }

    override fun onPurchaseSuccessful(purchases: List<Purchase>?) {
        if (purchases != null && purchases.isNotEmpty()) {
            val purchase = purchases[0]
            if (selectedProductDetails != null && selectedProductDetails?.oneTimePurchaseOfferDetails != null) {
                googleBillingManager.InAppConsume(purchase)
            } else {
                googleBillingManager.subscriptionConsume(purchase)
            }
        }
    }

    override fun querySkuDetails(products: List<QueryProductDetailsParams.Product>) {
        logger.info("Querying sku details...")
        googleBillingManager.querySkuDetailsAsync(products)
    }

    override fun restorePurchase() {
        amazonBillingManager.getPurchaseHistory()
    }

    override fun setBillingProcessStatus(bProcessFinished: Boolean) {
        billingProcessFinished = bProcessFinished
    }

    override fun setupPlans(windscribeInAppProduct: WindscribeInAppProduct) {
        if (windscribeInAppProduct.getSkus().isEmpty()) return
        this.plans = windscribeInAppProduct
        val monthly = windscribeInAppProduct.getMonthlyPlan()
        val yearly = windscribeInAppProduct.getYearlyPlan()
        val promo = windscribeInAppProduct.getPromoPlan()
        if (promo != null) {
            if (monthly != null) {
                hideYearlyView()
                setMonthlyPlan(monthly, true)
                setActivePlan(true)
            } else if (yearly != null) {
                hideMonthlyView()
                setYearlyPlan(yearly, null, true)
                setActivePlan(false)
            }
            binding.yearlyPlanSelection.visibility = View.GONE
            binding.monthlyPlanSelection.visibility = View.GONE
        } else {
            if (monthly != null) setMonthlyPlan(monthly, false)
            if (yearly != null) setYearlyPlan(yearly, monthly, false)
            setActivePlan(true)
        }
    }

    override fun showBillingError(errorMessage: String) {
        hideProgressBar()
        ErrorDialog.show(this, errorMessage, ContextCompat.getColor(this, R.color.colorDeepNavy80), true)
    }

    override fun showProgressBar(message: String) {
        val fragment = supportFragmentManager.findFragmentByTag(ProgressDialog.tag)
        if (fragment !is ProgressDialog) {
            ProgressDialog.show(this, message, ContextCompat.getColor(this, R.color.colorDeepNavy80))
        }
        if (fragment is ProgressDialog) {
            fragment.updateProgressStatus(message)
        }
    }

    override fun showToast(toastText: String) {
        logger.info("Showing toast to the user...")
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
    }

    override fun startPurchaseFlow(productDetailsParams: List<BillingFlowParams.ProductDetailsParams>, accountID: String?) {
        val builder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParams)
        val obfuscatedId = accountID ?: appContext.preference.deviceUuid
        if (!obfuscatedId.isNullOrEmpty()) {
            builder.setObfuscatedAccountId(obfuscatedId)
        }
        presenter.setPurchaseFlowState(PurchaseState.IN_PROCESS)
        googleBillingManager.launchBillingFlow(this, builder.build())
    }

    override fun startPurchaseFlow(product: Product) {
        amazonBillingManager.launchPurchaseFlow(product)
    }

    private fun <T> SharedFlow<T>.collectOnStart(block: (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { block(it) }
            }
        }
    }

    private fun initAmazonBillingLifecycleListeners() {
        // onBillingSetUpSuccess is a StateFlow seeded with false; only react once it flips to true.
        amazonBillingManager.onBillingSetUpSuccess.collectOnStart { setUp ->
            if (setUp) {
                logger.info("Billing client connected successfully...")
                presenter.onBillingSetupSuccessful()
            }
        }

        amazonBillingManager.onProductsResponseSuccess.collectOnStart { products ->
            presenter.onProductDataResponse(products)
        }

        amazonBillingManager.onProductsResponseFailure.collectOnStart {
            presenter.onProductResponseFailure()
        }

        amazonBillingManager.onPurchaseResponseSuccess.collectOnStart { purchaseResponse ->
            presenter.onPurchaseResponse(purchaseResponse)
        }

        amazonBillingManager.onPurchaseResponseFailure.collectOnStart { requestStatus ->
            presenter.onPurchaseResponseFailure(requestStatus)
        }

        amazonBillingManager.onAmazonPurchaseHistorySuccess.collectOnStart { purchases ->
            presenter.onAmazonPurchaseHistorySuccess(purchases)
        }

        amazonBillingManager.onAmazonPurchaseHistoryError.collectOnStart { error ->
            presenter.onAmazonPurchaseHistoryError(error)
        }
    }

    private fun initBillingLifecycleListeners() {
        googleBillingManager.onBillingSetUpSuccess.collectOnStart {
            logger.info("Billing client connected successfully...")
            presenter.onBillingSetupSuccessful()
        }
        googleBillingManager.onBillingSetupFailure.collectOnStart { code ->
            logger.info("Billing client set up failure...")
            presenter.onBillingSetupFailed(code)
        }

        googleBillingManager.onProductConsumeSuccess.collectOnStart { purchase ->
            logger.info("Product consumption successful...")
            showToast(resources.getString(com.windscribe.vpn.R.string.purchase_successful))
            presenter.onPurchaseConsumed(purchase)
        }

        googleBillingManager.onProductConsumeFailure.collectOnStart { customPurchase ->
            logger.debug("Product consumption failed...")
            presenter.onConsumeFailed(customPurchase.responseCode, customPurchase.purchase)
        }

        googleBillingManager.purchaseUpdateEvent.collectOnStart { customPurchases ->
            logger.info("Purchase updated...")
            presenter.onPurchaseUpdated(customPurchases.responseCode, customPurchases.purchase)
        }
        googleBillingManager.querySkuDetailEvent.collectOnStart { customSkuDetails ->
            presenter.onSkuDetailsReceived(
                customSkuDetails.billingResult.responseCode,
                customSkuDetails.productDetails
            )
        }
    }

    private fun setBillingType() {
        val installerPackageName = packageManager.getInstallerPackageName(packageName)
        billingTypeValue = if (installerPackageName != null && installerPackageName.startsWith("com.amazon")) {
            BillingType.Amazon
        } else {
            BillingType.Google
        }
    }

    override fun goToSuccessfulUpgrade(isGhostAccount: Boolean) {
        val startIntent = Intent(this, UpgradeSuccessActivity::class.java)
        startIntent.putExtra("isGhostAccount", isGhostAccount)
        startActivity(startIntent)
        finish()
    }

    enum class BillingType(val value: String) {
        Google("android"), Amazon("amazon")
    }

    companion object {
        private const val TAG = "billing"

        @JvmStatic
        fun getStartIntent(context: Context): Intent {
            return Intent(context, UpgradeActivity::class.java)
        }
    }
}
