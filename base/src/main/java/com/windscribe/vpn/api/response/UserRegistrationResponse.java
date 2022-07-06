/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Mustafizur on 2017-09-08.
 */

@SuppressWarnings("unused")
public class UserRegistrationResponse {

    @SerializedName("billing_plan_id")
    @Expose
    private Integer billingPlanId;

    @SerializedName("is_premium")
    @Expose
    private Integer isPremium;

    @SerializedName("loc_rev")
    @Expose
    private String locRev;

    @SerializedName("loc_hash")
    @Expose
    private String locationHash;

    @SerializedName("reg_date")
    @Expose
    private String regDate;

    @SerializedName("secure_links_secret")
    @Expose
    private String secureLinksSecret;

    @SerializedName("session_auth_hash")
    @Expose
    private String sessionAuthHash;

    @SerializedName("traffic_max")
    @Expose
    private String trafficMax;

    @SerializedName("traffic_used")
    @Expose
    private String trafficUsed;

    @SerializedName("status")
    @Expose
    private Integer userAccountStatus;

    @SerializedName("user_id")
    @Expose
    private String userId;

    @SerializedName("username")
    @Expose
    private String username;

    public Integer getBillingPlanId() {
        return billingPlanId;
    }

    public Integer getIsPremium() {
        return isPremium;
    }

    public String getLocRev() {
        return locRev;
    }

    public String getLocationHash() {
        return locationHash;
    }

    public String getRegDate() {
        return regDate;
    }

    public String getSecureLinksSecret() {
        return secureLinksSecret;
    }

    public String getSessionAuthHash() {
        return sessionAuthHash;
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

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username != null ? username : "na";
    }

    public boolean isInGhostMode() {
        return getUsername().equals("na");
    }
}
