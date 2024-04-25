/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.robert

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.RobertAdapterListener
import com.windscribe.mobile.adapter.RobertSettingsAdapter
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.CreateHashMap.createWebSessionMap
import com.windscribe.vpn.api.response.*
import com.windscribe.vpn.constants.FeatureExplainer
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.CallResult
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class RobertSettingsPresenterImpl(
    private val robertSettingsView: RobertSettingsView,
    private val interactor: ActivityInteractor
) : RobertSettingsPresenter, RobertAdapterListener {
    private val mPresenterLog = LoggerFactory.getLogger("robert_p")
    private var robertSettingsAdapter: RobertSettingsAdapter? = null
    override fun onDestroy() {
        interactor.getCompositeDisposable().clear()
    }

    override val savedLocale: String
        get() {
            val selectedLanguage =
                interactor.getAppPreferenceInterface().savedLanguage
            return selectedLanguage.substring(
                selectedLanguage.indexOf("(") + 1,
                selectedLanguage.indexOf(")")
            )
        }

    override fun init() {
        robertSettingsView.setTitle(interactor.getResourceString(R.string.robert))
        loadSettings()
    }

    override fun onCustomRulesClick() {
        robertSettingsView.setWebSessionLoading(true)
        mPresenterLog.info("Opening robert rules page in browser...")
        interactor.getCompositeDisposable()
            .add(interactor.getApiCallManager().getWebSession()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response: GenericResponseClass<WebSession?, ApiErrorResponse?> ->
                    handleWebSessionResponse(
                        response
                    )
                }) { throwable: Throwable -> handleWebSessionError(throwable) })
    }

    override fun onLearnMoreClick() {
        robertSettingsView.openUrl(FeatureExplainer.ROBERT)
    }

    override fun setTheme(context: Context) {
        val savedThem = interactor.getAppPreferenceInterface().selectedTheme
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    override fun settingChanged(
        originalList: List<RobertFilter>,
        filter: RobertFilter,
        position: Int
    ) {
        robertSettingsAdapter?.settingUpdateInProgress = true
        robertSettingsView.showProgress()
        interactor.getCompositeDisposable()
            .add(interactor.getApiCallManager().updateRobertSettings(filter.id, filter.status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response: GenericResponseClass<GenericSuccess?, ApiErrorResponse?> ->
                        handleRobertSettingUpdateResponse(
                            response,
                            originalList,
                            position
                        )
                    }
                ) {
                    handleRobertSettingsUpdateError(
                        interactor.getResourceString(R.string.failed_to_update_robert_rules),
                        originalList,
                        position
                    )
                })
    }

    private fun handleRobertLoadSettingResponse(robertFilters: List<RobertFilter>) {
        robertSettingsAdapter = RobertSettingsAdapter(this)
        robertSettingsAdapter?.let {
            it.data = robertFilters
            robertSettingsView.setAdapter(it)
        }
        robertSettingsView.hideProgress()
    }

    private fun handleRobertSettingUpdateResponse(
        response: GenericResponseClass<GenericSuccess?, ApiErrorResponse?>,
        originalList: List<RobertFilter>, position: Int
    ) {
        robertSettingsAdapter?.settingUpdateInProgress = false
        when (val result = response.callResult<GenericSuccess>()) {
            is CallResult.Error -> {
                if (result.code != NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA) {
                    handleRobertSettingsUpdateError(result.errorMessage, originalList, position)
                } else {
                    handleRobertSettingsUpdateError(
                        interactor.getResourceString(R.string.failed_to_update_robert_rules),
                        originalList,
                        position
                    )
                }
            }
            is CallResult.Success -> {
                robertSettingsView.hideProgress()
                robertSettingsView.showToast(interactor.getResourceString(R.string.successfully_updated_robert_rules))
                appContext.workManager.updateRobertRules()
            }
        }
    }

    private fun handleRobertSettingsUpdateError(
        error: String,
        originalList: List<RobertFilter>,
        position: Int
    ) {
        robertSettingsAdapter?.settingUpdateInProgress = false
        robertSettingsView.hideProgress()
        robertSettingsView.showToast(error)
        robertSettingsAdapter?.data = originalList
        robertSettingsAdapter?.notifyItemChanged(position)
    }

    private fun handleWebSessionError(throwable: Throwable) {
        mPresenterLog.debug(
            String.format(
                "Failed to generate web session: %s",
                throwable.localizedMessage
            )
        )
        robertSettingsView.setWebSessionLoading(false)
        robertSettingsView.showErrorDialog("Failed to generate web session. Check your network connection.")
    }

    private fun handleWebSessionResponse(response: GenericResponseClass<WebSession?, ApiErrorResponse?>) {
        robertSettingsView.setWebSessionLoading(false)
        when (val result = response.callResult<WebSession>()) {
            is CallResult.Error -> {
                if (result.code != NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA) {
                    mPresenterLog.debug(
                        String.format(
                            "Failed to generate web session: %s",
                            result.errorMessage
                        )
                    )
                    robertSettingsView.showErrorDialog(result.errorMessage)
                } else {
                    robertSettingsView.showErrorDialog("Failed to generate Web-Session. Check your network connection.")
                }
            }
            is CallResult.Success -> {
                robertSettingsView.openUrl(responseToUrl(result.data))
            }
        }
    }

    @Throws(WindScribeException::class)
    private fun loadFromDatabase(throwable: Throwable): Single<List<RobertFilter>> {
        val json = interactor.getAppPreferenceInterface()
            .getResponseString(PreferencesKeyConstants.ROBERT_FILTERS)
            ?: throw WindScribeException(throwable.localizedMessage)
        return Single.just(
            Gson().fromJson(
                json,
                object : TypeToken<List<RobertFilter>>() {}.type
            )
        )
    }

    private fun loadSettings() {
        robertSettingsView.showProgress()
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager().getRobertFilters()
                .flatMap { response: GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?> ->
                    saveToDatabase(
                        response
                    )
                }
                .onErrorResumeNext { throwable: Throwable -> loadFromDatabase(throwable) }
                .subscribeOn(Schedulers.io())
                .delaySubscription(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ robertFilters: List<RobertFilter> ->
                    handleRobertLoadSettingResponse(
                        robertFilters
                    )
                }
                ) {
                    robertSettingsView.hideProgress()
                    robertSettingsView
                        .showError("Failed to load to Robert settings. Check your network connection.")
                })
    }

    private fun responseToUrl(webSession: WebSession): String {
        val uri = Uri.Builder()
            .scheme("https")
            .authority(NetworkKeyConstants.WEB_URL?.replace("https://", ""))
            .path("myaccount")
            .fragment("robertrules")
            .appendQueryParameter("temp_session", webSession.tempSession)
            .build()
        return uri.toString()
    }

    @Throws(WindScribeException::class)
    private fun saveToDatabase(
        response: GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?>
    ): Single<List<RobertFilter>> {
        when (val result = response.callResult<RobertFilterResponse>()) {
            is CallResult.Error -> {
                if (result.code != NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA) {
                    throw WindScribeException(result.errorMessage)
                } else {
                    throw WindScribeException("Unexpected Api response.")
                }
            }
            is CallResult.Success -> {
                val robertSettings = result.data.filters
                val json = Gson().toJson(robertSettings)
                interactor.getAppPreferenceInterface()
                    .saveResponseStringData(PreferencesKeyConstants.ROBERT_FILTERS, json)
                return Single.just(robertSettings)
            }
        }
    }
}