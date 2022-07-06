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
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.serverlist.entity.ConfigFile;
import com.windscribe.vpn.serverlist.entity.ServerListData;
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener;

public class RemoveConfigHolder extends RecyclerView.ViewHolder {

    private final TextView configNameView;

    private final ImageView imgFavoriteItemStrengthBar;

    private final ImageView mDelete;

    private final ImageView mEdit;

    private final TextView tvFavouriteItemStrength;

    public RemoveConfigHolder(@NonNull View itemView) {
        super(itemView);
        configNameView = itemView.findViewById(R.id.config_name);
        mDelete = itemView.findViewById(R.id.config_item_delete);
        mEdit = itemView.findViewById(R.id.config_item_edit);
        tvFavouriteItemStrength = itemView.findViewById(R.id.tv_config_item_strength);
        imgFavoriteItemStrengthBar = itemView.findViewById(R.id.config_item_strength_bar);
    }

    public void onBind(ConfigFile configFile, ListViewClickListener listViewClickListener, ServerListData dataDetails,
            int pingResult) {
        int length = configFile.getName().length();
        configNameView.setText(configFile.getName().substring(length / 2));
        mDelete.setOnClickListener(v -> listViewClickListener.deleteConfigFile(configFile));
        mEdit.setOnClickListener(v -> listViewClickListener.editConfigFile(configFile));

        if (dataDetails.isShowLatencyInBar()) {
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
            if (pingResult != -1) {
                tvFavouriteItemStrength.setText(String.valueOf(pingResult));
            } else {
                tvFavouriteItemStrength.setText("--");
            }
        }
    }
}
