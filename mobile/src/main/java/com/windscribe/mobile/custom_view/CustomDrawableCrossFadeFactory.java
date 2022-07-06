/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.custom_view;


import android.graphics.drawable.Drawable;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.transition.DrawableCrossFadeTransition;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.transition.TransitionFactory;

@SuppressWarnings("WeakerAccess")
public class CustomDrawableCrossFadeFactory implements TransitionFactory<Drawable> {

    /**
     * A Builder for {@link CustomDrawableCrossFadeFactory}.
     */
    public static class Builder {

        private final int durationMillis;

        private boolean isCrossFadeEnabled;

        public Builder(int durationMillis) {
            this.durationMillis = durationMillis;
        }

        public CustomDrawableCrossFadeFactory build() {
            return new CustomDrawableCrossFadeFactory(durationMillis, isCrossFadeEnabled);
        }

        public Builder setCrossFadeEnabled(boolean isCrossFadeEnabled) {
            this.isCrossFadeEnabled = isCrossFadeEnabled;
            return this;
        }
    }

    private final int duration;

    private final boolean isCrossFadeEnabled;

    private DrawableCrossFadeTransition resourceTransition;

    protected CustomDrawableCrossFadeFactory(int duration, boolean isCrossFadeEnabled) {
        this.duration = duration;
        this.isCrossFadeEnabled = isCrossFadeEnabled;
    }

    @Override
    public Transition<Drawable> build(DataSource dataSource, boolean isFirstResource) {
        return getResourceTransition();
    }

    private Transition<Drawable> getResourceTransition() {
        if (resourceTransition == null) {
            resourceTransition = new DrawableCrossFadeTransition(duration, isCrossFadeEnabled);
        }
        return resourceTransition;
    }
}
