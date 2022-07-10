/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome;

import static com.windscribe.vpn.constants.UserStatusConstants.USER_STATUS_PREMIUM;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.installations.FirebaseInstallations;
import com.windscribe.mobile.BuildConfig;
import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.api.CreateHashMap;
import com.windscribe.vpn.api.response.ApiErrorResponse;
import com.windscribe.vpn.api.response.ClaimAccountResponse;
import com.windscribe.vpn.api.response.GenericResponseClass;
import com.windscribe.vpn.api.response.RegToken;
import com.windscribe.vpn.api.response.UserLoginResponse;
import com.windscribe.vpn.api.response.UserRegistrationResponse;
import com.windscribe.vpn.constants.NetworkErrorCodes;
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.constants.UserStatusConstants;
import com.windscribe.vpn.errormodel.SessionErrorHandler;
import com.windscribe.vpn.errormodel.WindError;
import com.windscribe.vpn.model.User;
import com.windscribe.vpn.services.ping.PingTestService;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class WelcomePresenterImpl implements WelcomePresenter {

    private final ActivityInteractor interactor;

    private final Logger mPresenterLog = LoggerFactory.getLogger("login-p");

    private final WelcomeView welcomeView;
    @Inject
    public WelcomePresenterImpl(WelcomeView welcomeView, ActivityInteractor activityInteractor
    ) {
        this.welcomeView = welcomeView;
        this.interactor = activityInteractor;
    }

    @Override
    public void onDestroy() {
        interactor.getCompositeDisposable().clear();
    }

    @Override
    public void exportLog() {
        try {
            File file = new File(interactor.getDebugFilePath());
            welcomeView.launchShareIntent(file);
        } catch (Exception e) {
            welcomeView.showToast(e.toString());
        }
    }

    @Override
    public boolean isUserPro() {
        return interactor.getAppPreferenceInterface().getUserStatus() == USER_STATUS_PREMIUM;
    }

    @Override
    public void onBackPressed() {
        interactor.getCompositeDisposable().clear();
        welcomeView.hideSoftKeyboard();
    }

    @Override
    public void printStartUri(Uri uri) {
        if (uri != null) {
            mPresenterLog.debug(uri.toString());
        }
    }

    @Override
    public void startAccountClaim(String username, String password, String email, boolean ignoreEmptyEmail) {
        welcomeView.hideSoftKeyboard();
        if (validateLoginInputs(username, password, email, false)) {
            if (!ignoreEmptyEmail && email.isEmpty()) {
                boolean proUser = interactor.getAppPreferenceInterface().getUserStatus()
                        == UserStatusConstants.USER_STATUS_PREMIUM;
                welcomeView.showNoEmailAttentionFragment(username, password, true, proUser);
                return;
            }
            mPresenterLog.info("Trying to claim account with provided credentials...");
            welcomeView.prepareUiForApiCallFinished();
            welcomeView.prepareUiForApiCallStart();
            Map<String, String> loginMap = CreateHashMap.INSTANCE.createClaimAccountMap(username, password);
            if (!email.isEmpty()) {
                loginMap.put(NetworkKeyConstants.ADD_EMAIL_KEY, email);
            }
            interactor.getCompositeDisposable().add(
                    interactor.getApiCallManager()
                            .claimAccount(loginMap)
                            .doOnSubscribe(disposable -> welcomeView.updateCurrentProcess("Signing up"))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(
                                    new DisposableSingleObserver<GenericResponseClass<ClaimAccountResponse, ApiErrorResponse>>() {
                                        @Override
                                        public void onError(@NotNull Throwable e) {
                                            mPresenterLog.debug("User SignUp error..." + e.getMessage());
                                            onSignUpFailedWithNoError();
                                        }

                                        @Override
                                        public void onSuccess(
                                                @NotNull GenericResponseClass<ClaimAccountResponse, ApiErrorResponse> genericLoginResponse) {
                                            if (genericLoginResponse.getDataClass() != null) {
                                                mPresenterLog.info("Account claimed successfully...");
                                                welcomeView.updateCurrentProcess("SignUp successful...");
                                                onAccountClaimSuccess(username);
                                            } else if (genericLoginResponse.getErrorClass() != null) {
                                                mPresenterLog.info("Account claim..." + genericLoginResponse
                                                        .getErrorClass());
                                                onLoginResponseError(genericLoginResponse.getErrorClass());
                                            } else {
                                                onSignUpFailedWithNoError();
                                            }
                                        }
                                    })
            );
        }
    }

    @Override
    public void startGhostAccountSetup() {
        welcomeView.prepareUiForApiCallStart();
        welcomeView.updateCurrentProcess("Signing In");
        interactor.getCompositeDisposable().add(interactor.getApiCallManager().getReg(null)
                .flatMap(
                        (Function<GenericResponseClass<RegToken, ApiErrorResponse>, SingleSource<GenericResponseClass<UserRegistrationResponse, ApiErrorResponse>>>) regToken -> {
                            if (regToken.getDataClass() != null) {
                                Map<String, String> ghostModeMap = CreateHashMap.INSTANCE.createGhostModeMap(regToken.getDataClass().getToken());
                                return interactor.getApiCallManager().signUserIn(ghostModeMap);
                            } else if (regToken.getErrorClass() != null) {
                                throw new Exception(regToken.getErrorClass().getErrorMessage());
                            } else {
                                throw new Exception("Unknown Error");
                            }
                        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                        new DisposableSingleObserver<GenericResponseClass<UserRegistrationResponse, ApiErrorResponse>>() {
                            @Override
                            public void onError(@NotNull Throwable e) {
                                welcomeView.prepareUiForApiCallFinished();
                                if (e instanceof IOException) {
                                    welcomeView.showError("Unable to reach server. Check your network connection.");
                                } else {
                                    mPresenterLog.debug(e.getMessage());
                                    welcomeView.goToSignUp();
                                }
                            }

                            @Override
                            public void onSuccess(
                                    @NotNull GenericResponseClass<UserRegistrationResponse, ApiErrorResponse> regResponse) {
                                if (regResponse.getErrorClass() != null) {
                                    mPresenterLog.debug(regResponse.getErrorClass().getErrorMessage());
                                    welcomeView.prepareUiForApiCallFinished();
                                    welcomeView.goToSignUp();
                                } else {
                                    interactor.getAppPreferenceInterface().setSessionHash(regResponse.getDataClass().getSessionAuthHash());
                                    getAndSetFireBaseDeviceToken();
                                }
                            }
                        }));
    }

    @Override
    public void startLoginProcess(String username, String password, String twoFa) {
        welcomeView.hideSoftKeyboard();
        if (validateLoginInputs(username, password, "", true)) {
            mPresenterLog.info("Trying to login with provided credentials...");
            welcomeView.prepareUiForApiCallStart();
            Map<String, String> loginMap = CreateHashMap.INSTANCE.createLoginMap(username, password, twoFa);
            interactor.getCompositeDisposable().add(
                    interactor.getApiCallManager()
                            .logUserIn(loginMap)
                            .doOnSubscribe(disposable -> welcomeView.updateCurrentProcess("Signing in..."))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(
                                    new DisposableSingleObserver<GenericResponseClass<UserLoginResponse, ApiErrorResponse>>() {
                                        @Override
                                        public void onError(@NotNull Throwable e) {
                                            if(e instanceof Exception){
                                                mPresenterLog.debug("Login Error: "+WindError.getInstance().rxErrorToString((Exception)e));
                                            }
                                            onLoginFailedWithNoError();
                                        }

                                        @Override
                                        public void onSuccess(
                                                @NotNull GenericResponseClass<UserLoginResponse, ApiErrorResponse> genericLoginResponse) {
                                            if (genericLoginResponse.getDataClass() != null) {
                                                mPresenterLog.info("Logged user in successfully...");
                                                welcomeView.updateCurrentProcess("Login successful...");
                                                interactor.getAppPreferenceInterface().setSessionHash(genericLoginResponse.getDataClass().getSessionAuthHash());
                                                getAndSetFireBaseDeviceToken();
                                            } else if (genericLoginResponse.getErrorClass() != null) {
                                                mPresenterLog.info("Login error..." + genericLoginResponse
                                                        .getErrorClass());
                                                onLoginResponseError(genericLoginResponse.getErrorClass());
                                            } else {
                                                onLoginFailedWithNoError();
                                            }
                                        }
                                    })
            );
        }
    }

    @Override
    public void startSignUpProcess(String username, String password, String email, boolean ignoreEmptyEmail) {
        welcomeView.hideSoftKeyboard();
        if (validateLoginInputs(username, password, email, false)) {
            if (!ignoreEmptyEmail && email.isEmpty()) {
                welcomeView.showNoEmailAttentionFragment(username, password, false, false);
                return;
            }
            mPresenterLog.info("Trying to sign up with provided credentials...");
            welcomeView.prepareUiForApiCallStart();
            Map<String, String> loginMap = CreateHashMap.INSTANCE.createRegistrationMap(username, password);
            if (!email.isEmpty()) {
                loginMap.put(NetworkKeyConstants.ADD_EMAIL_KEY, email);
            }

            interactor.getCompositeDisposable().add(
                    interactor.getApiCallManager()
                            .signUserIn(loginMap)
                            .doOnSubscribe(disposable -> welcomeView.updateCurrentProcess("Signing up"))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(
                                    new DisposableSingleObserver<GenericResponseClass<UserRegistrationResponse, ApiErrorResponse>>() {
                                        @Override
                                        public void onError(@NotNull Throwable e) {
                                            mPresenterLog.debug("User SignUp error..." + e.getMessage());
                                            onSignUpFailedWithNoError();
                                        }

                                        @Override
                                        public void onSuccess(
                                                @NotNull GenericResponseClass<UserRegistrationResponse, ApiErrorResponse> genericLoginResponse) {
                                            if (genericLoginResponse.getDataClass() != null) {
                                                mPresenterLog.info("Sign up user successfully...");
                                                welcomeView.updateCurrentProcess("SignUp successful...");
                                                interactor.getAppPreferenceInterface().setSessionHash(genericLoginResponse.getDataClass().getSessionAuthHash());
                                                getAndSetFireBaseDeviceToken();
                                            } else if (genericLoginResponse.getErrorClass() != null) {
                                                mPresenterLog
                                                        .info("SignUp..." + genericLoginResponse.getErrorClass());
                                                onLoginResponseError(genericLoginResponse.getErrorClass());
                                            } else {
                                                onSignUpFailedWithNoError();
                                            }
                                        }
                                    })
            );
        }
    }

    private boolean evaluatePassword(String password) {
        String pattern = "(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}";
        return password.matches(pattern);
    }

    private void getAndSetFireBaseDeviceToken() {
        Map<String,String> sessionMap = new HashMap<>();
        if(BuildConfig.API_KEY.isEmpty()){
            prepareLoginRegistrationDashboard(sessionMap);
        }else{
            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                try {
                String newToken = instanceIdResult.getToken();
                mPresenterLog.debug(""+newToken.length());
                mPresenterLog.info("Received firebase device token.");
                mPresenterLog.info(newToken);
                sessionMap.put(NetworkKeyConstants.FIREBASE_DEVICE_ID_KEY, newToken);
                } catch (RuntimeExecutionException e) {
                    mPresenterLog.debug("No registered account for the selected device! " + WindError.getInstance()
                            .convertErrorToString(e));
                }
                prepareLoginRegistrationDashboard(sessionMap);
            }).addOnFailureListener(e -> prepareLoginRegistrationDashboard(sessionMap)).addOnCanceledListener(() -> prepareLoginRegistrationDashboard(sessionMap));
        }
    }

    private void onAccountClaimSuccess(final String username) {
        welcomeView.updateCurrentProcess("Getting session");
        interactor.getCompositeDisposable().add(
                interactor.getApiCallManager().getSessionGeneric(null)
                        .flatMapCompletable(sessionResponse -> Completable.fromSingle(Single.fromCallable(() -> {
                            interactor.getUserRepository().reload(sessionResponse.getDataClass(),null);
                            return true;
                        }))).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableCompletableObserver() {
                            @Override
                            public void onComplete() {
                                welcomeView.gotoHomeActivity(true);
                            }

                            @Override
                            public void onError(@NotNull Throwable e) {
                                welcomeView.prepareUiForApiCallFinished();
                                welcomeView.showError("Unable to auto login. Log in using new credentials.");
                                mPresenterLog.debug("Error getting session"
                                        + WindError.getInstance().convertThrowableToString(e));
                            }
                        }));
    }

    private void onLoginFailedWithNoError() {
        welcomeView.prepareUiForApiCallFinished();
        welcomeView.showFailedAlert(interactor.getResourceString(R.string.failed_network_alert));
    }

    private void onLoginResponseError(ApiErrorResponse apiErrorResponse) {
        mPresenterLog.debug(apiErrorResponse.toString());
        welcomeView.prepareUiForApiCallFinished();
        String errorMessage = SessionErrorHandler.getInstance().getErrorMessage(apiErrorResponse);
        int errorCode = apiErrorResponse.getErrorCode();
        if (errorCode == NetworkErrorCodes.ERROR_2FA_REQUIRED | errorCode == NetworkErrorCodes.ERROR_INVALID_2FA) {
            welcomeView.setFaFieldsVisibility(View.VISIBLE);
            welcomeView.setTwoFaError(errorMessage);
        } else if (errorCode == NetworkErrorCodes.ERROR_USER_NAME_ALREADY_TAKEN
                | errorCode == NetworkErrorCodes.ERROR_USER_NAME_ALREADY_IN_USE) {
            welcomeView.setUsernameError(errorMessage);
        } else if (errorCode == NetworkErrorCodes.ERROR_EMAIL_ALREADY_EXISTS
                | errorCode == NetworkErrorCodes.ERROR_DISPOSABLE_EMAIL) {
            welcomeView.setEmailError(errorMessage);
        } else {
            welcomeView.setLoginRegistrationError(errorMessage);
        }
    }

    private void onSignUpFailedWithNoError() {
        welcomeView.prepareUiForApiCallFinished();
        welcomeView.showFailedAlert(interactor.getResourceString(R.string.sign_up_failed_network_alert));
    }

    private void prepareLoginRegistrationDashboard(Map<String, String> sessionMap) {
        if (interactor == null) {
            return;
        }
        welcomeView.updateCurrentProcess("Getting session");
        interactor.getCompositeDisposable().add(
                interactor.getApiCallManager().getSessionGeneric(sessionMap)
                        .flatMapCompletable(sessionResponse -> Completable.fromSingle(Single.fromCallable(() -> {
                            if (interactor.getAppPreferenceInterface().getDeviceUUID(sessionResponse.getDataClass().getUserName()) == null) {
                                mPresenterLog.debug("No device id is found for the current user, generating and saving UUID");
                                interactor.getAppPreferenceInterface().setDeviceUUID(sessionResponse.getDataClass().getUserName(), UUID.randomUUID().toString());
                            }
                            interactor.getUserRepository().reload(sessionResponse.getDataClass(),null);
                            return true;
                        }))).andThen(updateStaticIps())
                        .doOnComplete(() -> welcomeView.updateCurrentProcess("Getting user credentials"))
                        .andThen(interactor.getConnectionDataUpdater().update())
                        .doOnComplete(() -> welcomeView.updateCurrentProcess("Getting server list"))
                        .andThen(interactor.getServerListUpdater().update())
                        .andThen(Completable.fromAction(interactor.getPreferenceChangeObserver()::postCityServerChange))
                        .andThen(interactor.updateUserData())
                        .onErrorResumeNext(throwable -> {
                            mPresenterLog.info("*****Preparing dashboard failed: " + throwable.toString()
                                    + " Use reload button in server list in home activity.");
                            return Completable.fromAction(interactor.getPreferenceChangeObserver()::postCityServerChange)
                                    .andThen(interactor.updateUserData());
                        }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableCompletableObserver() {
                            @Override
                            public void onComplete() {
                                interactor.getWorkManager().onAppStart();
                                interactor.getWorkManager().onAppMovedToForeground();
                                PingTestService.startPingTestService();
                                interactor.getWorkManager().onAppStart();
                                welcomeView.gotoHomeActivity(true);
                            }

                            @Override
                            public void onError(@NotNull Throwable e) {
                                welcomeView.prepareUiForApiCallFinished();
                                mPresenterLog.debug("Error while updating server status to local db. StackTrace: "
                                        + WindError.getInstance().convertThrowableToString(e));
                            }
                        }));
    }

    private Completable updateStaticIps() {
        User user = interactor.getUserRepository().getUser().getValue();
        if (user!=null && user.getSipCount() > 0) {
            return interactor.getStaticListUpdater().update();
        } else {
            return Completable.fromAction(() -> {
            });
        }
    }

    private boolean validateLoginInputs(String username, String password, String email,
            boolean isLogin) {
        mPresenterLog.info("Validating login credentials");
        welcomeView.clearInputErrors();

        //Empty username
        if (TextUtils.isEmpty(username)) {
            mPresenterLog.info("[username] is empty, displaying toast to the user...");
            welcomeView.setUsernameError(interactor.getResourceString(R.string.username_empty));
            welcomeView.showToast(interactor.getResourceString(R.string.enter_username));
            return false;
        }

        //Invalid username
        if (!validateUsernameCharacters(username)) {
            mPresenterLog.info("[username] has invalid characters in , displaying toast to the user...");
            welcomeView.setUsernameError(interactor.getResourceString(R.string.login_with_username));
            welcomeView.showToast(interactor.getResourceString(R.string.login_with_username));
            return false;
        }

        //Empty Password
        if (TextUtils.isEmpty(password)) {
            mPresenterLog.info("[password] is empty, displaying toast to the user...");
            welcomeView.setPasswordError(interactor.getResourceString(R.string.password_empty));
            welcomeView.showToast(interactor.getResourceString(R.string.enter_password));
            return false;
        }

        if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mPresenterLog.info("[Email] is invalid, displaying toast to the user...");
            welcomeView.setEmailError(interactor.getResourceString(R.string.invalid_email_format));
            welcomeView.showToast(interactor.getResourceString(R.string.invalid_email_format));
            return false;
        }
        if (!isLogin && password.length() < 8) {
            mPresenterLog.info("[Password] is small, displaying toast to the user...");
            welcomeView.setPasswordError(interactor.getResourceString(R.string.small_password));
            welcomeView.showToast(interactor.getResourceString(R.string.small_password));
            return false;
        }
        // Sign up and claim account password minimum strength enforce.
        if (!isLogin && !evaluatePassword(password)) {
            mPresenterLog.info("[Password] is weak, displaying toast to the user...");
            welcomeView.setPasswordError(interactor.getResourceString(R.string.weak_password));
            welcomeView.showToast(interactor.getResourceString(R.string.weak_password));
            return false;
        }

        return true;
    }

    private boolean validateUsernameCharacters(String username) {
        return Pattern.matches("[a-zA-Z0-9_-]*", username);
    }
}
