/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.windscribe.mobile.R;
import com.windscribe.mobile.holder.CityViewHolder;
import com.windscribe.mobile.holder.RegionViewHolder;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.commonutils.ThemeUtils;
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.serverlist.entity.City;
import com.windscribe.vpn.serverlist.entity.Favourite;
import com.windscribe.vpn.serverlist.entity.Group;
import com.windscribe.vpn.serverlist.entity.PingTime;
import com.windscribe.vpn.serverlist.entity.Region;
import com.windscribe.vpn.serverlist.entity.ServerListData;
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener;

import java.util.ArrayList;
import java.util.List;

public class RegionsAdapter extends ExpandableRecyclerViewAdapter<RegionViewHolder, CityViewHolder> {

    private ServerListData serverListData;

    private final long mLastClickTime = 0;

    private final ListViewClickListener mListener;

    public RegionsAdapter(List<? extends ExpandableGroup> groups, ServerListData serverListData,
            ListViewClickListener mListener) {
        super(groups);
        this.mListener = mListener;
        this.serverListData = serverListData;
    }

    public ServerListData getServerListData() {
        return serverListData;
    }

    public void setServerListData(ServerListData serverListData) {
        this.serverListData = serverListData;
    }

    public List<Group> getGroupsList() {
        List<Group> groupList = new ArrayList<>();
        for (@SuppressWarnings("rawtypes") ExpandableGroup expandableGroup : getGroups()) {
            Group group = (Group) expandableGroup;
            groupList.add(group);
        }
        return groupList;
    }

    //City View Holder
    @Override
    public void onBindChildViewHolder(final CityViewHolder holder, final int flatPosition,
            final ExpandableGroup group, int childIndex) {
        final City city = (City) group.getItems().get(childIndex);
        bindCity(holder, city);
    }

    // Group View holder
    @Override
    public void onBindGroupViewHolder(final RegionViewHolder holder, final int flatPosition,
            final ExpandableGroup group) {
        Group expandableGroup = (Group) group;
        // Best Location and normal Location
        if (((Group) group).getRegion() == null) {
            bindBestLocation(holder);
        } else {
            bindRegion(expandableGroup.getRegion(), expandableGroup.getItems(), holder);
        }
    }

