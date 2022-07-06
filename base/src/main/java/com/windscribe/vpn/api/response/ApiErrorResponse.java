/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Mustafizur on 2017-09-16.
 */

public class ApiErrorResponse {

    @SerializedName("errorCode")
    @Expose
    private Integer errorCode;

    @SerializedName("errorDescription")
    @Expose
    private String errorDescription;

    @SerializedName("errorMessage")
    @Expose
    private String errorMessage;

    @SerializedName("logStatus")
    @Expose
    private String logStatus;

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @NonNull
    @Override
    public String toString() {
        return "ErrorResponse{" +
                "errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorDescription='" + errorDescription + '\'' +
                ", logStatus='" + logStatus + '\'' +
                '}';
    }
}
