/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing

import com.amazon.device.iap.model.Product
import com.android.billingclient.api.ProductDetails
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

    open fun getOriginalPrice(sku: String): String? {
        return billingPlans.first {
            it.extId == sku
        }.originalPrice
    }


    fun getMonthlyPlan(): String? {
        return billingPlans.firstOrNull {
            it.duration == 1
        }?.extId
    }

    fun getYearlyPlan(): String? {
        return billingPlans.firstOrNull {
            it.duration == 12
        }?.extId
    }

    fun getPromoPlan(): String? {
        return billingPlans.firstOrNull {
            it.discount > 0
        }?.extId
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
        return billingPlans.firstOrNull { it.discount > 0 } != null
    }

    fun getDiscountLabel(sku: String): String {
        val discount = billingPlans.first {
            it.extId == sku
        }.discount
        return "Save $discount%"
    }

    fun getUsdPrice(sku: String): String {
        return billingPlans.first {
            it.extId == sku
        }.planPrice
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
    val productDetailsList: List<ProductDetails>,
    val billingPlans: List<BillingPlans>,
    var pushNotificationAction: PushNotificationAction?
) : WindscribeInAppProduct(billingPlans, pushNotificationAction) {

    override fun getPrice(sku: String): String? {
        val product = productDetailsList.firstOrNull { it.productId == sku }
        val subDetail = product?.subscriptionOfferDetails
        if (subDetail != null) {
            return subDetail[0].pricingPhases.pricingPhaseList[0].formattedPrice
        } else {
            val oneTimeDetail = product?.oneTimePurchaseOfferDetails
            if (oneTimeDetail != null) {
                return oneTimeDetail.formattedPrice
            }
        }
        return null
    }

    fun getSkuDetails(sku: String): ProductDetails {
        return productDetailsList.first {
            it.productId == sku
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
