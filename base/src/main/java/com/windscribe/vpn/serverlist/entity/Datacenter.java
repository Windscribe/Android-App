/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@Keep
@Entity(tableName = "Datacenter")
public class Datacenter implements Parcelable {

    public static final Creator<Datacenter> CREATOR = new Creator<Datacenter>() {
        @Override
        public Datacenter createFromParcel(Parcel in) {
            return new Datacenter(in);
        }

        @Override
        public Datacenter[] newArray(int size) {
            return new Datacenter[size];
        }
    };

    @SerializedName("id")
    @Expose
    @ColumnInfo(name = "city_id")
    public int id;

    @PrimaryKey(autoGenerate = true)
    public int primaryKey;

    public int region_id;

    @SerializedName("gps")
    @Expose
    @ColumnInfo(name = "gps")
    private String coordinates;

    @SerializedName("iata")
    @Expose
    @ColumnInfo(name = "iata")
    private String iata;

    @SerializedName("link_speed")
    @Expose
    @ColumnInfo(name = "link_speed")
    private int linkSpeed = 100;

    @SerializedName("nick")
    @Expose
    @ColumnInfo(name = "nick")
    private String nickName;

    @SerializedName("city")
    @Expose
    @ColumnInfo(name = "city")
    private String nodeName;

    @SerializedName("ovpn_x509")
    @Expose
    @ColumnInfo(name = "ovpn_x509")
    private String ovpnX509;

    @SerializedName("p2p")
    @Expose
    @ColumnInfo(name = "p2p")
    private int p2p;

    @SerializedName("pro")
    @Expose
    @ColumnInfo(name = "pro")
    public int pro = 0;

    @SerializedName("wg_pubkey")
    @Expose
    @ColumnInfo(name = "wg_pubkey")
    private String pubKey;

    @SerializedName("status")
    @Expose
    @ColumnInfo(name = "status")
    private int status = 1;

    @SerializedName("tz")
    @Expose
    @ColumnInfo(name = "tz")
    private String tz;

    @SerializedName("wg_endpoint")
    @Expose
    @ColumnInfo(name = "wg_endpoint")
    private String wgEndpoint;

    public Datacenter(int region_id, int id, String nodeName, String nickName, String coordinates, String tz, String iata, int status, int p2p, int pro, String pubKey, String wgEndpoint, String ovpnX509, int linkSpeed) {
        this.region_id = region_id;
        this.id = id;
        this.nodeName = nodeName;
        this.nickName = nickName;
        this.coordinates = coordinates;
        this.tz = tz;
        this.iata = iata;
        this.status = status;
        this.p2p = p2p;
        this.pro = pro;
        this.pubKey = pubKey;
        this.wgEndpoint = wgEndpoint;
        this.ovpnX509 = ovpnX509;
        this.linkSpeed = linkSpeed;
    }

    @androidx.room.Ignore
    protected Datacenter(Parcel in) {
        region_id = in.readInt();
        id = in.readInt();
        nodeName = in.readString();
        nickName = in.readString();
        coordinates = in.readString();
        tz = in.readString();
        primaryKey = in.readInt();
        pubKey = in.readString();
        ovpnX509 = in.readString();
        linkSpeed = in.readInt();
        iata = in.readString();
        status = in.readInt();
        p2p = in.readInt();
        pro = in.readInt();
        wgEndpoint = in.readString();
    }

    public Datacenter() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Datacenter city = (Datacenter) o;
        return id == city.id && primaryKey == city.primaryKey && region_id == city.region_id && linkSpeed == city.linkSpeed && p2p == city.p2p && pro == city.pro && status == city.status && Objects.equals(coordinates, city.coordinates) && Objects.equals(iata, city.iata) && Objects.equals(nickName, city.nickName) && Objects.equals(nodeName, city.nodeName) && Objects.equals(ovpnX509, city.ovpnX509) && Objects.equals(pubKey, city.pubKey) && Objects.equals(tz, city.tz) && Objects.equals(wgEndpoint, city.wgEndpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, primaryKey, region_id, coordinates, iata, linkSpeed, nickName, nodeName, ovpnX509, p2p, pro, pubKey, status, tz, wgEndpoint);
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public String getIata() {
        return iata;
    }

    public void setIata(String iata) {
        this.iata = iata;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLinkSpeed() {
        return linkSpeed;
    }

    public void setLinkSpeed(int linkSpeed) {
        this.linkSpeed = linkSpeed;
    }

    public String getNickName() {
        if (nickName == null) {
            return "";
        }
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getOvpnX509() {
        return ovpnX509;
    }

    public void setOvpnX509(String ovpnX509) {
        this.ovpnX509 = ovpnX509;
    }

    public int getP2p() {
        return p2p;
    }

    public void setP2p(int p2p) {
        this.p2p = p2p;
    }

    public int getPro() {
        return pro;
    }

    public void setPro(int pro) {
        this.pro = pro;
    }

    public int getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(int primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getPubKey() {
        return pubKey;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }

    public int getRegionID() {
        return region_id;
    }

    public void setRegionID(int regionID) {
        this.region_id = regionID;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTz() {
        return tz;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public String getWgEndpoint() {
        return wgEndpoint;
    }

    public void setWgEndpoint(String wgEndpoint) {
        this.wgEndpoint = wgEndpoint;
    }

    public boolean nodesAvailable(int serverCount) {
        return serverCount > 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "Datacenter{" +
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
                '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(region_id);
        dest.writeInt(id);
        dest.writeString(nodeName);
        dest.writeString(nickName);
        dest.writeString(coordinates);
        dest.writeString(tz);
        dest.writeInt(primaryKey);
        dest.writeString(pubKey);
        dest.writeString(ovpnX509);
        dest.writeInt(linkSpeed);
        dest.writeString(iata);
        dest.writeInt(status);
        dest.writeInt(p2p);
        dest.writeInt(pro);
        dest.writeString(wgEndpoint);
    }
}
