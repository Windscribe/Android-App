/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.windscribe.vpn.serverlist.converter.NodeToJson;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Entity(tableName = "City")
public class City implements Parcelable {

    public static final Creator<City> CREATOR = new Creator<City>() {
        @Override
        public City createFromParcel(Parcel in) {
            return new City(in);
        }

        @Override
        public City[] newArray(int size) {
            return new City[size];
        }
    };

    @SerializedName("id")
    @Expose
    @ColumnInfo(name = "city_id")
    public int id;

    @SerializedName("nodes")
    @Expose
    @TypeConverters(NodeToJson.class)
    public List<Node> nodes;

    @PrimaryKey(autoGenerate = true)
    public int primaryKey;

    public int region_id;

    @SerializedName("gps")
    @Expose
    @ColumnInfo(name = "gps")
    private String coordinates;

    @SerializedName("health")
    @Expose
    @ColumnInfo(name = "health")
    private int health = 0;

    @SerializedName("link_speed")
    @Expose
    @ColumnInfo(name = "link_speed")
    private String linkSpeed = "100";

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

    @SerializedName("ping_ip")
    @Expose
    @ColumnInfo(name = "ping_ip", defaultValue = "")
    private String pingIp;

    @SerializedName("pro")
    @Expose
    @ColumnInfo(name = "pro")
    private int pro;

    @SerializedName("wg_pubkey")
    @Expose
    @ColumnInfo(name = "wg_pubkey")
    private String pubKey;

    @SerializedName("tz")
    @Expose
    @ColumnInfo(name = "tz")
    private String tz;

    protected City(Parcel in) {
        region_id = in.readInt();
        id = in.readInt();
        nodeName = in.readString();
        nickName = in.readString();
        pro = in.readInt();
        coordinates = in.readString();
        tz = in.readString();
        in.readList(nodes, Node.class.getClassLoader());
        primaryKey = in.readInt();
        pubKey = in.readString();
        pingIp = in.readString();
        ovpnX509 = in.readString();
        linkSpeed = in.readString();
        health = in.readInt();
    }

    public City() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof City) {
            City city = (City) obj;
            return city.getId() == id;
        }
        return false;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(final int health) {
        this.health = health;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLinkSpeed() {
        return linkSpeed;
    }

    public void setLinkSpeed(final String linkSpeed) {
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

    public List<Node> getNodes() {
        List<Node> nodesToRemove = new ArrayList<>();
        if (nodes != null) {
            for (Node node : nodes) {
                boolean forceDisconnected = node.isForceDisconnect();
                if (forceDisconnected) {
                    nodesToRemove.add(node);
                }
            }
            for (Node removeNode : nodesToRemove) {
                nodes.remove(removeNode);
            }
        }
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public String getOvpnX509() {
        return ovpnX509;
    }

    public void setOvpnX509(final String ovpnX509) {
        this.ovpnX509 = ovpnX509;
    }

    public String getPingIp() {
        return pingIp;
    }

    public void setPingIp(String pingIp) {
        this.pingIp = pingIp;
    }

    public int getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(int primaryKey) {
        this.primaryKey = primaryKey;
    }

    public int getPro() {
        return pro;
    }

    public void setPro(int pro) {
        this.pro = pro;
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

    public String getTz() {
        return tz;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public boolean nodesAvailable() {
        List<Node> nodesToRemove = new ArrayList<>();
        if (nodes != null) {
            for (Node node : nodes) {
                boolean forceDisconnected = node.isForceDisconnect();
                if (forceDisconnected) {
                    nodesToRemove.add(node);
                }
            }
            for (Node removeNode : nodesToRemove) {
                nodes.remove(removeNode);
            }
        }
        return nodes != null && nodes.size() > 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "City{" +
                "primaryKey=" + primaryKey +
                ", nodes=" + nodes +
                ", region_id=" + region_id +
                ", id=" + id +
                ", nodeName='" + nodeName + '\'' +
                ", nickName='" + nickName + '\'' +
                ", pro=" + pro +
                ", coordinates='" + coordinates + '\'' +
                ", tz='" + tz + '\'' +
                ", pubKey='" + pubKey + '\'' +
                ", ovpnX509='" + ovpnX509 + '\'' +
                '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(region_id);
        dest.writeInt(id);
        dest.writeString(nodeName);
        dest.writeString(nickName);
        dest.writeInt(pro);
        dest.writeString(coordinates);
        dest.writeString(tz);
        dest.writeList(nodes);
        dest.writeInt(primaryKey);
        dest.writeString(pubKey);
        dest.writeString(pingIp);
        dest.writeString(ovpnX509);
        dest.writeString(linkSpeed);
        dest.writeInt(health);
    }
}
