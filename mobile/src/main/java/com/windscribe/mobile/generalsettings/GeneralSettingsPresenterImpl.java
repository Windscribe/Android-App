/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.generalsettings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.commonutils.WindUtilities;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;


public class GeneralSettingsPresenterImpl implements GeneralSettingsPresenter {

    private static final String TAG = "gen_settings_p";

    private ActivityInteractor mGeneralSettingsInteractor;

    private GeneralSettingsView mGeneralSettingsView;

    private final Logger mPresenterLog = LoggerFactory.getLogger(TAG);

    @Inject
    public GeneralSettingsPresenterImpl(GeneralSettingsView mGeneralSettingsView, ActivityInteractor activityInteractor) {
        this.mGeneralSettingsView = mGeneralSettingsView;
        this.mGeneralSettingsInteractor = activityInteractor;
    }

    @Override
    public void onDestroy() {
        //Dispose any composite disposable
        mGeneralSettingsInteractor.getCompositeDisposable();
        if (!mGeneralSettingsInteractor.getCompositeDisposable().isDisposed()) {
            mPresenterLog.info("Disposing observer...");
            mGeneralSettingsInteractor.getCompositeDisposable().dispose();
        }
        mGeneralSettingsView = null;
        mGeneralSettingsInteractor = null;
    }

    @Override
    public String getSavedLocale() {
        String selectedLanguage = mGeneralSettingsInteractor.getAppPreferenceInterface().getSavedLanguage();
        return selectedLanguage.substring(selectedLanguage.indexOf("(") + 1, selectedLanguage.indexOf(")"));
    }

    @Override
    public void onConnectedFlagEditClicked(final int requestCode) {
        mGeneralSettingsView.openFileChooser(requestCode);
    }

    @Override
    public void onConnectedFlagPathPicked(final String path) {
        String lastPath = mGeneralSettingsInteractor.getAppPreferenceInterface().getConnectedFlagPath();
        if (lastPath != null) {
            File file = new File(Windscribe.getAppContext().getFilesDir(), lastPath);
            if (file.exists()) {
                boolean success = file.delete();
                if (success) {
                    mGeneralSettingsInteractor.getAppPreferenceInterface().setConnectedFlagPath(null);
                }
            }
        }
        mGeneralSettingsInteractor.getAppPreferenceInterface().setConnectedFlagPath(path);
        mGeneralSettingsView.setConnectedFlagPath(path);
    }

    @Override
    public void onCustomFlagToggleButtonClicked(String value) {
        boolean newValue = value.equals("Custom");
        if(newValue != mGeneralSettingsInteractor.getAppPreferenceInterface().isCustomBackground()){
            setAppBackground(newValue);
        }
    }

    @Override
    public void onDisConnectedFlagPathPicked(final String path) {
        String lastPath = mGeneralSettingsInteractor.getAppPreferenceInterface().getDisConnectedFlagPath();
        if (lastPath != null) {
            File file = new File(Windscribe.getAppContext().getFilesDir(), lastPath);
            if (file.exists()) {
                boolean success = file.delete();
                if (success) {
                    mGeneralSettingsInteractor.getAppPreferenceInterface().setDisconnectedFlagPath(null);
                }
            }
        }
        mGeneralSettingsInteractor.getAppPreferenceInterface().setDisconnectedFlagPath(path);
        mGeneralSettingsView.setDisconnectedFlagPath(path);
    }

    @Override
    public void onDisconnectedFlagEditClicked(final int requestCode) {
        mGeneralSettingsView.openFileChooser(requestCode);
    }

    @Override
    public void onHapticToggleButtonClicked() {
        if (mGeneralSettingsInteractor.getAppPreferenceInterface().isHapticFeedbackEnabled()) {
            mPresenterLog.info("Previous  haptic Toggle Settings: True");
            mGeneralSettingsInteractor.getAppPreferenceInterface().setHapticFeedbackEnabled(false);
            mGeneralSettingsView.setupHapticToggleImage(R.drawable.ic_toggle_button_off);
        } else {
            mPresenterLog.info("Previous haptic Toggle Settings: False");
            mGeneralSettingsInteractor.getAppPreferenceInterface().setHapticFeedbackEnabled(true);
            mGeneralSettingsView.setupHapticToggleImage(R.drawable.ic_toggle_button_on);
        }
    }

