/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing

import com.amazon.device.iap.model.Product
import com.android.billingclient.api.SkuDetails
import com.windscribe.vpn.api.response.BillingPlanResponse.BillingPlans
import com.windscribe.vpn.api.response.PushNotificationAction

open class WindscribeInAppProduct(
        private val billingPlans: List<BillingPlans>,
        private val pushNotificationAction: PushNotificationAction?
) {

    fun getSkus(): List<String> {
        return billingPlans.map {
            it.extId
        }
    }

    open fun getPrice(sku: String): String? {
        return billingPlans.first {
            it.extId == sku
        }.planPrice
    }

    fun getPlanDuration(sku: String): String {
        val numberOfMonths = billingPlans.first {
            it.extId == sku
        }.duration
        return when (numberOfMonths) {
            1 -> "month"
            12 -> "year"
            else -> {
                "$numberOfMonths months"
            }
        }
    }

    fun isPromo(): Boolean {
        return pushNotificationAction != null && pushNotificationAction.promoCode.isNotEmpty()
    }

    fun getDiscountLabel(sku: String): String {
        val discount = billingPlans.first {
            it.extId == sku
        }.discount
        return "Save $discount%"
    }

    fun getPromoStickerLabel(sku: String): String {
        val plan = billingPlans.first {
            it.extId == sku
        }
        return plan.planName
    }

    fun getPlanName(sku: String): CharSequence? {
        return billingPlans.first {
            it.extId == sku
        }.planName
    }
}

data class GoogleProducts(
        val skuDetails: List<SkuDetails>,
        val billingPlans: List<BillingPlans>,
        var pushNotificationAction: PushNotificationAction?
) : WindscribeInAppProduct(billingPlans, pushNotificationAction) {

    override fun getPrice(sku: String): String? {
        return skuDetails.filter {
            it.sku == sku
        }.getOrNull(0)?.price
    }

    fun getSkuDetails(sku: String): SkuDetails {
        return skuDetails.first {
            it.sku == sku
        }
    }
}

data class AmazonProducts(
        val products: Map<String, Product>,
        val billingPlans: List<BillingPlans>,
        var pushNotificationAction: PushNotificationAction?
) : WindscribeInAppProduct(billingPlans, pushNotificationAction) {

    override fun getPrice(sku: String): String? {
        return products.filter {
            it.value.sku == sku
        }.values.first().price
    }

    fun getProduct(sku: String): Product {
        return products.filter {
            it.value.sku == sku
        }.values.first()
    }
}
