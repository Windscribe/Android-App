/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.windscribe.vpn.serverlist.entity.ServerListData;
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener;

import java.util.List;

public class SearchRegionsAdapter extends RegionsAdapter {


    public SearchRegionsAdapter(List<? extends ExpandableGroup> groups, ServerListData serverListData,
            ListViewClickListener mListener) {
        super(groups, serverListData, mListener);
    }

}
