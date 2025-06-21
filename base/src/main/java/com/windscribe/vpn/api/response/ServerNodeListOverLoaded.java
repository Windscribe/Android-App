/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import androidx.annotation.Keep;

import java.util.List;

/**
 * Created by Mustafizur on 2017-11-28.
 */
@Keep
public class ServerNodeListOverLoaded implements Parcelable {

    public static final Creator<ServerNodeListOverLoaded> CREATOR = new Creator<ServerNodeListOverLoaded>() {
        @Override
        public ServerNodeListOverLoaded createFromParcel(Parcel in) {
            return new ServerNodeListOverLoaded(in);
        }

        @Override
        public ServerNodeListOverLoaded[] newArray(int size) {
            return new ServerNodeListOverLoaded[size];
        }
    };

    private final String countryCode;

    private final String countryName;

    private final String group;

    private final List<String> hostname;

    private final List<String> ip;

    private final List<String> ip2;

    private final List<String> ip3;

    private final Integer premiumOnly;

    private final Integer proNodeLocation;

    private final Integer randomNodeStrength;

    protected ServerNodeListOverLoaded(Parcel in) {
        ip = in.createStringArrayList();
        ip2 = in.createStringArrayList();
        ip3 = in.createStringArrayList();
        hostname = in.createStringArrayList();
        group = in.readString();
        countryName = in.readString();
        countryCode = in.readString();
        premiumOnly = in.readInt();
        proNodeLocation = in.readInt();
        randomNodeStrength = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(@androidx.annotation.Nullable Object obj) {
        if (!(obj instanceof ServerNodeListOverLoaded)) {
            return false;
        }
        ServerNodeListOverLoaded node = (ServerNodeListOverLoaded) obj;
        return getId() == node.getId();
    }

    public int getId() {
        return (getNodeNames().get(0) + getNodeNames().get(1)).hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(ip);
        dest.writeStringList(ip2);
        dest.writeStringList(ip3);
        dest.writeStringList(hostname);
        dest.writeString(group);
        dest.writeString(countryName);
        dest.writeString(countryCode);
        dest.writeInt(premiumOnly);
        dest.writeInt(proNodeLocation);
        dest.writeInt(randomNodeStrength);
    }

    private SparseArray<String> getNodeNames() {
        String[] locationArray = group.split("-", 2);
        String nodeName, nodeNickName;
        if (locationArray.length > 1) {
            nodeName = locationArray[0].trim();
            nodeNickName = locationArray[1].trim();
        } else if (locationArray.length > 0) {
            nodeName = locationArray[0].trim();
            nodeNickName = "";
        } else {
            nodeName = "Unknown";
            nodeNickName = "";
        }

        SparseArray<String> map = new SparseArray<>();
        map.put(0, nodeName);
        map.put(1, nodeNickName);
        return map;
    }
}
