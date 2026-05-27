package com.windscribe.vpn.billing

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests AmazonBillingManager's LiveData->SharedFlow migration: each PurchasingListener
 * callback must emit on the matching SharedFlow (success vs failure branches), and the
 * paging/empty logic in onPurchaseUpdatesResponse must route to history-success vs error.
 *
 * The only Android static is PurchasingService (mockked). The response flows have replay=0, so
 * those tests start an eager collector (UnconfinedTestDispatcher) BEFORE invoking the callback.
 * Billing setup is a StateFlow (it must survive emission before any collector attaches), so it's
 * asserted directly via .value.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AmazonBillingManagerTest {

    private lateinit var app: Application
    private lateinit var manager: AmazonBillingManager

    @Before
    fun setUp() {
        mockkStatic(PurchasingService::class)
        every { PurchasingService.registerListener(any(), any()) } returns Unit
        every { PurchasingService.getPurchaseUpdates(any()) } returns mockk()
        app = mockk(relaxed = true)
        manager = AmazonBillingManager(app)
    }

    @After
    fun tearDown() {
        unmockkStatic(PurchasingService::class)
    }

    /** Collect the first emission of [flow] into a list, started eagerly before the trigger. */
    private fun <T> TestScope.firstEmission(flow: SharedFlow<T>): MutableList<T> {
        val collected = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.takeWhile { collected.isEmpty() }.collect { collected.add(it) }
        }
        return collected
    }

    @Test
    fun `billing setup state defaults to false`() {
        assertEquals(false, manager.onBillingSetUpSuccess.value)
    }

    @Test
    fun `onCreate sets billing setup state to true and a late subscriber still sees it`() {
        // No collector is attached when onCreate fires (mirrors the real timing: the lifecycle
        // observer emits synchronously during addObserver, before repeatOnLifecycle(STARTED)
        // subscribes). As a StateFlow this value must be retained for the late subscriber.
        manager.onCreate(mockk<LifecycleOwner>())
        assertEquals(true, manager.onBillingSetUpSuccess.value)
    }

    @Test
    fun `onProductDataResponse SUCCESSFUL emits products on success flow`() = runTest {
        val products = mapOf("sku" to mockk<Product>())
        val collected = firstEmission(manager.onProductsResponseSuccess)
        val response = mockk<ProductDataResponse> {
            every { requestStatus } returns ProductDataResponse.RequestStatus.SUCCESSFUL
            every { productData } returns products
        }

        manager.onProductDataResponse(response)

        assertEquals(1, collected.size)
        assertEquals(products, collected[0])
    }

    @Test
    fun `onProductDataResponse FAILED emits status on failure flow`() = runTest {
        val collected = firstEmission(manager.onProductsResponseFailure)
        val response = mockk<ProductDataResponse> {
            every { requestStatus } returns ProductDataResponse.RequestStatus.FAILED
        }

        manager.onProductDataResponse(response)

        assertEquals(1, collected.size)
        assertEquals(ProductDataResponse.RequestStatus.FAILED, collected[0])
    }

    @Test
    fun `onPurchaseResponse SUCCESSFUL emits response on success flow`() = runTest {
        val collected = firstEmission(manager.onPurchaseResponseSuccess)
        val response = mockk<PurchaseResponse> {
            every { requestStatus } returns PurchaseResponse.RequestStatus.SUCCESSFUL
        }

        manager.onPurchaseResponse(response)

        assertEquals(1, collected.size)
        assertEquals(response, collected[0])
    }

    @Test
    fun `onPurchaseResponse FAILED emits status on failure flow`() = runTest {
        val collected = firstEmission(manager.onPurchaseResponseFailure)
        val response = mockk<PurchaseResponse> {
            every { requestStatus } returns PurchaseResponse.RequestStatus.FAILED
        }

        manager.onPurchaseResponse(response)

        assertEquals(1, collected.size)
        assertEquals(PurchaseResponse.RequestStatus.FAILED, collected[0])
    }

    @Test
    fun `onPurchaseUpdatesResponse with active receipt and no more emits history success`() = runTest {
        val collected = firstEmission(manager.onAmazonPurchaseHistorySuccess)
        val response = updatesResponse(
            status = PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL,
            hasMore = false,
            receipts = listOf(receipt(canceled = false, id = "r1")),
            userId = "u1"
        )

        manager.onPurchaseUpdatesResponse(response)

        assertEquals(1, collected.size)
        assertEquals(1, collected[0].size)
        assertEquals("r1", collected[0][0].receiptId)
        assertEquals("u1", collected[0][0].userId)
    }

    @Test
    fun `onPurchaseUpdatesResponse cancelled-only receipts emits history error`() = runTest {
        val collected = firstEmission(manager.onAmazonPurchaseHistoryError)
        val response = updatesResponse(
            status = PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL,
            hasMore = false,
            receipts = listOf(receipt(canceled = true, id = "r1")),
            userId = "u1"
        )

        manager.onPurchaseUpdatesResponse(response)

        assertEquals(1, collected.size)
        assertTrue(collected[0].contains("No existing purchase"))
    }

    @Test
    fun `onPurchaseUpdatesResponse FAILED emits history error`() = runTest {
        val collected = firstEmission(manager.onAmazonPurchaseHistoryError)
        val response = updatesResponse(
            status = PurchaseUpdatesResponse.RequestStatus.FAILED,
            hasMore = false,
            receipts = emptyList(),
            userId = "u1"
        )

        manager.onPurchaseUpdatesResponse(response)

        assertEquals(1, collected.size)
        assertTrue(collected[0].contains("No existing purchase"))
    }

    private fun receipt(canceled: Boolean, id: String): Receipt = mockk(relaxed = true) {
        every { isCanceled } returns canceled
        every { receiptId } returns id
        every { toJSON() } returns mockk(relaxed = true)
    }

    private fun updatesResponse(
        status: PurchaseUpdatesResponse.RequestStatus,
        hasMore: Boolean,
        receipts: List<Receipt>,
        userId: String
    ): PurchaseUpdatesResponse {
        val user = mockk<UserData>()
        every { user.userId } returns userId
        return mockk(relaxed = true) {
            every { requestStatus } returns status
            every { this@mockk.hasMore() } returns hasMore
            every { this@mockk.receipts } returns receipts
            every { userData } returns user
        }
    }
}
