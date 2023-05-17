/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.holder;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;
import com.windscribe.mobile.R;

public class CityViewHolder extends ChildViewHolder implements View.OnClickListener {

    public final ImageView imgFavorite;

    public final ImageView imgLinkSpeed;

    public final ImageView imgSignalStrengthBar;

    public final TextView nodeGroupName;

    public final LinearProgressIndicator serverHealth;

    public final TextView tvSignalStrength;


    public CityViewHolder(View itemView) {
        super(itemView);
        nodeGroupName = itemView.findViewById(R.id.node_name);
        imgFavorite = itemView.findViewById(R.id.img_favorite);
        imgSignalStrengthBar = itemView.findViewById(R.id.signal_strength_bar);
        tvSignalStrength = itemView.findViewById(R.id.tv_signal_strength);
        imgLinkSpeed = itemView.findViewById(R.id.link_speed);
        serverHealth = itemView.findViewById(R.id.server_health);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cl_node_locations:
                Log.d(this.getClass().getSimpleName(), "CONNECT TO " + nodeGroupName.getText().toString());
                break;
            case R.id.node_name:
                break;
            default:
        }
    }
}
