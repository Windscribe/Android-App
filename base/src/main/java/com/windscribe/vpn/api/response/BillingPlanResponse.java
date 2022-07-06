/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Mustafizur on 2018-01-16.
 */

@SuppressWarnings("unused")
public class BillingPlanResponse {

    @SerializedName("plans")
    @Expose
    private List<BillingPlans> plansList;

    @SerializedName("plans_override")
    @Expose
    private OverriddenPlans overriddenPlans;

    @SuppressWarnings("FieldMayBeFinal")
    @SerializedName("version")
    @Expose
    private int version = 1;

    public List<BillingPlans> getPlansList() {
        return plansList;
    }

    public OverriddenPlans getOverriddenPlans() {
        return overriddenPlans;
    }

    public int getVersion() {
        return version;
    }

    public static class OverriddenPlans {
        @SerializedName("ru")
        @Expose
        public SpecialPlan russianPlan;
    }

    public static class SpecialPlan {
        @SerializedName("pro_monthly")
        @Expose
        public String proMonthly;

        @SerializedName("pro_yearly")
        @Expose
        public String proYearly;
    }

    public static class BillingPlans {

        @SerializedName("discount")
        @Expose
        public int discount = 0;

        @SerializedName("duration")
        @Expose
        public int duration = 1;

        @SerializedName("ext_id")
        @Expose
        private String extId;

        @SerializedName("name")
        @Expose
        private String planName;

        @SerializedName("price")
        @Expose
        private String planPrice;

        @SerializedName("active")
        @Expose
        private Integer planStatus;

        @SerializedName("ws_plan_id")
        @Expose
        private Integer wsPlanId;

        @SerializedName("rebill")
        @Expose
        private Integer reBill;

        public String getExtId() {
            return extId;
        }

        public String getPlanName() {
            return planName;
        }

        public String getPlanPrice() {
            return planPrice;
        }

        public Integer getPlanStatus() {
            return planStatus;
        }

        public Integer getWsPlanId() {
            return wsPlanId;
        }

        public boolean isReBill() {
            return reBill != null && reBill == 1;
        }

        @NonNull
        @Override
        public String toString() {
            return "BillingPlan{" +
                    "name=" + planName +
                    ", ws_plan_id='" + wsPlanId + '\'' +
                    ", ext_id='" + extId + '\'' +
                    ", price='" + planPrice + '\'' +
                    ", active='" + planStatus + '\'' +
                    '}';
        }
    }

}
