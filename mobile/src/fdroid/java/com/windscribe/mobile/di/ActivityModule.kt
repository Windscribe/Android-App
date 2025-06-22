/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.di

import androidx.appcompat.app.AppCompatActivity
import dagger.Module

@Module
open class ActivityModule {
    private var activity: AppCompatActivity

    constructor(mActivity: AppCompatActivity) {
        this.activity = mActivity
    }
}