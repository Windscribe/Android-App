/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.holder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;


public class StaticRegionHolder extends RecyclerView.ViewHolder {

    public final ImageView mImageIpType;

    public final ImageView mImgSignalStrengthBar;

    public final TextView mIpCityName;

    public final TextView mStaticIp;

    public final TextView mTextViewPing;


    public StaticRegionHolder(@NonNull View itemView) {
        super(itemView);
        mImageIpType = itemView.findViewById(R.id.img_ip_type);
        mIpCityName = itemView.findViewById(R.id.ip_city_name);
        mStaticIp = itemView.findViewById(R.id.static_ip);
        mImgSignalStrengthBar = itemView.findViewById(R.id.static_ip_signal_strength_bar);
        mTextViewPing = itemView.findViewById(R.id.tv_signal_strength_static_ip);
    }
}
