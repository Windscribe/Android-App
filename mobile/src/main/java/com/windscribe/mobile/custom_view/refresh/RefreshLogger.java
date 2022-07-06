/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.custom_view.refresh;

import android.util.Log;

public final class RefreshLogger {

    private static final String TAG = "RefreshLayout";

    private static final boolean mEnableDebug = false;

    public static void e(String msg) {
        if (mEnableDebug) {
            Log.e(TAG, msg);
        }
    }

    public static void i(String msg) {
        if (mEnableDebug) {
            Log.i(TAG, msg);
        }
    }
}
