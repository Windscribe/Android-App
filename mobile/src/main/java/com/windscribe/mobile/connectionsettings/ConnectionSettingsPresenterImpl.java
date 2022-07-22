/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.connectionsettings;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.windscribe.vpn.constants.PreferencesKeyConstants.CONNECTION_MODE_AUTO;
import static com.windscribe.vpn.constants.PreferencesKeyConstants.CONNECTION_MODE_MANUAL;
import static com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_IKev2;
import static com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_STEALTH;
import static com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_TCP;
import static com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_UDP;
import static com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WIRE_GUARD;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.api.response.PortMapResponse;
import com.windscribe.vpn.commonutils.Ext;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import com.windscribe.vpn.decoytraffic.FakeTrafficVolume;
import com.windscribe.vpn.mocklocation.MockLocationManager;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class ConnectionSettingsPresenterImpl implements ConnectionSettingsPresenter {

    private static final String TAG = "con_settings_p";
    private final Logger mPresenterLog = LoggerFactory.getLogger(TAG);
    private int currentPoint = 1500;
    private ActivityInteractor mConnSettingsInteractor;
    private ConnectionSettingsView mConnSettingsView;

    @Inject
    public ConnectionSettingsPresenterImpl(ConnectionSettingsView mConnSettingsView, ActivityInteractor mConnSettingsInteractor) {
        this.mConnSettingsView = mConnSettingsView;
        this.mConnSettingsInteractor = mConnSettingsInteractor;
    }

    @Override
    public void onStart() {
        //Set split tunnel text view
        if (mConnSettingsInteractor.getAppPreferenceInterface().getSplitTunnelToggle()) {
            //Split tunnel is on
            mPresenterLog.info("Split tunnel settings is ON");
            mConnSettingsView.setSplitTunnelText(mConnSettingsInteractor.getResourceString(R.string.on),
                    mConnSettingsInteractor.getColorResource(R.color.colorNeonGreen));
        } else {
            mPresenterLog.info("Split tunnel settings is OFF");
            mConnSettingsView.setSplitTunnelText(mConnSettingsInteractor.getResourceString(R.string.off),
                    mConnSettingsInteractor.getColorResource(R.color.colorWhite50));
        }

        setAutoStartMenu();

        if (mConnSettingsInteractor.getAppPreferenceInterface().getLanByPass()) {
            mConnSettingsView.setLanBypassToggle(R.drawable.ic_toggle_button_on);
        } else {
            mConnSettingsView.setLanBypassToggle(R.drawable.ic_toggle_button_off);
        }

        if (mConnSettingsInteractor.getAppPreferenceInterface().isDecoyTrafficOn()) {
            mConnSettingsView.setDecoyTrafficToggle(R.drawable.ic_toggle_button_on);
        } else {
            mConnSettingsView.setDecoyTrafficToggle(R.drawable.ic_toggle_button_off);
        }
        setDecoyTrafficParameters();
    }

    @Override
    public void onDestroy() {
        mConnSettingsInteractor.getDecoyTrafficController().load();
        mConnSettingsInteractor.getCompositeDisposable();
        if (!mConnSettingsInteractor.getCompositeDisposable().isDisposed()) {
            mPresenterLog.info("Disposing observer...");
            mConnSettingsInteractor.getCompositeDisposable().dispose();
        }
        mConnSettingsView = null;
        mConnSettingsInteractor = null;
    }

    @Override
    public void init() {
        setupLayoutBasedOnConnectionMode();
        setUpAutoModePorts();
        setupPacketSizeMode();
        setUpKeepAlive();
    }

    @Override
    public void onAllowLanClicked() {
        if (mConnSettingsInteractor.getAppPreferenceInterface().getLanByPass()) {
            mConnSettingsView.setLanBypassToggle(R.drawable.ic_toggle_button_off);
            mConnSettingsInteractor.getAppPreferenceInterface().setLanByPass(false);
            mPresenterLog.info("Setting lan bypass to true");
        } else {
            mConnSettingsView.setLanBypassToggle(R.drawable.ic_toggle_button_on);
            mConnSettingsInteractor.getAppPreferenceInterface().setLanByPass(true);
            mPresenterLog.info("Setting lan bypass to false");
        }
    }

    @Override
    public void onAutoFillPacketSizeClicked() {
        getMtuSizeFromNetworkInterface();
    }

    @Override
    public void onAutoStartOnBootClick() {
        if (mConnSettingsInteractor.getAppPreferenceInterface().getAutoStartOnBoot()) {
            mConnSettingsView.setAutoStartOnBootToggle(R.drawable.ic_toggle_button_off);
            mConnSettingsInteractor.getAppPreferenceInterface().setAutoStartOnBoot(false);
            mPresenterLog.info("Setting auto start on boot to false");
        } else {
            mConnSettingsView.setAutoStartOnBootToggle(R.drawable.ic_toggle_button_on);
            mConnSettingsInteractor.getAppPreferenceInterface().setAutoStartOnBoot(true);
            mPresenterLog.info("Setting auto start on boot to true");
        }
    }

    @Override
    public void onDecoyTrafficClick() {
        if (mConnSettingsInteractor.getAppPreferenceInterface().isDecoyTrafficOn()) {
            mConnSettingsView.setDecoyTrafficToggle(R.drawable.ic_toggle_button_off);
            mConnSettingsInteractor.getAppPreferenceInterface().setDecoyTrafficOn(false);
            mConnSettingsInteractor.getAppPreferenceInterface().setPacketSize(1500);
            mPresenterLog.info("Setting decoy traffic to false");
            mConnSettingsInteractor.getDecoyTrafficController().stop();
        } else {
            mConnSettingsView.showExtraDataUseWarning();
        }
    }

    @Override
    public void turnOnDecoyTraffic() {
        mConnSettingsView.setDecoyTrafficToggle(R.drawable.ic_toggle_button_on);
        mConnSettingsInteractor.getAppPreferenceInterface().setDecoyTrafficOn(true);
        mPresenterLog.info("Setting decoy traffic to true");
        if(mConnSettingsInteractor.getVpnConnectionStateManager().isVPNConnected()){
            mConnSettingsInteractor.getDecoyTrafficController().load();
            mConnSettingsInteractor.getDecoyTrafficController().start();
        }
    }

    private void setDecoyTrafficParameters() {
        String[] multiplierOptions = Ext.INSTANCE.getFakeTrafficVolumeOptions();
        String lowerLimit = mConnSettingsInteractor.getAppPreferenceInterface().getFakeTrafficVolume().name();
        mConnSettingsView.setupFakeTrafficVolumeAdapter(lowerLimit, multiplierOptions);
        resetPotentialTrafficInfo();
    }

    @Override
    public void onFakeTrafficVolumeSelected(String label) {
        mConnSettingsInteractor.getAppPreferenceInterface().setFakeTrafficVolume(FakeTrafficVolume.valueOf(label));
        resetPotentialTrafficInfo();
    }

    @Override
    public void onConnectionModeAutoClicked() {
        //Save connection mode to preference only if  manual mode is selected
        if (!CONNECTION_MODE_AUTO.equals(mConnSettingsInteractor.getSavedConnectionMode())) {
            mConnSettingsInteractor.saveConnectionMode(PreferencesKeyConstants.CONNECTION_MODE_AUTO);
            mConnSettingsInteractor.getAppPreferenceInterface().setChosenProtocol(null);
            setUpAutoModePorts();
            mConnSettingsInteractor.getProtocolManager().loadProtocolConfigs();
        }
    }

    @Override
    public void onConnectionModeManualClicked() {
        //Save connection mode to preference only if a different connection mode is selected
        if (!CONNECTION_MODE_MANUAL.equals(mConnSettingsInteractor.getSavedConnectionMode())) {
            mConnSettingsInteractor.saveConnectionMode(PreferencesKeyConstants.CONNECTION_MODE_MANUAL);
            mConnSettingsView.setKeepAliveContainerVisibility(mConnSettingsInteractor.getAppPreferenceInterface().getSavedProtocol().equals(PROTO_IKev2));
        }
    }

    @Override
    public void onGpsSpoofingClick() {
        if (isPermissionProvided()) {
            onPermissionProvided();
        } else {
            mConnSettingsView.getLocationPermission(ConnectionSettingsActivity.LOCATION_PERMISSION_FOR_SPOOF);
        }

    }

    @Override
    public void onHotStart() {
        setGpsSpoofingMenu();
    }

    @Override
    public void onKeepAliveAutoModeClicked() {
        boolean keepAliveSizeModeAuto = mConnSettingsInteractor.getAppPreferenceInterface().isKeepAliveModeAuto();
        if (!keepAliveSizeModeAuto) {
            mConnSettingsInteractor.getAppPreferenceInterface().setKeepAliveModeAuto(true);
            mConnSettingsView.setKeepAliveModeAdapter("Auto", new String[]{"Auto", "Manual"});
        }
    }

    @Override
    public void onKeepAliveManualModeClicked() {
        boolean keepAliveSizeModeAuto = mConnSettingsInteractor.getAppPreferenceInterface().isKeepAliveModeAuto();
        if (keepAliveSizeModeAuto) {
            setKeepAlive(mConnSettingsInteractor.getAppPreferenceInterface().getKeepAlive());
            mConnSettingsInteractor.getAppPreferenceInterface().setKeepAliveModeAuto(false);
            mConnSettingsView.setKeepAliveModeAdapter("Manual", new String[]{"Auto", "Manual"});
        }
    }

    @Override
    public void onManualLayoutSetupCompleted() {
        mPresenterLog.info("Manual layout setup is completed...");
        setProtocolAdapter();
    }

    @Override
    public void onPacketSizeAutoModeClicked() {
        boolean packetSizeModeAuto = mConnSettingsInteractor.getAppPreferenceInterface().isPackageSizeModeAuto();
        if (!packetSizeModeAuto) {
            mConnSettingsInteractor.getAppPreferenceInterface().setPacketSizeModeToAuto(true);
        }
    }

    @Override
    public void onPacketSizeManualModeClicked() {
        boolean packetSizeModeAuto = mConnSettingsInteractor.getAppPreferenceInterface().isPackageSizeModeAuto();
        if (packetSizeModeAuto) {
            mConnSettingsInteractor.getAppPreferenceInterface().setPacketSizeModeToAuto(false);
        }
    }

    @Override
    public void onPermissionProvided() {
        if (MockLocationManager.isAppSelectedInMockLocationList(Windscribe.getAppContext())
                && MockLocationManager.isDevModeOn(Windscribe.getAppContext())) {
            if (mConnSettingsInteractor.getAppPreferenceInterface().isGpsSpoofingOn()) {
                mConnSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_off);
                mConnSettingsInteractor.getAppPreferenceInterface().setGpsSpoofing(false);
                mPresenterLog.info("Setting gps spoofing to true");
            } else {
                mConnSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_on);
                mConnSettingsInteractor.getAppPreferenceInterface().setGpsSpoofing(true);
                mPresenterLog.info("Setting gps spoofing to false");
            }
        } else {
            mConnSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_off);
            mConnSettingsInteractor.getAppPreferenceInterface().setGpsSpoofing(false);
            mConnSettingsView.openGpsSpoofSettings();
        }
    }

    @Override
    public void onPortSelected(String heading, String port) {
        mPresenterLog.info("Saving selected port...");
        mConnSettingsInteractor.loadPortMap(portMapResponse -> {
            String protocol = getProtocolFromHeading(portMapResponse, heading);
            switch (protocol) {
                case PROTO_IKev2:
                    mPresenterLog.info("Saving selected IKev2 port...");
                    mConnSettingsInteractor.getAppPreferenceInterface().saveIKEv2Port(port);
                    break;
                case PROTO_UDP:
                    mPresenterLog.info("Saving selected udp port...");
                    mConnSettingsInteractor.saveUDPPort(port);
                    break;
                case PROTO_TCP:
                    mPresenterLog.info("Saving selected tcp port...");
                    mConnSettingsInteractor.saveTCPPort(port);
                    break;
                case PROTO_STEALTH:
                    mPresenterLog.info("Saving selected stealth port...");
                    mConnSettingsInteractor.saveSTEALTHPort(port);
                    break;
                case PROTO_WIRE_GUARD:
                    mPresenterLog.info("Saving selected wire guard port...");
                    mConnSettingsInteractor.getAppPreferenceInterface().saveWireGuardPort(port);
                    break;
                default:
                    mPresenterLog.info("Saving default port (udp)...");
                    mConnSettingsInteractor.saveUDPPort(port);
            }
            mConnSettingsInteractor.getProtocolManager().loadProtocolConfigs();
        });
    }

    @Override
    public void onProtocolSelected(String heading) {
        mConnSettingsInteractor.loadPortMap(portMapResponse -> {
            String protocol = getProtocolFromHeading(portMapResponse, heading);
            String savedProtocol = mConnSettingsInteractor.getSavedProtocol();
            if (savedProtocol.equals(protocol)) {
                //Do nothing
                mPresenterLog.info("Protocol re-selected is same as saved. No action taken...");
            } else {
                mPresenterLog.info("Saving selected protocol...");
                mConnSettingsInteractor.saveProtocol(protocol);
                setPortMapAdapter(heading);
                mConnSettingsInteractor.getProtocolManager().loadProtocolConfigs();
            }
        });
    }

    @Override
    public void onSplitTunnelingOptionClicked() {
        mPresenterLog.info("Opening split tunnel settings activity..");
        mConnSettingsView.gotoSplitTunnelingSettings();
    }

    @Override
    public void saveKeepAlive(String keepAlive) {
        mConnSettingsInteractor.getAppPreferenceInterface().setKeepAlive(keepAlive);
    }

    @Override
    public void setKeepAlive(String keepAlive) {
        mConnSettingsInteractor.getAppPreferenceInterface().setKeepAlive(keepAlive);
    }

    @Override
    public void setPacketSize(String size) {
        mConnSettingsInteractor.getAppPreferenceInterface().setPacketSize(Integer.parseInt(size));
    }

    @Override
    public void setTheme(Context context) {
        String savedThem = mConnSettingsInteractor.getAppPreferenceInterface().getSelectedTheme();
        mPresenterLog.debug("Setting theme to " + savedThem);
        if (savedThem.equals(PreferencesKeyConstants.DARK_THEME)) {
            context.setTheme(R.style.DarkTheme);
        } else {
            context.setTheme(R.style.LightTheme);
        }
    }

    public void setUpAutoModePorts() {
        mPresenterLog.debug("Setting auto mode ports.");
        mConnSettingsInteractor.loadPortMap(portMapResponse -> {
            for (PortMapResponse.PortMap portMap : portMapResponse.getPortmap()) {
                if (portMap.getProtocol().equals(PROTO_IKev2)) {
                    mConnSettingsInteractor.getIKev2Port();
                }
                if (portMap.getProtocol().equals(PROTO_UDP)) {
                    mConnSettingsInteractor.getSavedUDPPort();
                }
                if (portMap.getProtocol().equals(PROTO_TCP)) {
                    mConnSettingsInteractor.getSavedTCPPort();
                }
                if (portMap.getProtocol().equals(PROTO_STEALTH)) {
                    mConnSettingsInteractor.getSavedSTEALTHPort();
                }
                if (portMap.getProtocol().equals(PROTO_WIRE_GUARD)) {
                    mConnSettingsInteractor.getWireGuardPort();
                }
            }
            String savedProtocol = mConnSettingsInteractor.getSavedProtocol();
            String savedConnectionMode = mConnSettingsInteractor.getSavedConnectionMode();
            mConnSettingsView.setKeepAliveContainerVisibility(
                    savedProtocol.equals(PROTO_IKev2) && savedConnectionMode.equals(CONNECTION_MODE_MANUAL));
            setUpKeepAlive();
        });
    }

    public void setUpKeepAlive() {
        boolean isKeepAliveModeAuto = mConnSettingsInteractor.getAppPreferenceInterface().isKeepAliveModeAuto();
        if (isKeepAliveModeAuto) {
            mConnSettingsView.setKeepAliveModeAdapter("Auto", new String[]{"Auto", "Manual"});
        }else{
            mConnSettingsView.setKeepAliveModeAdapter("Manual", new String[]{"Auto", "Manual"});
        }
        String keepAliveTime = mConnSettingsInteractor.getAppPreferenceInterface().getKeepAlive();
        mConnSettingsView.setKeepAlive(keepAliveTime);
    }


    public void setupLayoutBasedOnConnectionMode() {
        setGpsSpoofingMenu();
        String savedConnectionMode = mConnSettingsInteractor.getSavedConnectionMode();
        mConnSettingsView.setupConnectionModeAdapter(savedConnectionMode,new String[]{"Auto","Manual"});
        setProtocolAdapter();
    }


    public void setupPacketSizeMode() {
        boolean packetSizeModeAuto = mConnSettingsInteractor.getAppPreferenceInterface().isPackageSizeModeAuto();
        if (packetSizeModeAuto) {
            mConnSettingsView.setupPacketSizeModeAdapter("Auto", new String[]{"Auto", "Manual"});
        }else{
            mConnSettingsView.setupPacketSizeModeAdapter("Manual", new String[]{"Auto", "Manual"});
        }
        int packetSize = mConnSettingsInteractor.getAppPreferenceInterface().getPacketSize();
        mConnSettingsView.setPacketSize(String.valueOf(packetSize));
    }

    void setProtocolAdapter() {
        mConnSettingsInteractor.loadPortMap(portMapResponse -> {
            String savedProtocol = mConnSettingsInteractor.getSavedProtocol();
            PortMapResponse.PortMap selectedPortMap = null;
            List<String> protocols = new ArrayList<>();
            for (PortMapResponse.PortMap portMap : portMapResponse.getPortmap()) {
                if (portMap.getProtocol().equals(savedProtocol)) {
                    selectedPortMap = portMap;
                }
                protocols.add(portMap.getHeading());
            }
            selectedPortMap = selectedPortMap != null ? selectedPortMap : portMapResponse.getPortmap().get(0);
            mConnSettingsView.setupProtocolAdapter(selectedPortMap.getHeading(), protocols.toArray(new String[0]));
            setPortMapAdapter(selectedPortMap.getHeading());
        });
    }

    // MTU detection experimental feature
    private void getMtuSizeFromNetworkInterface() {
        // check network first
        if (mConnSettingsInteractor.getVpnConnectionStateManager().isVPNConnected()) {
            mConnSettingsView.showToast("Disconnect from VPN");
            return;
        }
        ConnectivityManager manager = (ConnectivityManager) Windscribe.getAppContext()
                .getSystemService(CONNECTIVITY_SERVICE);
        if (manager.getActiveNetworkInfo() == null || !manager.getActiveNetworkInfo().isConnected()) {
            mConnSettingsView.showToast("No Network Detected");
            return;
        }
        mConnSettingsView.packetSizeDetectionProgress(true);
        mConnSettingsView.setPacketSize("Auto detecting packet size...");
        LinkProperties prop = null;
        NetworkInterface iFace;
        Network[] networks = manager.getAllNetworks();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            prop = manager.getLinkProperties(manager.getActiveNetwork());
        } else {
            for (Network network : networks) {
                NetworkInfo networkInfo = manager.getActiveNetworkInfo();
                if (networkInfo.isConnected()) {
                    prop = manager.getLinkProperties(network);
                }
            }
        }
        try {
            if (prop != null) {
                iFace = NetworkInterface.getByName(prop.getInterfaceName());
                currentPoint = iFace.getMTU();
            } else {
                currentPoint = 1500;
            }
            repeatPing();

        } catch (IOException e) {
            e.printStackTrace();
            currentPoint = 1500;
            repeatPing();
        }

    }

    private String getProtocolFromHeading(PortMapResponse portMapResponse, String heading) {
        for (PortMapResponse.PortMap map : portMapResponse.getPortmap()) {
            if (map.getHeading().equals(heading)) {
                return map.getProtocol();
            }
        }
        return PROTO_IKev2;
    }

    private String getSavedPort(String protocol) {
        switch (protocol) {
            case PROTO_IKev2:
                return mConnSettingsInteractor.getIKev2Port();
            case PROTO_UDP:
                return mConnSettingsInteractor.getSavedUDPPort();
            case PROTO_TCP:
                return mConnSettingsInteractor.getSavedTCPPort();
            case PROTO_STEALTH:
                return mConnSettingsInteractor.getSavedSTEALTHPort();
            case PROTO_WIRE_GUARD:
                return mConnSettingsInteractor.getWireGuardPort();
            default:
                return "443";
        }
    }

    private Boolean isMtuSmallEnough(String response) {
        return !response.contains("100% packet loss");
    }

    /**
     *
     */

    private boolean isPermissionProvided() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return ContextCompat
                    .checkSelfPermission(Windscribe.getAppContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private String ping(int value) {
        String size = String.valueOf(value);
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime
                    .exec("/system/bin/ping -c 2 -s " + size + " -i 0.5 -W 3 -M do checkip.windscribe.com");
            InputStream inputStream = process.getInputStream();
            if (inputStream != null) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } else {
                showMtuFailed();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;

        }
    }

    private void repeatPing() {
        mConnSettingsInteractor.getCompositeDisposable()
                .add(Observable.fromCallable(() -> ping(currentPoint)).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<String>() {
                            @Override
                            public void onComplete() {
                                dispose();
                            }

                            @Override
                            public void onError(@NotNull Throwable e) {
                                showMtuFailed();
                                dispose();
                            }

                            @Override
                            public void onNext(@NotNull String s) {
                                if (isMtuSmallEnough(s)) {
                                    showMtuResult();
                                } else {
                                    if (currentPoint > 10) {
                                        currentPoint = currentPoint - 10;
                                        repeatPing();
                                    }
                                }
                            }
                        }));
    }

    private void setAutoStartMenu() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
            mConnSettingsView.showAutoStartOnBoot();
        }
        if (mConnSettingsInteractor.getAppPreferenceInterface().getAutoStartOnBoot()) {
            mConnSettingsView.setAutoStartOnBootToggle(R.drawable.ic_toggle_button_on);
        } else {
            mConnSettingsInteractor.getAppPreferenceInterface().setAutoStartOnBoot(false);
            mConnSettingsView.setAutoStartOnBootToggle(R.drawable.ic_toggle_button_off);
        }
    }

    private void setGpsSpoofingMenu() {
        mConnSettingsView.showGpsSpoofing();
        if (!MockLocationManager
                .isAppSelectedInMockLocationList(Windscribe.getAppContext().getApplicationContext())
                | !MockLocationManager.isDevModeOn(Windscribe.getAppContext()) | !isPermissionProvided()) {
            mConnSettingsInteractor.getAppPreferenceInterface().setGpsSpoofing(false);
        }
        // Gps spoofing
        if (mConnSettingsInteractor.getAppPreferenceInterface().isGpsSpoofingOn()) {
            mConnSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_on);
        } else {
            mConnSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_off);
        }
    }

    private void setPortMapAdapter(final String heading) {
        mConnSettingsInteractor.loadPortMap(portMapResponse -> {
            String protocol = getProtocolFromHeading(portMapResponse, heading);
            assert protocol != null;
            String savedPort = getSavedPort(protocol);

            for (PortMapResponse.PortMap portMap : portMapResponse.getPortmap()) {
                if (portMap.getProtocol().equals(protocol)) {
                    mConnSettingsView.setupPortMapAdapter(savedPort, portMap.getPorts());
                }
            }
            String savedProtocol = mConnSettingsInteractor.getSavedProtocol();
            String savedConnectionMode = mConnSettingsInteractor.getSavedConnectionMode();
            mConnSettingsView.setKeepAliveContainerVisibility(
                    savedProtocol.equals(PROTO_IKev2) && savedConnectionMode.equals(CONNECTION_MODE_MANUAL));
        });
    }

    private void showMtuFailed() {
        mConnSettingsView.setPacketSize("");
        mConnSettingsView.packetSizeDetectionProgress(false);
        mConnSettingsView.showToast("Auto packet size detection failed.");
        mPresenterLog.info("Error getting optimal MTU size.");
    }

    private void showMtuResult() {
        if (mConnSettingsView != null) {
            mConnSettingsView.setPacketSize(String.valueOf(currentPoint));
            mConnSettingsInteractor.getAppPreferenceInterface().setPacketSize(currentPoint);
            mConnSettingsView.showToast("Packet size detected successfully.");
            mConnSettingsView.packetSizeDetectionProgress(false);
            currentPoint = 1500;
        }
    }

    private void resetPotentialTrafficInfo(){
        FakeTrafficVolume trafficVolume = mConnSettingsInteractor.getAppPreferenceInterface().getFakeTrafficVolume();
        if(trafficVolume == FakeTrafficVolume.Low){
            mConnSettingsView.setPotentialTrafficUse(String.format(Locale.getDefault(), "%dMB/Hour", 1737));
        }else if(trafficVolume == FakeTrafficVolume.Medium){
            mConnSettingsView.setPotentialTrafficUse(String.format(Locale.getDefault(), "%dMB/Hour", 6948));
        }else{
            mConnSettingsView.setPotentialTrafficUse(String.format(Locale.getDefault(), "%dMB/Hour", 16572));
        }
    }
}
