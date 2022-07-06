/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.alert;

public interface DisclaimerAlertListener {

    void onRequestCancel();

    void onRequestPermission(int requestCode);
}
