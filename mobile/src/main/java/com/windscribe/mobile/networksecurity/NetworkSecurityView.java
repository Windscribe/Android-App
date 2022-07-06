/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.networksecurity;


import com.windscribe.vpn.localdatabase.tables.NetworkInfo;

import java.util.List;

public interface NetworkSecurityView {

    void hideProgress();

    void onAdapterLoadFailed(String showUpdate);

    void openNetworkSecurityDetails(String networkName);

    void setAdapter(List<NetworkInfo> mNetworkList);

    void showProgress(String progressTitle);


}
