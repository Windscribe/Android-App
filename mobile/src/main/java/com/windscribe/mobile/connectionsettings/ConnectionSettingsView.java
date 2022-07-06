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

    void setKeepAliveContainerVisibility(boolean visibility, boolean isAuto);

    void setLanBypassToggle(int toggleDrawable);

    void setPacketSize(String size);

    void setProtocolTextView(String protocolText);

    void setSplitTunnelText(String onOff, Integer color);

    void setupLayoutForAutoMode(Integer textViewColorOnFocus, Integer textViewColorOffFocus);

    void setupLayoutForKeepAliveModeAuto(Integer textViewColorOnFocus, Integer textViewColorOffFocus);

    void setupLayoutForKeepAliveModeManual(Integer textViewColorOnFocus, Integer textViewColorOffFocus);

    void setupLayoutForManualMode(Integer textViewColorOnFocus, Integer textViewColorOffFocus);

    void setupLayoutForPackageSizeModeAuto(Integer textViewColorOnFocus, Integer textViewColorOffFocus);

    void setupLayoutForPackageSizeModeManual(Integer textViewColorOnFocus, Integer textViewColorOffFocus);

    void setupPortMapAdapter(String port, List<String> portMap);

    void setupProtocolAdapter(String protocol, String[] mProtocols);

    void showAlwaysOnSettingDialog();

    void showGpsSpoofing();

    void showToast(String toastString);

    void setDecoyTrafficToggle(int toggleDrawable);

    void setDecoyTrafficContainerVisibility(boolean visibility);

    void showExtraDataUseWarning();

    void setupFakeTrafficVolumeAdapter(String selectedValue, String[] values);

    void setTrafficLowerLimit(String value);

    void setPotentialTrafficUse(String value);

    void showAutoStartOnBoot();
}
