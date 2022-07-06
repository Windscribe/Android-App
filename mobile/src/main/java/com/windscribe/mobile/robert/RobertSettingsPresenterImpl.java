/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.robert;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.windscribe.mobile.R;
import com.windscribe.mobile.adapter.RobertAdapterListener;
import com.windscribe.mobile.adapter.RobertSetting;
import com.windscribe.mobile.adapter.RobertSettingsAdapter;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.api.CreateHashMap;
import com.windscribe.vpn.api.response.ApiErrorResponse;
import com.windscribe.vpn.api.response.GenericResponseClass;
import com.windscribe.vpn.api.response.GenericSuccess;
import com.windscribe.vpn.api.response.RobertSettingsResponse;
import com.windscribe.vpn.api.response.WebSession;
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import com.windscribe.vpn.exceptions.WindScribeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class RobertSettingsPresenterImpl implements RobertSettingsPresenter, RobertAdapterListener {

    private final Logger mPresenterLog = LoggerFactory.getLogger("robert_p");

    private RobertSettingsAdapter mRobertSettingsAdapter;

    private final ActivityInteractor mRobertSettingsInteractor;

    private final RobertSettingsView mRobertSettingsView;

    public RobertSettingsPresenterImpl(RobertSettingsView robertSettingsView,
            ActivityInteractor activityInteractor) {
        this.mRobertSettingsView = robertSettingsView;
        this.mRobertSettingsInteractor = activityInteractor;
    }

    @Override
    public void onDestroy() {
        mRobertSettingsInteractor.getCompositeDisposable().clear();
    }

    @Override
    public String getSavedLocale() {
        String selectedLanguage = mRobertSettingsInteractor.getAppPreferenceInterface().getSavedLanguage();
        return selectedLanguage.substring(selectedLanguage.indexOf("(") + 1, selectedLanguage.indexOf(")"));
    }

    @Override
    public void init() {
        mRobertSettingsView.setTitle(mRobertSettingsInteractor.getResourceString(R.string.robert));
        loadSettings();
    }

    @Override
    public void onCustomRulesClick() {
        mRobertSettingsView.setWebSessionLoading(true);
        mPresenterLog.info("Opening robert rules page in browser...");
        Map<String, String> webSessionMap = CreateHashMap.INSTANCE.createWebSessionMap();
        mRobertSettingsInteractor.getCompositeDisposable()
                .add(mRobertSettingsInteractor.getApiCallManager().getWebSession(webSessionMap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleWebSessionResponse, this::handleWebSessionError));
    }

    @Override
    public void onLearnMoreClick() {
        mRobertSettingsView.openUrl(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.ROBERT));
    }

    @Override
    public void setTheme(Context context) {
        String savedThem = mRobertSettingsInteractor.getAppPreferenceInterface().getSelectedTheme();
        if (savedThem.equals(PreferencesKeyConstants.DARK_THEME)) {
            context.setTheme(R.style.DarkTheme);
        } else {
            context.setTheme(R.style.LightTheme);
        }
    }

    @Override
    public void settingChanged(@NonNull List<RobertSetting> originalList, @NonNull final List<String> enabledFilters,
            int position) {
        mRobertSettingsAdapter.setSettingUpdateInProgress(true);
        mRobertSettingsView.showProgress();
        Map<String, String> paramMap = new HashMap<>();
        for (String filter : enabledFilters) {
            paramMap.put(String.format("blocklists[%s]", filter), filter);
        }
        mPresenterLog.debug(String.format("Changing robert setting list to %S", paramMap.toString()));
        mRobertSettingsInteractor.getCompositeDisposable()
                .add(mRobertSettingsInteractor.getApiCallManager().updateRobertSettings(paramMap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> handleRobertSettingUpdateResponse(response, originalList, position),
                                throwable -> handleRobertSettingsUpdateError("Failed to update Robert rules.",
                                        originalList, position)
                        ));
    }

    private List<RobertSetting> apiResponseToRobertSettings(RobertSettingsResponse response) {
        List<RobertSetting> robertSettings = new ArrayList<>();
        for (String filter : Objects.requireNonNull(response.getList())) {
            robertSettings
                    .add(new RobertSetting(filter, Objects.requireNonNull(response.getSettings()).contains(filter)));
        }
        return robertSettings;
    }

    private void handleRobertLoadSettingResponse(List<RobertSetting> robertSettings) {
        mRobertSettingsView.hideProgress();
        mRobertSettingsAdapter = new RobertSettingsAdapter(this);
        mRobertSettingsAdapter.setData(robertSettings);
        mRobertSettingsView.setAdapter(mRobertSettingsAdapter);
    }

    private void handleRobertSettingUpdateResponse(GenericResponseClass<GenericSuccess, ApiErrorResponse> response,
            @NonNull List<RobertSetting> originalList, int position) {
        mRobertSettingsAdapter.setSettingUpdateInProgress(false);
        if (response.getDataClass() != null) {
            mRobertSettingsView.hideProgress();
            mRobertSettingsView.showToast("Successfully updated Robert rules.");
            Windscribe.getAppContext().getWorkManager().updateRobertRules();
        } else if (response.getErrorClass() != null) {
            handleRobertSettingsUpdateError(response.getErrorClass().getErrorMessage(), originalList, position);
        } else {
            handleRobertSettingsUpdateError("Failed to update Robert rules.", originalList, position);
        }
    }

    private void handleRobertSettingsUpdateError(String error, List<RobertSetting> originalList, int position) {
        mRobertSettingsAdapter.setSettingUpdateInProgress(false);
        mRobertSettingsView.hideProgress();
        mRobertSettingsView.showToast(error);
        mRobertSettingsAdapter.setData(originalList);
        mRobertSettingsAdapter.notifyItemChanged(position);
    }

    private void handleWebSessionError(Throwable throwable) {
        mPresenterLog.debug(String.format("Failed to generate web session: %s", throwable.getLocalizedMessage()));
        mRobertSettingsView.setWebSessionLoading(false);
        mRobertSettingsView.showErrorDialog("Failed to generate web session. Check your network connection.");
    }

    private void handleWebSessionResponse(GenericResponseClass<WebSession, ApiErrorResponse> response) {
        mRobertSettingsView.setWebSessionLoading(false);
        if (response.getDataClass() != null) {
            mRobertSettingsView.openUrl(responseToUrl(response.getDataClass()));
        } else if (response.getErrorClass() != null) {
            mPresenterLog.debug(String
                    .format("Failed to generate web session: %s", response.getErrorClass().getErrorDescription()));
            mRobertSettingsView.showErrorDialog(response.getErrorClass().getErrorMessage());
        } else {
            mRobertSettingsView.showErrorDialog("Failed to generate Web-Session. Check your network connection.");
        }
    }

    private Single<List<RobertSetting>> loadFromDatabase(Throwable throwable) throws WindScribeException {
        String json = mRobertSettingsInteractor.getAppPreferenceInterface()
                .getResponseString(PreferencesKeyConstants.ROBERT_SETTINGS);
        if (json == null) {
            throw new WindScribeException(throwable.getLocalizedMessage());
        }
        return Single.just(new Gson().fromJson(json, new TypeToken<List<RobertSetting>>() {
        }.getType()));
    }

    private void loadSettings() {
        mRobertSettingsView.showProgress();
        mRobertSettingsInteractor.getCompositeDisposable().add(
                mRobertSettingsInteractor.getApiCallManager().getRobertSettings(null)
                        .flatMap(
                                (Function<GenericResponseClass<RobertSettingsResponse, ApiErrorResponse>, SingleSource<List<RobertSetting>>>) this::saveToDatabase)
                        .onErrorResumeNext(this::loadFromDatabase)
                        .subscribeOn(Schedulers.io())
                        .delaySubscription(1, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleRobertLoadSettingResponse, throwable -> {
                                    mRobertSettingsView.hideProgress();
                                    mRobertSettingsView
                                            .showError("Failed to load to Robert settings. Check your network connection.");
                                }
                        ));
    }

    private String responseToUrl(WebSession webSession) {
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(NetworkKeyConstants.WEB_URL.replace("https://", ""))
                .path("myaccount")
                .fragment("robertrules")
                .appendQueryParameter("temp_session", webSession.getTempSession())
                .build();
        return uri.toString();
    }

    private Single<List<RobertSetting>> saveToDatabase(
            GenericResponseClass<RobertSettingsResponse, ApiErrorResponse> response)
            throws WindScribeException {
        if (response.getDataClass() != null) {
            List<RobertSetting> robertSettings = apiResponseToRobertSettings(response.getDataClass());
            String json = new Gson().toJson(robertSettings);
            mRobertSettingsInteractor.getAppPreferenceInterface()
                    .saveResponseStringData(PreferencesKeyConstants.ROBERT_SETTINGS, json);
            return Single.just(robertSettings);
        }
        if (response.getErrorClass() != null) {
            throw new WindScribeException(response.getErrorClass().getErrorMessage());
        }
        throw new WindScribeException("Unexpected Api response.");
    }
}
