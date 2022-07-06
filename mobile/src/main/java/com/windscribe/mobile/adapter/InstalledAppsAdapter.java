/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.api.response.InstalledAppsData;

import java.util.ArrayList;
import java.util.List;

public class InstalledAppsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static class InstalledAppsViewHolder extends RecyclerView.ViewHolder {

        final ImageView imgAppLogo;

        final ImageView imgCheck;

        final TextView tvAppName;

        public InstalledAppsViewHolder(View itemView) {
            super(itemView);
            tvAppName = itemView.findViewById(R.id.app_name);
            imgAppLogo = itemView.findViewById(R.id.app_logo);
            imgCheck = itemView.findViewById(R.id.img_check);
        }
    }

    public interface InstalledAppListener {

        void onInstalledAppClick(InstalledAppsData updatedModel, boolean reloadAdapter);
    }

    private final List<InstalledAppsData> copyItems = new ArrayList<>();

    private final InstalledAppListener installedAppListener;

    private final List<InstalledAppsData> mAppsList;

    public InstalledAppsAdapter(List<InstalledAppsData> mAppsList, InstalledAppListener installedAppListener) {
        this.mAppsList = mAppsList;
        this.installedAppListener = installedAppListener;
        copyItems.addAll(mAppsList);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String text) {
        mAppsList.clear();
        if (text.isEmpty()) {
            mAppsList.addAll(copyItems);
        } else {
            String query = text.toLowerCase();
            for (InstalledAppsData app : copyItems) {
                if (app.getAppName().toLowerCase().contains(query)) {
                    mAppsList.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mAppsList != null ? mAppsList.size() : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        InstalledAppsViewHolder listViewHolder = (InstalledAppsViewHolder) viewHolder;
        InstalledAppsData installedAppsData = mAppsList.get(viewHolder.getAdapterPosition());
        listViewHolder.imgAppLogo.setImageDrawable(installedAppsData.getAppIconDrawable());
        listViewHolder.tvAppName.setText(installedAppsData.getAppName());
        Resources resources = viewHolder.itemView.getResources();
        if (installedAppsData.isChecked()) {
            listViewHolder.imgCheck.setImageDrawable(ResourcesCompat
                    .getDrawable(resources, R.drawable.ic_checkmark_on, Windscribe.getAppContext().getTheme()));
        } else {
            listViewHolder.imgCheck.setImageDrawable(ResourcesCompat
                    .getDrawable(resources, R.drawable.ic_checkmark_off, Windscribe.getAppContext().getTheme()));
        }

        listViewHolder.itemView.setOnClickListener(v -> {
            installedAppsData.setChecked(!installedAppsData.isChecked());
            notifyItemChanged(listViewHolder.getAdapterPosition());
            installedAppListener.onInstalledAppClick(installedAppsData, false);
        });
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.installed_apps_viewholder, viewGroup, false);
        return new InstalledAppsViewHolder(itemView);
    }
}
