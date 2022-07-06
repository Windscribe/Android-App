/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services.verify

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentWorkAroundService
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.billing.AmazonPurchase
import com.windscribe.vpn.billing.PurchaseState.FINISHED
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.exceptions.WindScribeException
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory

class VerifyAmazonPurchaseService : JobIntentWorkAroundService() {

    @Inject
    lateinit var interactor: ServiceInteractor
    private val stateBoolean = AtomicBoolean()
    private val logger = LoggerFactory.getLogger("amazon_verify_s")
    override fun onCreate() {
        super.onCreate()
        stateBoolean.set(true)
        appContext.serviceComponent.inject(this)
    }

    override fun onHandleWork(intent: Intent) {
        interactor.compositeDisposable.add(
                savedAmazonPurchase
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object : DisposableSingleObserver<AmazonPurchase?>() {
                            override fun onError(e: Throwable) {
                                logger.debug(e.message)
                                interactor.compositeDisposable.clear()
                            }

                            override fun onSuccess(amazonPurchase: AmazonPurchase) {
                                verifyAmazonReceipt(amazonPurchase)
                            }
                        })
        )
    }

    private fun finish() {
        if (!interactor.compositeDisposable.isDisposed) {
            interactor.compositeDisposable.dispose()
        }
        stopSelf()
    }

    private val savedAmazonPurchase: Single<AmazonPurchase>
        get() = Single.fromCallable {
            val json = interactor.preferenceHelper.getResponseString(BillingConstants.AMAZON_PURCHASED_ITEM)
                    ?: throw WindScribeException("No amazon purchase found.")
            try {
                return@fromCallable Gson().fromJson(json, AmazonPurchase::class.java)
            } catch (jsonException: JsonSyntaxException) {
                throw WindScribeException("Fatal error: Invalid purchase response saved.")
            }
        }

    private fun verifyAmazonReceipt(amazonPurchase: AmazonPurchase) {
        logger.debug("Verifying amazon receipt.")
        val purchaseMap = HashMap<String, String>()
        purchaseMap[BillingConstants.PURCHASE_TOKEN] = amazonPurchase.receiptId
        purchaseMap[BillingConstants.PURCHASE_TYPE] = BillingConstants.AMAZON_PURCHASE_TYPE
        purchaseMap[BillingConstants.AMAZON_USER_ID] = amazonPurchase.userId
        logger.info(purchaseMap.toString())
        interactor.compositeDisposable.add(
                interactor.apiManager
                        .verifyPayment(purchaseMap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(
                                object : DisposableSingleObserver<GenericResponseClass<String?, ApiErrorResponse?>?>() {
                                    override fun onError(e: Throwable) {
                                        logger
                                                .debug(
                                                        "Payment verification failed. " + WindError.instance
                                                                .convertThrowableToString(e)
                                                )
                                        stopSelf()
                                    }

                                    override fun onSuccess(paymentVerificationResponse: GenericResponseClass<String?, ApiErrorResponse?>) {
                                        when {
                                            paymentVerificationResponse.dataClass != null -> {
                                                try {
                                                    val res = JSONObject(
                                                            paymentVerificationResponse.dataClass!!
                                                    )
                                                    val error = res.getInt("errorCode")
                                                    logger.info("Payment verification code :$error")
                                                    if (error == 4005) {
                                                        finish()
                                                        return
                                                    }
                                                    if (error == 500) {
                                                        finish()
                                                        return
                                                    }
                                                } catch (e: JSONException) {
                                                    logger
                                                            .info("Unable parse Error code from response.")
                                                }
                                                logger.info(
                                                        "Payment verification successful. " +
                                                                paymentVerificationResponse.dataClass +
                                                                " - Removing purchased item from storage."
                                                )
                                                interactor.preferenceHelper
                                                        .removeResponseData(BillingConstants.AMAZON_PURCHASED_ITEM)
                                                logger
                                                        .info("Setting item purchased to null & upgrading user account")
                                                interactor.preferenceHelper.savePurchaseFlowState(
                                                        FINISHED.name
                                                )
                                                appContext.workManager.updateSession()
                                            }
                                            paymentVerificationResponse.errorClass != null -> {
                                                logger
                                                        .debug(
                                                                "Purchase flow: Payment verification failed. Server error response..." +
                                                                        paymentVerificationResponse.errorClass
                                                                                .toString()
                                                        )
                                                interactor.preferenceHelper.savePurchaseFlowState(
                                                        FINISHED.name
                                                )
                                            }
                                            else -> {
                                                finish()
                                            }
                                        }
                                    }
                                })
        )
    }

    companion object {

        private const val PURCHASE_JOB_ID = 39911

        @JvmStatic
        fun enqueueWork(context: Context, intent: Intent) {
            try {
                enqueueWork(context, VerifyAmazonPurchaseService::class.java, PURCHASE_JOB_ID, intent)
            } catch (ignored: IllegalArgumentException) {
                // Lava Device exception!!
            }
        }
    }
}
