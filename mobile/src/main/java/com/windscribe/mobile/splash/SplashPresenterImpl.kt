/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.splash

import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.errormodel.WindError
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashPresenterImpl @Inject constructor(
    private var view: SplashView,
    private var interactor: ActivityInteractor
) : SplashPresenter {

    private val logger = LoggerFactory.getLogger("basic")

    /* Stop Session service if running
     * Check purchase token if available
     * Get User session.
     * Update User status
     * Check if server List need update
     * Update server config, server credentials op and ikEv2
     * Request , Parse  and save list
     * Check if ping test can be done
     * Ping every node and Save to database
     * Save best location id based on lowest ping.
     *
     * */
    override fun onDestroy() {
        if (!interactor.getCompositeDisposable().isDisposed) {
            interactor.getCompositeDisposable().dispose()
        }
    }

    fun checkApplicationInstanceAndDecideActivity() {
        if (interactor.getAppPreferenceInterface().isNewApplicationInstance) {
            interactor.getAppPreferenceInterface().isNewApplicationInstance = false
            val installation = interactor.getAppPreferenceInterface()
                .getResponseString(PreferencesKeyConstants.NEW_INSTALLATION)
            if (PreferencesKeyConstants.I_NEW == installation) {
                //Record new install
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
                                DisposableSingleObserver<GenericResponseClass<String?, ApiErrorResponse?>>() {
                                override fun onError(e: Throwable) {
                                    logger.debug(WindError.instance.rxErrorToString(e as Exception))
                                    decideActivity()
                                }

                                override fun onSuccess(
                                    recordInstallResponse: GenericResponseClass<String?, ApiErrorResponse?>
                                ) {
                                    if (recordInstallResponse.dataClass != null) {
                                        logger.info(
                                            "Recording app install success. "
                                                    + recordInstallResponse.dataClass
                                        )
                                    } else if (recordInstallResponse.errorClass != null) {
                                        logger.debug(
                                            "Recording app install failed. "
                                                    + recordInstallResponse.errorClass.toString()
                                        )
                                    }
                                    decideActivity()
                                }
                            })
                )
            } else {
                //Not a new install, decide activity
                decideActivity()
            }
        } else {
            //Decide which activity to goto
            decideActivity()
        }
    }

    override fun checkNewMigration() {
        interactor.getAutoConnectionManager().reset()
        migrateSessionAuthIfRequired()
        val userLoggedIn = interactor.getAppPreferenceInterface().sessionHash != null
        if (userLoggedIn) {
            interactor.getCompositeDisposable().add(
                interactor.serverDataAvailable()
                    .flatMap { serverListAvailable ->
                        return@flatMap Single.create { sub ->
                            interactor.getFireBaseManager().getFirebaseToken { token ->
                                sub.onSuccess(token ?: "")
                            }
                        }.flatMap {
                            interactor.getApiCallManager().getSessionGeneric(it)
                        }.flatMap {
                            return@flatMap Single.just(serverListAvailable)
                        }
                    }.timeout(500, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<Boolean?>() {
                        override fun onError(ignored: Throwable) {
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
        val sessionHash = interactor.getAppPreferenceInterface().sessionHash
        if (sessionHash != null) {
            if (interactor.getAppPreferenceInterface().loginTime == null){
                interactor.getAppPreferenceInterface().loginTime = Date()
            }
            logger.info("Session auth hash present. User is already logged in...")
            if (view.isConnectedToNetwork.not()) {
                logger.info("NO ACTIVE NETWORK FOUND! Starting home activity with stale data.")
            }
            if (shouldShowAccountSetUp()) {
                view.navigateToAccountSetUp()
            } else {
                view.navigateToHome()
            }
        } else {
            view.navigateToLogin()
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

    private fun shouldShowAccountSetUp(): Boolean {
        val ghostAccount = interactor.getAppPreferenceInterface().userIsInGhostMode()
        val proUser = (interactor.getAppPreferenceInterface().userStatus
                == UserStatusConstants.USER_STATUS_PREMIUM)
        return ghostAccount && proUser
    }

    private fun updateDataFromApiAndOldStorage() {
        interactor.getCompositeDisposable().add(
            interactor.getServerListUpdater().update()
                .doOnError { logger.info("Failed to download server list.") }
                .andThen(interactor.getStaticListUpdater().update())
                .doOnError { logger.info("Failed to download static server list.") }
                .andThen(interactor.updateUserData())
                .andThen(Completable.fromAction {
                    interactor.getPreferenceChangeObserver().postCityServerChange()
                })
                .onErrorResumeNext { throwable: Throwable ->
                    logger.info(
                        "*********Preparing dashboard failed: " + throwable.toString()
                                + " Use reload button in server list in home activity.*******"
                    )
                    interactor.updateUserData().andThen(
                        Completable.fromAction {
                            interactor.getPreferenceChangeObserver().postCityServerChange()
                        })
                }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        checkApplicationInstanceAndDecideActivity()
                    }

                    override fun onError(ignored: Throwable) {
                        checkApplicationInstanceAndDecideActivity()
                    }
                })
        )
    }
}