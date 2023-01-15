/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;
import com.windscribe.mobile.networksecurity.viewholder.NetworkAdapterActionListener;
import com.windscribe.mobile.networksecurity.viewholder.NetworkListViewHolder;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.localdatabase.tables.NetworkInfo;

import java.util.List;

public class NetworkListAdapter extends RecyclerView.Adapter<NetworkListViewHolder> {

    private NetworkAdapterActionListener mAdapterActionListener;

    private final List<NetworkInfo> mNetList;


    public NetworkListAdapter(List<NetworkInfo> mNetList) {
        this.mNetList = mNetList;
    }

    @Override
    public int getItemCount() {
        return mNetList != null ? mNetList.size() : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull final NetworkListViewHolder holder, int position) {
        NetworkInfo networkInfo = mNetList.get(position);
        holder.tvNetworkName.setText(networkInfo.getNetworkName());
        String protectionStatus = networkInfo.isAutoSecureOn() ? Windscribe.getAppContext()
                .getText(R.string.network_secured).toString()
                : Windscribe.getAppContext().getText(R.string.network_unsecured).toString();
        holder.tvProtection.setText(protectionStatus);

        holder.itemView.setOnClickListener(v -> mAdapterActionListener.onItemSelected(networkInfo));
        if(position == getItemCount()-1){
            holder.dividerView.setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public NetworkListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.network_list_card, parent, false);
        return new NetworkListViewHolder(itemView);
    }

    public void setAdapterActionListener(NetworkAdapterActionListener mListener) {
        this.mAdapterActionListener = mListener;
    }

}