    @Override
    public void onLanguageChanged() {
        mGeneralSettingsView.resetTextResources(
                mGeneralSettingsInteractor.getResourceString(R.string.general),
                mGeneralSettingsInteractor.getResourceString(R.string.sort_by),
                mGeneralSettingsInteractor.getResourceString(R.string.display_latency),
                mGeneralSettingsInteractor.getResourceString(R.string.preferred_language),
                mGeneralSettingsInteractor.getResourceString(R.string.theme),
                mGeneralSettingsInteractor.getResourceString(R.string.show_timer_in_notifications),
                mGeneralSettingsInteractor.getResourceString(R.string.haptic_setting_label),
                mGeneralSettingsInteractor.getResourceString(R.string.version),
                mGeneralSettingsInteractor.getResourceString(R.string.connected_lower_case),
                mGeneralSettingsInteractor.getResourceString(R.string.disconnected_lower_case),
                mGeneralSettingsInteractor.getResourceString(R.string.app_background)

        );
    }

    @Override
    public void onLanguageSelected(String selectedLanguage) {
        //Save the selected language
        String savedLanguage = mGeneralSettingsInteractor.getSavedLanguage();
        if (savedLanguage.equals(selectedLanguage)) {
            mPresenterLog.info("Language selected is same as saved. No action taken...");
        } else {
            String locale = selectedLanguage
                    .substring(selectedLanguage.indexOf("(") + 1, selectedLanguage.indexOf(")"));
            mPresenterLog.info("Saving selected language: " + selectedLanguage + " Locale: " + locale);
            mGeneralSettingsInteractor.saveSelectedLanguage(selectedLanguage);
            mGeneralSettingsView.setLanguageTextView(selectedLanguage);
            mGeneralSettingsInteractor.getPreferenceChangeObserver().postLanguageChange(locale);
        }
    }

    @Override
    public void onLatencyTypeSelected(String latencyType) {
        String savedLatencyType = mGeneralSettingsInteractor.getAppPreferenceInterface().getLatencyType();
        if (savedLatencyType.equals(latencyType)) {
            mPresenterLog.info("Same latency selected as saved.");
        } else {
            mPresenterLog.info("Saving selected latency type");
            mGeneralSettingsInteractor.getAppPreferenceInterface().setLatencyType(latencyType);
            mGeneralSettingsView.setLatencyType(latencyType);
            updateServerList();
        }
    }

    @Override
    public void onNotificationToggleButtonClicked() {
        if (mGeneralSettingsInteractor.getAppPreferenceInterface().getNotificationStat()) {
            mPresenterLog.info("Previous  notification Toggle Settings: True");
            mGeneralSettingsInteractor.getAppPreferenceInterface().setNotificationStat(false);
            mGeneralSettingsView.setupNotificationToggleImage(R.drawable.ic_toggle_button_off);
        } else {
            mPresenterLog.info("Previous Notification Toggle Settings: False");
            mGeneralSettingsInteractor.getAppPreferenceInterface().setNotificationStat(true);
            mGeneralSettingsView.setupNotificationToggleImage(R.drawable.ic_toggle_button_on);
        }
    }

    @Override
    public void onSelectionSelected(String selection) {
        String savedSelection = mGeneralSettingsInteractor.getSavedSelection();
        if (savedSelection.equals(selection)) {
            mPresenterLog.info("List selection selected is same as saved. No action taken...");
        } else {
            mGeneralSettingsInteractor.saveSelection(selection);
            mGeneralSettingsView.setSelectionTextView(selection);
            updateServerList();
        }
    }

    @Override
    public void onShowHealthToggleClicked() {
        if (mGeneralSettingsInteractor.getAppPreferenceInterface().isShowLocationHealthEnabled()) {
            mPresenterLog.info("Previous show location health Toggle Settings: True");
            mGeneralSettingsInteractor.getAppPreferenceInterface().setShowLocationHealthEnabled(false);
            mGeneralSettingsView.setupLocationHealthToggleImage(R.drawable.ic_toggle_button_off);
        } else {
            mPresenterLog.info("Previous show location health Toggle Settings: False");
            mGeneralSettingsInteractor.getAppPreferenceInterface().setShowLocationHealthEnabled(true);
            mGeneralSettingsView.setupLocationHealthToggleImage(R.drawable.ic_toggle_button_on);
        }
        mGeneralSettingsInteractor.getPreferenceChangeObserver().postShowLocationHealthChange();
    }

    @Override
    public void onThemeSelected(String theme) {
        String savedTheme = mGeneralSettingsInteractor.getAppPreferenceInterface().getSelectedTheme();
        if (savedTheme.equals(theme)) {
            mPresenterLog.info("Same theme selected as saved.");
        } else {
            mPresenterLog.info("Saving selected theme");
            mGeneralSettingsInteractor.getAppPreferenceInterface().setSelectedTheme(theme);
            Windscribe.getAppContext().getApplicationInterface().setTheme();
            mGeneralSettingsView.setThemeTextView(theme);
        }
    }

