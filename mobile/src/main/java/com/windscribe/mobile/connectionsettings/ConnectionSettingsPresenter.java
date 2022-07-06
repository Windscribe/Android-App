/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.connectionsettings;

import android.content.Context;

public interface ConnectionSettingsPresenter {

    void init();

    void onAllowLanClicked();

    void onAutoFillPacketSizeClicked();

    void onAutoStartOnBootClick();

    void onConnectionModeAutoClicked();

    void onConnectionModeManualClicked();

    void onDestroy();

    void onGpsSpoofingClick();

    void onHotStart();

    void onKeepAliveAutoModeClicked();

    void onKeepAliveManualModeClicked();

    void onManualLayoutSetupCompleted();

    void onPacketSizeAutoModeClicked();

    void onPacketSizeManualModeClicked();

    void onPermissionProvided();

    void onPortSelected(String protocol, String port);

    void onProtocolSelected(String protocol);

    void onSplitTunnelingOptionClicked();

    void onStart();

    void saveKeepAlive(String keepAlive);

    void setKeepAlive(String keepAlive);

    void setPacketSizeManual(String size);

    void setTheme(Context context);

    void onDecoyTrafficClick();

    void turnOnDecoyTraffic();

    void onFakeTrafficVolumeSelected(String label);
}
