/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.generalsettings;

import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.documentfile.provider.DocumentFile;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;

import com.windscribe.mobile.R;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.custom_view.CustomDialog;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.mainmenu.MainMenuActivity;
import com.windscribe.mobile.windscribe.WindscribeActivity;
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
import butterknife.OnItemSelected;

public class GeneralSettingsActivity extends BaseActivity implements GeneralSettingsView {

    @BindView(R.id.app_background_label)
    TextView appBackgroundLabel;

    @BindView(R.id.cl_app_background_settings)
    ConstraintLayout clAppBackground;

    @BindView(R.id.connected_flag_location_edit_view)
    TextView connectedFlagPathEditView;

    @BindView(R.id.connected_flag_label)
    TextView connectedFlagPathLabel;

    @BindView(R.id.connected_flag_size)
    TextView connectedFlagSizeLabel;

    final ConstraintSet constraintAppBackgroundCollapsed = new ConstraintSet();

    final ConstraintSet constraintAppBackgroundExpanded = new ConstraintSet();

    @BindView(R.id.custom_background_toggle)
    ImageView customBackgroundToggle;

    @BindView(R.id.disconnected_flag_location_edit_view)
    TextView disconnectedFlagPathEditView;

    @BindView(R.id.disconnected_flag_label)
    TextView disconnectedFlagPathLabel;

    @BindView(R.id.disconnected_flag_size)
    TextView disconnectedFlagSizeLabel;

    @BindView(R.id.nav_button)
    ImageView imgGeneralBackButton;

    @BindView(R.id.img_haptic_toggle_btn)
    ImageView imgHapticToggle;

    @BindView(R.id.img_notification_toggle_btn)
    ImageView imgNotificationToggle;

    @BindView(R.id.cl_settings_general)
    ConstraintLayout mConstraintLayoutGeneral;

    @Inject
    GeneralSettingsPresenter mGeneralPresenter;

    @Inject
    CustomDialog mSendDebugDialog;

    @Inject
    PreferenceChangeObserver mPreferenceChangeObserver;

    @BindView(R.id.tv_selection_label)
    TextView selectionLabelTextView;

    @BindView(R.id.img_show_health_toggle_btn)
    ImageView showHealthToggle;

    @BindView(R.id.spinner_language)
    Spinner spinnerLanguage;

    @BindView(R.id.spinner_latency)
    Spinner spinnerLatency;

    @BindView(R.id.spinner_selection)
    Spinner spinnerSelection;

    @BindView(R.id.spinner_theme)
    Spinner spinnerTheme;

    @BindView(R.id.img_theme_drop_down_btn)
    ImageView themeDropDown;

    @BindView(R.id.tv_theme_selection)
    TextView themeSelection;

    @BindView(R.id.nav_title)
    TextView tvActivityTitle;

    @BindView(R.id.tv_current_language)
    TextView tvCurrentLanguage;

    @BindView(R.id.tv_current_latency)
    TextView tvCurrentLatency;

    @BindView(R.id.tv_current_selection)
    TextView tvCurrentSelection;

    @BindView(R.id.tv_haptic_label)
    TextView tvHapticLabel;

    @BindView(R.id.tv_language_label)
    TextView tvLanguageLabel;

    @BindView(R.id.tv_latency_label)
    TextView tvLatencyLabel;

    @BindView(R.id.tv_notification_label)
    TextView tvNotificationLabel;

    @BindView(R.id.tv_theme_label)
    TextView tvThemeLabel;

    @BindView(R.id.tv_version_label)
    TextView tvVersionLabel;

    @BindView(R.id.tv_version_selection)
    TextView versionSelection;

    private final String TAG = "gen_settings_a";

    private final Logger mGeneralSettingsLog = LoggerFactory.getLogger(TAG);

