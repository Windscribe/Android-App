/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.splash

import com.google.gson.Gson
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.Windscribe.Companion.getExecutorService
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.ItemPurchased
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.BillingConstants.GP_PACKAGE_NAME
import com.windscribe.vpn.constants.BillingConstants.GP_PRODUCT_ID
import com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM
import com.windscribe.vpn.constants.BillingConstants.PURCHASE_TOKEN
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.services.ping.PingTestService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject

class SplashPresenterImpl @Inject constructor(
    private var splashView: SplashView,
    private var interactor: ActivityInteractor
) : SplashPresenter {
    private val logger = LoggerFactory.getLogger("splash_p")

    override fun onDestroy() {
        if (!interactor.getCompositeDisposable().isDisposed) {
            logger.info("Disposing network observer...")
            interactor.getCompositeDisposable().dispose()
        }
    }

    fun checkApplicationInstanceAndDecideActivity() {
        if (interactor.getAppPreferenceInterface().isNewApplicationInstance) {
            getExecutorService().submit {
                interactor.getAppPreferenceInterface().isNewApplicationInstance = false
            }
            val installation = interactor.getAppPreferenceInterface()
                .getResponseString(PreferencesKeyConstants.NEW_INSTALLATION)
            if (PreferencesKeyConstants.I_NEW == installation) {
                // Record new install
                logger.info("Recording new installation of the app")
                interactor.getAppPreferenceInterface()
                    .saveResponseStringData(
                        PreferencesKeyConstants.NEW_INSTALLATION,
                        PreferencesKeyConstants.I_OLD
                    )
                interactor.getCompositeDisposable().add(
                    interactor.getApiCallManager()
                        .recordAppInstall()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(
                            object :
                                DisposableSingleObserver<GenericResponseClass<String?, ApiErrorResponse?>?>() {
                                override fun onError(e: Throwable) {
                                    logger.debug(
                                        "Error: " + instance
                                            .convertThrowableToString(e)
                                    )
                                    decideActivity()
                                }

                                override fun onSuccess(
                                    recordInstallResponse: GenericResponseClass<String?, ApiErrorResponse?>
                                ) {
                                    if (recordInstallResponse.dataClass != null) {
                                        logger.info(
                                            "Recording app install success. " +
                                                recordInstallResponse.dataClass
                                        )
                                    } else if (recordInstallResponse.errorClass != null) {
                                        logger.debug(
                                            "Recording app install failed. " +
                                                recordInstallResponse.errorClass.toString()
                                        )
                                    }
                                    decideActivity()
                                }
                            })
                )
            } else {
                // Not a new install, decide activity
                decideActivity()
            }
        } else {
            // Decide which activity to goto
            decideActivity()
        }
    }

    override fun checkNewMigration() {
        migrateSessionAuthIfRequired()
        val userLoggedIn = interactor.getAppPreferenceInterface().sessionHash != null
        if (userLoggedIn) {
            interactor.getCompositeDisposable().add(
                interactor.serverDataAvailable()
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<Boolean?>() {
                        override fun onError(e: Throwable) {
                            checkApplicationInstanceAndDecideActivity()
                        }

                        override fun onSuccess(serverListAvailable: Boolean) {
                            if (serverListAvailable) {
                                logger.info("Migration not required.")
                                checkApplicationInstanceAndDecideActivity()
                            } else {
                                logger.info("Migration required. updating server list.")
                                updateDataFromApiAndOldStorage()
                            }
                        }
                    })
            )
        } else {
            checkApplicationInstanceAndDecideActivity()
        }
    }

    fun decideActivity() {
        logger.info("Checking if user already logged in...")
        val sessionHash = interactor.getAppPreferenceInterface().sessionHash
        if (sessionHash != null) {
            logger.info("Session auth hash present. User is already logged in...")
            if (WindUtilities.isOnline()) {
                PingTestService.startPingTestService()
                logger.info("Found active network connectivity. Updating application data...")
                if (interactor.getAppPreferenceInterface()
                    .getResponseString(PURCHASED_ITEM) != null
                ) {
                    logger.info("Found pending purchase token, making purchase verification call...")
                    val purchaseMap: MutableMap<String, String> = HashMap()
                    interactor.getCompositeDisposable().add(
                        Single.fromCallable {
                            Gson().fromJson(
                                interactor.getAppPreferenceInterface()
                                    .getResponseString(PURCHASED_ITEM),
                                ItemPurchased::class.java
                            )
                        }.flatMap { itemPurchased: ItemPurchased ->
                            purchaseMap[GP_PACKAGE_NAME] = itemPurchased.packageName
                            purchaseMap[GP_PRODUCT_ID] = itemPurchased.productId
                            purchaseMap[PURCHASE_TOKEN] = itemPurchased.purchaseToken
                            interactor.getApiCallManager()
                                .verifyPayment(purchaseMap)
                        }.subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .subscribeWith(
                                object :
                                    DisposableSingleObserver<GenericResponseClass<String?, ApiErrorResponse?>?>() {
                                    override fun onError(e: Throwable) {
                                        logger
                                            .debug(
                                                "Error while retrying payment verification. " +
                                                    instance
                                                        .convertThrowableToString(e)
                                            )
                                        splashView.navigateToHome()
                                    }

                                    override fun onSuccess(
                                        paymentVerificationResponse: GenericResponseClass<String?, ApiErrorResponse?>
                                    ) {
                                        if (paymentVerificationResponse.dataClass != null) {
                                            logger.info(
                                                "Payment verification successful. " +
                                                    paymentVerificationResponse.dataClass +
                                                    " - Removing purchased item from storage."
                                            )
                                            interactor.getAppPreferenceInterface()
                                                .removeResponseData(PURCHASED_ITEM)
                                        } else if (paymentVerificationResponse.errorClass != null) {
                                            logger
                                                .debug(
                                                    "Payment verification failed. Server error response..." +
                                                        paymentVerificationResponse.errorClass
                                                            .toString() + "- Retry on next start..."
                                                )
                                        }
                                        splashView.navigateToHome()
                                    }
                                })
                    )
                } else {
                    logger.info("No pending purchase ID...")
                    splashView.navigateToHome()
                }
            } else {
                logger.info("NO ACTIVE NETWORK FOUND! Starting home activity with stale data.")
                splashView.navigateToHome()
            }
        } else {
            // Goto Login/Registration Activity
            logger.info("Session auth hash not present. User not logged in...")
            splashView.navigateToLogin()
        }
    }

    // Move SessionAuth to secure preferences
    private fun migrateSessionAuthIfRequired() {
        val oldSessionAuth = interactor.getAppPreferenceInterface().oldSessionAuth
        val newSessionAuth = interactor.getAppPreferenceInterface().sessionHash
        if (oldSessionAuth != null && newSessionAuth == null) {
            logger.debug("Migrating session auth to secure preferences")
            interactor.getAppPreferenceInterface().sessionHash = oldSessionAuth
            interactor.getAppPreferenceInterface().clearOldSessionAuth()
        }
    }

    private fun updateDataFromApiAndOldStorage() {
        interactor.getCompositeDisposable().add(
            interactor.getServerListUpdater().update()
                .doOnError { logger.info("Failed to download server list.") }
                .andThen(interactor.getStaticListUpdater().update())
                .andThen(interactor.updateUserData())
                .andThen(interactor.updateServerData())
                .onErrorResumeNext { throwable: Throwable ->
                    logger.info(
                        "*********Preparing dashboard failed: " + throwable.toString() +
                            " Use reload button in server list in home activity.*******"
                    )
                    interactor.updateUserData().andThen(interactor.updateServerData())
                }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        checkApplicationInstanceAndDecideActivity()
                    }

                    override fun onError(e: Throwable) {
                        checkApplicationInstanceAndDecideActivity()
                    }
                })
        )
    }
}
