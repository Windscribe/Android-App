/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.rate

import kotlinx.coroutines.CoroutineScope

interface RateMyAppPresenter {
    fun bind(
        view: RateView,
        scope: CoroutineScope,
    )

    fun onAskMeLaterClick()

    fun onNeverAskClick()

    fun onRateNowClick()
}
