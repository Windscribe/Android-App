/*
 * Copyright (c) 2024 Windscribe Limited.
 */
package com.windscribe.vpn.billing

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserDataState
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
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

/**
 * Owns the durable post-purchase pipeline: verify receipt -> (optional) promo confirmation ->
 * account refresh. The work runs on the injected application scope, so it completes even if the
 * upgrade screen that started it is destroyed mid-flow. Callers collect the returned [SharedFlow]
 * to drive UI; cancelling the collection only stops observing, it does not cancel the work.
 *
 * Store-specific fulfillment (Amazon notifyFulfillment, clearing the cached purchase) stays in the
 * caller and is done when it observes [UserDataState.Success].
 */
@Singleton
class PurchaseManager(
    private val scope: CoroutineScope,
    private val apiCallManager: IApiCallManager,
    private val userRepository: UserRepository,
) {
    private val logger = LoggerFactory.getLogger("billing")

    fun completePurchase(
        receipt: ReceiptParams,
        promoPcpId: String? = null,
        firebaseToken: String? = null,
    ): SharedFlow<UserDataState> {
        val states = MutableSharedFlow<UserDataState>(replay = 1, extraBufferCapacity = 8)
        scope.launch {
            try {
                states.emit(UserDataState.Loading("Verifying purchase"))
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
                    states.emit(UserDataState.Error(verify.errorMessage))
                    return@launch
                }
                logger.info("Payment verification successful.")

                // Promo confirmation is best-effort and must not fail the upgrade.
                if (!promoPcpId.isNullOrEmpty()) {
                    val promo = result<GenericSuccess> { apiCallManager.postPromoPaymentConfirmation(promoPcpId) }
                    if (promo is CallResult.Error) {
                        logger.debug("Promo payment confirmation failed: ${promo.errorMessage}")
                    }
                }

                userRepository.refreshAccount(firebaseToken) { states.emit(it) }
                states.emit(UserDataState.Success)
            } catch (e: Exception) {
                logger.debug("Purchase completion failed: ${e.localizedMessage}")
                states.emit(UserDataState.Error(e.localizedMessage ?: "Unknown error"))
            }
        }
        return states.asSharedFlow()
    }
}
