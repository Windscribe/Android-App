/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.generalsettings;

import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.windscribe.mobile.R;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.custom_view.CustomDialog;
import com.windscribe.mobile.custom_view.preferences.DropDownView;
import com.windscribe.mobile.custom_view.preferences.AppBackgroundView;
import com.windscribe.mobile.custom_view.preferences.ToggleView;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.mainmenu.MainMenuActivity;
import com.windscribe.mobile.windscribe.WindscribeActivity;
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.state.PreferenceChangeObserver;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class GeneralSettingsActivity extends BaseActivity implements GeneralSettingsView {

    private final String TAG = "gen_settings_a";
    private final Logger mGeneralSettingsLog = LoggerFactory.getLogger(TAG);
    @BindView(R.id.cl_app_background_settings)
    AppBackgroundView appBackgroundDropDown;
    @BindView(R.id.cl_selection_settings)
    DropDownView locationSelectionDropDown;
    @BindView(R.id.cl_language_settings)
    DropDownView languageDropDown;
    @BindView(R.id.cl_latency_settings)
    DropDownView latencyDropDown;
    @BindView(R.id.cl_theme_settings)
    DropDownView themeDropDown;
    @BindView(R.id.cl_notification_settings)
    ToggleView notificationToggle;
    @BindView(R.id.cl_show_health)
    ToggleView locationLoadToggle;
    @BindView(R.id.cl_haptic_settings)
    ToggleView hapticToggle;
    @BindView(R.id.nav_button)
    ImageView imgGeneralBackButton;
    @Inject
    GeneralSettingsPresenter mGeneralPresenter;
    @Inject
    PreferenceChangeObserver mPreferenceChangeObserver;
    @Inject
    CustomDialog mSendDebugDialog;
    @BindView(R.id.nav_title)
    TextView tvActivityTitle;
    @BindView(R.id.tv_version_label)
    TextView tvVersionLabel;
    @BindView(R.id.tv_version_selection)
    TextView versionSelection;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, GeneralSettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_general_settings, true);
        setUpCustomViewDelegates();
        mGeneralSettingsLog.info("Setting up layout based on saved mode settings...");
        mGeneralPresenter.setupInitialLayout();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            mGeneralSettingsLog.info(String.format("Received file uri  %s", fileUri.toString()));
            File file = uriToFile(fileUri);
            if (file != null) {
                mGeneralSettingsLog.info("Converted uri to file");
                try {
                    try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
                        try (FileOutputStream outputStream = new FileOutputStream(file)) {
                            if (isTypeImage(fileUri)) {
                                mGeneralPresenter.resizeAndSaveBitmap(inputStream, outputStream);
                            } else {
                                IOUtils.copy(inputStream, outputStream);
                            }
                            String path = file.getAbsolutePath();
                            mGeneralSettingsLog.info(String.format("Saved file to %s", path));
                            if (requestCode == DISCONNECTED_FLAG_PATH_PICK_REQUEST) {
                                mGeneralPresenter.onDisConnectedFlagPathPicked(path);
                            } else if (requestCode == CONNECTED_FLAG_PATH_PICK_REQUEST) {
                                mGeneralPresenter.onConnectedFlagPathPicked(path);
                            }
                        }
                    }
                } catch (IOException e) {
                    mGeneralSettingsLog.info("Error copying file from input stream");
                    showToast("Error copying image to app's internal storage");
                }
            } else {
                mGeneralSettingsLog.info("Invalid file type");
                showToast("Invalid file.");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        mGeneralPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public String[] getOrderList() {
        return getResources().getStringArray(R.array.order_list);
    }

    @Override
    public String[] getThemeList() {
        return getResources().getStringArray(R.array.theme_list);
    }

    @OnClick(R.id.nav_button)
    public void onBackButtonClicked() {
        mGeneralSettingsLog.info("User clicked on back arrow ...");
        onBackPressed();
    }

    public void setUpCustomViewDelegates() {
        locationSelectionDropDown.setDelegate(new DropDownView.Delegate() {
            @Override
            public void onItemSelect(@NonNull String value) {
                mGeneralPresenter.onSelectionSelected(value);
            }
            @Override
            public void onExplainClick() { }
        });
        latencyDropDown.setDelegate(new DropDownView.Delegate() {
            @Override
            public void onItemSelect(@NonNull String value) {
                mGeneralPresenter.onLatencyTypeSelected(value);
            }

            @Override
            public void onExplainClick() {}
        });
        languageDropDown.setDelegate(new DropDownView.Delegate() {
            @Override
            public void onItemSelect(@NonNull String value) {
                mGeneralPresenter.onLanguageSelected(value);
            }

            @Override
            public void onExplainClick() {}
        });
        themeDropDown.setDelegate(new DropDownView.Delegate() {
            @Override
            public void onItemSelect(@NonNull String value) {
                mGeneralPresenter.onThemeSelected(value);
            }

            @Override
            public void onExplainClick() {}
        });
        notificationToggle.setDelegate(new ToggleView.Delegate() {
            @Override
            public void onToggleClick() {
                mGeneralPresenter.onNotificationToggleButtonClicked();
            }

            @Override
            public void onExplainClick() {}
        });
        locationLoadToggle.setDelegate(new ToggleView.Delegate() {
            @Override
            public void onToggleClick() {
                mGeneralPresenter.onShowHealthToggleClicked();
            }

            @Override
            public void onExplainClick() {}
        });
        hapticToggle.setDelegate(new ToggleView.Delegate() {
            @Override
            public void onToggleClick() {
                mGeneralPresenter.onHapticToggleButtonClicked();
            }

            @Override
            public void onExplainClick() {}
        });
        appBackgroundDropDown.setDelegate(new AppBackgroundView.Delegate() {
            @Override
            public void onItemSelect(@NonNull String value) {
                mGeneralPresenter.onCustomFlagToggleButtonClicked(value);
            }

            @Override
            public void onFirstRightIconClick() {
                mGeneralSettingsLog.info("User clicked on disconnected flag edit button...");
                mGeneralPresenter.onDisconnectedFlagEditClicked(DISCONNECTED_FLAG_PATH_PICK_REQUEST);
            }

            @Override
            public void onSecondRightIconClick() {
                mGeneralSettingsLog.info("User clicked on connected flag edit button...");
                mGeneralPresenter.onConnectedFlagEditClicked(CONNECTED_FLAG_PATH_PICK_REQUEST);
            }
        });
    }

    @Override
    public void openFileChooser(int requestCode) {
        Intent pickIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pickIntent.setType("*/*");
        mGeneralSettingsLog.info(String.format("Creating pick intent for %s", requestCode));
        if (pickIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(pickIntent, requestCode);
        } else {
            mGeneralSettingsLog.info("Pick intent did not resolve to any activity.");
            showToast("No File manager found.");
        }
    }

    @Override
    public void registerLocaleChangeListener() {
        mPreferenceChangeObserver.addLanguageChangeObserver(this, language -> {
            setLanguage();
            mGeneralPresenter.onLanguageChanged();
        });
    }

    @Override
    public void resetTextResources(String title, String sortBy, String latencyDisplay, String language,
                                   String appearance, String notificationState, String hapticFeedback, String version, String connected,
                                   String disconnected, String appBackground) {
        tvActivityTitle.setText(title);
        locationSelectionDropDown.setTitle(sortBy);
        latencyDropDown.setTitle(latencyDisplay);
        languageDropDown.setTitle(language);
        themeDropDown.setTitle(appearance);
        notificationToggle.setTitle(notificationState);
        hapticToggle.setTitle(hapticFeedback);
        tvVersionLabel.setText(version);
        appBackgroundDropDown.setTitle(appBackground);
        appBackgroundDropDown.setFirstItemDescription(disconnected);
        appBackgroundDropDown.setSecondItemDescription(connected);
    }

    @Override
    public void setActivityTitle(String activityTitle) {
        tvActivityTitle.setText(activityTitle);
    }

    @Override
    public void setAppVersionText(String versionText) {
        versionSelection.setText(versionText);
    }

    @Override
    public void setConnectedFlagPath(final String path) {
        appBackgroundDropDown.setSecondItemDescription(path);
    }

    @Override
    public void setDisconnectedFlagPath(final String path) {
        appBackgroundDropDown.setFirstItemDescription(path);
    }

    @Override
    public void setFlagSizeLabel(String label) {
        appBackgroundDropDown.setFirstItemTitle(label);
        appBackgroundDropDown.setSecondItemTitle(label);
    }

    @Override
    public void setLanguageTextView(String language) {
        languageDropDown.setCurrentValue(language);
        reloadApp();
    }

    @Override
    public void setLatencyType(String latencyType) {
        latencyDropDown.setCurrentValue(latencyType);
    }

    @Override
    public void setSelectionTextView(String selection) {
        locationSelectionDropDown.setCurrentValue(selection);
    }

    @Override
    public void setThemeTextView(String theme) {
        themeDropDown.setCurrentValue(theme);
        reloadApp();
    }

    @Override
    public void setupCustomFlagAdapter(String saved, String[] options) {
        appBackgroundDropDown.setAdapter(saved, options);
    }

    @Override
    public void setupHapticToggleImage(int ic_toggle_button_off) {
        hapticToggle.setToggleImage(ic_toggle_button_off);
    }

    @Override
    public void setupLanguageAdapter(String savedLanguage, String[] languages) {
        mGeneralSettingsLog.info("Setting up language adapter...");
        languageDropDown.setAdapter(savedLanguage, languages);
    }

    @Override
    public void setupLatencyAdapter(String savedLatency, String[] latencyTypes) {
        mGeneralSettingsLog.info("Setting up latency adapter...");
        latencyDropDown.setAdapter(savedLatency, latencyTypes);
    }

    @Override
    public void setupLocationHealthToggleImage(final int image) {
        locationLoadToggle.setToggleImage(image);
    }

    @Override
    public void setupNotificationToggleImage(int ic_toggle_button_off) {
        notificationToggle.setToggleImage(ic_toggle_button_off);
    }

    @Override
    public void setupSelectionAdapter(String savedSelection, String[] selections) {
        mGeneralSettingsLog.info("Setting up selection adapter...");
        locationSelectionDropDown.setAdapter(savedSelection, selections);
    }

    @Override
    public void setupThemeAdapter(String savedTheme, String[] themeList) {
        mGeneralSettingsLog.info("Setting up theme adapter..");
        themeDropDown.setAdapter(savedTheme, themeList);
    }

    public void showToast(String toastString) {
        Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show();
    }

    private String getFileName(Uri fileUri) {
        DocumentFile fileDocument = DocumentFile.fromSingleUri(this, fileUri);
        if (fileDocument != null) {
            return fileDocument.getName();
        }
        return null;
    }

    private boolean isTypeImage(Uri fileUri) {
        String fileName = getFileName(fileUri);
        if (fileName != null) {
            return fileName.endsWith(".jpeg") | fileName.endsWith(".jpg") | fileName.endsWith(".png");
        }
        return false;
    }

    private void reloadApp() {
        TaskStackBuilder.create(this)
                .addNextIntent(WindscribeActivity.getStartIntent(this))
                .addNextIntent(MainMenuActivity.getStartIntent(this))
                .addNextIntentWithParentStack(getIntent())
                .startActivities();
    }

    private File uriToFile(Uri fileUri) {
        String fileName = getFileName(fileUri);
        if (fileName != null) {
            if (fileName.endsWith(".jpeg") | fileName.endsWith(".jpg") | fileName.endsWith(".png") | fileName
                    .endsWith(".gif")) {
                return new File(getFilesDir(), fileName);
            }
        }
        return null;
    }
}