    @Override
    public void resizeAndSaveBitmap(final InputStream inputStream, final OutputStream outputStream) {
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        int requiredHeight = mGeneralSettingsInteractor.getAppPreferenceInterface().getFlagViewHeight();
        if (bitmap.getHeight() > requiredHeight) {
            Bitmap customBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), requiredHeight);
            customBitmap.compress(CompressFormat.JPEG, 100, outputStream);
        } else {
            bitmap.compress(CompressFormat.JPEG, 100, outputStream);
        }
    }

    @Override
    public void setTheme(Context context) {
        String savedThem = mGeneralSettingsInteractor.getAppPreferenceInterface().getSelectedTheme();
        mPresenterLog.debug("Setting theme to " + savedThem);
        if (savedThem.equals(PreferencesKeyConstants.DARK_THEME)) {
            context.setTheme(R.style.DarkTheme);
        } else {
            context.setTheme(R.style.LightTheme);
        }
    }

    @Override
    public void setupInitialLayout() {
        String savedLanguage = mGeneralSettingsInteractor.getSavedLanguage();

        //Setup language settings
        mGeneralSettingsView.setupLanguageAdapter(savedLanguage, mGeneralSettingsInteractor.getLanguageList());

        // Setup notification stats toggle
        mGeneralSettingsView
                .setupNotificationToggleImage(
                        mGeneralSettingsInteractor.getAppPreferenceInterface().getNotificationStat() ?
                                R.drawable.ic_toggle_button_on : R.drawable.ic_toggle_button_off);

        // Setup Haptic toggle
        mGeneralSettingsView
                .setupHapticToggleImage(
                        mGeneralSettingsInteractor.getAppPreferenceInterface().isHapticFeedbackEnabled() ?
                                R.drawable.ic_toggle_button_on : R.drawable.ic_toggle_button_off);

        // Setup Show Location health
        mGeneralSettingsView.setupLocationHealthToggleImage(
                mGeneralSettingsInteractor.getAppPreferenceInterface().isShowLocationHealthEnabled() ?
                        R.drawable.ic_toggle_button_on : R.drawable.ic_toggle_button_off);

        // Setup selection settings
        String savedSelection = mGeneralSettingsInteractor.getAppPreferenceInterface().getSelection();
        mGeneralSettingsView.setupSelectionAdapter(savedSelection, mGeneralSettingsView.getOrderList());

        // Setup theme
        String savedTheme = mGeneralSettingsInteractor.getAppPreferenceInterface().getSelectedTheme();
        mGeneralSettingsView.setupThemeAdapter(savedTheme, mGeneralSettingsView.getThemeList());

        // Setup latency settings
        String savedLatencyType = mGeneralSettingsInteractor.getAppPreferenceInterface().getLatencyType();
        mGeneralSettingsView.setupLatencyAdapter(savedLatencyType, new String[]{"Bars", "Ms"});

        mGeneralSettingsView.setAppVersionText(WindUtilities.getVersionName());

        mGeneralSettingsView.setActivityTitle(mGeneralSettingsInteractor.getResourceString(R.string.general));
        mGeneralSettingsView.registerLocaleChangeListener();

        setAppBackground(mGeneralSettingsInteractor.getAppPreferenceInterface().isCustomBackground());
        int expandedFlagWidth = mGeneralSettingsInteractor.getAppPreferenceInterface().getFlagViewWidth();
        int expandedFlagHeight = mGeneralSettingsInteractor.getAppPreferenceInterface().getFlagViewHeight();
        String flagDimensionsText = String.format("%sx%s", expandedFlagWidth, expandedFlagHeight);
        mGeneralSettingsView.setFlagSizeLabel(flagDimensionsText);
    }

    private void setAppBackground(boolean custom) {
        if (custom) {
            mGeneralSettingsInteractor.getAppPreferenceInterface().setCustomBackground(true);
            mGeneralSettingsView.setupCustomFlagAdapter("Custom", new String[]{"Flags", "Custom"});
        } else {
            mGeneralSettingsInteractor.getAppPreferenceInterface().setCustomBackground(false);
            mGeneralSettingsView.setupCustomFlagAdapter("Flags", new String[]{"Flags", "Custom"});
        }
        String disconnectedFlagPath = mGeneralSettingsInteractor.getAppPreferenceInterface()
                .getDisConnectedFlagPath();
        String connectedFlagPath = mGeneralSettingsInteractor.getAppPreferenceInterface().getConnectedFlagPath();
        mGeneralSettingsView.setDisconnectedFlagPath(
                disconnectedFlagPath != null ? Uri.parse(disconnectedFlagPath).getPath() : "");
        mGeneralSettingsView
                .setConnectedFlagPath(connectedFlagPath != null ? Uri.parse(connectedFlagPath).getPath() : "");
    }

    private void updateServerList() {
        mGeneralSettingsInteractor.getServerListUpdater().load();
        mGeneralSettingsInteractor.getStaticListUpdater().load();
    }
}
