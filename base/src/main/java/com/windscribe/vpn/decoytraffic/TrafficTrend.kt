package com.windscribe.vpn.decoytraffic

import kotlin.random.Random

class TrafficTrend {
    var currentUploadTrend = UploadTrend.Min
    var currentDownloadTrend = DownloadTrend.Max
    var upperLimitMultiplier: Int = 16
    private val lowerLimitMultiplier = 1
    private val minMaxRandomPercentage = 90
    private val minMaxPercentage = 25

    fun calculateTraffic(targetData: Int, attemptsToIncrease: Int, isUpload: Boolean): Int{
        val average = if (isUpload) getUploadAverage() else getDownloadAverage()
        val finalDataBasedOnAverage = targetData * average
        if(attemptsToIncrease <= 2) return finalDataBasedOnAverage
        if(isUpload){
            currentUploadTrend.attemptsToIncrease -= 1
        }else{
            currentDownloadTrend.attemptsToIncrease -= 1
        }
        val chunk = finalDataBasedOnAverage / attemptsToIncrease
        if(chunk > finalDataBasedOnAverage)return finalDataBasedOnAverage
        return chunk
    }

    private fun getUploadAverage(): Int {
        if (currentUploadTrend.isFinished()) {
            val random = Random.nextInt(0, 100)
            return if (random <= minMaxRandomPercentage) {
                val minOrMax = Random.nextInt(0, 100)
                return if (minOrMax >= minMaxPercentage) {
                    currentUploadTrend = UploadTrend.Max
                    currentUploadTrend.start()
                    upperLimitMultiplier
                } else {
                    currentUploadTrend = UploadTrend.Min
                    currentUploadTrend.start()
                    lowerLimitMultiplier
                }
            } else {
                currentUploadTrend = UploadTrend.MinMax
                Random.nextInt(lowerLimitMultiplier.coerceAtMost(upperLimitMultiplier), lowerLimitMultiplier.coerceAtLeast(upperLimitMultiplier))
            }
        } else {
            return when (currentUploadTrend) {
                UploadTrend.Min -> lowerLimitMultiplier
                UploadTrend.Max ->  upperLimitMultiplier
                UploadTrend.MinMax -> Random.nextInt(lowerLimitMultiplier.coerceAtMost(upperLimitMultiplier), lowerLimitMultiplier.coerceAtLeast(upperLimitMultiplier))
            }
        }
    }

    private fun getDownloadAverage(): Int {
        if (currentDownloadTrend.isFinished()) {
            val random = Random.nextInt(0, 100)
            return if (random <= minMaxRandomPercentage) {
                val minOrMax = Random.nextInt(0, 100)
                return if (minOrMax >= minMaxPercentage) {
                    currentDownloadTrend = DownloadTrend.Max
                    currentDownloadTrend.start()
                    upperLimitMultiplier
                } else {
                    currentDownloadTrend = DownloadTrend.Min
                    currentDownloadTrend.start()
                    lowerLimitMultiplier
                }
            } else {
                currentDownloadTrend = DownloadTrend.MinMax
                Random.nextInt(lowerLimitMultiplier.coerceAtMost(upperLimitMultiplier), lowerLimitMultiplier.coerceAtLeast(upperLimitMultiplier))
            }
        } else {
            return when (currentDownloadTrend) {
                DownloadTrend.Min -> lowerLimitMultiplier
                DownloadTrend.Max ->  upperLimitMultiplier
                DownloadTrend.MinMax -> Random.nextInt(lowerLimitMultiplier.coerceAtMost(upperLimitMultiplier), lowerLimitMultiplier.coerceAtLeast(upperLimitMultiplier))
            }
        }
    }


    override fun toString(): String {
        return "Upload: $currentUploadTrend | Download: $currentDownloadTrend"
    }

    enum class UploadTrend {
        Min, Max, MinMax;

        fun start() {
            resetTime = System.currentTimeMillis()
            uploadTries = when (this) {
                Min -> Random.nextInt(2, 60)
                Max -> Random.nextInt(60, 180)
                MinMax -> 0
            }
            attemptsToIncrease = when(this){
                Min -> Random.nextInt(1, 10)
                Max -> Random.nextInt(1, 8)
                MinMax -> 1
            }
        }

        fun isFinished(): Boolean{
            val currentTime = System.currentTimeMillis()
            val millis = currentTime - resetTime
            return millis > uploadTries * 1000
        }

        private var resetTime = 0L
        var uploadTries = 0
        var attemptsToIncrease = 1

        override fun toString(): String {
            return "$uploadTries"
        }
    }

    enum class DownloadTrend {
        Min, Max, MinMax;

        fun start() {
            resetTime = System.currentTimeMillis()
            downloadTries = when (this) {
                Min -> Random.nextInt(2, 60)
                Max -> Random.nextInt(60, 180)
                MinMax -> 0
            }
            attemptsToIncrease = when(this){
                Min -> Random.nextInt(1, 10)
                Max -> Random.nextInt(1, 8)
                MinMax -> 1
            }
        }

        fun isFinished(): Boolean{
            val currentTime = System.currentTimeMillis()
            val millis = currentTime - resetTime
            return millis > downloadTries * 1000
        }

        private var resetTime = 0L
        var downloadTries = 0
        var attemptsToIncrease = 1

        override fun toString(): String {
            return "$downloadTries"
        }
    }
}