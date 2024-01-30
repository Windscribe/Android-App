/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.commonutils;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.windscribe.vpn.R;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.backend.Util;
import com.windscribe.vpn.backend.utils.SelectedLocationType;
import com.windscribe.vpn.exceptions.BackgroundLocationPermissionNotAvailable;
import com.windscribe.vpn.exceptions.NoLocationPermissionException;
import com.windscribe.vpn.exceptions.NoNetworkException;
import com.windscribe.vpn.exceptions.WindScribeException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;

public class WindUtilities {

    public enum ConfigType {
        OpenVPN, WIRE_GUARD
    }

    public static Single<Boolean> deleteProfile(Context context) {
        return Single.fromCallable(() -> {
            File file = new File(context.getFilesDir() + "/" + "wd.vp");
            if (file.exists()) {
                return file.delete();
            } else {
                return false;
            }
        });
    }

    public static Completable deleteProfileCompletely(Context context) {
        return Completable.fromAction(() -> {
            File file = new File(context.getFilesDir() + "/" + Util.VPN_PROFILE_NAME);
            if (file.exists()) {
                file.delete();
            }
        }).andThen(Completable.fromAction(() -> {
            File file = new File(context.getFilesDir() + "/" + Util.LAST_SELECTED_LOCATION);
            if (file.exists()) {
                file.delete();
            }
        }));
    }

    public static ConfigType getConfigType(String content) {
        if (content.contains("[Peer]") && content.contains("[Interface]")) {
            return ConfigType.WIRE_GUARD;
        } else {
            return ConfigType.OpenVPN;
        }
    }

    public static String getNetworkName() throws WindScribeException {
        ConnectivityManager connectivityManager = (ConnectivityManager) Windscribe.getAppContext()
                .getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            if (!hasLocationPermission()) {
                throw new NoLocationPermissionException("No location permission provided");
            }
            String quoteReplacedNetworkName = "";
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wifiManager = (WifiManager) Windscribe.getAppContext().getApplicationContext()
                        .getSystemService(WIFI_SERVICE);
                quoteReplacedNetworkName = wifiManager != null
                        ? wifiManager.getConnectionInfo().getSSID().contains("\"")
                        ? wifiManager.getConnectionInfo().getSSID().replace("\"", "")
                        : wifiManager.getConnectionInfo().getSSID()
                        : "Unknown";
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                quoteReplacedNetworkName = networkInfo.getExtraInfo() != null
                        ? networkInfo.getExtraInfo().contains("\"")
                        ? networkInfo.getExtraInfo().replace("\"", "")
                        : networkInfo.getExtraInfo()
                        : "Unknown";
            }

            String networkName = networkInfo.getType() == ConnectivityManager.TYPE_WIFI ?
                    quoteReplacedNetworkName : networkInfo.getType() == ConnectivityManager.TYPE_MOBILE ?
                    (quoteReplacedNetworkName.contains(".") ? (quoteReplacedNetworkName.split("\\.", 3).length > 1 ?
                            quoteReplacedNetworkName.split("\\.", 3)[1].toUpperCase()
                            : quoteReplacedNetworkName.toUpperCase()) : quoteReplacedNetworkName.toUpperCase()) :
                    networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET ? "Ethernet"
                            : "Unknown";

            if (networkName.equals("<unknown ssid>")) {
                throw new BackgroundLocationPermissionNotAvailable("App tried to access network name in the background and Location permission is only available for while in use.");
            } else {
                return networkName;
            }
        } else {
            throw new NoNetworkException("No network is connected");
        }
    }

    public static SelectedLocationType getSourceTypeBlocking() {
        boolean isConnectingToStatic = Windscribe.getAppContext().getPreference().isConnectingToStaticIp();
        boolean isConnectingToConfigured = Windscribe.getAppContext().getPreference()
                .isConnectingToConfiguredLocation();
        if (isConnectingToConfigured) {
            return SelectedLocationType.CustomConfiguredProfile;
        } else if (isConnectingToStatic) {
            return SelectedLocationType.StaticIp;
        } else {
            return SelectedLocationType.CityLocation;
        }
    }

    @Nullable
    public static NetworkInfo getUnderLayNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) Windscribe.getAppContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null) {
            return connectivityManager.getActiveNetworkInfo();
        }
        return null;
    }

    public static String getVersionCode() {
        try {
            PackageInfo info = Windscribe.getAppContext().getPackageManager()
                    .getPackageInfo(Windscribe.getAppContext().getPackageName(), 0);
            return String.valueOf(info.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return "--";
        }
    }

    public static String getVersionName() {
        try {
            return Windscribe.getAppContext().getPackageManager()
                    .getPackageInfo(Windscribe.getAppContext().getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return "-";
        }
    }

    public static String humanReadableByteCount(long bytes, boolean speed, Resources res) {
        if (speed) {
            bytes = bytes * 8;
        }
        int unit = speed ? 1000 : 1024;

        int exp = Math.max(0, Math.min((int) (Math.log(bytes) / Math.log(unit)), 3));

        float bytesUnit = (float) (bytes / Math.pow(unit, exp));

        if (speed) {
            switch (exp) {
                case 0:
                    return res.getString(R.string.bits_per_second, bytesUnit);
                case 1:
                    return res.getString(R.string.kbits_per_second, bytesUnit);
                case 2:
                    return res.getString(R.string.mbits_per_second, bytesUnit);
                default:
                    return res.getString(R.string.gbits_per_second, bytesUnit);
            }
        } else {
            switch (exp) {
                case 0:
                    return res.getString(R.string.volume_byte, bytesUnit);
                case 1:
                    return res.getString(R.string.volume_kbyte, bytesUnit);
                case 2:
                    return res.getString(R.string.volume_mbyte, bytesUnit);
                default:
                    return res.getString(R.string.volume_gbyte, bytesUnit);

            }
        }


    }

    public static boolean isOnline() {
        NetworkInfo activeNetworkInfo = getUnderLayNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private static boolean hasLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return ContextCompat
                    .checkSelfPermission(Windscribe.getAppContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }
}

