/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.windscribe.mobile.holder.RegionViewHolder;
import com.windscribe.vpn.serverlist.entity.ServerListData;
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener;

import java.util.List;

public class StreamingNodeAdapter extends RegionsAdapter {

    public StreamingNodeAdapter(List<? extends ExpandableGroup> groups, ServerListData serverListData,
            ListViewClickListener mListener) {
        super(groups, serverListData, mListener);
    }

    @Override
    public void setPremiumStatus(int isPro, RegionViewHolder holder) {
        holder.imgProBadge.setImageDrawable(null);
    }
}
