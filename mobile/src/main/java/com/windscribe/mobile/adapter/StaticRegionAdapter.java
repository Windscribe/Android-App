/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;
import com.windscribe.mobile.holder.StaticRegionHolder;
import com.windscribe.vpn.commonutils.ThemeUtils;
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.serverlist.entity.ServerListData;
import com.windscribe.vpn.serverlist.entity.PingTime;
import com.windscribe.vpn.serverlist.entity.StaticRegion;
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener;

import java.util.List;


public class StaticRegionAdapter extends RecyclerView.Adapter<StaticRegionHolder> {

    private ServerListData dataDetails;

    private final ListViewClickListener mListener;

    private List<StaticRegion> mStaticIpList;


    public StaticRegionAdapter(List<StaticRegion> mStaticIpList, ServerListData dataDetails,
            ListViewClickListener mListener) {
        this.mStaticIpList = mStaticIpList;
        this.dataDetails = dataDetails;
        this.mListener = mListener;
    }

    @Override
    public int getItemCount() {
        return mStaticIpList != null ? mStaticIpList.size() : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull StaticRegionHolder staticRegionHolder, int i) {
        //Setup icon
        if (NetworkKeyConstants.STATIC_IP_TYPE_DATA_CENTER
                .equals(mStaticIpList.get(staticRegionHolder.getAdapterPosition()).getType())) {
            staticRegionHolder.mImageIpType.setImageResource(R.drawable.ic_datacenter_ip_icon);
        } else {
            staticRegionHolder.mImageIpType.setImageResource(R.drawable.ic_residential_ip_icon);
        }

        staticRegionHolder.mIpCityName
                .setText(mStaticIpList.get(staticRegionHolder.getAdapterPosition()).getCityName());
        staticRegionHolder.mStaticIp
                .setText(mStaticIpList.get(staticRegionHolder.getAdapterPosition()).getStaticIp());

        int pingResult = getPingTime(mStaticIpList.get(staticRegionHolder.getAdapterPosition()).getId());
        if (dataDetails.isShowLatencyInBar()) {
            staticRegionHolder.mTextViewPing.setVisibility(View.GONE);
            staticRegionHolder.mImgSignalStrengthBar.setVisibility(View.VISIBLE);
            if (pingResult != -1) {
                if (pingResult > -1 && pingResult < NetworkKeyConstants.PING_TEST_3_BAR_UPPER_LIMIT) {
                    staticRegionHolder.mImgSignalStrengthBar.setImageResource(R.drawable.ic_network_ping_black_3_bar);
                } else if (pingResult >= NetworkKeyConstants.PING_TEST_3_BAR_UPPER_LIMIT
                        && pingResult < NetworkKeyConstants.PING_TEST_2_BAR_UPPER_LIMIT) {
                    staticRegionHolder.mImgSignalStrengthBar.setImageResource(R.drawable.ic_network_ping_black_2_bar);

                } else if (pingResult >= NetworkKeyConstants.PING_TEST_2_BAR_UPPER_LIMIT
                        && pingResult < NetworkKeyConstants.PING_TEST_1_BAR_UPPER_LIMIT) {

                    staticRegionHolder.mImgSignalStrengthBar.setImageResource(R.drawable.ic_network_ping_black_1_bar);

                } else {
                    staticRegionHolder.mImgSignalStrengthBar
                            .setImageResource(R.drawable.ic_network_ping_black_no_bar);
                }
            }
        } else {
            staticRegionHolder.mTextViewPing.setVisibility(View.VISIBLE);
            staticRegionHolder.mImgSignalStrengthBar.setVisibility(View.GONE);
            if (pingResult != -1) {
                staticRegionHolder.mTextViewPing.setText(String.valueOf(pingResult));
            } else {
                staticRegionHolder.mTextViewPing.setText("--");
            }
        }

        staticRegionHolder.itemView.setOnClickListener(
                v -> mListener.onStaticIpClick(mStaticIpList.get(staticRegionHolder.getAdapterPosition()).getId()));
        setTouchListener(staticRegionHolder);
    }

    @NonNull
    @Override
    public StaticRegionHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.static_ip_list_view_holder, viewGroup, false);
        return new StaticRegionHolder(view);
    }

    public void setDataDetails(ServerListData dataDetails) {
        this.dataDetails = dataDetails;
    }

    public void setStaticIpList(List<StaticRegion> mStaticIpList) {
        this.mStaticIpList = mStaticIpList;
    }

    private int getPingTime(int id) {
        for (PingTime pingTime : dataDetails.getPingTimes()) {
            if (id == pingTime.ping_id) {
                return pingTime.getPingTime();
            }
        }
        return -1;
    }

    private void setTextAndIconColors(StaticRegionHolder holder, int selectedColor) {
        holder.mImgSignalStrengthBar.setImageTintList(ColorStateList.valueOf(selectedColor));
        holder.mImageIpType.setImageTintList(ColorStateList.valueOf(selectedColor));
        holder.mIpCityName.setTextColor(ColorStateList.valueOf(selectedColor));
        holder.mStaticIp.setTextColor(ColorStateList.valueOf(selectedColor));
        holder.mTextViewPing.setTextColor(ColorStateList.valueOf(selectedColor));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListener(StaticRegionHolder holder) {
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