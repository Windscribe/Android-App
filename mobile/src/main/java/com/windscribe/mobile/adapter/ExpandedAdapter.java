/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.windscribe.vpn.serverlist.entity.ServerListData;
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener;

import java.util.List;

public class ExpandedAdapter extends SearchRegionsAdapter {

    public ExpandedAdapter(List<? extends ExpandableGroup> groups, ServerListData serverListData,
            ListViewClickListener mListener) {
        super(groups, serverListData, mListener);
    }


}