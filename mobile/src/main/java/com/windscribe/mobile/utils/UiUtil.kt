/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.utils

import java.util.regex.Pattern


object UiUtil {
    fun getPriceWithCurrency(price: String?): Pair<String, Double>? {
        if (price == null){
            return null
        }
        val rawPrice = price.replace("\u00A0", " ").trim()
        val pattern = Pattern.compile("([A-Za-z]{3}|[^\\d,\\.]+)?\\s?([\\d,.]+)")
        val matcher = pattern.matcher(rawPrice)

        return if (matcher.find()) {
            val currency = matcher.group(1)?.trim().orEmpty()
            val priceString = matcher.group(2)?.replace(",", "")
            priceString?.toDoubleOrNull()?.let { priceValue ->
                Pair(currency, priceValue)
            }
        } else {
            null
        }
    }
}