/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.base

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseActivity : AppCompatActivity() {
    val coldLoad = AtomicBoolean()

    protected fun setContentLayout(layoutID: Int) {
        coldLoad.set(true)
        setContentView(layoutID)
    }

    protected fun onActivityLaunch() {
        coldLoad.set(true)
    }

    fun activityScope(block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED, block)
        }
    }
}
