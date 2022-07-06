/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@SuppressWarnings("unused")
public class PortMapResponse {

    @SerializedName("portmap")
    @Expose
    private final List<PortMap> portmap = null;
    @SerializedName("suggested")
    @Expose
    private Suggested suggested;

    public List<PortMap> getPortmap() {
        return portmap;
    }

    @Nullable
    public Suggested getSuggested() {
        return suggested;
    }

    public boolean isProtocolSuggested() {
        return suggested != null;
    }

    @NonNull
    @Override
    public String toString() {
        return "PortMapResponse{" +
                ", portmap=" + portmap +
                ", suggested=" + suggested +
                '}';
    }

    public static class PortMap {

        @SerializedName("legacy_ports")
        @Expose
        private final List<String> legacyPorts = null;
        @SerializedName("ports")
        @Expose
        private final List<String> ports = null;
        @SerializedName("heading")
        @Expose
        private String heading;
        @SerializedName("protocol")
        @Expose
        private String protocol;

        @SerializedName("use")
        @Expose
        private String use;

        public String getHeading() {
            return heading;
        }

        public List<String> getLegacyPorts() {
            return legacyPorts;
        }

        public List<String> getPorts() {
            return ports;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getUse() {
            return use;
        }

        @NonNull
        @Override
        public String toString() {
            return "PortMap{" +
                    "protocol='" + protocol + '\'' +
                    ", heading='" + heading + '\'' +
                    ", use='" + use + '\'' +
                    ", ports=" + ports +
                    ", legacyPorts=" + legacyPorts +
                    '}';
        }
    }

    public static class Suggested {

        @SerializedName("port")
        @Expose
        private int port;

        @SerializedName("protocol")
        @Expose
        private String protocol;

        public int getPort() {
            return port;
        }

        public String getProtocol() {
            return protocol;
        }

        @NonNull
        @Override
        public String toString() {
            return "Suggested{" +
                    "protocol='" + protocol + '\'' +
                    ", port='" + port + '\'' +
                    '}';
        }
    }

}
