/*
 * Copyright (c) 2024 Windscribe Limited.
 */
package com.windscribe.vpn.billing

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserDataState
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import javax.inject.Singleton

/**
 * Receipt parameters for [PurchaseManager.completePurchase]. Mirrors the fields of
 * [IApiCallManager.verifyPurchaseReceipt]; Google and Amazon fill different subsets.
 */
data class ReceiptParams(
    val purchaseToken: String,
    val gpPackageName: String = "",
    val gpProductId: String = "",
    val type: String = "",
    val amazonUserId: String = "",
)

fun String.truncatedBillingToken(): String =
    when {
        length <= 8 -> "<redacted>"
        else -> "${take(4)}...${takeLast(4)}"
    }

const val PURCHASE_VERIFICATION_IN_PROGRESS = "Verification already in progress"

/**
 * Owns the durable post-purchase pipeline: verify receipt -> (optional) promo confirmation ->
 * account refresh. The work runs on the injected application scope, so it completes even if the
 * upgrade screen that started it is destroyed mid-flow. Callers collect the returned [SharedFlow]
 * to drive UI; cancelling the collection only stops observing, it does not cancel the work.
 *
 * Store-specific fulfillment (Amazon notifyFulfillment, clearing the cached purchase) stays in the
 * caller and is done when it observes [UserDataState.Success].
 *
 * Thread-safe and idempotent - safe to call from UI or background workers.
 */
@Singleton
class PurchaseManager(
    private val scope: CoroutineScope,
    private val apiCallManager: IApiCallManager,
    private val userRepository: UserRepository,
    private val preferencesHelper: PreferencesHelper,
) {
    private val logger = LoggerFactory.getLogger("billing")
    private val mutex = Mutex()
    private val inFlightTokens = mutableSetOf<String>()

    /**
     * Complete purchase verification pipeline. Idempotent - safe to call multiple times with same token.
     *
     * @param receipt Purchase receipt details
     * @param promoPcpId Optional promo ID for confirmation
     * @param firebaseToken Optional Firebase token for session refresh
     * @return SharedFlow of verification states (Loading, Success, Error)
     */
    fun completePurchase(
        receipt: ReceiptParams,
        promoPcpId: String? = null,
        firebaseToken: String? = null,
    ): SharedFlow<UserDataState> {
        val states = MutableSharedFlow<UserDataState>(replay = 1, extraBufferCapacity = 8)
        val token = receipt.purchaseToken

        scope.launch {
            try {
                // Check if already verified (deduplication)
                if (preferencesHelper.verifiedPurchaseTokens.contains(token)) {
                    logger.debug("Receipt ${token.truncatedBillingToken()} already verified, skipping")
                    states.emit(UserDataState.Success)
                    return@launch
                }

                // Check if verification in progress (concurrency control)
                mutex.withLock {
                    if (inFlightTokens.contains(token)) {
                        logger.debug("Receipt ${token.truncatedBillingToken()} verification already in progress")
                        states.emit(UserDataState.Error(error = PURCHASE_VERIFICATION_IN_PROGRESS))
                        return@launch
                    }
                    inFlightTokens.add(token)
                }

                try {
                    states.emit(UserDataState.Loading("Verifying purchase"))
                    // Step 1: Verify receipt with backend
                    logger.info("Verifying receipt: ${token.truncatedBillingToken()}")
                    val verify =
                        result<GenericSuccess> {
                            apiCallManager.verifyPurchaseReceipt(
                                receipt.purchaseToken,
                                receipt.gpPackageName,
                                receipt.gpProductId,
                                receipt.type,
                                receipt.amazonUserId,
                            )
                        }
                    if (verify is CallResult.Error) {
                        logger.debug("Payment verification failed: ${verify.errorMessage}")
                        states.emit(UserDataState.Error(error = verify.errorMessage))
                        return@launch
                    }
                    logger.info("Payment verification successful.")

                    // Step 2: Promo confirmation (best-effort, must not fail the upgrade)
                    if (!promoPcpId.isNullOrEmpty()) {
                        val promo =
                            result<GenericSuccess> {
                                apiCallManager.postPromoPaymentConfirmation(promoPcpId)
                            }
                        if (promo is CallResult.Error) {
                            logger.debug("Promo payment confirmation failed: ${promo.errorMessage}")
                        }
                    }

                    // Step 3: Refresh account session
                    userRepository.refreshAccount(firebaseToken) { states.emit(it) }

                    // Mark as verified (persistent deduplication)
                    mutex.withLock {
                        preferencesHelper.verifiedPurchaseTokens += token
                    }

                    // Step 4: Clear pending purchase data (single source of truth)
                    mutex.withLock {
                        when (receipt.type) {
                            BillingConstants.AMAZON_PURCHASE_TYPE -> {
                                logger.debug("Clearing pending Amazon purchase")
                                preferencesHelper.amazonPurchasedItem = null
                            }
                            else -> {
                                logger.debug("Clearing pending Google purchase")
                                preferencesHelper.purchasedItem = null
                            }
                        }
                    }

                    states.emit(UserDataState.Success)
                } finally {
                    // Always remove from in-flight set
                    mutex.withLock {
                        inFlightTokens.remove(token)
                    }
                }
            } catch (e: Exception) {
                logger.debug("Purchase completion failed: ${e.localizedMessage}")
                states.emit(UserDataState.Error(error = e.localizedMessage ?: "Unknown error"))
            }
        }
        return states.asSharedFlow()
    }
}
