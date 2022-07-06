/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.tv.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import java.util.ArrayList

class InstalledAppsAdapter(
    mAppsList: List<InstalledAppsData>,
    installedAppListener: InstalledAppListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private class InstalledAppsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAppLogo: ImageView = itemView.findViewById(R.id.banner)
        val imgCheck: ImageView = itemView.findViewById(R.id.check)
        val tvAppName: TextView = itemView.findViewById(R.id.app_name)
    }

    interface InstalledAppListener {
        fun onInstalledAppClick(updatedModel: InstalledAppsData?, reloadAdapter: Boolean)
    }

    private val specialPackages: MutableList<String> = ArrayList()
    private val copyAppsList: List<InstalledAppsData>
    private val installedAppListener: InstalledAppListener
    private var mAppsList: List<InstalledAppsData>?
    override fun getItemCount(): Int {
        return if (mAppsList != null) mAppsList!!.size else 0
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        val listViewHolder = viewHolder as InstalledAppsViewHolder
        val installedAppsData = mAppsList!![viewHolder.getAdapterPosition()]
        listViewHolder.imgAppLogo.setImageDrawable(installedAppsData.appIconDrawable)
        listViewHolder.tvAppName.text = installedAppsData.appName
        if (installedAppsData.isChecked) {
            listViewHolder.itemView.background = ResourcesCompat
                .getDrawable(
                    listViewHolder.itemView.context.resources,
                    R.drawable.apps_background_selected, appContext.theme
                )
            listViewHolder.imgCheck.visibility = View.VISIBLE
        } else {
            listViewHolder.itemView.background = ResourcesCompat
                .getDrawable(
                    listViewHolder.itemView.context.resources, R.drawable.apps_background,
                    appContext.theme
                )
            listViewHolder.imgCheck.visibility = View.INVISIBLE
        }
        listViewHolder.itemView.setOnClickListener {
            installedAppsData.isChecked = !installedAppsData.isChecked
            notifyItemChanged(listViewHolder.adapterPosition)
            installedAppListener.onInstalledAppClick(installedAppsData, false)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        val itemView = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.installed_apps_viewholder, viewGroup, false)
        return InstalledAppsViewHolder(itemView)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setFilterType(showSystemApps: Boolean) {
        mAppsList = if (showSystemApps) {
            copyAppsList
        } else {
            val filteredList: MutableList<InstalledAppsData> = ArrayList()
            for (installedAppsData in copyAppsList) {
                if (!installedAppsData.isSystemApp) {
                    filteredList.add(installedAppsData)
                }
            }
            filteredList
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateApp(name: String, check: Boolean) {
        for (app in mAppsList!!) {
            if (app.packageName == name) {
                app.isChecked = check
                notifyDataSetChanged()
            }
        }
    }

    init {
        this.mAppsList = mAppsList
        this.installedAppListener = installedAppListener
        copyAppsList = mAppsList
        specialPackages.add("Netflix")
        specialPackages.add("Prime Video")
        specialPackages.add("Chrome")
    }
}
