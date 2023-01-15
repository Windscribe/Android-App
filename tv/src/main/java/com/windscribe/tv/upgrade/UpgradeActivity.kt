/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.upgrade

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.PurchaseResponse
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.common.collect.ImmutableList
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.confirmemail.ConfirmActivity
import com.windscribe.tv.customview.CustomDialog
import com.windscribe.tv.customview.ErrorFragment
import com.windscribe.tv.customview.ProgressFragment
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.email.AddEmailActivity
import com.windscribe.tv.upgrade.PlansFragment.Companion.newInstance
import com.windscribe.tv.welcome.WelcomeActivity
import com.windscribe.tv.windscribe.WindscribeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.billing.*
import com.windscribe.vpn.constants.ExtraConstants.PROMO_EXTRA
import okhttp3.internal.toImmutableList
import org.slf4j.LoggerFactory
import javax.inject.Inject

class UpgradeActivity : BaseActivity(), UpgradeView, BillingFragmentCallback {
    enum class BillingType(val value: String) {
        Google("android"), Amazon("amazon")
    }
    @Inject
    lateinit var upgradeDialog: CustomDialog
    @Inject
    lateinit var presenter: UpgradePresenter
    override var isBillingProcessFinished = false
        private set
    override var billingType = BillingType.Google
        private set
    private var amazonBillingManager: AmazonBillingManager? = null
    private var googleBillingManager: GoogleBillingManager? = null
    private val logger = LoggerFactory.getLogger("upgrade_a")
    private var selectedProductDetails: ProductDetails? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_upgrade)
        logger.info("OnCreate: Upgrade Activity")
        showProgressBar("Loading Billing Plans...")
        if (intent.hasExtra(PROMO_EXTRA)) {
            val pushNotificationAction = intent
                .getSerializableExtra(PROMO_EXTRA) as PushNotificationAction?
            pushNotificationAction?.let {
                presenter.setPushNotificationAction(it)
            }
        }
        setBillingType()
        if (billingType == BillingType.Amazon) {
            amazonBillingManager = appContext.amazonBillingManager
            amazonBillingManager?.let {
                lifecycle.addObserver(it)
                initAmazonBillingLifecycleListeners()
            }
        } else {
            googleBillingManager = appContext.billingManager
            googleBillingManager?.let {
                lifecycle.addObserver(it)
                initBillingLifecycleListeners()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (billingType == BillingType.Google) {
            presenter.checkBillingProcessStatus()
        }
        presenter.setLayoutFromApiSession()
    }

    override fun onDestroy() {
        upgradeDialog.cancel()
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun getProducts(skuList: List<String>) {
        amazonBillingManager?.getProducts(skuList)
    }

    override fun goBackToMainActivity() {
        logger.info("Going back to previous activity...")
        onBackPressed()
    }

    override fun goToAddEmail() {
        val startIntent = Intent(this, AddEmailActivity::class.java)
        startIntent.putExtra("goToHomeAfterFinish", false)
        startActivity(startIntent)
    }

    override fun goToConfirmEmail() {
        val startIntent = Intent(this, ConfirmActivity::class.java)
        startActivity(startIntent)
    }

    override fun gotToClaimAccount() {
        val startIntent = WelcomeActivity.getStartIntent(this)
        startIntent.putExtra("startFragmentName", "AccountSetUp")
        startActivity(startIntent)
        finish()
    }

    override fun hideProgressBar() {
        val fragment = supportFragmentManager.findFragmentById(R.id.cl_upgrade)
        if (fragment is ProgressFragment) {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onContinuePlanClick(productDetails: ProductDetails, selectedIndex: Int) {
        logger.info("User clicked on plan item...")
        selectedProductDetails = productDetails
        val builder = ProductDetailsParams.newBuilder()
        builder.setProductDetails(productDetails)
        productDetails.subscriptionOfferDetails?.let {
            val offerToken = it[selectedIndex].offerToken
            builder.setOfferToken(offerToken)
        }
        val productDetailsParamsList = ImmutableList.of(builder.build())
        presenter.onMonthlyItemClicked(productDetailsParamsList)
    }

    override fun onContinuePlanClick(selectedSku: Product) {
        presenter.onContinuePlanClick(selectedSku)
    }

    override fun onPurchaseCancelled() {
        logger.info("Setting billing process finished...")
        isBillingProcessFinished = true
    }

    override fun onPurchaseSuccessful(purchases: List<Purchase>) {
        val purchase = purchases[0]
        if (selectedProductDetails?.oneTimePurchaseOfferDetails != null) {
            googleBillingManager?.InAppConsume(purchase)
        } else {
            googleBillingManager?.subscriptionConsume(purchase)
        }
    }


    override fun onRestorePurchaseClick() {
        presenter.restorePurchase()
    }

    override fun onTenGbFreeClick() {
        logger.info("User clicked on continue free...")
        presenter.onContinueFreeClick()
    }

    override fun onPolicyClick() {}

    override fun onTermsClick() {}

    override fun querySkuDetails(products: List<QueryProductDetailsParams.Product>) {
        logger.info("Querying sku details...")
        googleBillingManager?.querySkuDetailsAsync(products)
    }

    override fun restorePurchase() {
        amazonBillingManager?.getPurchaseHistory()
    }

    override fun setBillingProcessStatus(processFinished: Boolean) {
        isBillingProcessFinished = processFinished
    }

    override fun setEmailStatus(isEmailAdded: Boolean, isEmailConfirmed: Boolean) {}

    override fun showBillingDialog(
        windscribeInAppProduct: WindscribeInAppProduct,
        isEmailAdded: Boolean,
        isEmailConfirmed: Boolean
    ) {
        newInstance()
            .add(
                this,
                windscribeInAppProduct,
                R.id.cl_upgrade,
                false,
                isEmailAdded,
                isEmailConfirmed
            )
    }

    override fun showBillingErrorDialog(errorMessage: String) {
        hideProgressBar()
        ErrorFragment.instance.add(errorMessage, this, R.id.cl_upgrade, false)
    }

    override fun showProgressBar(message: String) {
        runOnUiThread {
            val fragment = supportFragmentManager.findFragmentById(R.id.cl_upgrade)
            if (fragment !is ProgressFragment) {
                ProgressFragment.instance.add(
                    message,
                    this@UpgradeActivity,
                    R.id.cl_upgrade,
                    false
                )
                supportFragmentManager.executePendingTransactions()
            }
            if (fragment is ProgressFragment) {
                fragment.updateProgressStatus(message)
            }
        }
    }

    override fun showToast(toastText: String) {
        logger.info("Showing toast to the user...")
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
    }

    override fun startPurchaseFlow(
        productDetailsParams: ImmutableList<ProductDetailsParams>,
        accountID: String?
    ) {
        val builder =
            BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParams)
        accountID?.let { builder.setObfuscatedAccountId(it) }
        logger.info("Launching billing flow...")
        presenter.setPurchaseFlowState(PurchaseState.IN_PROCESS)
        googleBillingManager?.launchBillingFlow(this, builder.build())
    }

    override fun startPurchaseFlow(product: Product) {
        amazonBillingManager?.launchPurchaseFlow(product)
    }

    override fun startSignUpActivity() {
        val startIntent = WelcomeActivity.getStartIntent(this)
        startIntent.putExtra("startFragmentName", "AccountSetUp")
        startActivity(startIntent)
        finish()
    }

    override fun startWindscribeActivity() {
        val startIntent = Intent(this, WindscribeActivity::class.java)
        startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(startIntent)
        finish()
    }

    private fun initAmazonBillingLifecycleListeners() {
        amazonBillingManager?.onBillingSetUpSuccess?.observe(this) {
            logger.info("Billing client connected successfully...")
            presenter.onBillingSetupSuccessful()
        }
        amazonBillingManager?.onProductsResponseSuccess?.observe(
            this
        ) { products: Map<String, Product> ->
            presenter.onProductDataResponse(products)
        }
        amazonBillingManager?.onProductsResponseFailure?.observe(
            this
        ) { presenter.onProductResponseFailure() }
        amazonBillingManager?.onPurchaseResponseSuccess?.observe(this) { purchaseResponse: PurchaseResponse ->
            presenter.onPurchaseResponse(
                    purchaseResponse
            )
        }
        amazonBillingManager?.onPurchaseResponseFailure?.observe(this) { requestStatus: PurchaseResponse.RequestStatus ->
            presenter.onPurchaseResponseFailure(
                    requestStatus
            )
        }
        amazonBillingManager?.onAmazonPurchaseHistorySuccess?.observe(
            this
        ) { purchases: List<AmazonPurchase> ->
            presenter.onAmazonPurchaseHistorySuccess(purchases)
        }
        amazonBillingManager?.onAmazonPurchaseHistoryError?.observe(this) { error: String ->
            presenter.onAmazonPurchaseHistoryError(
                    error
            )
        }
    }

    private fun initBillingLifecycleListeners() {
        googleBillingManager?.onBillingSetUpSuccess?.observe(this) {
            logger.info("Billing client connected successfully...")
            presenter.onBillingSetupSuccessful()
        }
        googleBillingManager?.onBillingSetupFailure?.observe(this) { code: Int ->
            logger.info("Billing client set up failure...")
            presenter.onBillingSetupFailed(code)
        }
        googleBillingManager?.onProductConsumeSuccess?.observe(this) { purchase: Purchase ->
            logger.info("Product consumption successful...")
            showToast(resources.getString(R.string.purchase_successful))
            presenter.onPurchaseConsumed(purchase)
        }
        googleBillingManager?.onProductConsumeFailure?.observe(
            this
        ) { customPurchase: CustomPurchase ->
            logger.debug("Product consumption failed...")
            presenter.onConsumeFailed(
                    customPurchase.responseCode,
                    customPurchase.purchase
            )
        }
        googleBillingManager?.purchaseUpdateEvent?.observe(
            this
        ) { customPurchases: CustomPurchases ->
            logger.info("Purchase updated...")
            presenter.onPurchaseUpdated(
                    customPurchases.responseCode,
                    customPurchases.purchase
            )
        }
        googleBillingManager?.querySkuDetailEvent?.observe(
            this
        ) { customProductDetails: CustomProductDetails ->
            presenter
                .onSkuDetailsReceived(
                    customProductDetails.billingResult.responseCode,
                    customProductDetails.productDetails.toImmutableList()
                )
        }
    }

    private fun setBillingType() {
        val pkgManager = packageManager
        val installerPackageName = pkgManager.getInstallerPackageName(packageName)
        billingType =
            if (installerPackageName != null && installerPackageName.startsWith("com.amazon")) {
                BillingType.Amazon
            } else {
                BillingType.Google
            }
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context): Intent {
            return Intent(context, UpgradeActivity::class.java)
        }
    }
}
