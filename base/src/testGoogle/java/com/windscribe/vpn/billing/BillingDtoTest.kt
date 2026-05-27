package com.windscribe.vpn.billing

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the billing value classes converted from Java to Kotlin.
 * These guard the getter->property rename (responseCode/purchase/billingResult/productDetails)
 * that billing consumers depend on.
 */
class BillingDtoTest {

    @Test
    fun `CustomPurchase exposes responseCode and purchase`() {
        val purchase = mockk<Purchase>()
        val custom = CustomPurchase(7, purchase)
        assertEquals(7, custom.responseCode)
        assertSame(purchase, custom.purchase)
    }

    @Test
    fun `CustomPurchases exposes responseCode and purchase list`() {
        val purchases = listOf(mockk<Purchase>(), mockk<Purchase>())
        val custom = CustomPurchases(0, purchases)
        assertEquals(0, custom.responseCode)
        assertEquals(2, custom.purchase.size)
        assertSame(purchases, custom.purchase)
    }

    @Test
    fun `CustomProductDetails exposes billingResult and productDetails`() {
        val result = mockk<BillingResult>()
        val details = listOf(mockk<ProductDetails>())
        val custom = CustomProductDetails(result, details)
        assertSame(result, custom.billingResult)
        assertSame(details, custom.productDetails)
    }

    @Test
    fun `AmazonPurchase exposes receiptId and userId`() {
        val purchase = AmazonPurchase("receipt-1", "user-1")
        assertEquals("receipt-1", purchase.receiptId)
        assertEquals("user-1", purchase.userId)
    }

    @Test
    fun `AmazonPurchase toString contains both fields`() {
        val text = AmazonPurchase("receipt-1", "user-1").toString()
        assertTrue(text.contains("receiptId='receipt-1'"))
        assertTrue(text.contains("userId='user-1'"))
    }

    @Test
    fun `PurchaseState has the two expected values`() {
        assertEquals(2, PurchaseState.entries.size)
        assertEquals(PurchaseState.IN_PROCESS, PurchaseState.valueOf("IN_PROCESS"))
        assertEquals(PurchaseState.FINISHED, PurchaseState.valueOf("FINISHED"))
    }
}