    public static Intent getStartIntent(Context context) {
        return new Intent(context, GeneralSettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAppBackgroundTab();
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_general_settings, true);
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

    @OnClick(R.id.connected_flag_edit_button)
    public void onConnectedFlagEditClick() {
        mGeneralSettingsLog.info("User clicked on connected flag edit button...");
        mGeneralPresenter.onConnectedFlagEditClicked(CONNECTED_FLAG_PATH_PICK_REQUEST);
    }

    @OnClick({R.id.tv_current_language, R.id.img_language_drop_down_btn})
    public void onCurrentLanguageClick() {
        mGeneralSettingsLog.info("User clicked to open port adapter..");
        spinnerLanguage.performClick();
    }

    @OnClick({R.id.tv_current_latency, R.id.img_latency_drop_down_btn})
    public void onCurrentLatencyClick() {
        mGeneralSettingsLog.info("User clicked to latency type adapter..");
        spinnerLatency.performClick();
    }

    @OnClick({R.id.tv_current_selection, R.id.img_selection_drop_down_btn})
    public void onCurrentSelectionClick() {
        mGeneralSettingsLog.info("User clicked to open selection adapter..");
        spinnerSelection.performClick();
    }

    @OnClick(R.id.disconnected_flag_edit_button)
    public void onDisconnectedFlagEditClick() {
        mGeneralSettingsLog.info("User clicked on disconnected flag edit button...");
        mGeneralPresenter.onDisconnectedFlagEditClicked(DISCONNECTED_FLAG_PATH_PICK_REQUEST);
    }

    @SuppressWarnings("unused")
    @OnItemSelected(R.id.spinner_language)
    public void onLanguageSelected(View view, int position) {
        if (view != null) {
            ((TextView) view.findViewById(R.id.tv_drop_down)).setText("");
            mGeneralPresenter.onLanguageSelected(spinnerLanguage.getSelectedItem().toString());
        }
    }

    @SuppressWarnings("unused")
    @OnItemSelected(R.id.spinner_latency)
    public void onLatencySelected(View view, int position) {
        if (view != null) {
            ((TextView) view.findViewById(R.id.tv_drop_down)).setText("");
        }
        if (mGeneralPresenter != null) {
            mGeneralPresenter.onLatencyTypeSelected(spinnerLatency.getSelectedItem().toString());
        }
    }

    @SuppressWarnings("unused")
    @OnItemSelected(R.id.spinner_selection)
    public void onListSelectionSelected(View view, int position) {
        if (view != null) {
            ((TextView) view.findViewById(R.id.tv_drop_down)).setText("");
            mGeneralPresenter.onSelectionSelected(spinnerSelection.getSelectedItem().toString());
        }
    }

    @OnClick(R.id.img_show_health_toggle_btn)
    public void onShowHealthToggleClick() {
        mGeneralSettingsLog.info("User clicked on show health toggle button...");
        mGeneralPresenter.onShowHealthToggleClicked();
    }

    @SuppressWarnings("unused")
    @OnItemSelected(R.id.spinner_theme)
    public void onThemeSelected(View view, int position) {
        if (view != null) {
            ((TextView) view.findViewById(R.id.tv_drop_down)).setText("");
        }
        if (mGeneralPresenter != null) {
            mGeneralPresenter.onThemeSelected(spinnerTheme.getSelectedItem().toString());
        }
    }

    @OnClick({R.id.img_theme_drop_down_btn, R.id.tv_theme_selection})
    public void onThemeSelectionClick() {
        mGeneralSettingsLog.info("User clicked theme adapter..");
        spinnerTheme.performClick();
    }

    @OnClick(R.id.custom_background_toggle)
    public void onToggleCustomFlagClick() {
        mGeneralSettingsLog.info("User clicked on Custom background toggle...");
        mGeneralPresenter.onCustomFlagToggleButtonClicked();
    }

    @OnClick(R.id.img_haptic_toggle_btn)
    public void onToggleHapticClick() {
        mGeneralSettingsLog.info("User clicked on haptic toggle button...");
        mGeneralPresenter.onHapticToggleButtonClicked();
    }

    @OnClick(R.id.img_notification_toggle_btn)
    public void onToggleNotificationClick() {
        mGeneralSettingsLog.info("User clicked on notification toggle button...");
        mGeneralPresenter.onNotificationToggleButtonClicked();
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
        selectionLabelTextView.setText(sortBy);
        tvLatencyLabel.setText(latencyDisplay);
        tvLanguageLabel.setText(language);
        tvThemeLabel.setText(appearance);
        tvNotificationLabel.setText(notificationState);
        tvHapticLabel.setText(hapticFeedback);
        tvVersionLabel.setText(version);
        appBackgroundLabel.setText(appBackground);
        connectedFlagPathLabel.setText(connected);
        disconnectedFlagPathLabel.setText(disconnected);
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
        connectedFlagPathEditView.setText(path);
    }

    @Override
    public void setDisconnectedFlagPath(final String path) {
        disconnectedFlagPathEditView.setText(path);
    }

    @Override
    public void setFlagSizeLabel(String label) {
        connectedFlagSizeLabel.setText(label);
        disconnectedFlagSizeLabel.setText(label);
    }

    @Override
    public void setLanguageTextView(String language) {
        tvCurrentLanguage.setText(language);
        reloadApp();
    }

    @Override
    public void setLatencyType(String latencyType) {
        tvCurrentLatency.setText(latencyType);
    }

    @Override
    public void setSelectionTextView(String selection) {
        tvCurrentSelection.setText(selection);
    }

    @Override
    public void setThemeTextView(String theme) {
        themeSelection.setText(theme);
        reloadApp();
    }

    @Override
    public void setupAppBackgroundLayoutCollapsed() {
        Fade autoTransition = new Fade();
        autoTransition.setDuration(500);
        TransitionManager.beginDelayedTransition(clAppBackground, autoTransition);
        constraintAppBackgroundCollapsed.applyTo(clAppBackground);
    }

    @Override
    public void setupAppBackgroundLayoutExpanded() {
        Fade autoTransition = new Fade();
        autoTransition.setDuration(500);
        TransitionManager.beginDelayedTransition(clAppBackground, autoTransition);
        constraintAppBackgroundExpanded.applyTo(clAppBackground);
    }

    @Override
    public void setupCustomFlagToggleImage(final int ic_toggle_button_off) {
        customBackgroundToggle.setImageResource(ic_toggle_button_off);
    }

    @Override
    public void setupHapticToggleImage(int ic_toggle_button_off) {
        imgHapticToggle.setImageResource(ic_toggle_button_off);
    }

    @Override
    public void setupLanguageAdapter(String savedLanguage, String[] languages) {
        mGeneralSettingsLog.info("Setting up language adapter...");
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(this, R.layout.drop_down_layout,
                R.id.tv_drop_down, languages);
        spinnerLanguage.setAdapter(languageAdapter);
        spinnerLanguage.setSelected(false);
        spinnerLanguage.setSelection(languageAdapter.getPosition(savedLanguage));
        tvCurrentLanguage.setText(savedLanguage);
    }

    @Override
    public void setupLatencyAdapter(String savedLatency, String[] latencyTypes) {
        mGeneralSettingsLog.info("Setting up latency adapter...");
        ArrayAdapter<String> latencyAdapter = new ArrayAdapter<>(this, R.layout.drop_down_layout,
                R.id.tv_drop_down, latencyTypes);
        spinnerLatency.setAdapter(latencyAdapter);
        spinnerLatency.setSelected(false);
        spinnerLatency.setSelection(latencyAdapter.getPosition(savedLatency));
        tvCurrentLatency.setText(savedLatency);
    }

    @Override
    public void setupLocationHealthToggleImage(final int image) {
        showHealthToggle.setImageResource(image);
    }

    @Override
    public void setupNotificationToggleImage(int ic_toggle_button_off) {
        imgNotificationToggle.setImageResource(ic_toggle_button_off);
    }

    @Override
    public void setupSelectionAdapter(String savedSelection, String[] selections) {
        mGeneralSettingsLog.info("Setting up selection adapter...");
        ArrayAdapter<String> selectionAdapter = new ArrayAdapter<>(this, R.layout.drop_down_layout,
                R.id.tv_drop_down, selections);
        spinnerSelection.setAdapter(selectionAdapter);
        spinnerSelection.setSelected(false);
        spinnerSelection.setSelection(selectionAdapter.getPosition(savedSelection));
        tvCurrentSelection.setText(savedSelection);
    }

    @Override
    public void setupThemeAdapter(String savedTheme, String[] themeList) {
        mGeneralSettingsLog.info("Setting up theme adapter..");
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this, R.layout.drop_down_layout, R.id.tv_drop_down,
                themeList);
        spinnerTheme.setAdapter(themeAdapter);
        spinnerTheme.setSelected(false);
        spinnerTheme.setSelection(themeAdapter.getPosition(savedTheme));
        themeSelection.setText(savedTheme);
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

    private void setAppBackgroundTab() {
        constraintAppBackgroundCollapsed.clone(this, R.layout.app_background_tab);
        constraintAppBackgroundExpanded.clone(this, R.layout.app_background_tab);
        constraintAppBackgroundExpanded.setVisibility(R.id.app_background_settings_divider, ConstraintSet.GONE);
        constraintAppBackgroundExpanded.setVisibility(R.id.connected_flag_label, ConstraintSet.VISIBLE);
        constraintAppBackgroundExpanded.setVisibility(R.id.connected_flag_size, ConstraintSet.VISIBLE);
        constraintAppBackgroundExpanded.setVisibility(R.id.connected_edit_background, ConstraintSet.VISIBLE);
        constraintAppBackgroundExpanded.setVisibility(R.id.connected_flag_location_edit_view, ConstraintSet.VISIBLE);
        constraintAppBackgroundExpanded.setVisibility(R.id.connected_flag_edit_button, ConstraintSet.VISIBLE);

        constraintAppBackgroundExpanded.setVisibility(R.id.disconnected_flag_label, ConstraintSet.VISIBLE);
        constraintAppBackgroundExpanded.setVisibility(R.id.disconnected_flag_size, ConstraintSet.VISIBLE);
        constraintAppBackgroundExpanded.setVisibility(R.id.disconnected_edit_background, ConstraintSet.VISIBLE);
        constraintAppBackgroundExpanded
                .setVisibility(R.id.disconnected_flag_location_edit_view, ConstraintSet.VISIBLE);
        constraintAppBackgroundExpanded.setVisibility(R.id.disconnected_flag_edit_button, ConstraintSet.VISIBLE);

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
