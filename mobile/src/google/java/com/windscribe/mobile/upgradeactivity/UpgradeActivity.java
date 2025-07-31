/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity;

import static com.windscribe.vpn.Windscribe.appContext;
import static com.windscribe.vpn.constants.ExtraConstants.PROMO_EXTRA;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.amazon.device.iap.model.Product;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;
import com.windscribe.mobile.R;
import com.windscribe.mobile.databinding.ActivityUpgradeBinding;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.utils.UiUtil;
import com.windscribe.vpn.api.response.PushNotificationAction;
import com.windscribe.vpn.billing.AmazonBillingManager;
import com.windscribe.vpn.billing.AmazonProducts;
import com.windscribe.vpn.billing.GoogleBillingManager;
import com.windscribe.vpn.billing.GoogleProducts;
import com.windscribe.vpn.billing.PurchaseState;
import com.windscribe.vpn.billing.WindscribeInAppProduct;
import com.windscribe.vpn.constants.NetworkKeyConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import kotlin.Pair;

public class UpgradeActivity extends BaseActivity
        implements UpgradeView {

    private static final String TAG = "billing";
    private final Logger logger = LoggerFactory.getLogger(TAG);
    @Inject
    UpgradePresenter presenter;
    @Inject
    AmazonBillingManager amazonBillingManager;
    @Inject
    GoogleBillingManager googleBillingManager;
    private ActivityUpgradeBinding binding = null;
    private boolean billingProcessFinished = false;
    private BillingType billingType = BillingType.Google;
    private ProductDetails selectedProductDetails;
    private Boolean upgradingFromWebsite = false;

    private WindscribeInAppProduct plans = null;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, UpgradeActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_upgrade);
        setContentLayout(false);
        addClickListeners();
        deactivatePlans();
        setTermAndPolicyText();
        logger.info("OnCreate: Upgrade Activity");
        showProgressBar("Loading Billing Plans...");
        // In app notification promo
        if (getIntent().hasExtra(PROMO_EXTRA)) {
            PushNotificationAction pushNotificationAction = (PushNotificationAction) getIntent()
                    .getSerializableExtra(PROMO_EXTRA);
            if (pushNotificationAction != null) {
                presenter.setPushNotificationAction(pushNotificationAction);
            }
        } else {
            // Push notification promo
            if (appContext.appLifeCycleObserver.getPushNotificationAction() != null) {
                presenter.setPushNotificationAction(appContext.appLifeCycleObserver.getPushNotificationAction());
            }
        }
        setBillingType();
        if (billingType == BillingType.Amazon) {
            getLifecycle().addObserver(amazonBillingManager);
            initAmazonBillingLifecycleListeners();
        } else {
            getLifecycle().addObserver(googleBillingManager);
            initBillingLifecycleListeners();
            binding.restoreBtn.setVisibility(View.GONE);
        }
    }

    private void addClickListeners() {
        binding.monthlyPlanContainer.setOnClickListener(v -> setActivePlan(true));
        binding.yearlyPlanContainer.setOnClickListener(v -> setActivePlan(false));
        binding.restoreBtn.setOnClickListener(v -> presenter.restorePurchase());
        binding.closeBtn.setOnClickListener(v -> finish());
        binding.subscribe.setOnClickListener(v -> {
            if (plans == null) return;
            Object tag = v.getTag();
            if (tag == null) return;
            String sku = tag.toString();
            if (plans instanceof GoogleProducts) {
                ProductDetails productDetails = ((GoogleProducts) plans).getSkuDetails(sku);
                buyGoogleProduct(productDetails);
            } else if (plans instanceof AmazonProducts) {
                Product productDetails = ((AmazonProducts) plans).getProduct(sku);
                presenter.onContinuePlanClick(productDetails);
            }
        });
    }

    private void buyGoogleProduct(ProductDetails productDetails) {
        logger.info("User clicked on plan item...");
        String regionPlanUrl = presenter.regionalPlanIfAvailable(productDetails.getProductId());
        if (regionPlanUrl != null) {
            presenter.onRegionalPlanSelected(regionPlanUrl);
        } else {
            this.selectedProductDetails = productDetails;
            BillingFlowParams.ProductDetailsParams.Builder builder = BillingFlowParams.ProductDetailsParams.newBuilder();
            builder.setProductDetails(productDetails);
            if (productDetails.getSubscriptionOfferDetails() != null) {
                String offerToken = productDetails
                        .getSubscriptionOfferDetails()
                        .get(0)
                        .getOfferToken();
                builder.setOfferToken(offerToken);
            }
            ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = ImmutableList.of(builder.build());
            presenter.buyGoogleProduct(productDetailsParamsList);
        }
    }

    private void setActivePlan(Boolean isMonthly) {
        if (isMonthly) {
            binding.yearlyPlanContainer.setActive(false);
            binding.monthlyPlanContainer.setActive(true);
            binding.monthlyPlanSelection.setChecked(true);
            binding.yearlyPlanSelection.setChecked(false);
            binding.subscribe.setTag(binding.monthlyPlanContainer.getTag());
            binding.subscribe.setEnabled(true);
        } else {
            binding.monthlyPlanContainer.setActive(false);
            binding.yearlyPlanContainer.setActive(true);
            binding.monthlyPlanSelection.setChecked(false);
            binding.yearlyPlanSelection.setChecked(true);
            binding.subscribe.setTag(binding.yearlyPlanContainer.getTag());
            binding.subscribe.setEnabled(true);
        }
    }

    private void hideMonthlyView() {
        binding.monthlyPlanContainer.setVisibility(View.GONE);
        binding.monthlyPlanSelection.setVisibility(View.GONE);
        binding.monthlyBilled.setVisibility(View.GONE);
        binding.monthlyPlanTitle.setVisibility(View.GONE);
        binding.monthlyPlanPrice.setVisibility(View.GONE);
    }

    private void hideYearlyView() {
        binding.yearlyPlanContainer.setVisibility(View.GONE);
        binding.yearlyPlanSelection.setVisibility(View.GONE);
        binding.yearlyBilled.setVisibility(View.GONE);
        binding.yearlyPlanTitle.setVisibility(View.GONE);
        binding.yearlyPlanPrice.setVisibility(View.GONE);
        binding.yearlyPlanDiscount.setVisibility(View.GONE);
    }

    private void deactivatePlans() {
        binding.monthlyPlanContainer.setActive(false);
        binding.monthlyPlanContainer.setEnabled(false);
        binding.yearlyPlanContainer.setActive(false);
        binding.yearlyPlanSelection.setChecked(false);
        binding.monthlyPlanPrice.setText("--");
        binding.yearlyPlanPrice.setText("--");
        binding.yearlyPlanDiscount.setVisibility(View.GONE);
        binding.subscribe.setEnabled(false);
    }

    private void setYearlyPlan(String yearlySku, @Nullable String monthlySku, boolean isPromo) {
        String yearlyPrice = plans.getPrice(yearlySku);
        binding.yearlyPlanPrice.setText(yearlyPrice);
        binding.yearlyPlanContainer.setEnabled(true);
        binding.yearlyPlanContainer.setTag(yearlySku);

        if (isPromo) {
            String discount = plans.getDiscountLabel(yearlySku);
            binding.yearlyPromoDiscount.setText(discount);
            binding.yearlyPromoDiscount.setVisibility(View.VISIBLE);
            binding.yearlyPlanDiscount.setVisibility(View.GONE);

            Pair<String, Double> exchangeRateWithCurrency = getExchangeRate(yearlySku);
            String originalPriceStr = plans.getOriginalPrice(yearlySku);

            if (exchangeRateWithCurrency != null && originalPriceStr != null) {
                try {
                    double originalPrice = Double.parseDouble(originalPriceStr);
                    double localFullPrice = exchangeRateWithCurrency.getSecond() * originalPrice;
                    String formattedPrice = String.format(Locale.getDefault(), "%.2f", localFullPrice);
                    String currency = exchangeRateWithCurrency.getFirst();
                    SpannableString originalPriceSpan = new SpannableString(currency + " " + formattedPrice);
                    originalPriceSpan.setSpan(new StrikethroughSpan(), 0, originalPriceSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    binding.yearlyBilled.setText(TextUtils.concat(originalPriceSpan, " ", yearlyPrice, " ", getString(com.windscribe.vpn.R.string.charged_every_12_months)));
                } catch (NumberFormatException ignored) {
                }
            }
        } else {
            setMonthlyBilledAmount(yearlySku, monthlySku);
        }
    }

    private @Nullable Pair<String, Double> getExchangeRate(String sku) {
        Pair<String, Double> localPricePair = UiUtil.INSTANCE.getPriceWithCurrency(plans.getPrice(sku));
        Pair<String, Double> usdPricePair = UiUtil.INSTANCE.getPriceWithCurrency(plans.getUsdPrice(sku));
        if (localPricePair != null && usdPricePair != null) {
            Double localPrice = localPricePair.getSecond();
            Double usdPrice = usdPricePair.getSecond();
            if (localPrice != null && usdPrice != null && usdPrice > 0) {
                return new Pair<>(localPricePair.getFirst(), localPrice / usdPrice);
            }
        }
        return null;
    }

    private void setMonthlyPlan(String sku, boolean isPromo) {
        String monthlyPrice = plans.getPrice(sku);
        binding.monthlyPlanPrice.setText(monthlyPrice);
        binding.monthlyPlanContainer.setEnabled(true);
        binding.monthlyPlanContainer.setTag(sku);

        if (isPromo) {
            String discount = plans.getDiscountLabel(sku);
            binding.monthlyPromoDiscount.setText(discount);
            binding.monthlyPromoDiscount.setVisibility(View.VISIBLE);

            Pair<String, Double> exchangeRateWithCurrency = getExchangeRate(sku);
            String originalPriceStr = plans.getOriginalPrice(sku);

            if (exchangeRateWithCurrency != null && originalPriceStr != null) {
                try {
                    double originalPrice = Double.parseDouble(originalPriceStr);
                    double localFullPrice = exchangeRateWithCurrency.getSecond() * originalPrice;
                    String formattedPrice = String.format(Locale.getDefault(), "%.2f", localFullPrice);
                    String currency = exchangeRateWithCurrency.getFirst();
                    SpannableString originalPriceSpan = new SpannableString(currency + " " + formattedPrice);
                    originalPriceSpan.setSpan(new StrikethroughSpan(), 0, originalPriceSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    binding.monthlyBilled.setText(TextUtils.concat(originalPriceSpan, " ", monthlyPrice, " ", getString(com.windscribe.vpn.R.string.charged_every_month)));

                } catch (NumberFormatException ignored) {
                }
            }
        } else {
            binding.monthlyBilled.setText(getString(com.windscribe.vpn.R.string.billed_monthly));
        }
    }

    private void setMonthlyBilledAmount(String yearlySku, @Nullable String monthlySku) {
        Pair<String, Double> yearlyPriceWithCurrency = UiUtil.INSTANCE.getPriceWithCurrency(plans.getPrice(yearlySku));
        if (yearlyPriceWithCurrency != null) {
            double monthlyPrice = yearlyPriceWithCurrency.getSecond() / 12;
            String monthly = String.format(Locale.getDefault(), "%s %.2f", yearlyPriceWithCurrency.getFirst(), monthlyPrice);
            binding.yearlyBilled.setText(getString(com.windscribe.vpn.R.string.monthly_billed, monthly));

            if (monthlySku != null) {
                Pair<String, Double> monthlyPriceWithCurrency = UiUtil.INSTANCE.getPriceWithCurrency(plans.getPrice(monthlySku));
                if (monthlyPriceWithCurrency != null) {
                    double monthlyPriceWithDiscount = monthlyPriceWithCurrency.getSecond();
                    double discount = ((monthlyPriceWithDiscount - monthlyPrice) / monthlyPriceWithDiscount) * 100;
                    binding.yearlyPlanDiscount.setVisibility(View.VISIBLE);
                    binding.yearlyPlanDiscount.setText(String.format(Locale.getDefault(), "-%d%%", Math.round(discount)));
                }
            }
        } else {
            binding.yearlyBilled.setText(getString(com.windscribe.vpn.R.string.yearly_billed));
        }
    }

    private void setTermAndPolicyText() {
        String appName = getString(com.windscribe.vpn.R.string.app_name);
        String termAndPolicyText = getString(com.windscribe.vpn.R.string.terms_policy_en);
        String fullText = appName + " " + termAndPolicyText;
        Spannable spannable = new SpannableString(fullText);
        int spanStart = fullText.length() - termAndPolicyText.length();
        spannable.setSpan(
                new ForegroundColorSpan(Color.WHITE),
                spanStart,
                fullText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View textView) {
                openURLInBrowser(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_TERMS));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint textPaint) {
                textPaint.setColor(textPaint.linkColor);
                textPaint.setUnderlineText(true);
            }
        };

        // Clickable span for Policy
        ClickableSpan policySpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View textView) {
                openURLInBrowser(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_PRIVACY));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint textPaint) {
                textPaint.setColor(textPaint.linkColor);
                textPaint.setUnderlineText(true);
            }
        };
        int andIndex = fullText.indexOf("&");
        if (andIndex != -1) {
            spannable.setSpan(
                    termsSpan, spanStart, andIndex - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            spannable.setSpan(
                    policySpan, andIndex + 1, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            binding.terms.setMovementMethod(LinkMovementMethod.getInstance());
            binding.terms.setText(spannable, TextView.BufferType.SPANNABLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (upgradingFromWebsite) {
            finish();
        } else {
            if (billingType == BillingType.Google) {
                presenter.checkBillingProcessStatus();
            }
        }
    }

    @Override
    protected void onDestroy() {
        presenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public BillingType getBillingType() {
        return billingType;
    }

    @Override
    public void getProducts(List<String> skuList) {
        amazonBillingManager.getProducts(skuList);
    }

    @Override
    public void goBackToMainActivity() {
        finish();
    }

    @Override
    public void hideProgressBar() {
        ProgressDialog.hide(this);
    }

    @Override
    public boolean isBillingProcessFinished() {
        return billingProcessFinished;
    }

    @Override
    public void openUrlInBrowser(String url) {
        upgradingFromWebsite = true;
        openURLInBrowser(url);
    }

    @Override
    public void onPurchaseCancelled() {
        logger.info("Setting billing process finished...");
        billingProcessFinished = true;
    }

    @Override
    public void onPurchaseSuccessful(@Nullable List<Purchase> purchases) {
        if (purchases != null) {
            Purchase purchase = purchases.get(0);
            if (selectedProductDetails != null && selectedProductDetails.getOneTimePurchaseOfferDetails() != null) {
                googleBillingManager.InAppConsume(purchase);
            } else {
                googleBillingManager.subscriptionConsume(purchase);
            }
        }
    }

    @Override
    public void querySkuDetails(List<QueryProductDetailsParams.Product> products) {
        logger.info("Querying sku details...");
        googleBillingManager.querySkuDetailsAsync(products);
    }

    @Override
    public void restorePurchase() {
        amazonBillingManager.getPurchaseHistory();
    }

    @Override
    public void setBillingProcessStatus(boolean bProcessFinished) {
        billingProcessFinished = bProcessFinished;
    }

    @Override
    public void setupPlans(final WindscribeInAppProduct windscribeInAppProduct) {
        if (windscribeInAppProduct.getSkus().isEmpty()) return;
        this.plans = windscribeInAppProduct;
        String monthly = plans.getMonthlyPlan();
        String yearly = plans.getYearlyPlan();
        String promo = plans.getPromoPlan();
        if (promo != null) {
            if (monthly != null) {
                hideYearlyView();
                setMonthlyPlan(monthly, true);
                setActivePlan(true);
            } else {
                hideMonthlyView();
                setYearlyPlan(yearly, null, true);
                setActivePlan(false);
            }
            binding.yearlyPlanSelection.setVisibility(View.GONE);
            binding.monthlyPlanSelection.setVisibility(View.GONE);
        } else {
            if (monthly != null) setMonthlyPlan(monthly, false);
            if (yearly != null) setYearlyPlan(yearly, monthly, false);
            setActivePlan(true);
        }
    }

    @Override
    public void showBillingError(String errorMessage) {
        hideProgressBar();
        ErrorDialog.show(this, errorMessage, getResources().getColor(R.color.colorDeepNavy80), true);
    }

    @Override
    public void showProgressBar(final String progressHeaderText) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ProgressDialog.tag);
        if (!(fragment instanceof ProgressDialog)) {
            ProgressDialog.show(this, progressHeaderText, getResources().getColor(R.color.colorDeepNavy80));
        }
        if (fragment instanceof ProgressDialog) {
            ((ProgressDialog) fragment).updateProgressStatus(progressHeaderText);
        }
    }

    @Override
    public void showToast(String toastText) {
        logger.info("Showing toast to the user...");
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void startPurchaseFlow(ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParams, String accountID) {
        BillingFlowParams.Builder builder = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParams);
        if (accountID != null) {
            builder.setObfuscatedAccountId(accountID);
        }
        logger.info("Launching billing flow...");
        presenter.setPurchaseFlowState(PurchaseState.IN_PROCESS);
        googleBillingManager.launchBillingFlow(this, builder.build());
    }

    @Override
    public void startPurchaseFlow(final Product product) {
        amazonBillingManager.launchPurchaseFlow(product);
    }

    void initAmazonBillingLifecycleListeners() {
        amazonBillingManager.onBillingSetUpSuccess.observe(this, code -> {
            logger.info("Billing client connected successfully...");
            presenter.onBillingSetupSuccessful();
        });

        amazonBillingManager.onProductsResponseSuccess
                .observe(this, products -> presenter.onProductDataResponse(products));

        amazonBillingManager.onProductsResponseFailure
                .observe(this, requestStatus -> presenter.onProductResponseFailure());

        amazonBillingManager.onPurchaseResponseSuccess
                .observe(this, purchaseResponse -> presenter.onPurchaseResponse(purchaseResponse));

        amazonBillingManager.onPurchaseResponseFailure
                .observe(this, requestStatus -> presenter.onPurchaseResponseFailure(requestStatus));

        amazonBillingManager.onAmazonPurchaseHistorySuccess
                .observe(this, purchases -> presenter.onAmazonPurchaseHistorySuccess(purchases));

        amazonBillingManager.onAmazonPurchaseHistoryError
                .observe(this, error -> presenter.onAmazonPurchaseHistoryError(error));
    }

    void initBillingLifecycleListeners() {
        googleBillingManager.onBillingSetUpSuccess.observe(this, code -> {
            logger.info("Billing client connected successfully...");
            presenter.onBillingSetupSuccessful();
        });
        googleBillingManager.onBillingSetupFailure.observe(this, code -> {
            logger.info("Billing client set up failure...");
            presenter.onBillingSetupFailed(code);
        });

        googleBillingManager.onProductConsumeSuccess.observe(this, purchase -> {
            logger.info("Product consumption successful...");
            showToast(getResources().getString(com.windscribe.vpn.R.string.purchase_successful));
            presenter.onPurchaseConsumed(purchase);
        });

        googleBillingManager.onProductConsumeFailure.observe(this, customPurchase -> {
            logger.debug("Product consumption failed...");
            presenter.onConsumeFailed(customPurchase.getResponseCode(), customPurchase.getPurchase());
        });

        googleBillingManager.purchaseUpdateEvent.observe(this, customPurchases -> {
            logger.info("Purchase updated...");
            presenter.onPurchaseUpdated(customPurchases.getResponseCode(), customPurchases.getPurchase());
        });
        googleBillingManager.querySkuDetailEvent.observe(this, customSkuDetails -> presenter
                .onSkuDetailsReceived(customSkuDetails.getBillingResult().getResponseCode(),
                        customSkuDetails.getProductDetails()));

    }

    private void setBillingType() {
        PackageManager pkgManager = getPackageManager();
        String installerPackageName = pkgManager.getInstallerPackageName(getPackageName());
        if (installerPackageName != null && installerPackageName.startsWith("com.amazon")) {
            billingType = BillingType.Amazon;
        } else {
            billingType = BillingType.Google;
        }
    }

    @Override
    public void goToSuccessfulUpgrade(Boolean isGhostAccount) {
        Intent startIntent = new Intent(this, UpgradeSuccessActivity.class);
        startIntent.putExtra("isGhostAccount", isGhostAccount);
        startActivity(startIntent);
        finish();
    }

    public enum BillingType {
        Google("android"), Amazon("amazon");

        private final String value;

        BillingType(String value) {
            this.value = value;
        }

        @SuppressWarnings("unused")
        public String getValue() {
            return value;
        }
    }
}
