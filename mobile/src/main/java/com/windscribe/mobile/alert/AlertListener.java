/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.alert;

import com.windscribe.vpn.serverlist.entity.ConfigFile;

public interface AlertListener {

    void contactSupport();

    void onConfigFileUpdated(ConfigFile configFile);

    void onSubmitUsernameAndPassword(ConfigFile configFile);

    void sendLog();
}
