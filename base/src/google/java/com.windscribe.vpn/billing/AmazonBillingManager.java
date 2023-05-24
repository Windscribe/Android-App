/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing;

import android.app.Application;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.ProductDataResponse.RequestStatus;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.RequestId;
import com.amazon.device.iap.model.UserDataResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AmazonBillingManager implements PurchasingListener, LifecycleObserver {

    private static volatile AmazonBillingManager INSTANCE;

    public final SingleLiveEvent<String> onAmazonPurchaseHistoryError = new SingleLiveEvent<>();

    public final SingleLiveEvent<List<AmazonPurchase>> onAmazonPurchaseHistorySuccess = new SingleLiveEvent<>();

    public final SingleLiveEvent<Boolean> onBillingSetUpSuccess = new SingleLiveEvent<>();

    public final SingleLiveEvent<RequestStatus> onProductsResponseFailure = new SingleLiveEvent<>();

    public final SingleLiveEvent<Map<String, Product>> onProductsResponseSuccess = new SingleLiveEvent<>();

    public final SingleLiveEvent<PurchaseResponse.RequestStatus> onPurchaseResponseFailure = new SingleLiveEvent<>();

    public final SingleLiveEvent<PurchaseResponse> onPurchaseResponseSuccess = new SingleLiveEvent<>();

    private final List<AmazonPurchase> amazonPurchases = new ArrayList<>();

    private final Application app;

    private final Logger logger = LoggerFactory.getLogger("Amazon:Billing_m");

    public AmazonBillingManager(Application app) {
        this.app = app;
    }

    public static AmazonBillingManager getInstance(Application app) {
        if (INSTANCE == null) {
            synchronized (GoogleBillingManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AmazonBillingManager(app);
                }
            }
        }
        return INSTANCE;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void create() {
        PurchasingService.registerListener(app, this);
        onBillingSetUpSuccess.postValue(true);
    }

    public void getProducts(List<String> skuList) {
        logger.debug(String.format("Amazon billing is in sandbox mode: %s", PurchasingService.IS_SANDBOX_MODE));
        HashSet<String> skuSet = new HashSet<>(skuList);
        PurchasingService.getProductData(skuSet);
    }

    public void getPurchaseHistory() {
        amazonPurchases.clear();
        PurchasingService.getPurchaseUpdates(true);
    }

    public void launchPurchaseFlow(final Product selectedSku) {
        logger.debug("Launching purchase flow: " + selectedSku.getSku());
        @SuppressWarnings("unused") final RequestId requestId = PurchasingService.purchase(selectedSku.getSku());
    }

    @Override
    public void onProductDataResponse(final ProductDataResponse productDataResponse) {
        if (productDataResponse.getRequestStatus() == RequestStatus.SUCCESSFUL) {
            onProductsResponseSuccess.postValue(productDataResponse.getProductData());
        } else {
            onProductsResponseFailure.postValue(productDataResponse.getRequestStatus());
        }
    }

    @Override
    public void onPurchaseResponse(final PurchaseResponse purchaseResponse) {
        if (purchaseResponse.getRequestStatus() == PurchaseResponse.RequestStatus.SUCCESSFUL) {
            onPurchaseResponseSuccess.postValue(purchaseResponse);
        } else {
            onPurchaseResponseFailure.postValue(purchaseResponse.getRequestStatus());
        }
    }

    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
        logger.debug("Amazon purchase history:" + response.toString());
        if (response.getRequestStatus() == PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {
            logger.debug("Saving active payments receipts");
            saveActiveReceipts(response);
            if (response.hasMore()) {
                logger.debug("Getting more active payment receipts");
                PurchasingService.getPurchaseUpdates(false);
            } else {
                if (amazonPurchases.size() > 0) {
                    onAmazonPurchaseHistorySuccess.postValue(amazonPurchases);
                } else {
                    onAmazonPurchaseHistoryError.postValue("No existing purchase found on this account.");
                }
            }
        } else {
            onAmazonPurchaseHistoryError.postValue("No existing purchase found on this account.");
        }
    }

    @Override
    public void onUserDataResponse(final UserDataResponse userDataResponse) {

    }

    private void saveActiveReceipts(final PurchaseUpdatesResponse response) {
        for (Receipt receipt : response.getReceipts()) {
            if (receipt.isCanceled()) {
                logger.debug("Cancelled: " + receipt.toJSON());
            } else {
                AmazonPurchase amazonPurchase = new AmazonPurchase(receipt.getReceiptId(),
                        response.getUserData().getUserId());
                amazonPurchases.add(amazonPurchase);
                logger.debug("Active: " + receipt.toJSON());
            }
        }
    }
}
