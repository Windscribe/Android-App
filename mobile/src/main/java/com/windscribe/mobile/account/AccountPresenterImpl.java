/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.account;


import android.content.Context;

import androidx.annotation.NonNull;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.api.CreateHashMap;
import com.windscribe.vpn.api.response.ApiErrorResponse;
import com.windscribe.vpn.api.response.GenericResponseClass;
import com.windscribe.vpn.api.response.UserSessionResponse;
import com.windscribe.vpn.api.response.VerifyExpressLoginResponse;
import com.windscribe.vpn.api.response.WebSession;
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import com.windscribe.vpn.constants.UserStatusConstants;
import com.windscribe.vpn.errormodel.WindError;
import com.windscribe.vpn.model.User;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class AccountPresenterImpl implements AccountPresenter {

    private final String TAG = "account_p";

    private ActivityInteractor mAccountInteractor;

    private AccountView mAccountView;

    private final Logger mPresenterLog = LoggerFactory.getLogger(TAG);

    @Inject
    public AccountPresenterImpl(AccountView accountView, ActivityInteractor mAccountInteractor) {
        this.mAccountView = accountView;
        this.mAccountInteractor = mAccountInteractor;
    }

    @Override
    public void onDestroy() {
        if (!mAccountInteractor.getCompositeDisposable().isDisposed()) {
            mPresenterLog.info("Disposing observer on destroy...");
            mAccountInteractor.getCompositeDisposable().dispose();
        }

        mAccountView = null;
        mAccountInteractor = null;
    }

    @Override
    public void onAddEmailClicked(String tvEmailText) {
        if (mAccountInteractor.getResourceString(R.string.add_email).equals(tvEmailText)) {
            mPresenterLog.info("Go to add Email activity");
            mAccountView.goToEmailActivity();
        } else {
            mPresenterLog.info("User already confirmed email...");
        }
    }

    @Override
    public void observeUserData(AccountActivity accountActivity){
        mAccountInteractor.getUserRepository().getUser().observe(accountActivity, this::setUserInfo);
    }

    @Override
    public void onCodeEntered(final String code) {
        mAccountView.showProgress("Verifying code...");
        mPresenterLog.debug("verifying express login code.");
        String sessionAuth = mAccountInteractor.getAppPreferenceInterface().getSessionHash();
        Map<String, String> verifyLoginMap = CreateHashMap.INSTANCE.createVerifyExpressLoginMap(code);
        mAccountInteractor.getCompositeDisposable().add(mAccountInteractor.getApiCallManager()
                .verifyExpressLoginCode(verifyLoginMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                        new DisposableSingleObserver<GenericResponseClass<VerifyExpressLoginResponse, ApiErrorResponse>>() {
                            @Override
                            public void onError(@NotNull final Throwable e) {
                                mPresenterLog.debug(String
                                        .format("Error verifying login code: %s", e.getLocalizedMessage()));
                                mAccountView.hideProgress();
                                mAccountView.showErrorDialog(
                                        "Error verifying login code. Check your network connection.");
                            }

                            @Override
                            public void onSuccess(
                                    @NotNull final GenericResponseClass<VerifyExpressLoginResponse, ApiErrorResponse> response) {
                                mAccountView.hideProgress();
                                if (response.getDataClass() != null && response.getDataClass().isSuccessful()) {
                                    mPresenterLog.debug("Successfully verified login code");
                                    mAccountView.showSuccessDialog("Sweet, you should be\n" + "all good to go now.");
                                } else if (response.getErrorClass() != null) {
                                    mPresenterLog.debug(String.format("Error verifying login code: %s",
                                            response.getErrorClass().getErrorMessage()));
                                    mAccountView.showErrorDialog(response.getErrorClass().getErrorMessage());
                                } else {
                                    mPresenterLog.debug("Failed to verify lazy login code.");
                                    mAccountView.showErrorDialog("Failed to verify lazy login code.");
                                }
                            }
                        }));

    }

    @Override
    public void onEditAccountClicked() {
        mAccountView.setWebSessionLoading(true);
        mPresenterLog.info("Opening My Account page in browser...");
        Map<String, String> webSessionMap = CreateHashMap.INSTANCE.createWebSessionMap();
        mAccountInteractor.getCompositeDisposable()
                .add(mAccountInteractor.getApiCallManager().getWebSession(webSessionMap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(
                                new DisposableSingleObserver<GenericResponseClass<WebSession, ApiErrorResponse>>() {
                                    @Override
                                    public void onError(@NonNull final Throwable e) {
                                        mAccountView.setWebSessionLoading(false);
                                        mAccountView.showErrorDialog(
                                                "Unable to generate web session. Check your network connection.");
                                    }

                                    @Override
                                    public void onSuccess(
                                            @NonNull final GenericResponseClass<WebSession, ApiErrorResponse> webSession) {
                                        mAccountView.setWebSessionLoading(false);
                                        if (webSession.getDataClass() != null) {
                                            mAccountView.openEditAccountInBrowser(
                                                    NetworkKeyConstants
                                                            .getWebsiteLink(NetworkKeyConstants.URL_MY_ACCOUNT)
                                                            + webSession.getDataClass().getTempSession());
                                        } else if (webSession.getErrorClass() != null) {
                                            mAccountView
                                                    .showErrorDialog(webSession.getErrorClass().getErrorMessage());
                                        } else {
                                            mAccountView.showErrorDialog(
                                                    "Unable to generate Web-Session. Check your network connection.");
                                        }
                                    }
                                }));
    }

    @Override
    public void onResendEmail() {
        mAccountView.goToConfirmEmailActivity();
    }

    @Override
    public void onUpgradeClicked(String textViewText) {
        if (mAccountInteractor.getResourceString(R.string.upgrade_case_normal).equals(textViewText)) {
            mPresenterLog.info("Showing upgrade dialog to the user...");
            mAccountView.openUpgradeActivity();
        } else {
            mPresenterLog.info("User is already pro no actions taken...");
        }
    }

    @Override
    public void onLazyloginClicked() {
        mAccountView.showEnterCodeDialog();
    }

    @Override
    public void setLayoutFromApiSession() {
        mAccountInteractor.getCompositeDisposable().add(mAccountInteractor.getApiCallManager()
                .getSessionGeneric(null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                        new DisposableSingleObserver<GenericResponseClass<UserSessionResponse, ApiErrorResponse>>() {
                            @Override
                            public void onError(@NotNull Throwable e) {
                                mPresenterLog.debug("Error while making get session call:" +
                                        WindError.getInstance().convertThrowableToString(e));
                            }

                            @Override
                            public void onSuccess(
                                    @NotNull GenericResponseClass<UserSessionResponse, ApiErrorResponse> userSessionResponse) {
                                if (userSessionResponse.getDataClass() != null) {
                                    mAccountInteractor.getUserRepository().reload(userSessionResponse.getDataClass(),null);
                                } else if (userSessionResponse.getErrorClass() != null) {
                                    //Server responded with error!
                                    mPresenterLog.debug("Server returned error during get session call."
                                            + userSessionResponse.getErrorClass().toString());
                                }
                            }
                        }));
    }

    @Override
    public void setTheme(Context context) {
        String savedThem = mAccountInteractor.getAppPreferenceInterface().getSelectedTheme();
        mPresenterLog.debug("Setting theme to " + savedThem);
        if (savedThem.equals(PreferencesKeyConstants.DARK_THEME)) {
            context.setTheme(R.style.DarkTheme);
        } else {
            context.setTheme(R.style.LightTheme);
        }
    }

    private void setUserInfo(User user) {
        mAccountView.setActivityTitle(mAccountInteractor.getResourceString(R.string.account));
        if (user.isGhost()) {
            mAccountView.setupLayoutForGhostMode(user.isPro());
        } else if (user.getMaxData() != -1L) {
            mAccountView.setupLayoutForFreeUser(mAccountInteractor.getResourceString(R.string.upgrade_case_normal));
        } else {
            mAccountView.setupLayoutForPremiumUser(mAccountInteractor.getResourceString(R.string.plan_pro));
        }
        mAccountView.setUsername(user.getUserName());
        switch (user.getEmailStatus()){
            case NoEmail:
                mAccountView.setEmail(mAccountInteractor.getResourceString(R.string.add_email), mAccountInteractor.getColorResource(R.color.colorNeonGreen), mAccountInteractor.getThemeColor(R.attr.wdPrimaryColor));
                break;
            case EmailProvided:
                mAccountView.setEmailConfirm(user.getEmail(), mAccountInteractor.getResourceString(R.string.confirm_your_email), mAccountInteractor.getColorResource(R.color.colorYellow50), mAccountInteractor.getColorResource(R.color.colorYellow), R.drawable.ic_warning_icon, R.drawable.attention_container_background);
                break;
            case Confirmed:
                mAccountView.setEmailConfirmed(user.getEmail(), mAccountInteractor.getResourceString(R.string.get_10gb_data), mAccountInteractor.getThemeColor(R.attr.wdSecondaryColor), mAccountInteractor.getThemeColor(R.attr.wdPrimaryColor), R.drawable.ic_email_attention, R.drawable.confirmed_email_container_background);
        }
        if(user.getMaxData() == -1L){
            mAccountView.setPlanName(mAccountInteractor.getResourceString(R.string.unlimited_data));
            mAccountView.setDataLeft("");
        }else {
            mAccountView.setPlanName(user.getMaxData() / UserStatusConstants.GB_DATA + mAccountInteractor.getResourceString(R.string.gb_per_month));
            if (user.getDataLeft() != null) {
                String dataLeft = new DecimalFormat("##.00").format(user.getDataLeft());
                mAccountView.setDataLeft(dataLeft + " GB");
            }
        }
        setExpiryOrResetDate(user);
    }

    private void setExpiryOrResetDate(User user) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = null;
        if(user.isPro() && user.getExpiryDate()!=null){
            date = user.getExpiryDate();
        }else if(user.getResetDate()!=null){
            date = user.getResetDate();
        }
        if (date!= null) {
            try {
                Date lastResetDate = formatter.parse(date);
                Calendar c = Calendar.getInstance();
                c.setTime(Objects.requireNonNull(lastResetDate));
                if (!user.isPro()) {
                    c.add(Calendar.MONTH, 1);
                    Date nextResetDate = c.getTime();
                    mAccountView.setResetDate(mAccountInteractor.getResourceString(R.string.reset_date),
                            formatter.format(nextResetDate));
                } else {
                    Date nextResetDate = c.getTime();
                    mAccountView.setResetDate(mAccountInteractor.getResourceString(R.string.expiry_date),
                            formatter.format(nextResetDate));

                }
            } catch (ParseException e) {
                mPresenterLog.debug("Could not parse date data. " + WindError.getInstance().convertErrorToString(e));
            }
        }
    }
}
