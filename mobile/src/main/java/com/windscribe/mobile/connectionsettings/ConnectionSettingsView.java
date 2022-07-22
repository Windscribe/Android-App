/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.connectionsettings;

import java.util.List;

public interface ConnectionSettingsView {

    void getLocationPermission(int requestCode);

    void gotoSplitTunnelingSettings();

    void openGpsSpoofSettings();

    void packetSizeDetectionProgress(boolean progress);

    void setAutoStartOnBootToggle(int toggleDrawable);

    void setGpsSpoofingToggle(int toggleDrawable);

    void setKeepAlive(String keepAlive);

    void setLanBypassToggle(int toggleDrawable);

    void setPacketSize(String size);

    void setSplitTunnelText(String onOff, Integer color);

    void setupConnectionModeAdapter(String savedValue, String[] connectionModes);

    void setupPacketSizeModeAdapter(String savedValue, String[] types);

    void setKeepAliveModeAdapter(String savedValue, String[] types);

    void setupPortMapAdapter(String port, List<String> portMap);

    void setupProtocolAdapter(String protocol, String[] mProtocols);

    void showGpsSpoofing();

    void showToast(String toastString);

    void setDecoyTrafficToggle(int toggleDrawable);

    void showExtraDataUseWarning();

    void setupFakeTrafficVolumeAdapter(String selectedValue, String[] values);

    void setPotentialTrafficUse(String value);

    void showAutoStartOnBoot();

    void setKeepAliveContainerVisibility(boolean isAutoKeepAlive);
}
