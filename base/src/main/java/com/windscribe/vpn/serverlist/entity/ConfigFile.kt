/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

@Keep
@Entity(tableName = "ConfigFile")
class ConfigFile : Serializable {
    @PrimaryKey
    @ColumnInfo(name = "primary_key")
    var primaryKey: Int = 0

    @ColumnInfo(name = "Content")
    var content: String? = null

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "password")
    var password: String? = null

    @ColumnInfo(name = "remember")
    var isRemember: Boolean = false

    @Ignore
    var type: Int = 1

    @ColumnInfo(name = "username")
    var username: String? = null

    @Ignore
    constructor(
        primaryKey: Int,
        name: String?,
        content: String?,
        username: String?,
        password: String?,
        remember: Boolean,
    ) {
        this.primaryKey = primaryKey
        this.content = content
        this.username = username
        this.password = password
        this.name = name
        this.isRemember = remember
    }

    constructor()

    override fun toString(): String =
        "ConfigFile{" +
            "primaryKey=" + primaryKey +
            ", content='[redacted]'" +
            ", username='" + mask(username) + '\'' +
            ", password='" + mask(password) + '\'' +
            ", remember=" + isRemember +
            ", type=" + type +
            ", name='" + name + '\'' +
            '}'

    private companion object {
        fun mask(value: String?): String {
            if (value.isNullOrEmpty()) return "****"
            if (value.length <= 4) return "****"
            val maskedLength = value.length - 4
            return "*".repeat(maskedLength) + value.substring(value.length - 4)
        }
    }
}
