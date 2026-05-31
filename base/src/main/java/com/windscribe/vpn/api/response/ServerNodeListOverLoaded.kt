/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import androidx.annotation.Keep

/**
 * Created by Mustafizur on 2017-11-28.
 */
@Keep
class ServerNodeListOverLoaded : Parcelable {
    private val countryCode: String?

    private val countryName: String?

    private val group: String?

    private val hostname: List<String>?

    private val ip: List<String>?

    private val ip2: List<String>?

    private val ip3: List<String>?

    private val premiumOnly: Int

    private val proNodeLocation: Int

    private val randomNodeStrength: Int

    protected constructor(parcel: Parcel) {
        ip = parcel.createStringArrayList()
        ip2 = parcel.createStringArrayList()
        ip3 = parcel.createStringArrayList()
        hostname = parcel.createStringArrayList()
        group = parcel.readString()
        countryName = parcel.readString()
        countryCode = parcel.readString()
        premiumOnly = parcel.readInt()
        proNodeLocation = parcel.readInt()
        randomNodeStrength = parcel.readInt()
    }

    override fun describeContents(): Int = 0

    override fun equals(other: Any?): Boolean {
        if (other !is ServerNodeListOverLoaded) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int = id

    val id: Int
        get() = (nodeNames[0] + nodeNames[1]).hashCode()

    override fun writeToParcel(
        dest: Parcel,
        flags: Int,
    ) {
        dest.writeStringList(ip)
        dest.writeStringList(ip2)
        dest.writeStringList(ip3)
        dest.writeStringList(hostname)
        dest.writeString(group)
        dest.writeString(countryName)
        dest.writeString(countryCode)
        dest.writeInt(premiumOnly)
        dest.writeInt(proNodeLocation)
        dest.writeInt(randomNodeStrength)
    }

    private val nodeNames: SparseArray<String>
        get() {
            val locationArray = group!!.split("-".toRegex(), limit = 2).toTypedArray()
            val nodeName: String
            val nodeNickName: String
            if (locationArray.size > 1) {
                nodeName = locationArray[0].trim()
                nodeNickName = locationArray[1].trim()
            } else if (locationArray.isNotEmpty()) {
                nodeName = locationArray[0].trim()
                nodeNickName = ""
            } else {
                nodeName = "Unknown"
                nodeNickName = ""
            }
            val map = SparseArray<String>()
            map.put(0, nodeName)
            map.put(1, nodeNickName)
            return map
        }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ServerNodeListOverLoaded> =
            object : Parcelable.Creator<ServerNodeListOverLoaded> {
                override fun createFromParcel(parcel: Parcel): ServerNodeListOverLoaded = ServerNodeListOverLoaded(parcel)

                override fun newArray(size: Int): Array<ServerNodeListOverLoaded?> = arrayOfNulls(size)
            }
    }
}
