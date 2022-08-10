/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.account

import android.content.Context
import com.windscribe.mobile.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.api.CreateHashMap.createVerifyExpressLoginMap
import com.windscribe.vpn.api.CreateHashMap.createWebSessionMap
import com.windscribe.vpn.api.response.*
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NetworkKeyConstants.getWebsiteLink
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.model.User
import com.windscribe.vpn.model.User.EmailStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AccountPresenterImpl @Inject constructor(
    private val accountView: AccountView,
    private val interactor: ActivityInteractor
) : AccountPresenter {
    private val logger = LoggerFactory.getLogger("account_p")
    override fun onDestroy() {
        if (interactor.getCompositeDisposable().isDisposed.not()) {
            logger.info("Disposing observer on destroy...")
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun onAddEmailClicked(tvEmailText: String) {
        if (interactor.getResourceString(R.string.add_email) == tvEmailText) {
            logger.info("Go to add Email activity")
            accountView.goToEmailActivity()
        } else {
            logger.info("User already confirmed email...")
        }
    }

    override fun observeUserData(accountActivity: AccountActivity) {
        interactor.getUserRepository().user.observe(accountActivity) { user: User ->
            setUserInfo(
                user
            )
        }
    }

    override fun onCodeEntered(code: String) {
        accountView.showProgress("Verifying code...")
        logger.debug("verifying express login code.")
        val verifyLoginMap = createVerifyExpressLoginMap(code)
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager()
                .verifyExpressLoginCode(verifyLoginMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    object :
                        DisposableSingleObserver<GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>>() {
                        override fun onError(e: Throwable) {
                            logger.debug(
                                String.format(
                                    "Error verifying login code: %s",
                                    e.localizedMessage
                                )
                            )
                            accountView.hideProgress()
                            accountView.showErrorDialog(
                                "Error verifying login code. Check your network connection."
                            )
                        }

                        override fun onSuccess(
                            response: GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>
                        ) {
                            accountView.hideProgress()
                            if (response.dataClass != null && response.dataClass!!.isSuccessful) {
                                logger.debug("Successfully verified login code")
                                accountView.showSuccessDialog(
                                    """
    Sweet, you should be
    all good to go now.
    """.trimIndent()
                                )
                            } else if (response.errorClass != null) {
                                logger.debug(
                                    String.format(
                                        "Error verifying login code: %s",
                                        response.errorClass!!.errorMessage
                                    )
                                )
                                accountView.showErrorDialog(response.errorClass!!.errorMessage)
                            } else {
                                logger.debug("Failed to verify lazy login code.")
                                accountView.showErrorDialog("Failed to verify lazy login code.")
                            }
                        }
                    })
        )
    }

    override fun onEditAccountClicked() {
        accountView.setWebSessionLoading(true)
        logger.info("Opening My Account page in browser...")
        val webSessionMap = createWebSessionMap()
        interactor.getCompositeDisposable()
            .add(
                interactor.getApiCallManager().getWebSession(webSessionMap)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(
                        object :
                            DisposableSingleObserver<GenericResponseClass<WebSession?, ApiErrorResponse?>?>() {
                            override fun onError(e: Throwable) {
                                accountView.setWebSessionLoading(false)
                                accountView.showErrorDialog(
                                    "Unable to generate web session. Check your network connection."
                                )
                            }

                            override fun onSuccess(
                                webSession: GenericResponseClass<WebSession?, ApiErrorResponse?>
                            ) {
                                accountView.setWebSessionLoading(false)
                                if (webSession.dataClass != null) {
                                    accountView.openEditAccountInBrowser(
                                        getWebsiteLink(NetworkKeyConstants.URL_MY_ACCOUNT)
                                                + webSession.dataClass!!.tempSession
                                    )
                                } else if (webSession.errorClass != null) {
                                    accountView
                                        .showErrorDialog(webSession.errorClass!!.errorMessage)
                                } else {
                                    accountView.showErrorDialog(
                                        "Unable to generate Web-Session. Check your network connection."
                                    )
                                }
                            }
                        })
            )
    }

    override fun onResendEmail() {
        accountView.goToConfirmEmailActivity()
    }

    override fun onUpgradeClicked(textViewText: String) {
        if (interactor.getResourceString(R.string.upgrade_case_normal) == textViewText) {
            logger.info("Showing upgrade dialog to the user...")
            accountView.openUpgradeActivity()
        } else {
            logger.info("User is already pro no actions taken...")
        }
    }

    override fun onLazyLoginClicked() {
        accountView.showEnterCodeDialog()
    }

    override fun setLayoutFromApiSession() {
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager()
                .getSessionGeneric(null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    object :
                        DisposableSingleObserver<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>?>() {
                        override fun onError(e: Throwable) {
                            logger.debug(
                                "Error while making get session call:" +
                                        instance.convertThrowableToString(e)
                            )
                        }

                        override fun onSuccess(
                            userSessionResponse: GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>
                        ) {
                            if (userSessionResponse.dataClass != null) {
                                interactor.getUserRepository()
                                    .reload(userSessionResponse.dataClass, null)
                            } else if (userSessionResponse.errorClass != null) {
                                //Server responded with error!
                                logger.debug(
                                    "Server returned error during get session call."
                                            + userSessionResponse.errorClass.toString()
                                )
                            }
                        }
                    })
        )
    }

    override fun setTheme(context: Context) {
        val savedThem = interactor.getAppPreferenceInterface().selectedTheme
        logger.debug("Setting theme to $savedThem")
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    private fun setUserInfo(user: User) {
        accountView.setActivityTitle(interactor.getResourceString(R.string.account))
        if (user.isGhost) {
            accountView.setupLayoutForGhostMode(user.isPro)
        } else if (user.maxData != -1L) {
            accountView.setupLayoutForFreeUser(
                interactor.getResourceString(R.string.upgrade_case_normal),
                interactor.getThemeColor(R.attr.wdActionColor)
            )
        } else {
            accountView.setupLayoutForPremiumUser(
                interactor.getResourceString(R.string.plan_pro),
                interactor.getThemeColor(R.attr.wdActionColor)
            )
        }
        accountView.setUsername(user.userName)
        when (user.emailStatus) {
            EmailStatus.NoEmail -> accountView.setEmail(
                interactor.getResourceString(R.string.add_email),
                interactor.getResourceString(
                    R.string.get_10gb_data
                ),
                interactor.getThemeColor(R.attr.wdSecondaryColor),
                interactor.getThemeColor(R.attr.wdActionColor),
                interactor.getThemeColor(R.attr.wdPrimaryColor),
                R.drawable.ic_email_attention,
                R.drawable.confirmed_email_container_background
            )
            EmailStatus.EmailProvided -> accountView.setEmailConfirm(
                user.email!!, interactor.getResourceString(
                    R.string.confirm_your_email
                ), interactor.getColorResource(R.color.colorYellow50), interactor.getColorResource(
                    R.color.colorYellow
                ), R.drawable.ic_warning_icon, R.drawable.attention_container_background
            )
            EmailStatus.Confirmed -> accountView.setEmailConfirmed(
                user.email!!, interactor.getResourceString(
                    R.string.get_10gb_data
                ), interactor.getThemeColor(R.attr.wdSecondaryColor), interactor.getThemeColor(
                    R.attr.wdPrimaryColor
                ), R.drawable.ic_email_attention, R.drawable.confirmed_email_container_background
            )
        }
        if (user.maxData == -1L) {
            accountView.setPlanName(interactor.getResourceString(R.string.unlimited_data))
            accountView.setDataLeft("")
        } else {
            val maxData = user.maxData / UserStatusConstants.GB_DATA
            accountView.setPlanName("$maxData ${interactor.getResourceString(R.string.gb_per_month)}")
            if (user.dataLeft != null) {
                val dataLeft = DecimalFormat("##.00").format(user.dataLeft)
                accountView.setDataLeft("$dataLeft GB")
            }
        }
        setExpiryOrResetDate(user)
    }

    private fun setExpiryOrResetDate(user: User) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var date: String? = null
        if (user.isPro && user.expiryDate != null) {
            date = user.expiryDate
        } else if (user.resetDate != null) {
            date = user.resetDate
        }
        if (date != null) {
            try {
                val lastResetDate = formatter.parse(date)
                val c = Calendar.getInstance()
                c.time = Objects.requireNonNull(lastResetDate)
                if (!user.isPro) {
                    c.add(Calendar.MONTH, 1)
                    val nextResetDate = c.time
                    accountView.setResetDate(
                        interactor.getResourceString(R.string.reset_date),
                        formatter.format(nextResetDate)
                    )
                } else {
                    val nextResetDate = c.time
                    accountView.setResetDate(
                        interactor.getResourceString(R.string.expiry_date),
                        formatter.format(nextResetDate)
                    )
                }
            } catch (e: ParseException) {
                logger.debug("Could not parse date data. " + instance.convertErrorToString(e))
            }
        }
    }
}