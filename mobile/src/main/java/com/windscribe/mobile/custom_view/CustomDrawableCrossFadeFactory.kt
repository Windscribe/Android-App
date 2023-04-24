/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.custom_view

import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.transition.DrawableCrossFadeTransition
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.transition.TransitionFactory

open class CustomDrawableCrossFadeFactory protected constructor(
    private val duration: Int, private val isCrossFadeEnabled: Boolean
) : TransitionFactory<Drawable> {
    /**
     * A Builder for [CustomDrawableCrossFadeFactory].
     */
    class Builder(private val durationMillis: Int) {
        private var isCrossFadeEnabled = false
        fun build(): CustomDrawableCrossFadeFactory {
            return CustomDrawableCrossFadeFactory(durationMillis, isCrossFadeEnabled)
        }

        fun setCrossFadeEnabled(isCrossFadeEnabled: Boolean): Builder {
            this.isCrossFadeEnabled = isCrossFadeEnabled
            return this
        }
    }

    private var resourceTransition: DrawableCrossFadeTransition? = null
        get() {
            if (field == null) {
                field = DrawableCrossFadeTransition(duration, isCrossFadeEnabled)
            }
            return field
        }

    override fun build(dataSource: DataSource, isFirstResource: Boolean): Transition<Drawable>? {
        return resourceTransition
    }
}