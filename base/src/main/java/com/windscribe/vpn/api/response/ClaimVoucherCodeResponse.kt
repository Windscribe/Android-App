package com.windscribe.vpn.api.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ClaimVoucherCodeResponse(
    @SerializedName("voucher_claimed") @Expose
    var isClaimed: Boolean,
    @SerializedName("voucher_used") @Expose
    var isUsed: Boolean,
    @SerializedName("voucher_taken") @Expose
    var isTaken: Boolean,
    @SerializedName("email_required") @Expose
    var emailRequired: Boolean?,
    @SerializedName("billing_plan_id") @Expose
    var planId: Long?,
    @SerializedName("is_premium") @Expose
    var isPremium: Int?,
    @SerializedName("new_plan_id") @Expose
    var newPlanId: String?,
    @SerializedName("new_plan_name") @Expose
    var newPlanName: String?
)