package com.windscribe.vpn.billing

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GooglePlaySubscriptionUrl {
    const val NOTIFICATION_TYPE = "subscription_grace"
    const val PRODUCT_ID_EXTRA = "product_id"

    private const val SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"

    fun productIdFromPayload(
        packageName: String,
        payload: Map<String, String>,
    ): String? {
        if (payload["type"] != NOTIFICATION_TYPE) return null
        val productId = payload[PRODUCT_ID_EXTRA].orEmpty()
        return productId.takeIf { build(packageName, it) != null }
    }

    fun build(
        packageName: String,
        productId: String,
    ): String? {
        if (packageName.isBlank() || productId.isBlank()) return null

        return "$SUBSCRIPTIONS_URL?sku=${encode(productId)}&package=${encode(packageName)}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
