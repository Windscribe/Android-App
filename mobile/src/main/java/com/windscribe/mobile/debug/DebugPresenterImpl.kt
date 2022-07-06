package com.windscribe.mobile.debug

import com.windscribe.mobile.adapter.LogViewAdapter
import com.windscribe.vpn.ActivityInteractor

class DebugPresenterImpl(val view: DebugView, val activityInteractor: ActivityInteractor): DebugPresenter {

    override suspend fun init() {
        view.showProgress(true)
        val logs = activityInteractor.getPartialLog().takeLast(350)
        val logViewAdapter = LogViewAdapter(logs)
        view.setAdapter(logViewAdapter)
        view.showProgress(false)
    }
}