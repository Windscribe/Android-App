/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory

class AmazonBillingManager(private val app: Application) : PurchasingListener, DefaultLifecycleObserver {

    private val _onAmazonPurchaseHistoryError = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val onAmazonPurchaseHistoryError: SharedFlow<String> = _onAmazonPurchaseHistoryError.asSharedFlow()

    private val _onAmazonPurchaseHistorySuccess = MutableSharedFlow<List<AmazonPurchase>>(replay = 0, extraBufferCapacity = 1)
    val onAmazonPurchaseHistorySuccess: SharedFlow<List<AmazonPurchase>> = _onAmazonPurchaseHistorySuccess.asSharedFlow()

    // State, not a one-shot event: onCreate() emits this synchronously while the lifecycle
    // observer is being registered — i.e. BEFORE the activity's repeatOnLifecycle(STARTED)
    // collector subscribes. A replay=0 SharedFlow would drop it (the buffer is unused with no
    // subscribers), leaving the screen stuck on "Loading Billing Plans...". StateFlow retains the
    // latest value so a later subscriber still sees setup succeeded.
    private val _onBillingSetUpSuccess = MutableStateFlow(false)
    val onBillingSetUpSuccess: StateFlow<Boolean> = _onBillingSetUpSuccess.asStateFlow()

    private val _onProductsResponseFailure = MutableSharedFlow<ProductDataResponse.RequestStatus>(replay = 0, extraBufferCapacity = 1)
    val onProductsResponseFailure: SharedFlow<ProductDataResponse.RequestStatus> = _onProductsResponseFailure.asSharedFlow()

    private val _onProductsResponseSuccess = MutableSharedFlow<Map<String, Product>>(replay = 0, extraBufferCapacity = 1)
    val onProductsResponseSuccess: SharedFlow<Map<String, Product>> = _onProductsResponseSuccess.asSharedFlow()

    private val _onPurchaseResponseFailure = MutableSharedFlow<PurchaseResponse.RequestStatus>(replay = 0, extraBufferCapacity = 1)
    val onPurchaseResponseFailure: SharedFlow<PurchaseResponse.RequestStatus> = _onPurchaseResponseFailure.asSharedFlow()

    private val _onPurchaseResponseSuccess = MutableSharedFlow<PurchaseResponse>(replay = 0, extraBufferCapacity = 1)
    val onPurchaseResponseSuccess: SharedFlow<PurchaseResponse> = _onPurchaseResponseSuccess.asSharedFlow()

    private val amazonPurchases = mutableListOf<AmazonPurchase>()

    private val logger = LoggerFactory.getLogger("billing")

    override fun onCreate(owner: LifecycleOwner) {
        PurchasingService.registerListener(app, this)
        _onBillingSetUpSuccess.value = true
    }

    fun getProducts(skuList: List<String>) {
        logger.debug(String.format("Amazon billing is in sandbox mode: %s", PurchasingService.IS_SANDBOX_MODE))
        val skuSet = HashSet(skuList)
        PurchasingService.getProductData(skuSet)
    }

    fun getPurchaseHistory() {
        amazonPurchases.clear()
        PurchasingService.getPurchaseUpdates(true)
    }

    fun launchPurchaseFlow(selectedSku: Product) {
        logger.debug("Launching purchase flow: " + selectedSku.sku)
        PurchasingService.purchase(selectedSku.sku)
    }

    override fun onProductDataResponse(productDataResponse: ProductDataResponse) {
        if (productDataResponse.requestStatus == ProductDataResponse.RequestStatus.SUCCESSFUL) {
            _onProductsResponseSuccess.tryEmit(productDataResponse.productData)
        } else {
            _onProductsResponseFailure.tryEmit(productDataResponse.requestStatus)
        }
    }

    override fun onPurchaseResponse(purchaseResponse: PurchaseResponse) {
        if (purchaseResponse.requestStatus == PurchaseResponse.RequestStatus.SUCCESSFUL) {
            _onPurchaseResponseSuccess.tryEmit(purchaseResponse)
        } else {
            _onPurchaseResponseFailure.tryEmit(purchaseResponse.requestStatus)
        }
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        logger.debug("Amazon purchase history:$response")
        if (response.requestStatus == PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {
            logger.debug("Saving active payments receipts")
            saveActiveReceipts(response)
            if (response.hasMore()) {
                logger.debug("Getting more active payment receipts")
                PurchasingService.getPurchaseUpdates(false)
            } else {
                if (amazonPurchases.isNotEmpty()) {
                    _onAmazonPurchaseHistorySuccess.tryEmit(amazonPurchases.toList())
                } else {
                    _onAmazonPurchaseHistoryError.tryEmit("No existing purchase found on this account.")
                }
            }
        } else {
            _onAmazonPurchaseHistoryError.tryEmit("No existing purchase found on this account.")
        }
    }

    override fun onUserDataResponse(userDataResponse: UserDataResponse) {
    }

    private fun saveActiveReceipts(response: PurchaseUpdatesResponse) {
        for (receipt in response.receipts) {
            if (receipt.isCanceled) {
                logger.debug("Cancelled: " + receipt.toJSON())
            } else {
                val amazonPurchase = AmazonPurchase(receipt.receiptId, response.userData.userId)
                amazonPurchases.add(amazonPurchase)
                logger.debug("Active: " + receipt.toJSON())
            }
        }
    }
}
