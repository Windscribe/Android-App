/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.connectionsettings;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.windscribe.mobile.R;
import com.windscribe.mobile.alert.AlwaysOnFragment;
import com.windscribe.mobile.alert.ExtraDataUseWarningFragment;
import com.windscribe.mobile.alert.LocationPermissionRationale;
import com.windscribe.mobile.alert.PermissionRationaleListener;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.custom_view.preferences.ConnectionModeView;
import com.windscribe.mobile.custom_view.preferences.DecoyTrafficView;
import com.windscribe.mobile.custom_view.preferences.ExpandableDropDownView;
import com.windscribe.mobile.custom_view.preferences.ExpandableToggleView;
import com.windscribe.mobile.custom_view.preferences.KeepAliveView;
import com.windscribe.mobile.custom_view.preferences.PacketSizeView;
import com.windscribe.mobile.custom_view.preferences.ToggleView;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.gpsspoofing.GpsSpoofingSettingsActivity;
import com.windscribe.mobile.networksecurity.NetworkSecurityActivity;
import com.windscribe.mobile.splittunneling.SplitTunnelingActivity;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.alert.ForegroundAlertKt;
import com.windscribe.vpn.constants.FeatureExplainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import kotlin.Unit;

public class ConnectionSettingsActivity extends BaseActivity
        implements ConnectionSettingsView, AlwaysOnFragment.AlwaysOnDialogCallBack, PermissionRationaleListener, ExtraDataUseWarningFragment.CallBack {

    public static final int LOCATION_PERMISSION_FOR_SPOOF = 901;

    private static final String TAG = "conn_settings_a";
    private final Logger logger = LoggerFactory.getLogger(TAG);
    @BindView(R.id.nav_title)
    TextView activityTitleView;
    @Inject
    ConnectionSettingsPresenter presenter;
    @BindView(R.id.connection_parent)
    ConstraintLayout mConstraintLayoutConnection;
    @BindView(R.id.split_tunnel_status)
    TextView splitTunnelStatusView;
    @BindView(R.id.cl_boot_settings)
    ToggleView autoStartToggleView;
    @BindView(R.id.cl_lan_settings)
    ToggleView allowLanToggleView;
    @BindView(R.id.cl_gps_settings)
    ToggleView gpsToggleView;
    @BindView(R.id.cl_connection_mode)
    ExpandableDropDownView connectionModeDropDownView;
    @BindView(R.id.cl_packet_size)
    ExpandableDropDownView packetSizeModeDropDownView;
    @BindView(R.id.cl_keep_alive)
    ExpandableDropDownView keepAliveExpandableView;
    @BindView(R.id.cl_decoy_traffic)
    ExpandableToggleView decoyTrafficToggleView;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, ConnectionSettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.connection_layout, true);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setupCustomViewDelegates();
        logger.info("Setting up layout based on saved mode settings...");
        presenter.init();
        activityTitleView.setText(getString(R.string.connection));
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.onHotStart();
    }

    @Override
    protected void onDestroy() {
        presenter.onDestroy();
        super.onDestroy();
    }

    @OnClick(R.id.always_on_container)
    public void clickOnAlwaysOn() {
        logger.info("User clicked to always on..");
        onGoToSettings();
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

    private void setupCustomViewDelegates() {
        allowLanToggleView.setDelegate(new ToggleView.Delegate() {
            @Override
            public void onToggleClick() {
                logger.info("User clicked on allow lan...");
                presenter.onAllowLanClicked();
            }

            @Override
            public void onExplainClick() {
                openURLInBrowser(FeatureExplainer.ALLOW_LAN);
            }
        });
        gpsToggleView.setDelegate(new ToggleView.Delegate() {
            @Override
            public void onToggleClick() {
                logger.info("User clicked on gps spoof.");
                presenter.onGpsSpoofingClick();
            }

            @Override
            public void onExplainClick() {
                openURLInBrowser(FeatureExplainer.GPS_SPOOFING);
            }
        });
        autoStartToggleView.setDelegate(new ToggleView.Delegate() {
            @Override
            public void onToggleClick() {
                logger.info("User clicked on auto start on boot.");
                presenter.onAutoStartOnBootClick();
            }

            @Override
            public void onExplainClick() {
            }
        });
        decoyTrafficToggleView.setDelegate(new ExpandableToggleView.Delegate() {
            @Override
            public void onToggleClick() {
                presenter.onDecoyTrafficClick();
            }

            @Override
            public void onExplainClick() {
                openURLInBrowser(FeatureExplainer.DECOY_TRAFFIC);
            }
        });

        DecoyTrafficView decoyTrafficView = (DecoyTrafficView) decoyTrafficToggleView.getChildView();
        decoyTrafficView.setDelegate(volume -> presenter.onFakeTrafficVolumeSelected(volume));

        connectionModeDropDownView.setDelegate(new ExpandableDropDownView.Delegate() {
            @Override
            public void onItemSelect(int position) {
                if (position == 0) {
                    presenter.onConnectionModeAutoClicked();
                } else {
                    presenter.onConnectionModeManualClicked();
                }
            }

            @Override
            public void onExplainClick() {
                openURLInBrowser(FeatureExplainer.CONNECTION_MODE);
            }
        });

        ConnectionModeView connectionModeView = (ConnectionModeView) connectionModeDropDownView.getChildView();
        connectionModeView.setDelegate(new ConnectionModeView.Delegate() {
            @Override
            public void onProtocolSelected(@NonNull String protocol) {
                presenter.onProtocolSelected(protocol);
            }

            @Override
            public void onPortSelected(@NonNull String protocol, @NonNull String port) {
                presenter.onPortSelected(protocol, port);
            }
        });
        packetSizeModeDropDownView.setDelegate(new ExpandableDropDownView.Delegate() {
            @Override
            public void onItemSelect(int position) {
                if (position == 0) {
                    presenter.onPacketSizeAutoModeClicked();
                } else {
                    presenter.onPacketSizeManualModeClicked();
                }
            }

            @Override
            public void onExplainClick() {
                openURLInBrowser(FeatureExplainer.PACKET_SIZE);
            }
        });

        PacketSizeView packetSizeView = (PacketSizeView) packetSizeModeDropDownView.getChildView();
        packetSizeView.setDelegate(new PacketSizeView.Delegate() {
            @Override
            public void onAutoFillButtonClick() {
                presenter.onAutoFillPacketSizeClicked();
            }

            @Override
            public void onPacketSizeChanged(@NonNull String packetSize) {
                presenter.setPacketSize(packetSize);
            }
        });
        keepAliveExpandableView.setDelegate(new ExpandableDropDownView.Delegate() {
            @Override
            public void onItemSelect(int position) {
                if (position == 0) {
                    presenter.onKeepAliveAutoModeClicked();
                } else {
                    presenter.onKeepAliveManualModeClicked();
                }
            }

            @Override
            public void onExplainClick() {
                openURLInBrowser(FeatureExplainer.KEEP_ALIVE);
            }
        });

        KeepAliveView keepAliveView = (KeepAliveView) keepAliveExpandableView.getChildView();
        keepAliveView.setDelegate(time -> presenter.saveKeepAlive(time));
    }

    @OnClick(R.id.nav_button)
    public void onBackButtonClicked() {
        logger.info("User clicked on back arrow...");
        onBackPressed();
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

    @OnClick({R.id.split_tunnel_title, R.id.split_tunnel_status, R.id.split_tunnel_right_icon})
    public void onSplitTunnelingClick() {
        logger.info("User clicked on split tunneling...");
        presenter.onSplitTunnelingOptionClicked();
    }

    @OnClick({R.id.network_options_right_icon, R.id.network_options_title})
    public void onWhitelistClick() {
        checkLocationPermission(R.id.connection_parent, BaseActivity.REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void openGpsSpoofSettings() {
        startActivity(GpsSpoofingSettingsActivity.getStartIntent(this));
    }

    @Override
    public void packetSizeDetectionProgress(boolean progress) {
        ((PacketSizeView) packetSizeModeDropDownView.getChildView()).packetSizeDetectionProgress(progress);
    }

    @Override
    public void permissionDenied(int requestCode) {
        Toast.makeText(this, "Please provide location permission to access this feature.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void permissionGranted(int requestCode) {
        if (BaseActivity.REQUEST_LOCATION_PERMISSION == requestCode) {
            if (isBackgroundLocationPermissionGranted()) {
                startActivity(NetworkSecurityActivity.getStartIntent(this));
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ForegroundAlertKt.showAlertDialog("App requires background location permission to access WIFI SSID on Android 11+. If you wish to use this feature, press Okay and select \"Allow all the time\" from the permission dialog.", this::askForBackgroundLocationPermission);
                }
            }
        } else if (requestCode == LOCATION_PERMISSION_FOR_SPOOF) {
            presenter.onPermissionProvided();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Unit askForBackgroundLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_BACKGROUND_PERMISSION);
        return null;
    }

    private boolean isBackgroundLocationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat
                    .checkSelfPermission(Windscribe.getAppContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    @Override
    public void setAutoStartOnBootToggle(int toggleDrawable) {
        autoStartToggleView.setToggleImage(toggleDrawable);
    }

    @Override
    public void setGpsSpoofingToggle(int toggleDrawable) {
        gpsToggleView.setToggleImage(toggleDrawable);
    }

    @Override
    public void setKeepAlive(String keepAlive) {
        ((KeepAliveView) keepAliveExpandableView.getChildView()).setKeepAlive(keepAlive);
    }

    @Override
    public void setLanBypassToggle(int toggleDrawable) {
        allowLanToggleView.setToggleImage(toggleDrawable);
    }

    @Override
    public void setPacketSize(String size) {
        PacketSizeView packetSizeView = (PacketSizeView) packetSizeModeDropDownView.getChildView();
        packetSizeView.setPacketSize(size);
    }

    @Override
    public void setSplitTunnelText(String onOff, Integer color) {
        logger.info("Setting tunnel status " + onOff);
        splitTunnelStatusView.setText(onOff);
        splitTunnelStatusView.setTextColor(color);
    }

    @Override
    public void setupPacketSizeModeAdapter(String savedValue, String[] types) {
        packetSizeModeDropDownView.setAdapter(savedValue, types);
    }

    @Override
    public void setKeepAliveModeAdapter(String savedValue, String[] types) {
        keepAliveExpandableView.setAdapter(savedValue, types);
    }

    @Override
    public void setupPortMapAdapter(String port, List<String> portMap) {
        ConnectionModeView connectionModeView = (ConnectionModeView) connectionModeDropDownView.getChildView();
        connectionModeView.sePortAdapter(port, portMap);
    }

    @Override
    public void setupProtocolAdapter(String protocol, String[] protocols) {
        ConnectionModeView connectionModeView = (ConnectionModeView) connectionModeDropDownView.getChildView();
        connectionModeView.seProtocolAdapter(protocol, protocols);
    }

    @Override
    public void showGpsSpoofing() {
        gpsToggleView.setVisibility(View.VISIBLE);
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

    @Override
    public void showExtraDataUseWarning() {
        ExtraDataUseWarningFragment extraDataUseWarningFragment = new ExtraDataUseWarningFragment();
        extraDataUseWarningFragment.showNow(getSupportFragmentManager(), extraDataUseWarningFragment.getClass().getName());
    }

    @Override
    public void turnOnDecoyTraffic() {
        presenter.turnOnDecoyTraffic();
    }

    @Override
    public void showAutoStartOnBoot() {
        autoStartToggleView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setKeepAliveContainerVisibility(boolean isAutoKeepAlive) {
        keepAliveExpandableView.setVisibility(isAutoKeepAlive ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setupConnectionModeAdapter(String savedValue, String[] connectionModes) {
        connectionModeDropDownView.setAdapter(savedValue, connectionModes);
    }

    @Override
    public void setupFakeTrafficVolumeAdapter(String selectedValue, String[] values) {
        ((DecoyTrafficView) decoyTrafficToggleView.getChildView()).setAdapter(selectedValue, values);
    }

    @Override
    public void setPotentialTrafficUse(String value) {
        ((DecoyTrafficView) decoyTrafficToggleView.getChildView()).setPotentialTraffic(value);
    }

    @Override
    public void setDecoyTrafficToggle(int toggleDrawable) {
        decoyTrafficToggleView.setToggleImage(toggleDrawable);
    }
}
