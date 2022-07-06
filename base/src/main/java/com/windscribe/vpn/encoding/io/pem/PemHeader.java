/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.encoding.io.pem;

public class PemHeader {

    private final String name;

    private final String value;

    public PemHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public boolean equals(Object o) {
        if (!(o instanceof PemHeader)) {
            return false;
        }

        PemHeader other = (PemHeader) o;

        return other == this || (isEqual(this.name, other.name) && isEqual(this.value, other.value));
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int hashCode() {
        return getHashCode(this.name) + 31 * getHashCode(this.value);
    }

    private int getHashCode(String s) {
        if (s == null) {
            return 1;
        }

        return s.hashCode();
    }

    private boolean isEqual(String s1, String s2) {
        return s1.equals(s2);
    }
}
