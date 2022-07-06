/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.holder;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;
import com.windscribe.vpn.commonutils.ThemeUtils;
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.serverlist.entity.ConfigFile;
import com.windscribe.vpn.serverlist.entity.ServerListData;
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener;

public class ConfigViewHolder extends RecyclerView.ViewHolder {

    private final TextView configNameView;

    private final ImageView imgFavoriteItemStrengthBar;

    private final TextView tvFavouriteItemStrength;

    public ConfigViewHolder(@NonNull View itemView) {
        super(itemView);
        configNameView = itemView.findViewById(R.id.config_name);
        tvFavouriteItemStrength = itemView.findViewById(R.id.tv_config_item_strength);
        imgFavoriteItemStrengthBar = itemView.findViewById(R.id.config_item_strength_bar);
    }

    public void onBind(ConfigFile configFile, ListViewClickListener listViewClickListener, ServerListData serverListData,
            int pingResult) {
        configNameView.setText(configFile.getName());
        itemView.setOnClickListener(v -> listViewClickListener.onConfigFileClicked(configFile));
        if (serverListData.isShowLatencyInBar()) {
            tvFavouriteItemStrength.setVisibility(View.GONE);
            imgFavoriteItemStrengthBar.setVisibility(View.VISIBLE);
            if (pingResult != -1) {
                if (pingResult > -1 && pingResult < NetworkKeyConstants.PING_TEST_3_BAR_UPPER_LIMIT) {
                    imgFavoriteItemStrengthBar.setImageResource(R.drawable.ic_network_ping_black_3_bar);
                } else if (pingResult >= NetworkKeyConstants.PING_TEST_3_BAR_UPPER_LIMIT
                        && pingResult < NetworkKeyConstants.PING_TEST_2_BAR_UPPER_LIMIT) {

                    imgFavoriteItemStrengthBar.setImageResource(R.drawable.ic_network_ping_black_2_bar);

                } else if (pingResult >= NetworkKeyConstants.PING_TEST_2_BAR_UPPER_LIMIT
                        && pingResult < NetworkKeyConstants.PING_TEST_1_BAR_UPPER_LIMIT) {

                    imgFavoriteItemStrengthBar.setImageResource(R.drawable.ic_network_ping_black_1_bar);

                } else {
                    imgFavoriteItemStrengthBar.setImageResource(R.drawable.ic_network_ping_black_no_bar);
                }
            }
        } else {
            tvFavouriteItemStrength.setVisibility(View.VISIBLE);
            imgFavoriteItemStrengthBar.setVisibility(View.GONE);
            tvFavouriteItemStrength.setText(pingResult != -1 ? String.valueOf(pingResult) : "--");
        }
        setTouchListener(this);
    }

    private void setTextAndIconColors(ConfigViewHolder holder, int selectedColor) {
        holder.imgFavoriteItemStrengthBar.setImageTintList(ColorStateList.valueOf(selectedColor));
        holder.configNameView.setTextColor(ColorStateList.valueOf(selectedColor));
        holder.tvFavouriteItemStrength.setTextColor(ColorStateList.valueOf(selectedColor));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListener(ConfigViewHolder holder) {
        holder.itemView.setOnTouchListener((View v, MotionEvent event) -> {
            int defaultColor = ThemeUtils.getColor(v.getContext(), R.attr.wdSecondaryColor, R.color.colorWhite50);
            int selectedColor = ThemeUtils.getColor(v.getContext(), R.attr.wdPrimaryColor, R.color.colorWhite50);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setTextAndIconColors(holder, selectedColor);
            } else {
                setTextAndIconColors(holder, defaultColor);
            }
            return false;
        });
    }
}
