/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.networksecurity.viewholder;

import com.windscribe.vpn.localdatabase.tables.NetworkInfo;

public interface NetworkAdapterActionListener {

    void onItemSelected(NetworkInfo networkInfo);
}
