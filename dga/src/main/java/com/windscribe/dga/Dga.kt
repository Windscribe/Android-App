package com.windscribe.dga

object Dga {
    fun load() {
        System.loadLibrary("dga-library")
    }

    external fun getDomain(ctx: Any?): String?
}