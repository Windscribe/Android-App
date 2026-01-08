/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import android.graphics.drawable.Drawable;

import androidx.annotation.Keep;

@Keep
public class InstalledAppsData implements Comparable<InstalledAppsData> {

    private Drawable appIconDrawable;

    private String appName;

    private boolean checked = false;

    private String packageName;

    private boolean systemApp = false;

    public InstalledAppsData(String appName, String packageName, Drawable appIconDrawable) {
        this.appName = appName;
        this.packageName = packageName;
        this.appIconDrawable = appIconDrawable;
    }

    @Override
    public int compareTo(InstalledAppsData o) {
        return o.appName.compareTo(appName);
    }

    public Drawable getAppIconDrawable() {
        return appIconDrawable;
    }

    public void setAppIconDrawable(Drawable appIconDrawable) {
        this.appIconDrawable = appIconDrawable;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isSystemApp() {
        return systemApp;
    }

    public void setSystemApp(boolean systemApp) {
        this.systemApp = systemApp;
    }
}

