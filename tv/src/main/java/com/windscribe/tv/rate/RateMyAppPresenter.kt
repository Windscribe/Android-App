/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.rate

import com.windscribe.tv.rate.RateView
import kotlinx.coroutines.CoroutineScope

interface RateMyAppPresenter {
    fun bind(view: RateView, scope: CoroutineScope)
    fun onAskMeLaterClick()
    fun onNeverAskClick()
    fun onRateNowClick()
}
