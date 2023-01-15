/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.vpn.serverlist.entity.Node
import java.util.ArrayList

object NodeToJson {
    @JvmStatic
    @TypeConverter
    fun jsonFromNodes(nodes: List<Node>?): String? {
        return if (nodes == null) null else Gson().toJson(nodes)
    }

    @JvmStatic
    @TypeConverter
    fun jsonToNodes(json: String?): List<Node>? {
        return if (json != null) {
            Gson().fromJson(
                json,
                object :
                    TypeToken<ArrayList<Node>>() {}.type
            )
        } else null
    }
}