/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by Mustafizur on 2018-01-16.
 */
@Keep
class BillingPlanResponse {
    @SerializedName("plans")
    @Expose
    val plansList: List<BillingPlans>? = null

    @SerializedName("plans_override")
    @Expose
    val overriddenPlans: OverriddenPlans? = null

    @SerializedName("payment_token")
    @Expose
    val paymentToken: String? = null

    @SerializedName("version")
    @Expose
    val version: Int = 1

    @Keep
    class OverriddenPlans {
        @SerializedName("ru")
        @Expose
        var russianPlan: SpecialPlan? = null
    }

    @Keep
    class SpecialPlan {
        @SerializedName("pro_monthly")
        @Expose
        var proMonthly: String? = null

        @SerializedName("pro_yearly")
        @Expose
        var proYearly: String? = null
    }

    @Keep
    class BillingPlans {
        @SerializedName("discount")
        @Expose
        var discount = 0

        @SerializedName("duration")
        @Expose
        var duration = 1

        @SerializedName("ext_id")
        @Expose
        val extId: String? = null

        @SerializedName("name")
        @Expose
        val planName: String? = null

        @SerializedName("price")
        @Expose
        val planPrice: String? = null

        @SerializedName("active")
        @Expose
        val planStatus: Int? = null

        @SerializedName("ws_plan_id")
        @Expose
        val wsPlanId: Int? = null

        @SerializedName("rebill")
        @Expose
        private val reBill: Int? = null

        @SerializedName("price_original")
        @Expose
        val originalPrice: String? = null

        val isReBill: Boolean
            get() = reBill != null && reBill == 1

        override fun toString(): String =
            "BillingPlan{" +
                "name=" + planName +
                ", ws_plan_id='" + wsPlanId + '\'' +
                ", ext_id='" + extId + '\'' +
                ", price='" + planPrice + '\'' +
                ", active='" + planStatus + '\'' +
                ", rebill='" + isReBill + '\'' +
                '}'
    }
}
