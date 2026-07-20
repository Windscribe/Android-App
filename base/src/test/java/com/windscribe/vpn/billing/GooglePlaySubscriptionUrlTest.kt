package com.windscribe.vpn.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GooglePlaySubscriptionUrlTest {
    @Test
    fun `builds Google Play subscription management URL`() {
        assertEquals(
            "https://play.google.com/store/account/subscriptions?sku=pro_yearly&package=com.windscribe.vpn",
            GooglePlaySubscriptionUrl.build("com.windscribe.vpn", "pro_yearly"),
        )
    }

    @Test
    fun `encodes query parameter values`() {
        assertEquals(
            "https://play.google.com/store/account/subscriptions?sku=pro+yearly%26promo&package=com.windscribe.vpn",
            GooglePlaySubscriptionUrl.build("com.windscribe.vpn", "pro yearly&promo"),
        )
    }

    @Test
    fun `rejects a blank product ID`() {
        assertNull(GooglePlaySubscriptionUrl.build("com.windscribe.vpn", " "))
    }

    @Test
    fun `extracts product ID from grace notification payload`() {
        val productId =
            GooglePlaySubscriptionUrl.productIdFromPayload(
                "com.windscribe.vpn",
                mapOf(
                    "type" to GooglePlaySubscriptionUrl.NOTIFICATION_TYPE,
                    GooglePlaySubscriptionUrl.PRODUCT_ID_EXTRA to "pro_yearly",
                ),
            )

        assertEquals("pro_yearly", productId)
    }

    @Test
    fun `rejects a grace payload with blank product ID`() {
        assertNull(
            GooglePlaySubscriptionUrl.productIdFromPayload(
                "com.windscribe.vpn",
                mapOf(
                    "type" to GooglePlaySubscriptionUrl.NOTIFICATION_TYPE,
                    GooglePlaySubscriptionUrl.PRODUCT_ID_EXTRA to " ",
                ),
            ),
        )
    }

    @Test
    fun `rejects an unrelated notification payload`() {
        assertNull(
            GooglePlaySubscriptionUrl.productIdFromPayload(
                "com.windscribe.vpn",
                mapOf(
                    "type" to "promo",
                    GooglePlaySubscriptionUrl.PRODUCT_ID_EXTRA to "pro_yearly",
                ),
            ),
        )
    }
}
