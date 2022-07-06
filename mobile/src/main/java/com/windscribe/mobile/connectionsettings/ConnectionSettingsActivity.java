/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.connectionsettings;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;

import com.windscribe.mobile.R;
import com.windscribe.mobile.alert.AlwaysOnFragment;
import com.windscribe.mobile.alert.ExtraDataUseWarningFragment;
import com.windscribe.mobile.alert.LocationPermissionRationale;
import com.windscribe.mobile.alert.PermissionRationaleListener;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.di.DaggerActivityComponent;
import com.windscribe.mobile.gpsspoofing.GpsSpoofingSettingsActivity;
import com.windscribe.mobile.networksecurity.NetworkSecurityActivity;
import com.windscribe.mobile.splittunneling.SplitTunnelingActivity;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.commonutils.InputFilterMinMax;
import com.windscribe.vpn.constants.AnimConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;

public class ConnectionSettingsActivity extends BaseActivity
        implements ConnectionSettingsView, AlwaysOnFragment.AlwaysOnDialogCallBack, PermissionRationaleListener, ExtraDataUseWarningFragment.CallBack {

    public static final int LOCATION_PERMISSION_FOR_SPOOF = 901;

    private static final String TAG = "conn_settings_a";

    @BindView(R.id.img_auto_fill_packet_size)
    ImageView autoFillBtn;

    @BindView(R.id.edit_packet_progress)
    TextView autoFillProgress;

    @BindView(R.id.img_boot_toggle_btn)
    ImageView bootToggleBtn;

    @BindView(R.id.cl_boot_settings)
    ConstraintLayout clAutoStartOnBoot;

    @BindView(R.id.img_lan_settings_divider)
    ImageView lanDivider;

    @BindView(R.id.cl_gps_settings)
    ConstraintLayout clGpsSettings;

    @BindView(R.id.decoy_traffic_parent)
    ConstraintLayout clDecoyTrafficSettings;

    @BindView(R.id.img_decoy_traffic_toggle_btn)
    ImageView decoyTrafficToggleBtn;

    @BindView(R.id.spinner_fake_traffic_volume)
    Spinner spinnerFakeTrafficVolume;

    @BindView(R.id.tv_current_potential_traffic)
    TextView tvCurrentTrafficUse;

    @BindView(R.id.tv_current_fake_traffic_volume)
    TextView tvCurrentFakeTrafficVolume;

    final ConstraintSet constraintKeepAliveAuto = new ConstraintSet();

    final ConstraintSet constraintKeepAliveHidden = new ConstraintSet();

    final ConstraintSet constraintKeepAliveManual = new ConstraintSet();

    @BindView(R.id.edit_packet_size)
    EditText ed_package_size;

    @BindView(R.id.img_gps_settings_divider)
    ImageView gpsSpoofDivider;

    @BindView(R.id.img_gps_toggle_btn)
    ImageView gpsToggleBtn;

    @BindView(R.id.keep_alive_edit_button)
    ImageView keepAliveEditBtn;

    @BindView(R.id.keep_alive_edit_view)
    EditText keepAliveEditText;

    @BindView(R.id.keep_alive_parent)
    ConstraintLayout keepAliveTabParent;

    @BindView(R.id.img_lan_toggle_btn)
    ImageView lanToggleBtn;

    @BindView(R.id.nav_title)
    TextView mActivityTitleView;

    @Inject
    ConnectionSettingsPresenter mConnSettingsPresenter;

    @BindView(R.id.connection_parent)
    ConstraintLayout mConstraintLayoutConnection;

    final ConstraintSet mConstraintSetParent = new ConstraintSet();

    androidx.transition.AutoTransition mSupportTransition;

    AutoTransition mTransition;

    @BindView(R.id.make_packet_size_editable)
    ImageView packetSizeEditableBtn;

    @BindView(R.id.progress_packet_size)
    ProgressBar progressBar;

    @BindView(R.id.spinner_port)
    Spinner spinnerPort;

    @BindView(R.id.spinner_protocol)
    Spinner spinnerProtocol;

    @BindView(R.id.tv_connection_mode_auto)
    TextView tvConnectionModeAuto;

    @BindView(R.id.tv_connection_mode_manual)
    TextView tvConnectionModeManual;

    @BindView(R.id.tv_current_port)
    TextView tvCurrentPort;

    @BindView(R.id.tv_current_protocol)
    TextView tvCurrentProtocol;

    @BindView(R.id.keep_alive_auto)
    TextView tvKeepAliveModeAuto;

    @BindView(R.id.keep_alive_manual)
    TextView tvKeepAliveModeManual;

    @BindView(R.id.tv_packet_size_mode_auto)
    TextView tvPacketSizeModeAuto;

    @BindView(R.id.tv_packet_size_mode_manual)
    TextView tvPacketSizeModeManual;

    @BindView(R.id.tv_split_tunnel_status)
    TextView tvSplitTunnelStatus;

    private final Logger mConnectionSettingsLog = LoggerFactory.getLogger(TAG);

    public static Intent getStartIntent(Context context) {
        return new Intent(context, ConnectionSettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConstraintSetParent.clone(this, R.layout.connection_layout);
        setKeepAliveTab();
        DaggerActivityComponent.builder().activityModule(new ActivityModule(this, this))
                .applicationComponent(Windscribe.getAppContext()
                        .getApplicationComponent()).build().inject(this);
        mConnSettingsPresenter.setTheme(this);
        setContentView(R.layout.activity_connection_settings);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        ButterKnife.bind(this);

        mConnectionSettingsLog.info("Setting up layout based on saved mode settings...");
        mConnSettingsPresenter.init();

        mActivityTitleView.setText(getString(R.string.connection));
        setTextListeners();
    }

    private void setTextListeners(){
        ed_package_size.setFilters(new InputFilter[]{new InputFilterMinMax("0", "2000")});
        ed_package_size.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (!ed_package_size.getText().toString().trim().isEmpty()) {
                    mConnSettingsPresenter.setPacketSizeManual(ed_package_size.getText().toString().trim());
                }
                ed_package_size.clearFocus();
                ed_package_size.setEnabled(false);
                return false;
            }
            return false;
        });

        keepAliveEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (!keepAliveEditText.getText().toString().trim().isEmpty()) {
                    mConnSettingsPresenter.setKeepAlive(keepAliveEditText.getText().toString().trim());
                }
                keepAliveEditText.clearFocus();
                keepAliveEditText.setEnabled(false);
                return false;
            }
            return false;
        });

        keepAliveEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!keepAliveEditText.getText().toString().trim().isEmpty()) {
                    mConnSettingsPresenter.saveKeepAlive(keepAliveEditText.getText().toString().trim());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mConnSettingsPresenter.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConnSettingsPresenter.onHotStart();
    }

    @Override
    protected void onDestroy() {
        mConnSettingsPresenter.onDestroy();
        super.onDestroy();
    }

    @OnClick({R.id.cl_always_on, R.id.tv_always_on_label})
    public void clickOnAlwaysOn() {
        mConnectionSettingsLog.info("User clicked to always on..");
        AlwaysOnFragment alwaysOnFragment = new AlwaysOnFragment(false);
        if (!getSupportFragmentManager().isStateSaved() && !alwaysOnFragment.isAdded()) {
            alwaysOnFragment.show(getSupportFragmentManager(), null);
        }
    }

    @Override
    public void getLocationPermission(int requestCode) {
        checkLocationPermission(R.id.connection_parent, requestCode);
    }

    @Override
    public void goToAppInfoSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void gotoSplitTunnelingSettings() {
        Intent intent = SplitTunnelingActivity.getStartIntent(this);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
        startActivity(intent, options.toBundle());
    }

    @OnClick(R.id.make_packet_size_editable)
    public void makePacketSizeEditable() {
        ed_package_size.setEnabled(true);
        ed_package_size.requestFocus();
        ed_package_size.setSelection(ed_package_size.getText().length());
        showKeyboard(ed_package_size);
    }

    @OnClick(R.id.img_lan_toggle_btn)
    public void onAllowLanClicked() {
        mConnectionSettingsLog.info("User clicked on allow lan...");
        mConnSettingsPresenter.onAllowLanClicked();
    }

    @OnClick(R.id.img_auto_fill_packet_size)
    public void onAutoFillPacketSizeClicked() {
        mConnSettingsPresenter.onAutoFillPacketSizeClicked();
    }

    @OnClick(R.id.nav_button)
    public void onBackButtonClicked() {
        mConnectionSettingsLog.info("User clicked on back arrow...");
        onBackPressed();
    }

    @OnClick(R.id.img_boot_toggle_btn)
    public void onBootStartClick() {
        mConnectionSettingsLog.info("User clicked on auto start on boot...");
        mConnSettingsPresenter.onAutoStartOnBootClick();
    }

    @OnClick(R.id.tv_connection_mode_auto)
    public void onConnectionModeAutoClick() {
        mConnectionSettingsLog
                .info("User clicked on " + tvConnectionModeAuto.getText().toString() + " in settings...");
        mConnSettingsPresenter.onConnectionModeAutoClicked();
    }

    @OnClick(R.id.tv_connection_mode_manual)
    public void onConnectionModeManualClick() {
        mConnectionSettingsLog
                .info("User clicked on " + tvConnectionModeManual.getText().toString() + " in settings...");
        mConnSettingsPresenter.onConnectionModeManualClicked();
    }

    @OnClick({R.id.tv_current_port, R.id.img_port_drop_down_btn})
    public void onCurrentPortClick() {
        mConnectionSettingsLog.info("User clicked to open port adapter..");
        spinnerPort.performClick();
    }

    @OnClick({R.id.tv_current_protocol, R.id.img_protocol_drop_down_btn})
    public void onCurrentProtocolClick() {
        mConnectionSettingsLog.info("User clicked to open protocol adapter..");
        spinnerProtocol.performClick();
    }

    @OnClick({R.id.tv_current_fake_traffic_volume, R.id.img_fake_traffic_volume_drop_down_btn})
    public void onCurrentFakeTrafficClick() {
        mConnectionSettingsLog.info("User clicked to open fake traffic volume adapter..");
        spinnerFakeTrafficVolume.performClick();
    }

    @OnClick(R.id.img_decoy_traffic_toggle_btn)
    public void onDecoyTrafficClick() {
        mConnectionSettingsLog.info("User clicked on auto start on decoy traffic...");
        mConnSettingsPresenter.onDecoyTrafficClick();
    }

    @Override
    public void onGoToSettings() {
        Intent intent = new Intent("android.net.vpn.SETTINGS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "VPN settings not found.", Toast.LENGTH_SHORT).show();
        }

    }

    @OnClick(R.id.img_gps_toggle_btn)
    public void onGpsSpoofClick() {
        mConnectionSettingsLog.info("User clicked on gps spoofing...");
        mConnSettingsPresenter.onGpsSpoofingClick();
    }

    @OnClick(R.id.keep_alive_auto)
    public void onKeepAliveAutoModeClicked() {
        ed_package_size.setEnabled(false);
        mConnSettingsPresenter.onKeepAliveAutoModeClicked();
    }

    @OnClick({R.id.keep_alive_edit_button})
    public void onKeepAliveEditClicked() {
        keepAliveEditText.setEnabled(true);
        keepAliveEditText.requestFocus();
        keepAliveEditText.setSelection(keepAliveEditText.getText().length());
        showKeyboard(keepAliveEditText);
    }

    @OnClick(R.id.keep_alive_manual)
    public void onKeepAliveManualModeClicked() {
        mConnSettingsPresenter.onKeepAliveManualModeClicked();
    }

    @OnClick(R.id.tv_packet_size_mode_auto)
    public void onPacketSizeAutoModeClicked() {
        ed_package_size.setEnabled(false);
        mConnSettingsPresenter.onPacketSizeAutoModeClicked();
    }

    @OnClick(R.id.tv_packet_size_mode_manual)
    public void onPacketSizeManualModeClicked() {
        mConnSettingsPresenter.onPacketSizeManualModeClicked();
    }

    @OnItemSelected(R.id.spinner_port)
    public void onPortSelected(View view, @SuppressWarnings("unused") int position) {
        if (view != null) {
            mConnectionSettingsLog.info("User selected " + spinnerPort.getSelectedItem().toString());
            ((TextView) view.findViewById(R.id.tv_drop_down)).setText("");
            tvCurrentPort.setText(spinnerPort.getSelectedItem().toString());
            mConnSettingsPresenter.onPortSelected(spinnerProtocol.getSelectedItem().toString(),
                    spinnerPort.getSelectedItem().toString());
        }
    }

    @SuppressWarnings("unused")
    @OnItemSelected(R.id.spinner_protocol)
    public void onProtocolSelected(View view, int position) {
        if (view != null) {
            ((TextView) view.findViewById(R.id.tv_drop_down)).setText("");
        }
        if (mConnSettingsPresenter != null) {
            mConnSettingsPresenter.onProtocolSelected(spinnerProtocol.getSelectedItem().toString());
        }
    }

    @OnItemSelected(R.id.spinner_fake_traffic_volume)
    public void onFakeVolumeSelected(View view, int position) {
        if (view != null) {
            ((TextView) view.findViewById(R.id.tv_drop_down)).setText("");
        }
        if (mConnSettingsPresenter != null) {
            mConnSettingsPresenter.onFakeTrafficVolumeSelected(spinnerFakeTrafficVolume.getSelectedItem().toString());
        }
    }

    @OnClick(R.id.cl_split_tunneling)
    public void onSplitTunnelingClick() {
        mConnectionSettingsLog.info("User clicked on split tunneling...");
        mConnSettingsPresenter.onSplitTunnelingOptionClicked();
    }

    @OnClick({R.id.cl_whitelist, R.id.img_network_whitelist_arrow, R.id.tv_whitelist_label})
    public void onWhitelistClick() {
        checkLocationPermission(R.id.connection_parent, BaseActivity.REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void openGpsSpoofSettings() {
        startActivity(GpsSpoofingSettingsActivity.getStartIntent(this));
    }

    @Override
    public void packetSizeDetectionProgress(boolean progress) {
        if (progress) {
            autoFillBtn.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            ed_package_size.setVisibility(View.INVISIBLE);
            autoFillProgress.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            autoFillBtn.setVisibility(View.VISIBLE);
            autoFillProgress.setVisibility(View.INVISIBLE);
            ed_package_size.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void permissionDenied(int requestCode) {
        Toast.makeText(this, "Please provide location permission to access this feature.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void permissionGranted(int requestCode) {
        if (BaseActivity.REQUEST_LOCATION_PERMISSION == requestCode) {
            startActivity(NetworkSecurityActivity.getStartIntent(this));
        } else if (requestCode == LOCATION_PERMISSION_FOR_SPOOF) {
            mConnSettingsPresenter.onPermissionProvided();
        }
    }

    @Override
    public void setAutoStartOnBootToggle(int toggleDrawable) {
        bootToggleBtn.setImageDrawable(ResourcesCompat.getDrawable(getResources(), toggleDrawable, getTheme()));
    }

    @Override
    public void setGpsSpoofingToggle(int toggleDrawable) {
        gpsToggleBtn.setImageDrawable(ResourcesCompat.getDrawable(getResources(), toggleDrawable, getTheme()));
    }

    @Override
    public void setKeepAlive(String keepAlive) {
        keepAliveEditText.setText(keepAlive);
    }

    @Override
    public void setKeepAliveContainerVisibility(boolean visibility, boolean isAuto) {
        if (visibility && isAuto) {
            constraintKeepAliveAuto.applyTo(keepAliveTabParent);
            return;
        }
        if (visibility) {
            constraintKeepAliveManual.applyTo(keepAliveTabParent);
            return;
        }
        constraintKeepAliveHidden.applyTo(keepAliveTabParent);
    }

    @Override
    public void setLanBypassToggle(int toggleDrawable) {
        lanToggleBtn.setImageDrawable(ResourcesCompat.getDrawable(getResources(), toggleDrawable, getTheme()));
    }

    @Override
    public void setPacketSize(String size) {
        runOnUiThread(() -> ed_package_size.setText(size));
    }

    @Override
    public void setProtocolTextView(String protocolText) {
        tvCurrentProtocol.setText(protocolText);
    }

    @Override
    public void setSplitTunnelText(String onOff, Integer color) {
        mConnectionSettingsLog.info("Setting tunnel status " + onOff);
        tvSplitTunnelStatus.setText(onOff);
        tvSplitTunnelStatus.setTextColor(color);
    }

    @Override
    @RequiresApi(api = 19)
    public void setupLayoutForAutoMode(Integer textViewColorOnFocus, Integer textViewColorOffFocus) {
        mConnectionSettingsLog.info("Setting up layout for auto connection mode...");
        mConstraintSetParent.setVisibility(R.id.cl_protocol, ConstraintSet.GONE);
        mConstraintSetParent.setVisibility(R.id.cl_port, ConstraintSet.GONE);
        mConstraintSetParent.setVisibility(R.id.img_connection_settings_divider, ConstraintSet.GONE);
        mConstraintSetParent.setVisibility(R.id.img_connection_mode_manual_divider, ConstraintSet.GONE);
        mConstraintSetParent.connect(R.id.img_connection_mode_selection_mask, ConstraintSet.START,
                R.id.tv_connection_mode_auto, ConstraintSet.START);
        mConstraintSetParent.connect(R.id.img_connection_mode_selection_mask, ConstraintSet.END,
                R.id.tv_connection_mode_auto, ConstraintSet.END);
        tvConnectionModeAuto.setTextColor(textViewColorOnFocus);
        tvConnectionModeManual.setTextColor(textViewColorOffFocus);

        //Start transition
        androidx.transition.ChangeBounds mTransition = new androidx.transition.ChangeBounds();
        mTransition.setDuration(150);
        androidx.transition.TransitionManager.beginDelayedTransition(mConstraintLayoutConnection, mTransition);
        mConstraintSetParent.applyTo(mConstraintLayoutConnection);
    }

    @Override
    public void setupLayoutForKeepAliveModeAuto(Integer textViewColorOnFocus, Integer textViewColorOffFocus) {
        tvKeepAliveModeAuto.setTextColor(textViewColorOnFocus);
        tvKeepAliveModeManual.setTextColor(textViewColorOffFocus);
        //Start transition
        mSupportTransition = new androidx.transition.AutoTransition();
        mSupportTransition.setDuration(AnimConstants.CONNECTION_MODE_ANIM_DURATION);
        androidx.transition.TransitionManager.beginDelayedTransition(mConstraintLayoutConnection, mSupportTransition);
        constraintKeepAliveAuto.applyTo(keepAliveTabParent);
    }

    @Override
    public void setupLayoutForKeepAliveModeManual(Integer textViewColorOnFocus, Integer textViewColorOffFocus) {

        tvKeepAliveModeAuto.setTextColor(textViewColorOffFocus);
        tvKeepAliveModeManual.setTextColor(textViewColorOnFocus);
        //Start transition
        mSupportTransition = new androidx.transition.AutoTransition();
        mSupportTransition.setDuration(AnimConstants.CONNECTION_MODE_ANIM_DURATION);
        androidx.transition.TransitionManager.beginDelayedTransition(mConstraintLayoutConnection, mSupportTransition);
        constraintKeepAliveManual.applyTo(keepAliveTabParent);
    }

    @Override
    @RequiresApi(api = 19)
    public void setupLayoutForManualMode(Integer textViewColorOnFocus, Integer textViewColorOffFocus) {
        mConnectionSettingsLog.info("Setting up layout for manual connection mode...");
        mConstraintSetParent.setVisibility(R.id.cl_protocol, ConstraintSet.VISIBLE);
        mConstraintSetParent.setVisibility(R.id.cl_port, ConstraintSet.VISIBLE);
        mConstraintSetParent.setVisibility(R.id.img_connection_settings_divider, ConstraintSet.VISIBLE);
        mConstraintSetParent.setVisibility(R.id.img_connection_mode_manual_divider, ConstraintSet.VISIBLE);
        mConstraintSetParent.connect(R.id.img_connection_mode_selection_mask, ConstraintSet.START,
                R.id.tv_connection_mode_manual, ConstraintSet.START);
        mConstraintSetParent.connect(R.id.img_connection_mode_selection_mask, ConstraintSet.LEFT,
                R.id.tv_connection_mode_manual, ConstraintSet.LEFT);
        mConstraintSetParent.connect(R.id.img_connection_mode_selection_mask, ConstraintSet.END,
                R.id.tv_connection_mode_manual, ConstraintSet.END);
        mConstraintSetParent.connect(R.id.img_connection_mode_selection_mask, ConstraintSet.RIGHT,
                R.id.tv_connection_mode_manual, ConstraintSet.RIGHT);
        tvConnectionModeManual.setTextColor(textViewColorOnFocus);
        tvConnectionModeAuto.setTextColor(textViewColorOffFocus);

        //Start transition
        mTransition = new AutoTransition();
        mTransition.setDuration(AnimConstants.CONNECTION_MODE_ANIM_DURATION);
        mTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionCancel(Transition transition) {
                transition.removeListener(this);
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                mConnectionSettingsLog.info("Connection mode transition finished...");
                mConnSettingsPresenter.onManualLayoutSetupCompleted();
                transition.removeListener(this);
            }

            @Override
            public void onTransitionPause(Transition transition) {
                transition.removeListener(this);
            }

            @Override
            public void onTransitionResume(Transition transition) {

            }

            @Override
            public void onTransitionStart(Transition transition) {

            }
        });

        TransitionManager.beginDelayedTransition(mConstraintLayoutConnection, mTransition);
        mConstraintSetParent.applyTo(mConstraintLayoutConnection);
    }

    @Override
    public void setupLayoutForPackageSizeModeAuto(Integer textViewColorOnFocus, Integer textViewColorOffFocus) {
        mConnectionSettingsLog.info("Setting up layout for auto connection mode...");
        mConstraintSetParent.setVisibility(R.id.cl_packet_size, ConstraintSet.GONE);
        mConstraintSetParent.setVisibility(R.id.img_packet_size_settings_divider, ConstraintSet.GONE);
        // mConstraintSetParent.setVisibility(R.id.img_packet_manual_size_settings_divider, ConstraintSet.GONE);
        mConstraintSetParent.connect(R.id.img_packet_size_selection_mask, ConstraintSet.START,
                R.id.tv_packet_size_mode_auto, ConstraintSet.START);
        mConstraintSetParent.connect(R.id.img_packet_size_selection_mask, ConstraintSet.LEFT,
                R.id.tv_packet_size_mode_auto, ConstraintSet.LEFT);
        mConstraintSetParent.connect(R.id.img_packet_size_selection_mask, ConstraintSet.END,
                R.id.tv_packet_size_mode_auto, ConstraintSet.END);
        mConstraintSetParent.connect(R.id.img_packet_size_selection_mask, ConstraintSet.RIGHT,
                R.id.tv_packet_size_mode_auto, ConstraintSet.RIGHT);
        tvPacketSizeModeAuto.setTextColor(textViewColorOnFocus);
        tvPacketSizeModeManual.setTextColor(textViewColorOffFocus);

        //Start transition
        mSupportTransition = new androidx.transition.AutoTransition();
        mSupportTransition.setDuration(AnimConstants.CONNECTION_MODE_ANIM_DURATION);
        androidx.transition.TransitionManager.beginDelayedTransition(mConstraintLayoutConnection, mSupportTransition);
        mConstraintSetParent.applyTo(mConstraintLayoutConnection);
    }

    @Override
    public void setupLayoutForPackageSizeModeManual(Integer textViewColorOnFocus, Integer textViewColorOffFocus) {
        mConnectionSettingsLog.info("Setting up layout for manual connection mode...");
        mConstraintSetParent.setVisibility(R.id.cl_packet_size, ConstraintSet.VISIBLE);
        // mConstraintSetParent.setVisibility(R.id.img_auto_mtu_settings_divider, ConstraintSet.VISIBLE);
        mConstraintSetParent.setVisibility(R.id.img_packet_size_settings_divider, ConstraintSet.VISIBLE);
        mConstraintSetParent.connect(R.id.img_packet_size_selection_mask, ConstraintSet.START,
                R.id.tv_packet_size_mode_manual, ConstraintSet.START);
        mConstraintSetParent.connect(R.id.img_packet_size_selection_mask, ConstraintSet.LEFT,
                R.id.tv_packet_size_mode_manual, ConstraintSet.LEFT);
        mConstraintSetParent.connect(R.id.img_packet_size_selection_mask, ConstraintSet.END,
                R.id.tv_packet_size_mode_manual, ConstraintSet.END);
        mConstraintSetParent.connect(R.id.img_packet_size_selection_mask, ConstraintSet.RIGHT,
                R.id.tv_packet_size_mode_manual, ConstraintSet.RIGHT);
        tvPacketSizeModeManual.setTextColor(textViewColorOnFocus);
        tvPacketSizeModeAuto.setTextColor(textViewColorOffFocus);

        //Start transition
        mSupportTransition = new androidx.transition.AutoTransition();
        mSupportTransition.setDuration(AnimConstants.CONNECTION_MODE_ANIM_DURATION);
        androidx.transition.TransitionManager.beginDelayedTransition(mConstraintLayoutConnection, mSupportTransition);
        mConstraintSetParent.applyTo(mConstraintLayoutConnection);
    }

    @Override
    public void setupPortMapAdapter(String port, List<String> portMap) {
        mConnectionSettingsLog.info("Setting up protocol adapter...");
        ArrayAdapter<String> portMapAdapter = new ArrayAdapter<>(this, R.layout.drop_down_layout,
                R.id.tv_drop_down, portMap);
        spinnerPort.setAdapter(portMapAdapter);
        spinnerPort.setSelected(false);
        spinnerPort.setSelection(portMapAdapter.getPosition(port));
        tvCurrentPort.setText(port);
    }

    @Override
    public void setupProtocolAdapter(String protocol, String[] mProtocols) {
        mConnectionSettingsLog.info("Setting up protocol adapter...");
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(this, R.layout.drop_down_layout,
                R.id.tv_drop_down, mProtocols);
        spinnerProtocol.setAdapter(spinnerAdapter);
        spinnerProtocol.setSelected(false);
        spinnerProtocol.setSelection(spinnerAdapter.getPosition(protocol));
        tvCurrentProtocol.setText(protocol);
    }

    @Override
    public void showAlwaysOnSettingDialog() {
        AlwaysOnFragment alwaysOnFragment = new AlwaysOnFragment(true);
        if (!getSupportFragmentManager().isStateSaved() && !alwaysOnFragment.isAdded()) {
            alwaysOnFragment.show(getSupportFragmentManager(), null);
        }
    }

    @Override
    public void showGpsSpoofing() {
        clGpsSettings.setVisibility(View.VISIBLE);
        gpsSpoofDivider.setVisibility(View.VISIBLE);
        mConstraintSetParent.setVisibility(R.id.cl_gps_settings, ConstraintSet.VISIBLE);
        mConstraintSetParent.setVisibility(R.id.img_gps_settings_divider, ConstraintSet.VISIBLE);
    }

    @Override
    public void showLocationRational(int requestCode) {
        LocationPermissionRationale locationPermissionRationale = new LocationPermissionRationale();
        if (!getSupportFragmentManager().isStateSaved() && !locationPermissionRationale.isAdded()) {
            locationPermissionRationale.show(getSupportFragmentManager(), null);
        }
    }

    @Override
    public void showToast(String toastString) {
        Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show();
    }

    private void setKeepAliveTab() {
        constraintKeepAliveAuto.clone(this, R.layout.connection_keep_alive_tab);

        constraintKeepAliveAuto.setVisibility(R.id.keep_alive_label, ConstraintSet.VISIBLE);
        constraintKeepAliveAuto.setVisibility(R.id.keep_alive_manual, ConstraintSet.VISIBLE);
        constraintKeepAliveAuto.setVisibility(R.id.keep_alive_auto, ConstraintSet.VISIBLE);
        constraintKeepAliveAuto
                .connect(R.id.keep_alive_mask, ConstraintSet.END, R.id.keep_alive_auto, ConstraintSet.END);
        constraintKeepAliveAuto
                .connect(R.id.keep_alive_mask, ConstraintSet.START, R.id.keep_alive_auto, ConstraintSet.START);
        constraintKeepAliveAuto.setVisibility(R.id.keep_alive_edit_view, ConstraintSet.GONE);
        constraintKeepAliveAuto.setVisibility(R.id.keep_alive_edit_button, ConstraintSet.GONE);

        constraintKeepAliveManual.clone(this, R.layout.connection_keep_alive_tab);

        constraintKeepAliveHidden.clone(this, R.layout.connection_keep_alive_tab);
        constraintKeepAliveHidden.setVisibility(R.id.keep_alive_label, ConstraintSet.GONE);
        constraintKeepAliveHidden.setVisibility(R.id.keep_alive_manual, ConstraintSet.GONE);
        constraintKeepAliveHidden.setVisibility(R.id.keep_alive_auto, ConstraintSet.GONE);
        constraintKeepAliveHidden.setVisibility(R.id.keep_alive_mask, ConstraintSet.GONE);
        constraintKeepAliveHidden.setVisibility(R.id.keep_alive_edit_view, ConstraintSet.GONE);
        constraintKeepAliveHidden.setVisibility(R.id.keep_alive_edit_button, ConstraintSet.GONE);
    }

    private void showKeyboard(EditText editText) {
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.showSoftInput(editText, 0);
    }

    @Override
    public void setDecoyTrafficToggle(int toggleDrawable){
        decoyTrafficToggleBtn.setImageDrawable(ResourcesCompat.getDrawable(getResources(), toggleDrawable, getTheme()));
    }

    @Override
    public void setDecoyTrafficContainerVisibility(boolean visibility) {
        clDecoyTrafficSettings.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showExtraDataUseWarning(){
        ExtraDataUseWarningFragment extraDataUseWarningFragment = new ExtraDataUseWarningFragment();
        extraDataUseWarningFragment.showNow(getSupportFragmentManager(),extraDataUseWarningFragment.getClass().getName());
    }

    @Override
    public void turnOnDecoyTraffic() {
        mConnSettingsPresenter.turnOnDecoyTraffic();
    }

    @Override
    public void setupFakeTrafficVolumeAdapter(String selectedValue, String[] values) {
        mConnectionSettingsLog.info("Setting up fake traffic volume adapter.");
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(this, R.layout.drop_down_layout,
                R.id.tv_drop_down, values);
        spinnerFakeTrafficVolume.setAdapter(spinnerAdapter);
        spinnerFakeTrafficVolume.setSelected(false);
        spinnerFakeTrafficVolume.setSelection(spinnerAdapter.getPosition(selectedValue));
    }

    @Override
    public void setPotentialTrafficUse(String value){
        tvCurrentTrafficUse.setText(value);
    }

    @Override
    public void setTrafficLowerLimit(String value) {
        tvCurrentFakeTrafficVolume.setText(value);
    }

    @Override
    public void showAutoStartOnBoot() {
        clAutoStartOnBoot.setVisibility(View.VISIBLE);
        lanDivider.setVisibility(View.VISIBLE);
    }
}
