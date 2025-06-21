/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Mustafizur on 2017-09-08.
 */

@Keep
public class UserLoginResponse {

    @SerializedName("alc")
    @Expose
    private List<String> alcList;

    @SerializedName("billing_plan_id")
    @Expose
    private Integer billingPlanID;

    @SerializedName("email_status")
    @Expose
    private String emailStatus;

    @SerializedName("is_premium")
    @Expose
    private Integer isPremium;

    @SerializedName("loc_hash")
    @Expose
    private String locationHash;

    @SerializedName("loc_rev")
    @Expose
    private String locationRevision;

    @SerializedName("reg_date")
    @Expose
    private String registrationDate;

    @SerializedName("session_auth_hash")
    @Expose
    private String sessionAuthHash;

    @SerializedName("sip")
    @Expose
    private Sip sip;

    @SerializedName("traffic_max")
    @Expose
    private String trafficMax;

    @SerializedName("traffic_used")
    @Expose
    private String trafficUsed;

    @SerializedName("status")
    @Expose
    private Integer userAccountStatus;

    @SerializedName("email")
    @Expose
    private String userEmail;

    @SerializedName("user_id")
    @Expose
    private String userID;

    @SerializedName("username")
    @Expose
    private String userName;

    public List<String> getAlcList() {
        return alcList;
    }

    public Integer getBillingPlanID() {
        return billingPlanID;
    }

    public String getEmailStatus() {
        return emailStatus;
    }

    public Integer getIsPremium() {
        return isPremium;
    }

    public String getLocationHash() {
        return locationHash;
    }

    public String getLocationRevision() {
        return locationRevision;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public String getSessionAuthHash() {
        return sessionAuthHash;
    }

    public Sip getSip() {
        return sip;
    }

    public String getTrafficMax() {
        return trafficMax;
    }

    public String getTrafficUsed() {
        return trafficUsed;
    }

    public Integer getUserAccountStatus() {
        return userAccountStatus;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserID() {
        return userID;
    }

    public String getUserName() {
        return userName != null ? userName : "na";
    }

    public boolean isInGhostMode() {
        return getUserName().equals("na");
    }

    @NonNull
    @Override
    public String toString() {
        return "UserLoginResponse{" +
                "session_auth_hash=" + sessionAuthHash +
                ", username='" + userName + '\'' +
                ", user_id='" + userID + '\'' +
                ", traffic_used='" + trafficUsed + '\'' +
                ", traffic_max='" + trafficMax + '\'' +
                ", status='" + userAccountStatus + '\'' +
                ", email='" + userEmail + '\'' +
                ", email_status='" + emailStatus + '\'' +
                ", billing_plan_id='" + billingPlanID + '\'' +
                ", is_premium='" + isPremium + '\'' +
                ", reg_date='" + registrationDate + '\'' +
                ", loc_rev='" + locationRevision + '\'' +
                ", loc_hash='" + locationHash + '\'' +
                '}';
    }
}