    @Override
    public CityViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.server_node_list_view_holder, parent, false);
        return new CityViewHolder(view);
    }

    @Override
    public RegionViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.server_locations_view_holder, parent, false);
        return new RegionViewHolder(view);
    }

    public void setExpandStatus(RegionViewHolder holder) {
        if (!isGroupExpanded(holder.getAdapterPosition())) {
            holder.imgAnimationLine.setVisibility(View.GONE);
            holder.imgDropDown.setImageResource(ThemeUtils
                    .getResourceId(holder.itemView.getContext(), R.attr.close_list_icon,
                            R.drawable.ic_location_dropdown_collapse));
            int color = ThemeUtils
                    .getColor(holder.itemView.getContext(), R.attr.nodeListGroupTextColor, R.color.colorWhite);
            holder.tvCountryName.setTextColor(color);
        } else {
            holder.imgDropDown.setImageResource(ThemeUtils
                    .getResourceId(holder.itemView.getContext(), R.attr.expand_list_icon,
                            R.drawable.ic_location_drop_down_expansion));
            int color = ThemeUtils.getColor(holder.itemView.getContext(), R.attr.nodeListGroupTextColorSelected,
                    R.color.colorWhite);
            holder.tvCountryName.setTextColor(color);
            holder.imgAnimationLine.setVisibility(View.VISIBLE);
        }
    }

    public void setGroupClickListener(Region region, RegionViewHolder holder) {
        holder.itemView.setOnClickListener(view -> {
            if (serverListData.isProUser()
                    && region.getStatus() == NetworkKeyConstants.SERVER_STATUS_TEMPORARILY_UNAVAILABLE) {
                mListener.onUnavailableRegion();
            } else {
                holder.onClick(view);
            }
        });

        //Drop down image
        holder.imgDropDown.setOnClickListener(view -> {
            if (serverListData.isProUser()
                    && region.getStatus() == NetworkKeyConstants.SERVER_STATUS_TEMPORARILY_UNAVAILABLE) {
                mListener.onUnavailableRegion();
            } else {
                holder.onClick(view);
            }
        });
    }

    public void setPremiumStatus(int isPro, RegionViewHolder holder) {
        if (isPro == 1 && !serverListData.isProUser()) {
            int drawable = ThemeUtils
                    .getResourceId(holder.itemView.getContext(), R.attr.proBadge, R.drawable.ic_hs_pro_badge);
            holder.imgProBadge.setImageResource(drawable);
        } else {
            holder.imgProBadge.setImageDrawable(null);
        }
    }

    private void bindBestLocation(RegionViewHolder holder) {
        if (serverListData.getBestLocation().getRegion() != null) {
            holder.tvCountryName.setText(holder.itemView.getContext().getString(R.string.best_location));
            setFlags(serverListData.getBestLocation().getRegion().getCountryCode(), holder);
            setPremiumStatus(0, holder);
            setExpandStatus(holder);
            holder.imgDropDown.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(
                    v -> mListener.onCityClick(serverListData.getBestLocation().getCity().getId()));
            setGroupHealth(serverListData.getBestLocation().getCity().getHealth(), holder);
        }
    }

    private void bindCity(CityViewHolder holder, City city) {
        //setClickListeners
        setClickListeners(city, holder);

        setTouchListener(holder);

        // Pings
        int pingTime = getPingTime(city);
        setPings(holder, pingTime);

        // Favourites
        setFavourites(city, holder);

        // Set Node name/ Nick name
        setNameAndNickName(city, holder);

        //Set Link Speed
        setLinkSpeed(city, holder);

        //Set server load
        setServerHealth(city, holder);
    }

    private void bindRegion(Region region, List<City> cities, RegionViewHolder holder) {
        // Group Name
        holder.setGroupName(region.getName());
        // Expand or collapsed status
        setExpandStatus(holder);
        // On Item Click
        setGroupClickListener(region, holder);
        // Disabled Whole group Highly unlikely
        noCityAvailable(region, holder);
        //Setup flag if present
        setFlags(region.getCountryCode(), holder);
        // if All sub locations are premium
        setPremiumStatus(region.getPremium(), holder);
        // Setup force expand
        holder.tvCountryName.setTag(R.string.force_expand, 1);
        // Show expand icon
        holder.imgDropDown.setVisibility(View.VISIBLE);
        int averageHealth = 0;
        int numberOfCities = 0;
        for (City city : cities) {
            if (city.getHealth() > 0) {
                numberOfCities++;
                averageHealth = averageHealth + city.getHealth();
            }
        }
        if (averageHealth > 0 && numberOfCities > 0) {
            averageHealth = averageHealth / numberOfCities;
        }
        int finalAverageHealth = averageHealth;
        holder.setItemExpandListener(new RegionViewHolder.ItemExpandListener() {
            @Override
            public void onItemExpand() {
                int scrollTo = holder.getAdapterPosition() + (Math.min(cities.size(), 5));
                mListener.setScrollTo(scrollTo);
            }
            @Override
            public void onItemCollapse() {
                setGroupHealth(finalAverageHealth, holder);
            }
        });
        setGroupHealth(averageHealth, holder);
        holder.imgP2pBadge.setVisibility(region.getP2p() == 0 ? View.VISIBLE : View.INVISIBLE);
        holder.imgP2pBadge.setOnClickListener(v -> Toast.makeText(v.getContext(), v.getContext().getString(R.string.file_sharing_frowned_upon), Toast.LENGTH_SHORT).show());
    }

    private boolean enabledNode(City city) {
        return (city.nodesAvailable() || (!serverListData.isProUser() && city.getPro() == 1));
    }

    private int getPingTime(City city) {
        for (PingTime pingTime : serverListData.getPingTimes()) {
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

    private boolean isFavourite(City city) {
        for (Favourite favourite : serverListData.getFavourites()) {
            if (favourite.getId() == city.getId()) {
                return true;
            }
        }
        return false;
    }

    private void noCityAvailable(Region region, RegionViewHolder holder) {
        if (serverListData.isProUser()
                && region.getStatus() == NetworkKeyConstants.SERVER_STATUS_TEMPORARILY_UNAVAILABLE) {
            Drawable drawable = ResourcesCompat
                    .getDrawable(holder.itemView.getResources(), R.drawable.construction_icon,
                            Windscribe.getAppContext().getTheme());
            holder.imgDropDown.setImageDrawable(drawable);
        }
    }

    private void setClickListeners(City city, CityViewHolder holder) {
        // Add and remove from favourite
        if (city.nodesAvailable()) {
            holder.imgFavorite.setOnClickListener(v -> {
                if (isFavourite(city)) {
                    mListener.removeFromFavourite(city.getId(), holder.getAdapterPosition(), this);
                } else {
                    mListener.addToFavourite(city.getId(), holder.getAdapterPosition(), this);
                }
            });
        }

        //On City Click
        holder.itemView.setOnClickListener(view -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            if (!city.nodesAvailable() && city.getPro() != 1) {
                mListener.onUnavailableRegion();
            } else if (!city.nodesAvailable() && city.getPro() == 1 && serverListData.isProUser()) {
                mListener.onUnavailableRegion();
            } else {
                mListener.onCityClick(city.getId());
            }
        });
    }

    private void setFavourites(City city, CityViewHolder holder) {
        // Reset alpha
        holder.nodeGroupName.setAlpha(1f);
        holder.imgSignalStrengthBar.setAlpha(1f);
        holder.tvSignalStrength.setAlpha(1f);

        //Setup Pro icon for Unavailable locations
        if (city.getPro() == 1 && !serverListData.isProUser()) {
            holder.imgFavorite.setImageResource(R.drawable.pro_loc_icon);
            holder.imgFavorite.setTag(2);
            holder.imgFavorite.setClickable(false);
        } else if (!enabledNode(city)) {
            //holder.nodeGroupName.setTextColor(holder.itemView.getResources().getColor(R.color.textColorWhite50));
            Drawable drawable = ResourcesCompat
                    .getDrawable(holder.itemView.getResources(), R.drawable.construction_icon,
                            Windscribe.getAppContext().getTheme());
            holder.imgFavorite.setImageDrawable(drawable);
            holder.nodeGroupName.setEnabled(false);
            holder.imgFavorite.setSelected(false);
            holder.nodeGroupName.setAlpha(0.5f);
            holder.imgSignalStrengthBar.setAlpha(0.5f);
            holder.tvSignalStrength.setAlpha(0.5f);
        } else if (isFavourite(city)) {
            holder.imgFavorite.setImageResource(R.drawable.modal_add_to_favs);
            holder.imgFavorite.setSelected(true);
            holder.imgFavorite.setClickable(true);
        } else {
            holder.imgFavorite.setImageResource(R.drawable.modal_add_to_favs);
            holder.imgFavorite.setSelected(false);
            holder.imgFavorite.setClickable(true);
        }
    }

    private void setFlags(String countryCode, RegionViewHolder holder) {
        Integer iconId = serverListData.getFlags().get(countryCode);
        if (iconId != null) {
            holder.imgCountryFlag.setImageResource(iconId);
        } else {
            holder.imgCountryFlag.setImageDrawable(null);
        }
    }

    private void setGroupHealth(final int health, final RegionViewHolder holder) {
        if (!isGroupExpanded(holder.getAdapterPosition()) && serverListData.isShowLocationHealthEnabled()
                && health > 0) {
            Context context = holder.itemView.getContext();
            int color = getServerHealthColor(health, context);
            holder.serverLoadBar.setIndicatorColor(color);
            holder.serverLoadBar.setProgress(health);
            holder.serverLoadBar.setVisibility(View.VISIBLE);
        } else {
            holder.serverLoadBar.setVisibility(View.GONE);
        }
    }

    private void setLinkSpeed(final City city, final CityViewHolder holder) {
        int visibility = "10000".equals(city.getLinkSpeed()) ? View.VISIBLE : View.INVISIBLE;
        holder.imgLinkSpeed.setVisibility(visibility);
    }

    private void setNameAndNickName(City city, CityViewHolder holder) {
        String sourceString = "<b>" + city.getNodeName() + "</b> " + city.getNickName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.nodeGroupName.setText(Html.fromHtml(sourceString, Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.nodeGroupName.setText(Html.fromHtml(sourceString));
        }
    }

    private void setPings(CityViewHolder holder, int pingResult) {
        if (serverListData.isShowLatencyInBar()) {
            holder.tvSignalStrength.setVisibility(View.GONE);
            holder.imgSignalStrengthBar.setVisibility(View.VISIBLE);
            if (pingResult != -1) {
                if (pingResult > -1 && pingResult < NetworkKeyConstants.PING_TEST_3_BAR_UPPER_LIMIT) {
                    holder.imgSignalStrengthBar.setImageResource(R.drawable.ic_network_ping_black_3_bar);
                } else if (pingResult >= NetworkKeyConstants.PING_TEST_3_BAR_UPPER_LIMIT
                        && pingResult < NetworkKeyConstants.PING_TEST_2_BAR_UPPER_LIMIT) {
                    holder.imgSignalStrengthBar.setImageResource(R.drawable.ic_network_ping_black_2_bar);
                } else if (pingResult >= NetworkKeyConstants.PING_TEST_2_BAR_UPPER_LIMIT
                        && pingResult < NetworkKeyConstants.PING_TEST_1_BAR_UPPER_LIMIT) {
                    holder.imgSignalStrengthBar.setImageResource(R.drawable.ic_network_ping_black_1_bar);
                } else {
                    holder.imgSignalStrengthBar.setImageResource(R.drawable.ic_network_ping_black_no_bar);
                }
            }
        } else {
            holder.tvSignalStrength.setVisibility(View.VISIBLE);
            holder.imgSignalStrengthBar.setVisibility(View.GONE);
            holder.tvSignalStrength.setText(pingResult != -1 ? String.valueOf(pingResult) : "--");
        }
    }

    private void setServerHealth(final City city, final CityViewHolder holder) {
        int health = city.getHealth();
        if (serverListData.isShowLocationHealthEnabled() && health > 0) {
            Context context = holder.itemView.getContext();
            int color = getServerHealthColor(health, context);
            holder.serverHealth.setIndicatorColor(color);
            holder.serverHealth.setProgress(health);
            holder.serverHealth.setVisibility(View.VISIBLE);
        } else {
            holder.serverHealth.setVisibility(View.GONE);
        }
    }

    private void setTextAndIconColors(CityViewHolder holder, int selectedColor) {
        holder.imgFavorite.setImageTintList(ColorStateList.valueOf(selectedColor));
        holder.imgSignalStrengthBar.setImageTintList(ColorStateList.valueOf(selectedColor));
        holder.imgLinkSpeed.setImageTintList(ColorStateList.valueOf(selectedColor));
        holder.nodeGroupName.setTextColor(ColorStateList.valueOf(selectedColor));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListener(CityViewHolder holder) {
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
