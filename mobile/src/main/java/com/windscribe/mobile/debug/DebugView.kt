package com.windscribe.mobile.debug

import com.windscribe.mobile.adapter.LogViewAdapter

interface DebugView {
    fun showProgress(show: Boolean)
    fun setAdapter(logViewAdapter: LogViewAdapter)
}