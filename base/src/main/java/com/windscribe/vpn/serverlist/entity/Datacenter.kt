/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.Objects

@Keep
@Entity(tableName = "Datacenter")
class Datacenter : Parcelable {
    @SerializedName("id")
    @Expose
    @ColumnInfo(name = "city_id")
    var id: Int = 0

    @PrimaryKey(autoGenerate = true)
    var primaryKey: Int = 0

    var region_id: Int = 0

    @SerializedName("gps")
    @Expose
    @ColumnInfo(name = "gps")
    var coordinates: String? = null

    @SerializedName("iata")
    @Expose
    @ColumnInfo(name = "iata")
    var iata: String? = null

    @SerializedName("link_speed")
    @Expose
    @ColumnInfo(name = "link_speed")
    var linkSpeed: Int = 100

    @SerializedName("nick")
    @Expose
    @ColumnInfo(name = "nick")
    var nickName: String? = null

    @SerializedName("city")
    @Expose
    @ColumnInfo(name = "city")
    var nodeName: String? = null

    @SerializedName("ovpn_x509")
    @Expose
    @ColumnInfo(name = "ovpn_x509")
    var ovpnX509: String? = null

    @SerializedName("p2p")
    @Expose
    @ColumnInfo(name = "p2p")
    var p2p: Int = 0

    @SerializedName("pro")
    @Expose
    @ColumnInfo(name = "pro")
    var pro: Int = 0

    @SerializedName("wg_pubkey")
    @Expose
    @ColumnInfo(name = "wg_pubkey")
    var pubKey: String? = null

    @SerializedName("status")
    @Expose
    @ColumnInfo(name = "status")
    var status: Int = 1

    @SerializedName("tz")
    @Expose
    @ColumnInfo(name = "tz")
    var tz: String? = null

    @SerializedName("wg_endpoint")
    @Expose
    @ColumnInfo(name = "wg_endpoint")
    var wgEndpoint: String? = null

    @Ignore
    constructor(
        region_id: Int,
        id: Int,
        nodeName: String?,
        nickName: String?,
        coordinates: String?,
        tz: String?,
        iata: String?,
        status: Int,
        p2p: Int,
        pro: Int,
        pubKey: String?,
        wgEndpoint: String?,
        ovpnX509: String?,
        linkSpeed: Int,
    ) {
        this.region_id = region_id
        this.id = id
        this.nodeName = nodeName
        this.nickName = nickName
        this.coordinates = coordinates
        this.tz = tz
        this.iata = iata
        this.status = status
        this.p2p = p2p
        this.pro = pro
        this.pubKey = pubKey
        this.wgEndpoint = wgEndpoint
        this.ovpnX509 = ovpnX509
        this.linkSpeed = linkSpeed
    }

    @Ignore
    constructor(parcel: Parcel) {
        region_id = parcel.readInt()
        id = parcel.readInt()
        nodeName = parcel.readString()
        nickName = parcel.readString()
        coordinates = parcel.readString()
        tz = parcel.readString()
        primaryKey = parcel.readInt()
        pubKey = parcel.readString()
        ovpnX509 = parcel.readString()
        linkSpeed = parcel.readInt()
        iata = parcel.readString()
        status = parcel.readInt()
        p2p = parcel.readInt()
        pro = parcel.readInt()
        wgEndpoint = parcel.readString()
    }

    constructor()

    override fun describeContents(): Int = 0

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val city = o as Datacenter
        return id == city.id &&
            primaryKey == city.primaryKey &&
            region_id == city.region_id &&
            linkSpeed == city.linkSpeed &&
            p2p == city.p2p &&
            pro == city.pro &&
            status == city.status &&
            coordinates == city.coordinates &&
            iata == city.iata &&
            nickName == city.nickName &&
            nodeName == city.nodeName &&
            ovpnX509 == city.ovpnX509 &&
            pubKey == city.pubKey &&
            tz == city.tz &&
            wgEndpoint == city.wgEndpoint
    }

    override fun hashCode(): Int =
        Objects.hash(
            id,
            primaryKey,
            region_id,
            coordinates,
            iata,
            linkSpeed,
            nickName,
            nodeName,
            ovpnX509,
            p2p,
            pro,
            pubKey,
            status,
            tz,
            wgEndpoint,
        )

    fun getRegionID(): Int = region_id

    fun setRegionID(regionID: Int) {
        this.region_id = regionID
    }

    fun nodesAvailable(serverCount: Int): Boolean = serverCount > 0

    override fun toString(): String =
        "Datacenter{" +
            "primaryKey=" + primaryKey +
            ", region_id=" + region_id +
            ", id=" + id +
            ", nodeName='" + nodeName + '\'' +
            ", nickName='" + nickName + '\'' +
            ", coordinates='" + coordinates + '\'' +
            ", tz='" + tz + '\'' +
            ", iata='" + iata + '\'' +
            ", status=" + status +
            ", p2p=" + p2p +
            ", pubKey='" + pubKey + '\'' +
            ", wgEndpoint='" + wgEndpoint + '\'' +
            ", ovpnX509='" + ovpnX509 + '\'' +
            ", linkSpeed=" + linkSpeed +
            '}'

    override fun writeToParcel(
        dest: Parcel,
        flags: Int,
    ) {
        dest.writeInt(region_id)
        dest.writeInt(id)
        dest.writeString(nodeName)
        dest.writeString(nickName)
        dest.writeString(coordinates)
        dest.writeString(tz)
        dest.writeInt(primaryKey)
        dest.writeString(pubKey)
        dest.writeString(ovpnX509)
        dest.writeInt(linkSpeed)
        dest.writeString(iata)
        dest.writeInt(status)
        dest.writeInt(p2p)
        dest.writeInt(pro)
        dest.writeString(wgEndpoint)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Datacenter> =
            object : Parcelable.Creator<Datacenter> {
                override fun createFromParcel(parcel: Parcel): Datacenter = Datacenter(parcel)

                override fun newArray(size: Int): Array<Datacenter?> = arrayOfNulls(size)
            }
    }
}
