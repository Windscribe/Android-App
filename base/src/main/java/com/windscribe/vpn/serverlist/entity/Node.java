/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Ignore;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class Node {

    @SerializedName("force_disconnect")
    @Expose
    private int forceDisconnect = 0;

    @SerializedName("health")
    @Expose
    private int health = 0;

    @SerializedName("hostname")
    @Expose
    private String hostname;

    @SerializedName("ip")
    @Expose
    private String ip;

    @SerializedName("ip2")
    @Expose
    private String ip2;

    @SerializedName("ip3")
    @Expose
    private String ip3;

    @SerializedName("weight")
    @Expose
    private int weight;


    public Node() {

    }

    public Node(String hostname, String ip, String ip2, String ip3, int weight, int forceDisconnect){
        this.hostname = hostname;
        this.ip = ip;
        this.ip2 = ip2;
        this.ip3 = ip3;
        this.weight = weight;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Node) {
            Node node = (Node) obj;
            return node.getHostname().equals(hostname);
        }
        return false;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(final int health) {
        this.health = health;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp2() {
        return ip2;
    }

    public void setIp2(String ip2) {
        this.ip2 = ip2;
    }

    public String getIp3() {
        return ip3;
    }

    public void setIp3(String ip3) {
        this.ip3 = ip3;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isForceDisconnect() {
        return forceDisconnect == 1;
    }

    public void setForceDisconnect(int forceDisconnect) {
        this.forceDisconnect = forceDisconnect;
    }

    @Ignore
    @NonNull
    @Override
    public String toString() {
        return "Node{" +
                "ip='" + ip + '\'' +
                ", ip2='" + ip2 + '\'' +
                ", ip3='" + ip3 + '\'' +
                ", hostname='" + hostname + '\'' +
                ", weight=" + weight +
                ", forceDisconnect=" + forceDisconnect +
                '}';
    }
}
