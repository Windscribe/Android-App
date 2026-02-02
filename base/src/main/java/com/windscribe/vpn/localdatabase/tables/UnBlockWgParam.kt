package com.windscribe.vpn.localdatabase.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.windscribe.vpn.localdatabase.ListToString

@Entity(tableName = "UnBlockWgParam")
@TypeConverters(ListToString::class)
data class UnBlockWgParam(
    @ColumnInfo(name = "title")
    @PrimaryKey
    val title: String = "",

    @ColumnInfo(name = "countries")
    val countries: List<String> = listOf(),

    @ColumnInfo(name = "Jc")
    val jc: Int = 0,

    @ColumnInfo(name = "Jmin")
    val jMin: Int = 0,

    @ColumnInfo(name = "Jmax")
    val jMax: Int = 0,

    @ColumnInfo(name = "S1")
    val s1: Int = 0,

    @ColumnInfo(name = "S2")
    val s2: Int = 0,

    @ColumnInfo(name = "S3")
    val s3: Int = 0,

    @ColumnInfo(name = "S4")
    val s4: Int = 0,

    @ColumnInfo(name = "H1")
    val h1: String = "",

    @ColumnInfo(name = "H2")
    val h2: String = "",

    @ColumnInfo(name = "H3")
    val h3: String = "",

    @ColumnInfo(name = "H4")
    val h4: String = "",

    @ColumnInfo(name = "I1")
    val i1: String = "",

    @ColumnInfo(name = "I2")
    val i2: String = "",

    @ColumnInfo(name = "I3")
    val i3: String = "",

    @ColumnInfo(name = "I4")
    val i4: String = "",

    @ColumnInfo(name = "I5")
    val i5: String = ""
)