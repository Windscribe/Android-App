/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Keep;

@Keep
public enum QueryType {
    Account(1),
    Technical(2),
    Sales(3),
    Feedback(4);

    private final int value;

    QueryType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name();
    }
}
