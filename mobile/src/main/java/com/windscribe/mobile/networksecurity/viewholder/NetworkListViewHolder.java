/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.networksecurity.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;


public class NetworkListViewHolder extends RecyclerView.ViewHolder {

    public final TextView tvNetworkName;

    public final TextView tvProtection;

    public final ImageView dividerView;


    public NetworkListViewHolder(View itemView) {
        super(itemView);
        tvNetworkName = itemView.findViewById(R.id.network_name);
        tvProtection = itemView.findViewById(R.id.tv_current_protection);
        dividerView = itemView.findViewById(R.id.divider);
    }
}
