package com.windscribe.vpn.repository

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.UnblockWgResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.UnBlockWgParam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnblockWgParamsRepository @Inject constructor(
    val scope: CoroutineScope,
    val preferencesHelper: PreferencesHelper,
    val api: IApiCallManager,
    val db: LocalDbInterface
) {
    private val logger = LoggerFactory.getLogger("UnblockWgParamsRepository")
    private val _unblockWgParams = MutableStateFlow(emptyList<UnBlockWgParam>())
    val unblockWgParams: StateFlow<List<UnBlockWgParam>> = _unblockWgParams

    init {
        load()
    }

    private fun load() {
        scope.launch(Dispatchers.IO) {
            db.getUnblockWgParams().collect {
                _unblockWgParams.emit(it)
            }
        }
    }

    suspend fun update(): Boolean {
        when (val result = api.unblockWgParams().callResult<UnblockWgResponse>()) {
            is CallResult.Success<UnblockWgResponse> -> {
                logger.info("Fetched ${result.data.params.size} unblock wg params")
                val savedPreset = preferencesHelper.selectedUnblockWgParam
                val selected = result.data.params.firstOrNull { it.title == savedPreset }
                if (selected == null) {
                    preferencesHelper.selectedUnblockWgParam = result.data.params.firstOrNull()?.title
                }
                if (result.data.params.isNotEmpty()) {
                    val mappedData = result.data.params.map { param ->
                        UnBlockWgParam(
                            title = param.title,
                            countries = param.countries,
                            jc = param.jc ?: 0,
                            jMin = param.jMin ?: 0,
                            jMax = param.jMax ?: 0,
                            s1 = param.s1 ?: 0,
                            s2 = param.s2 ?: 0,
                            s3 = param.s3 ?: 0,
                            s4 = param.s4 ?: 0,
                            h1 = param.h1?: "",
                            h2 = param.h2?: "",
                            h3 = param.h3?: "",
                            h4 = param.h4?: "",
                            i1 = param.i1?: "",
                            i2 = param.i2 ?: "",
                            i3 = param.i3 ?: "",
                            i4 = param.i4 ?: "",
                            i5 = param.i5 ?: ""
                        )
                    }
                    db.deleteUnblockWgParams()
                    db.insertUnblockWgParams(mappedData)
                }
                return true
            }

            is CallResult.Error -> {
                logger.info("Fetched Errror {} unblock wg params", result.code)
                logger.error(
                    "Error while fetching unblock wg params Code:{}, Error:{}",
                    result.code,
                    result.errorMessage
                )
                return false
            }
        }
    }

    fun getSelectedUnblockWgParam(): UnBlockWgParam? {
        var selected = preferencesHelper.selectedUnblockWgParam
        if (selected == null) {
            selected = unblockWgParams.value.firstOrNull()?.title
        }
        return unblockWgParams.value.firstOrNull { it.title == selected }
    }

    fun setSelectedUnblockWgParam(id: String) {
        preferencesHelper.selectedUnblockWgParam = id
    }
}