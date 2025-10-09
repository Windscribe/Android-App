/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity;


import static com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED;
import static com.windscribe.vpn.constants.BillingConstants.AMAZON_PURCHASED_ITEM;
import static com.windscribe.vpn.constants.BillingConstants.GP_PACKAGE_NAME;
import static com.windscribe.vpn.constants.BillingConstants.GP_PRODUCT_ID;
import static com.windscribe.vpn.constants.BillingConstants.PLAY_STORE_UPDATE;
import static com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM;
import static com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM_NULL;
import static com.windscribe.vpn.constants.BillingConstants.PURCHASE_TOKEN;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductType;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.gson.Gson;
import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.api.response.ApiErrorResponse;
import com.windscribe.vpn.api.response.BillingPlanResponse;
import com.windscribe.vpn.api.response.GenericResponseClass;
import com.windscribe.vpn.api.response.GenericSuccess;
import com.windscribe.vpn.api.response.PushNotificationAction;
import com.windscribe.vpn.api.response.UserSessionResponse;
import com.windscribe.vpn.api.response.WebSession;
import com.windscribe.vpn.billing.AmazonProducts;
import com.windscribe.vpn.billing.AmazonPurchase;
import com.windscribe.vpn.billing.GoogleProducts;
import com.windscribe.vpn.billing.PurchaseState;
import com.windscribe.vpn.commonutils.RegionLocator;
import com.windscribe.vpn.constants.BillingConstants;
import com.windscribe.vpn.constants.NetworkErrorCodes;
import com.windscribe.vpn.errormodel.WindError;
import com.windscribe.vpn.exceptions.GenericApiException;
import com.windscribe.vpn.exceptions.InvalidSessionException;
import com.windscribe.vpn.exceptions.UnknownException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;


public class UpgradePresenterImpl implements UpgradePresenter {

    private static final String TAG = "billing";
    private final Logger presenterLog = LoggerFactory.getLogger(TAG);
    private Purchase mPurchase;
    private PushNotificationAction mPushNotificationAction;
    private ActivityInteractor mUpgradeInteractor;
    private UpgradeView mUpgradeView;
    private List<BillingPlanResponse.BillingPlans> mobileBillingPlans = new ArrayList<>();
    private BillingPlanResponse.OverriddenPlans overriddenPlans = null;

    public UpgradePresenterImpl(UpgradeView mUpgradeView, ActivityInteractor activityInteractor) {
        this.mUpgradeView = mUpgradeView;
        this.mUpgradeInteractor = activityInteractor;
    }

    @Override
    public void onDestroy() {
        presenterLog.info("Stopping billing connection...");

        //Start the background service to verify purchase before destroying
        if (mPurchase != null) {
            presenterLog.info("Starting purchase verification service...");
            mUpgradeInteractor.getReceiptValidator().checkPendingAccountUpgrades();
        }

        if (!mUpgradeInteractor.getCompositeDisposable().isDisposed()) {
            mUpgradeInteractor.getCompositeDisposable().dispose();
        }

        mUpgradeView = null;
        mUpgradeInteractor = null;
    }

    @Override
    public void checkBillingProcessStatus() {
        if (mUpgradeView.isBillingProcessFinished()) {
            mUpgradeView.setBillingProcessStatus(false);
        }
    }

    public void handleAmazonReceipt(final Receipt receipt, final UserData userData) {
        switch (receipt.getProductType()) {
            case ENTITLED:
                break;
            case SUBSCRIPTION:
            case CONSUMABLE:
                handleAmazonPurchase(receipt, userData);
                break;
        }
    }

    public void handleAmazonPurchase(final Receipt receipt, final UserData userData) {
        mUpgradeView.showProgressBar("Verifying purchase.");
        if (!receipt.isCanceled()) {
            AmazonPurchase amazonPurchase = new AmazonPurchase(receipt.getReceiptId(), userData.getUserId());
            saveAmazonSubscriptionRecord(amazonPurchase);
            try {
                verifyAmazonReceipt(amazonPurchase);
            } catch (Exception ignored) {
                presenterLog.debug("Error saving fulfilling amazon order.");
                mUpgradeView.showBillingError("Error saving fulfilling amazon order.");
            }
        } else {
            presenterLog.debug("Subscription/Consumable with receipt is already cancelled.");
            mUpgradeView.showBillingError("Receipt cancelled already.");
        }
    }

