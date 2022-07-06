/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.splittunneling;

import com.windscribe.mobile.adapter.InstalledAppsAdapter;

public interface SplitTunnelingView {

    String[] getSplitRoutingModes();

    void hideTunnelSettingsLayout();

    void restartConnection();

    void setRecyclerViewAdapter(InstalledAppsAdapter mAdapter);

    void setSplitModeTextView(String mode, int textDescription);

    void setSplitRoutingModeAdapter(String[] modes, String savedMode);

    void setupToggleImage(Integer resourceId);

    void showProgress(boolean progress);

    void showTunnelSettingsLayout();

}
