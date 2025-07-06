/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.holder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.windscribe.mobile.R;

public class FavoriteViewHolder extends RecyclerView.ViewHolder {

    public final ImageView imgFavorite;

    public final ImageView imgFavoriteItemStrengthBar;

    public final ImageView imgLinkSpeed;

    public final LinearProgressIndicator serverHealth;

    public final TextView tvFavoriteCityName;

    public final TextView tvFavouriteItemStrength;

    public FavoriteViewHolder(View itemView) {
        super(itemView);
        tvFavoriteCityName = itemView.findViewById(R.id.favorite_city_name);
        imgFavoriteItemStrengthBar = itemView.findViewById(R.id.favorite_item_strength_bar);
        tvFavouriteItemStrength = itemView.findViewById(R.id.tv_favorite_item_strength);
        imgFavorite = itemView.findViewById(R.id.img_favorite_in_favorites);
        imgLinkSpeed = itemView.findViewById(R.id.link_speed);
        serverHealth = itemView.findViewById(R.id.server_health);
    }
}