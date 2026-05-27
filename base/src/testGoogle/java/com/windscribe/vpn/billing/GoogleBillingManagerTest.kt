package com.windscribe.vpn.billing

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import com.windscribe.vpn.constants.BillingConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests GoogleBillingManager's LiveData->SharedFlow migration. BillingClient.newBuilder is a
 * static factory, so we mockkStatic it to return a mocked BillingClient and drive the SDK
 * callbacks (BillingClientStateListener, ConsumeResponseListener, PurchasesUpdatedListener)
 * by capturing them with slots. Asserts each callback emits on the matching SharedFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoogleBillingManagerTest {
    private lateinit var app: Application
    private lateinit var billingClient: BillingClient
    private lateinit var manager: GoogleBillingManager
    private val stateListener = slot<BillingClientStateListener>()

    @Before
    fun setUp() {
        app = mockk(relaxed = true)
        billingClient = mockk(relaxed = true)

        mockkStatic(BillingClient::class)
        val builder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns builder
        every { builder.setListener(any()) } returns builder
        every { builder.enablePendingPurchases() } returns builder
        every { builder.build() } returns billingClient

        every { billingClient.isReady } returns false
        every { billingClient.startConnection(capture(stateListener)) } returns Unit
        // getRecentPurchases() fires two queryPurchasesAsync calls on setup success; make them no-ops.
        every {
            billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any<PurchasesResponseListener>())
        } returns Unit

        manager = GoogleBillingManager(app)
    }

    @After
    fun tearDown() {
        unmockkStatic(BillingClient::class)
    }

    private fun <T> TestScope.firstEmission(flow: SharedFlow<T>): MutableList<T> {
        val collected = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.takeWhile { collected.isEmpty() }.collect { collected.add(it) }
        }
        return collected
    }

    private fun billingResult(code: Int): BillingResult = mockk { every { responseCode } returns code }

    @Test
    fun `onCreate billing setup success emits response code on success flow`() =
        runTest {
            val collected = firstEmission(manager.onBillingSetUpSuccess)
            manager.onCreate(mockk<LifecycleOwner>())

            stateListener.captured.onBillingSetupFinished(billingResult(BillingResponseCode.OK))

            assertEquals(1, collected.size)
            assertEquals(BillingResponseCode.OK, collected[0])
        }

    @Test
    fun `onCreate billing setup failure emits response code on failure flow`() =
        runTest {
            val collected = firstEmission(manager.onBillingSetupFailure)
            manager.onCreate(mockk<LifecycleOwner>())

            stateListener.captured.onBillingSetupFinished(billingResult(BillingResponseCode.ERROR))

            assertEquals(1, collected.size)
            assertEquals(BillingResponseCode.ERROR, collected[0])
        }

    @Test
    fun `billing service disconnected emits PLAY_STORE_UPDATE on failure flow`() =
        runTest {
            val collected = firstEmission(manager.onBillingSetupFailure)
            manager.onCreate(mockk<LifecycleOwner>())

            stateListener.captured.onBillingServiceDisconnected()

            assertEquals(1, collected.size)
            assertEquals(BillingConstants.PLAY_STORE_UPDATE, collected[0])
        }

    @Test
    fun `onPurchasesUpdated with purchases emits them on purchaseUpdateEvent`() =
        runTest {
            val collected = firstEmission(manager.purchaseUpdateEvent)
            val purchases = listOf(mockk<Purchase>())

            manager.onPurchasesUpdated(billingResult(BillingResponseCode.OK), purchases)

            assertEquals(1, collected.size)
            assertEquals(BillingResponseCode.OK, collected[0].responseCode)
            assertEquals(purchases, collected[0].purchase)
        }

    @Test
    fun `onPurchasesUpdated with null purchases emits empty list`() =
        runTest {
            val collected = firstEmission(manager.purchaseUpdateEvent)

            manager.onPurchasesUpdated(billingResult(BillingResponseCode.USER_CANCELED), null)

            assertEquals(1, collected.size)
            assertEquals(BillingResponseCode.USER_CANCELED, collected[0].responseCode)
            assertEquals(emptyList<Purchase>(), collected[0].purchase)
        }

    @Test
    fun `InAppConsume success emits on consume-success flow`() =
        runTest {
            manager.onCreate(mockk<LifecycleOwner>())
            val consumeListener = slot<ConsumeResponseListener>()
            every { billingClient.consumeAsync(any<ConsumeParams>(), capture(consumeListener)) } returns Unit

            val purchase = mockk<Purchase> { every { purchaseToken } returns "token" }
            val collected = firstEmission(manager.onProductConsumeSuccess)

            manager.InAppConsume(purchase)
            consumeListener.captured.onConsumeResponse(billingResult(BillingResponseCode.OK), "token")

            assertEquals(1, collected.size)
            assertEquals(purchase, collected[0])
        }

    @Test
    fun `InAppConsume failure emits CustomPurchase on consume-failure flow`() =
        runTest {
            manager.onCreate(mockk<LifecycleOwner>())
            val consumeListener = slot<ConsumeResponseListener>()
            every { billingClient.consumeAsync(any<ConsumeParams>(), capture(consumeListener)) } returns Unit

            val purchase = mockk<Purchase> { every { purchaseToken } returns "token" }
            val collected = firstEmission(manager.onProductConsumeFailure)

            manager.InAppConsume(purchase)
            consumeListener.captured.onConsumeResponse(billingResult(BillingResponseCode.ERROR), "token")

            assertEquals(1, collected.size)
            assertEquals(BillingResponseCode.ERROR, collected[0].responseCode)
            assertEquals(purchase, collected[0].purchase)
        }

    @Test
    fun `subscriptionConsume on non-purchased state emits success directly`() =
        runTest {
            val collected = firstEmission(manager.onProductConsumeSuccess)
            val purchase =
                mockk<Purchase> {
                    every { purchaseState } returns Purchase.PurchaseState.PENDING
                }

            manager.subscriptionConsume(purchase)

            assertEquals(1, collected.size)
            assertEquals(purchase, collected[0])
        }
}