    public void launchPurchaseFlowWithAccountID(List<BillingFlowParams.ProductDetailsParams> productDetailsParams) {
        mUpgradeInteractor.getCompositeDisposable().add(mUpgradeInteractor.getUserSessionData()
                .flatMap((Function<UserSessionResponse, SingleSource<String>>)
                        userSessionResponse -> Single.fromCallable(() -> {
                            byte[] userID = userSessionResponse.getUserID().getBytes();
                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            byte[] digest = md.digest(userID);
                            return new String(digest);
                        }))
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<String>() {
                    @Override
                    public void onError(@NonNull final Throwable e) {
                        presenterLog.info("Failed to generate encrypted account ID.");
                        mUpgradeView.startPurchaseFlow(productDetailsParams, null);
                    }

                    @Override
                    public void onSuccess(@NonNull final String accountID) {
                        presenterLog.info("Generated encrypted account ID.");
                        mUpgradeView.startPurchaseFlow(productDetailsParams, accountID);
                    }
                }));
    }

    @Override
    public void onAmazonPurchaseHistoryError(final String error) {
        mUpgradeView.hideProgressBar();
        mUpgradeView.showBillingError(error);
    }

    @Override
    public void onAmazonPurchaseHistorySuccess(final List<AmazonPurchase> amazonPurchases) {
        verifyAmazonReceipt(amazonPurchases.get(0));
    }

    @Override
    public void onBillingSetupFailed(int errorCode) {
        String errorMessage = getBillingErrorMessage(errorCode);
        if (mUpgradeView != null) {
            mUpgradeView.showBillingError(errorMessage);
        }
    }

    @Override
    public void onBillingSetupSuccessful() {
        //Get Billing Plans
        presenterLog.info("Getting billing plans...");
        if (mUpgradeInteractor != null && mUpgradeView != null) {
            final Map<String, String> billingPlanMap = new HashMap<>();
            String promoCode = "";
            if (mPushNotificationAction != null) {
                promoCode = mPushNotificationAction.getPromoCode();
            }
            mUpgradeInteractor.getCompositeDisposable().add(
                    mUpgradeInteractor.getApiCallManager()
                            .getBillingPlans(promoCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onBillingResponse, this::onBillingResponseError));
        }
    }

    @Override
    public void onConsumeFailed(int responseCode, Purchase purchase) {
        presenterLog
                .debug("Failed to consume the purchased product. If product token is [null] then play billing did not return the purchased item. "
                        +
                        "User will be asked to contact support. [Product Token]: " + purchase.getPackageName() + "-"
                        + purchase.getPurchaseToken());
        presenterLog.info("Saving purchased product for later update...");
        mUpgradeInteractor.getAppPreferenceInterface()
                .saveResponseStringData(PURCHASED_ITEM, purchase.getOriginalJson());
        onBillingSetupFailed(responseCode);
    }

    @Override
    public void onContinuePlanClick(final Product selectedSku) {
        mUpgradeView.startPurchaseFlow(selectedSku);
    }

    @Override
    public void buyGoogleProduct(@Nullable List<BillingFlowParams.ProductDetailsParams> productDetailsParams) {
        if (productDetailsParams != null) {
            //Start purchase flow
            presenterLog.info("Starting purchase flow...");
            launchPurchaseFlowWithAccountID(productDetailsParams);
        } else {
            presenterLog.debug("sku returned null! This should not happen... Notify user to retry...");
            mUpgradeView.showToast(
                    Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.unable_to_process_request));
        }
    }

    @Override
    public void onProductDataResponse(final Map<String, Product> products) {
        mUpgradeView.hideProgressBar();
        mUpgradeView.setupPlans(new AmazonProducts(products, mobileBillingPlans, mPushNotificationAction));
    }

    @Override
    public void onProductResponseFailure() {
        presenterLog.debug("Unable query product for your account.");
    }

    @Override
    public void onPurchaseConsumed(Purchase itemPurchased) {
        //Set the purchase item
        mPurchase = itemPurchased;
        presenterLog.info("Saving purchased item to process later...");
        mUpgradeView.showProgressBar("#Verifying purchase...");
        if (mUpgradeInteractor != null) {
            mUpgradeInteractor.getAppPreferenceInterface()
                    .saveResponseStringData(PURCHASED_ITEM, itemPurchased.getOriginalJson());
            presenterLog.info("Verifying payment for purchased item: " + itemPurchased.getOriginalJson());
            Map<String, String> purchaseMap = new HashMap<>();
            //Add purchase maps
            purchaseMap.put(GP_PACKAGE_NAME, itemPurchased.getPackageName());
            purchaseMap.put(GP_PRODUCT_ID, itemPurchased.getProducts().get(0));
            purchaseMap.put(PURCHASE_TOKEN, itemPurchased.getPurchaseToken());
            presenterLog.info(purchaseMap.toString());
            mUpgradeInteractor.getCompositeDisposable().add(
                    mUpgradeInteractor.getApiCallManager()
                            .verifyPurchaseReceipt(itemPurchased.getPurchaseToken(), itemPurchased.getPackageName(), itemPurchased.getProducts().get(0), "", "")
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(
                                    new DisposableSingleObserver<GenericResponseClass<GenericSuccess, ApiErrorResponse>>() {
                                        @Override
                                        public void onError(@NotNull Throwable e) {
                                            presenterLog.debug("Payment verification failed. " + WindError.getInstance().convertThrowableToString(e));
                                            if (mUpgradeView != null) {
                                                mUpgradeView.showBillingError("Payment verification failed!");
                                            }
                                        }

                                        @Override
                                        public void onSuccess(@NotNull GenericResponseClass<GenericSuccess, ApiErrorResponse> paymentVerificationResponse) {
                                            if (paymentVerificationResponse.getDataClass() != null) {
                                                mUpgradeInteractor.getAppPreferenceInterface().removeResponseData(PURCHASED_ITEM);
                                                //Item purchased and verified
                                                presenterLog.info("Setting item purchased to null & upgrading user account");
                                                mPurchase = null;
                                                upgradeUserAccount();
                                                setPurchaseFlowState(PurchaseState.FINISHED);
                                            } else if (paymentVerificationResponse.getErrorClass() != null) {
                                                showBillingError(paymentVerificationResponse.getErrorClass());
                                            } else {
                                                showBillingError(new ApiErrorResponse());
                                            }
                                        }
                                    }));
        } else {
            presenterLog.info("Upgrade activity destroy method already completed. Purchase Item: " + mPurchase);
            //Start the background service to verify purchase before destroying
            if (mPurchase != null) {
                presenterLog.info("Starting purchase verification service...");
                mUpgradeInteractor.getReceiptValidator().checkPendingAccountUpgrades();
            }
        }
    }

    @Override
    public void onPurchaseResponse(final PurchaseResponse response) {
        final String requestId = response.getRequestId().toString();
        final String userId = response.getUserData().getUserId();
        final PurchaseResponse.RequestStatus status = response.getRequestStatus();
        presenterLog.debug(String
                .format("OnPurchaseResponse: requestId:%s userId:%s status:%s", requestId, userId, status));
        mUpgradeView.showProgressBar("Purchase successful");
        final Receipt receipt = response.getReceipt();
        presenterLog.debug(receipt.toJSON().toString());
        handleAmazonReceipt(receipt, response.getUserData());
    }

    @Override
    public void onPurchaseResponseFailure(final PurchaseResponse.RequestStatus requestStatus) {
        switch (requestStatus) {
            case ALREADY_PURCHASED:
                presenterLog.debug("onPurchaseResponse: already purchased, running verify service.");
                mUpgradeInteractor.getReceiptValidator().checkPendingAccountUpgrades();
                mUpgradeView.goBackToMainActivity();
                break;
            case INVALID_SKU:
                presenterLog.debug("onPurchaseResponse: invalid SKU!.");
                mUpgradeView.goBackToMainActivity();
                break;
            case FAILED:
            case NOT_SUPPORTED:
                presenterLog.debug("onPurchaseResponse: failed to complete purchase");
                mUpgradeView.goBackToMainActivity();
                break;
        }
    }

    @Override
    public void onPurchaseUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        switch (responseCode) {
            case USER_CANCELED:
                setPurchaseFlowState(PurchaseState.FINISHED);
                if (mUpgradeView != null) {
                    presenterLog.info("User cancelled the purchase...");
                    mUpgradeView.showToast(
                            Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.purchase_cancelled));
                    mUpgradeView.onPurchaseCancelled();
                }
                break;
            case OK:
                if (mUpgradeView != null) {
                    presenterLog.info("Purchase successful...Need to consume the product...");
                    presenterLog.info(purchases != null ? purchases.toString() : "Purchase not available");
                    mUpgradeView.onPurchaseSuccessful(purchases);
                }
                break;
            case ITEM_ALREADY_OWNED:
                presenterLog.debug("Item already owned by user: Running verify Purchase service.");
                mUpgradeInteractor.getReceiptValidator().checkPendingAccountUpgrades();
                break;
            default:
                setPurchaseFlowState(PurchaseState.FINISHED);
                presenterLog.debug("Showing dialog for error. Purchase failed with response code: " + responseCode
                        + " Error Message: " + getBillingErrorMessage(responseCode));
                onBillingSetupFailed(responseCode);

        }
    }

    @Override
    public void onSkuDetailsReceived(int responseCode, final List<ProductDetails> productDetails) {
        if (mUpgradeInteractor == null | mUpgradeView == null) {
            return;
        }
        if (responseCode == OK && !productDetails.isEmpty()) {
            mUpgradeView.hideProgressBar();
            mUpgradeView.setupPlans(new GoogleProducts(productDetails, mobileBillingPlans, mPushNotificationAction));
        } else if (productDetails.isEmpty()) {
            presenterLog.debug("Failed to find requested products from the store.");
            mUpgradeView.showBillingError("Promo is not valid anymore.");
        } else {
            String errorMessage = getBillingErrorMessage(responseCode);
            presenterLog.debug("Error while retrieving sku details from play billing. Error Code: " + responseCode
                    + " Message: " + errorMessage);
            mUpgradeView.showBillingError(errorMessage);
        }
    }

    @Override
    public void restorePurchase() {
        mUpgradeView.showProgressBar("Loading user data...");
        mUpgradeView.restorePurchase();
    }

    @Override
    public void setPurchaseFlowState(PurchaseState state) {
        mUpgradeInteractor.getAppPreferenceInterface().savePurchaseFlowState(state.name());
        presenterLog.debug("Purchase flow: state changed To: " + mUpgradeInteractor.getAppPreferenceInterface()
                .getPurchaseFlowState());

    }

    @Override
    public void setPushNotificationAction(@NonNull PushNotificationAction pushNotificationAction) {
        presenterLog.debug(pushNotificationAction.toString());
        mPushNotificationAction = pushNotificationAction;
    }

    private List<String> billingResponseToSkuList(BillingPlanResponse billingPlanResponse) {
        List<String> inAppSkuList = new ArrayList<>();
        if (!billingPlanResponse.getPlansList().isEmpty()) {
            this.mobileBillingPlans = billingPlanResponse.getPlansList();
            this.overriddenPlans = billingPlanResponse.getOverriddenPlans();
            presenterLog.debug("Getting in app skus from billing plan...");
            for (BillingPlanResponse.BillingPlans billingPlan : mobileBillingPlans) {
                presenterLog.debug("Billing plan: " + billingPlan.toString());
                inAppSkuList.add(billingPlan.getExtId());
            }
        }
        return inAppSkuList;
    }

    private String getBillingErrorMessage(int responseCode) {
        switch (responseCode) {
            case BILLING_UNAVAILABLE:
                presenterLog.debug("Billing unavailable for the device. Response code: " + responseCode);
                return Windscribe.getAppContext()
                        .getResources().getString(com.windscribe.vpn.R.string.billing_unavailable);

            case ITEM_UNAVAILABLE:
                presenterLog.debug("Item user requested is not available. Response code: " + responseCode);
                return Windscribe.getAppContext()
                        .getResources().getString(com.windscribe.vpn.R.string.item_unavailable);

            case SERVICE_UNAVAILABLE:
                presenterLog
                        .debug("Billing service unavailable, user may not be connected to a network. Response Code: "
                                + responseCode);
                return Windscribe.getAppContext()
                        .getResources().getString(com.windscribe.vpn.R.string.billing_service_unavailable);

            case ERROR:
                presenterLog
                        .info("Fatal error during api call, user most likely lost network connection during the process or pressed the "
                                +
                                "button while not connected to internet. Response Code: " + responseCode);
                return Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.play_store_generic_api_error);

            case FEATURE_NOT_SUPPORTED:
                presenterLog.debug("Requested feature is not supported by Play Store on the current device." +
                        "Response Code: " + responseCode);
                return Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.fatal_error);

            case ITEM_ALREADY_OWNED:
                presenterLog.debug("Item already owned. Unknown error will be shown to user... Response code: "
                        + responseCode);
                return Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.unknown_billing_error);

            case ITEM_NOT_OWNED:
                presenterLog.debug("Item not owned. Unknown error will be shown to user... Response code: "
                        + responseCode);
                return Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.unknown_billing_error);

            case DEVELOPER_ERROR:
                presenterLog
                        .debug("Developer error. We probably failed to provide valid data to the api... Response code: "
                                + responseCode);
                return Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.unknown_billing_error);
            case PLAY_STORE_UPDATE:
                presenterLog.debug("Play store is updating in the background. Need to try later... Response code: "
                        + responseCode);
                return Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.play_store_updating);
            case PURCHASED_ITEM_NULL:
                presenterLog
                        .debug("User purchased the item but purchase list returned null.\n User will be shown unknown error."
                                +
                                " Support please look for the token in the log. Response code: " + responseCode);
                Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.unknown_billing_error);
        }
        return Windscribe.getAppContext().getResources().getString(com.windscribe.vpn.R.string.unknown_billing_error);
    }

    private Single<UserSessionResponse> getUserSession() {
        return mUpgradeInteractor.getApiCallManager().getSessionGeneric(null)
                .flatMap(
                        (Function<GenericResponseClass<UserSessionResponse, ApiErrorResponse>, SingleSource<UserSessionResponse>>) genericSessionResponse -> {
                            if (genericSessionResponse.getDataClass() != null) {
                                return Single.fromCallable(genericSessionResponse::getDataClass);
                            } else if (genericSessionResponse.getErrorClass() != null) {
                                if (genericSessionResponse.getErrorClass().getErrorCode()
                                        == NetworkErrorCodes.ERROR_RESPONSE_SESSION_INVALID) {
                                    throw new InvalidSessionException("Session request Success: Invalid session.");
                                } else {
                                    throw new GenericApiException(genericSessionResponse.getErrorClass());
                                }
                            } else {
                                throw new UnknownException("Unknown exception");
                            }
                        });
    }

    private void onBillingResponse(GenericResponseClass<BillingPlanResponse, ApiErrorResponse> billingPlanResponse) {
        if (billingPlanResponse.getDataClass() != null) {
            presenterLog.debug("Billing plan received. ");
            List<String> skuList = billingResponseToSkuList(billingPlanResponse.getDataClass());
            if (!skuList.isEmpty()) {
                if (mUpgradeView.getBillingType() == UpgradeActivity.BillingType.Amazon) {
                    presenterLog.debug("Querying amazon products");
                    mUpgradeView.getProducts(skuList);
                } else {
                    presenterLog.debug("Querying google products");
                    List<QueryProductDetailsParams.Product> products = new ArrayList<>();
                    for (String sku : skuList) {
                        String planType = mobileBillingPlans.stream().filter(billingPlans -> Objects.equals(billingPlans.getExtId(), sku)).findFirst().map(new java.util.function.Function<BillingPlanResponse.BillingPlans, String>() {
                            @Override
                            public String apply(BillingPlanResponse.BillingPlans billingPlans) {
                                return billingPlans.isReBill() ? "subs" : "inapp";
                            }
                        }).orElse(ProductType.SUBSCRIPTION.name());
                        products.add(QueryProductDetailsParams.Product.newBuilder()
                                .setProductType(planType)
                                .setProductId(sku)
                                .build());
                    }
                    mUpgradeView.querySkuDetails(products);
                }
            } else if (mPushNotificationAction != null) {
                mUpgradeView.showBillingError("Promo is not valid anymore.");
            } else {
                mUpgradeView.showBillingError("Failed to get billing plans check your network connection.");
            }

        } else if (billingPlanResponse.getErrorClass() != null) {
            presenterLog.debug(String
                    .format("Billing response error: %s", billingPlanResponse.getErrorClass().getErrorMessage()));
            mUpgradeView.showBillingError(billingPlanResponse.getErrorClass().getErrorMessage());
        }
    }

    private void onBillingResponseError(Throwable throwable) {
        presenterLog
                .debug("Failed to get the billing plans... proceeding with default plans" + WindError.getInstance()
                        .convertThrowableToString(throwable));
        mUpgradeView.showBillingError("Failed to get billing plans check your network connection.");
    }

    private Completable postPromoPaymentConfirmation() {
        return mUpgradeInteractor.getApiCallManager().postPromoPaymentConfirmation(mPushNotificationAction.getPcpID())
                .onErrorReturn(throwable -> new GenericResponseClass<>(null, null))
                .flatMapCompletable(response -> Completable.fromAction(() -> {
                    if (response.getErrorClass() != null) {
                        presenterLog.debug(String.format("Error posting promo payment confirmation : %s",
                                response.getErrorClass().getErrorMessage()));
                    }
                    if (response.getDataClass() != null) {
                        presenterLog.debug("Successfully posted promo payment confirmation.");
                    }
                }));
    }

    private void saveAmazonSubscriptionRecord(final AmazonPurchase amazonPurchase) {
        presenterLog.debug("Saving amazon purchase:" + amazonPurchase.toString());
        String purchaseJson = new Gson().toJson(amazonPurchase);
        mUpgradeInteractor.getAppPreferenceInterface().saveResponseStringData(AMAZON_PURCHASED_ITEM, purchaseJson);
    }

    private void showBillingError(ApiErrorResponse errorResponse) {
        presenterLog.info(errorResponse.toString());
        mUpgradeView.showBillingError(errorResponse.getErrorMessage());
        if (errorResponse.getErrorCode() == 4005) {
            presenterLog.debug("Purchase flow: Token was already verified once. Ignore");
            mUpgradeInteractor.getAppPreferenceInterface()
                    .savePurchaseFlowState(PurchaseState.FINISHED.name());
        }
    }

    private Completable updateUserStatus() {
        return getUserSession().flatMapCompletable(userSessionResponse ->
                Completable.fromAction(() -> mUpgradeInteractor.getUserRepository().reload(userSessionResponse, null))
                        .doFinally(() -> mUpgradeInteractor.getServerListUpdater().load())
                        .doOnError(throwable -> presenterLog.debug("Error updating user status table. "
                                + WindError.getInstance().convertThrowableToString(throwable))));
    }

    private void upgradeUserAccount() {
        presenterLog.info("Updating server locations,credentials, server config and port map...");
        mUpgradeView.showProgressBar("#Upgrading to pro...");
        mUpgradeInteractor.getCompositeDisposable()
                .add((mPushNotificationAction != null ? postPromoPaymentConfirmation()
                        : Completable.fromAction(() -> {
                }))
                        .andThen(mUpgradeInteractor.getConnectionDataUpdater().update())
                        .andThen(mUpgradeInteractor.getServerListUpdater().update())
                        .andThen(updateUserStatus())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableCompletableObserver() {
                            @Override
                            public void onComplete() {
                                setPurchaseFlowState(PurchaseState.FINISHED);
                                mUpgradeView.hideProgressBar();
                                presenterLog
                                        .info("User status before going to Home: " + mUpgradeInteractor
                                                .getAppPreferenceInterface()
                                                .getUserStatus());
                                boolean ghostMode = mUpgradeInteractor.getAppPreferenceInterface()
                                        .userIsInGhostMode();
                                mUpgradeView.goToSuccessfulUpgrade(ghostMode);
                            }

                            @Override
                            public void onError(@NotNull Throwable e) {
                                presenterLog.debug("Could not modify the server list data..."
                                        + WindError.getInstance().convertThrowableToString(e));
                                mUpgradeView.hideProgressBar();
                                boolean ghostMode = mUpgradeInteractor.getAppPreferenceInterface()
                                        .userIsInGhostMode();
                                mUpgradeView.goToSuccessfulUpgrade(ghostMode);
                            }
                        }));

    }

    private void verifyAmazonReceipt(final AmazonPurchase amazonPurchase) {
        presenterLog.debug("Verifying amazon receipt.");
        mUpgradeView.showProgressBar("#Verifying purchase...");
        if (mUpgradeInteractor != null) {
            mUpgradeInteractor.getCompositeDisposable().add(
                    mUpgradeInteractor.getApiCallManager()
                            .verifyPurchaseReceipt(amazonPurchase.getReceiptId(), "", "", BillingConstants.AMAZON_PURCHASE_TYPE, amazonPurchase.getUserId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(
                                    new DisposableSingleObserver<GenericResponseClass<GenericSuccess, ApiErrorResponse>>() {
                                        @Override
                                        public void onError(@NotNull Throwable e) {
                                            presenterLog.debug("Payment verification failed. " + WindError.getInstance()
                                                    .convertThrowableToString(e));
                                            if (mUpgradeView != null) {
                                                mUpgradeView.showBillingError("Payment verification failed!");
                                            }
                                        }

                                        @Override
                                        public void onSuccess(@NotNull GenericResponseClass<GenericSuccess, ApiErrorResponse> paymentVerificationResponse) {
                                            if (paymentVerificationResponse.getDataClass() != null) {
                                                presenterLog.info("Payment verification successful.");
                                                mUpgradeInteractor.getAppPreferenceInterface().removeResponseData(AMAZON_PURCHASED_ITEM);
                                                //Item purchased and verified
                                                presenterLog.info("Setting item purchased to null & upgrading user account");
                                                mPurchase = null;
                                                PurchasingService.notifyFulfillment(amazonPurchase.getReceiptId(), FulfillmentResult.FULFILLED);
                                                upgradeUserAccount();
                                                setPurchaseFlowState(PurchaseState.FINISHED);
                                            } else if (paymentVerificationResponse.getErrorClass() != null) {
                                                showBillingError(paymentVerificationResponse.getErrorClass());
                                            } else {
                                                showBillingError(new ApiErrorResponse());
                                            }
                                        }
                                    }));
        } else {
            presenterLog.info("Upgrade activity destroy method already completed. Purchase Item: " + mPurchase);
            //Start the background service to verify purchase before destroying
            presenterLog.info("Starting purchase verification service...");
            mUpgradeInteractor.getReceiptValidator().checkPendingAccountUpgrades();
        }
    }

    @Override
    public String regionalPlanIfAvailable(String sku) {
        if (overriddenPlans != null) {
            if (overriddenPlans.russianPlan != null && RegionLocator.INSTANCE.matchesCountryCode("ru")) {
                if (sku.equals("pro_monthly")) {
                    return overriddenPlans.russianPlan.proMonthly;
                } else if (sku.equals("pro_yearly")) {
                    return overriddenPlans.russianPlan.proYearly;
                }
            }
        }
        return null;
    }

    @Override
    public void onRegionalPlanSelected(String url) {
        mUpgradeView.showProgressBar("Getting Web Session");
        presenterLog.info("Requesting web session...");
        mUpgradeInteractor.getCompositeDisposable()
                .add(mUpgradeInteractor.getApiCallManager().getWebSession()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(
                                new DisposableSingleObserver<GenericResponseClass<WebSession, ApiErrorResponse>>() {
                                    @Override
                                    public void onError(@NonNull final Throwable e) {
                                        mUpgradeView.hideProgressBar();
                                        mUpgradeView.showBillingError("Unable to generate web session. Check your network connection.");
                                    }

                                    @Override
                                    public void onSuccess(
                                            @NonNull final GenericResponseClass<WebSession, ApiErrorResponse> webSession) {
                                        mUpgradeView.hideProgressBar();
                                        if (webSession.getDataClass() != null) {
                                            String urlWithSession = url + "&temp_session=" + webSession.getDataClass().getTempSession();
                                            presenterLog.debug("Url: " + urlWithSession);
                                            mUpgradeView.openUrlInBrowser(urlWithSession);
                                        } else if (webSession.getErrorClass() != null) {
                                            mUpgradeView.showBillingError(webSession.getErrorClass().getErrorMessage());
                                        } else {
                                            mUpgradeView.showBillingError("Unable to generate web session. Check your network connection.");
                                        }
                                    }
                                }));
    }
}
