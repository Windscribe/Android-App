package com.windscribe.vpn.decoytraffic

import kotlin.random.Random

enum class FakeTrafficVolume {

    Low, Medium, High;

    fun toBytes(): Int {
        return when (this) {
            Low -> 100 * 1000
            Medium -> 1 * 1000 * 1000
            High -> 10 * 1000 * 1000
        }
    }

    fun multiplier(): Int {
        return when (this) {
            Low -> 175
            Medium -> 80
            High -> 16
        }
    }

    fun interval(): Int {
        return when (this) {
            Low -> Random.nextInt(16, 20)
            Medium -> Random.nextInt(5, 12)
            High -> Random.nextInt(1, 8)
        }
    }
}