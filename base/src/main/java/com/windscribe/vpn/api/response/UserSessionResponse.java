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
 * Created by Mustafizur on 2017-11-24.
 */

@Keep
public class UserSessionResponse {

    @SerializedName("alc")
    @Expose
    private List<String> alcList;

    @SerializedName("billing_plan_id")
    @Expose
    private Integer billingPlanID;

    @SerializedName("email_status")
    @Expose
    private Integer emailStatus;

    @SerializedName("ignore_udp_tests")
    @Expose
    private Integer ignoreUdpTest;

    @SerializedName("is_premium")
    @Expose
    private Integer isPremium;

    @SerializedName("last_reset")
    @Expose
    private String lastResetDate;

    @SerializedName("loc_hash")
    @Expose
    private String locationHash;

    @SerializedName("loc_rev")
    @Expose
    private String locationRevision;

    @SerializedName("our_addr")
    @Expose
    private String ourAddress;

    @SerializedName("our_dc")
    @Expose
    private Integer ourDc;

    @SerializedName("our_ip")
    @Expose
    private Integer ourIp;

    @SerializedName("our_location")
    @Expose
    private String ourLocation;

    @SerializedName("premium_expiry_date")
    @Expose
    private String premiumExpiryDate;

    @SerializedName("rebill")
    @Expose
    private Integer reBill;

    @SerializedName("reg_date")
    @Expose
    private String registrationDate;

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

    public void setAlcList(List<String> alcList) {
        this.alcList = alcList;
    }

    public Integer getBillingPlanID() {
        return billingPlanID;
    }

    public void setBillingPlanID(Integer billingPlanID) {
        this.billingPlanID = billingPlanID;
    }

    public Integer getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(Integer emailStatus) {
        this.emailStatus = emailStatus;
    }

    public Integer getIgnoreUdpTest() {
        return ignoreUdpTest;
    }

    public void setIgnoreUdpTest(Integer ignoreUdpTest) {
        this.ignoreUdpTest = ignoreUdpTest;
    }

    public Integer getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(Integer isPremium) {
        this.isPremium = isPremium;
    }

    public String getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(String lastResetDate) {
        this.lastResetDate = lastResetDate;
    }

    public String getLocationHash() {
        return locationHash;
    }

    public void setLocationHash(String locationHash) {
        this.locationHash = locationHash;
    }

    public String getLocationRevision() {
        return locationRevision;
    }

    public void setLocationRevision(String locationRevision) {
        this.locationRevision = locationRevision;
    }

    public String getOurAddress() {
        return ourAddress;
    }

    public void setOurAddress(String ourAddress) {
        this.ourAddress = ourAddress;
    }

    public Integer getOurDc() {
        return ourDc;
    }

    public void setOurDc(Integer ourDc) {
        this.ourDc = ourDc;
    }

    public Integer getOurIp() {
        return ourIp;
    }

    public void setOurIp(Integer ourIp) {
        this.ourIp = ourIp;
    }

    public String getOurLocation() {
        return ourLocation;
    }

    public void setOurLocation(String ourLocation) {
        this.ourLocation = ourLocation;
    }

    public String getPremiumExpiryDate() {
        return premiumExpiryDate;
    }

    public void setPremiumExpiryDate(String premiumExpiryDate) {
        this.premiumExpiryDate = premiumExpiryDate;
    }

    public Integer getReBill() {
        return reBill;
    }

    public void setReBill(Integer reBill) {
        this.reBill = reBill;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Sip getSip() {
        return sip;
    }

    public void setSip(Sip sip) {
        this.sip = sip;
    }

    public String getTrafficMax() {
        return trafficMax;
    }

    public void setTrafficMax(String trafficMax) {
        this.trafficMax = trafficMax;
    }

    public String getTrafficUsed() {
        return trafficUsed;
    }

    public void setTrafficUsed(String trafficUsed) {
        this.trafficUsed = trafficUsed;
    }

    public Integer getUserAccountStatus() {
        return userAccountStatus;
    }

    public void setUserAccountStatus(Integer userAccountStatus) {
        this.userAccountStatus = userAccountStatus;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserName() {
        return userName != null ? userName : "na";
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int sipCount() {
        return sip != null ? sip.getCount() : 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "UserSessionResponse{" +
                "ourIp=" + ourIp +
                ", ourLocation='" + ourLocation + '\'' +
                ", ourDc=" + ourDc +
                ", ourAddress='" + ourAddress + '\'' +
                ", reBill=" + reBill +
                ", ignoreUdpTest=" + ignoreUdpTest +
                ", trafficUsed='" + trafficUsed + '\'' +
                ", trafficMax='" + trafficMax + '\'' +
                ", userAccountStatus=" + userAccountStatus +
                ", emailStatus=" + emailStatus +
                ", billingPlanID=" + billingPlanID +
                ", isPremium=" + isPremium +
                ", registrationDate='" + registrationDate + '\'' +
                ", lastResetDate='" + lastResetDate + '\'' +
                ", locationRevision='" + locationRevision + '\'' +
                ", locationHash='" + locationHash + '\'' +
                ", premiumExpiryDate='" + premiumExpiryDate + '\'' +
                '}';
    }
}
