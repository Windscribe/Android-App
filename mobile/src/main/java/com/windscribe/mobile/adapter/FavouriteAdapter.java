/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;
import com.windscribe.mobile.holder.FavoriteViewHolder;
import com.windscribe.vpn.commonutils.ThemeUtils;
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.serverlist.entity.City;
import com.windscribe.vpn.serverlist.entity.Favourite;
import com.windscribe.vpn.serverlist.entity.PingTime;
import com.windscribe.vpn.serverlist.entity.ServerListData;
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener;

import java.util.ArrayList;
import java.util.List;

public class FavouriteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public ServerListData dataDetails;

    private List<City> mFavouriteList;

    private final ListViewClickListener mListener;


    public FavouriteAdapter(List<City> mFavouriteList, ServerListData dataDetails, ListViewClickListener mListener) {
        this.mFavouriteList = mFavouriteList;
        this.dataDetails = dataDetails;
        this.mListener = mListener;
    }

    public boolean enabledNode(City city) {
        return (city.nodesAvailable() | (!dataDetails.isProUser() && city.getPro() == 1));
    }

    public City getCity(int cityId) {
        for (City city : mFavouriteList) {
            if (city.getId() == cityId) {
                return city;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return mFavouriteList != null ? mFavouriteList.size() : 0;
    }

    public boolean isFavourite(City city) {
        for (Favourite favourite : dataDetails.getFavourites()) {
            if (favourite.getId() == city.getId()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FavoriteViewHolder listViewHolder = (FavoriteViewHolder) holder;
        bindCity(listViewHolder, mFavouriteList.get(holder.getAdapterPosition()));
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.favorite_list_view_holder, parent, false);
        return new FavoriteViewHolder(itemView);
    }

    public void setDataDetails(ServerListData dataDetails) {
        this.dataDetails = dataDetails;
        List<City> cities = new ArrayList<>();
        for (Favourite favourite : dataDetails.getFavourites()) {
            City city = getCity(favourite.getId());
            if (city != null) {
                cities.add(city);
            }
        }
        mFavouriteList = cities;
    }

    public void setFavourites(City city, FavoriteViewHolder holder) {
        holder.tvFavoriteCityName.setAlpha(1f);
        holder.tvFavouriteItemStrength.setAlpha(1f);
        holder.imgFavoriteItemStrengthBar.setAlpha(1f);

        //Setup Pro icon for Unavailable locations
        if (!enabledNode(city)) {
            holder.imgFavorite.setImageResource(R.drawable.construction_icon);
            holder.tvFavoriteCityName.setEnabled(false);
            holder.imgFavorite.setSelected(false);
            holder.tvFavoriteCityName.setAlpha(0.5f);
            holder.tvFavouriteItemStrength.setAlpha(0.5f);
            holder.imgFavoriteItemStrengthBar.setAlpha(0.5f);
        } else if (isFavourite(city)) {
            holder.imgFavorite.setImageResource(R.drawable.modal_add_to_favs);
            holder.imgFavorite.setSelected(true);
        } else {
            holder.imgFavorite.setImageResource(R.drawable.modal_add_to_favs);
            holder.imgFavorite.setSelected(false);
        }
    }

    private void bindCity(FavoriteViewHolder holder, City city) {
        // Pings
        int pingTime = getPingTime(city);
        setPings(holder, pingTime);

        // Favourites
        setFavourites(city, holder);

        // Set Node name/ Nick name
        setNameAndNickName(city, holder);

        //setClickListeners
        setClickListeners(city, holder);

        setTouchListener(holder);

        setLinkSpeed(city, holder);
        //Set server load
        setServerHealth(city, holder);
    }

    private int getPingTime(City city) {
        for (PingTime pingTime : dataDetails.getPingTimes()) {
            if (city.getId() == pingTime.ping_id) {
                return pingTime.getPingTime();
            }
        }
        return -1;
    }

    private int getServerHealthColor(int health, Context context) {
        if (health < 60) {
            return context.getResources().getColor(R.color.colorNeonGreen);
        } else if (health < 89) {
            return context.getResources().getColor(R.color.colorYellow);
        } else {
            return context.getResources().getColor(R.color.colorRed);
        }
    }

    private void setClickListeners(City city, FavoriteViewHolder holder) {
        holder.imgFavorite.setOnClickListener(v -> {
            if (holder.getAdapterPosition() != -1) {
                mListener.removeFromFavourite(mFavouriteList.get(holder.getAdapterPosition()).getId());
            }
        });

        //On click item
        holder.itemView.setOnClickListener(v -> {
            if (!city.nodesAvailable() && city.getPro() != 1) {
                mListener.onUnavailableRegion();
            } else if (!city.nodesAvailable() && city.getPro() == 1 && dataDetails.isProUser()) {
                mListener.onUnavailableRegion();
            } else {
                mListener.onCityClick(city.getId());
            }
        });
    }

    private void setLinkSpeed(final City city, final FavoriteViewHolder holder) {
        int visibility = "10000".equals(city.getLinkSpeed()) && dataDetails.isShowLocationHealthEnabled()
                ? View.VISIBLE : View.INVISIBLE;
        holder.imgLinkSpeed.setVisibility(visibility);
    }

    private void setNameAndNickName(City city, FavoriteViewHolder holder) {
        String sourceString = "<b>" + city.getNodeName() + "</b> " + city.getNickName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.tvFavoriteCityName.setText(Html.fromHtml(sourceString, Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvFavoriteCityName.setText(Html.fromHtml(sourceString));
        }
    }

    private void setPings(FavoriteViewHolder holder, int pingResult) {
        if (dataDetails.isShowLatencyInBar()) {
            holder.tvFavouriteItemStrength.setVisibility(View.GONE);
            holder.imgFavoriteItemStrengthBar.setVisibility(View.VISIBLE);
            if (pingResult != -1) {
                if (pingResult > -1 && pingResult < NetworkKeyConstants.PING_TEST_3_BAR_UPPER_LIMIT) {
                    holder.imgFavoriteItemStrengthBar.setImageResource(R.drawable.ic_network_ping_black_3_bar);
                } else if (pingResult >= NetworkKeyConstants.PING_TEST_3_BAR_UPPER_LIMIT
                        && pingResult < NetworkKeyConstants.PING_TEST_2_BAR_UPPER_LIMIT) {

                    holder.imgFavoriteItemStrengthBar.setImageResource(R.drawable.ic_network_ping_black_2_bar);

                } else if (pingResult >= NetworkKeyConstants.PING_TEST_2_BAR_UPPER_LIMIT
                        && pingResult < NetworkKeyConstants.PING_TEST_1_BAR_UPPER_LIMIT) {

                    holder.imgFavoriteItemStrengthBar.setImageResource(R.drawable.ic_network_ping_black_1_bar);

                } else {
                    holder.imgFavoriteItemStrengthBar.setImageResource(R.drawable.ic_network_ping_black_no_bar);
                }
            }
        } else {
            holder.tvFavouriteItemStrength.setVisibility(View.VISIBLE);
            holder.imgFavoriteItemStrengthBar.setVisibility(View.GONE);
            holder.tvFavouriteItemStrength.setText(pingResult != -1 ? String.valueOf(pingResult) : "--");
        }
    }

    private void setServerHealth(final City city, final FavoriteViewHolder holder) {
        int health = city.getHealth();
        if (dataDetails.isShowLocationHealthEnabled() && health > 0) {
            Context context = holder.itemView.getContext();
            int color = getServerHealthColor(health, context);
            holder.serverHealth.setIndicatorColor(color);
            holder.serverHealth.setProgress(health);
            holder.serverHealth.setVisibility(View.VISIBLE);
        } else {
            holder.serverHealth.setVisibility(View.GONE);
        }
    }

    private void setTextAndIconColors(FavoriteViewHolder holder, int selectedColor) {
        holder.imgFavorite.setImageTintList(ColorStateList.valueOf(selectedColor));
        holder.imgFavoriteItemStrengthBar.setImageTintList(ColorStateList.valueOf(selectedColor));
        holder.imgLinkSpeed.setImageTintList(ColorStateList.valueOf(selectedColor));
        holder.tvFavoriteCityName.setTextColor(ColorStateList.valueOf(selectedColor));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListener(FavoriteViewHolder holder) {
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