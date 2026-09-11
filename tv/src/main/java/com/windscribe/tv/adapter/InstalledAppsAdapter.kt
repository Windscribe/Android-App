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
import com.windscribe.vpn.cache.AppIconCache
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class InstalledAppsAdapter(
    mAppsList: List<InstalledAppsData>,
    installedAppListener: InstalledAppListener,
    private val appIconCache: AppIconCache,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private class InstalledAppsViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val imgAppLogo: ImageView = itemView.findViewById(R.id.banner)
        val imgCheck: ImageView = itemView.findViewById(R.id.check)
        val tvAppName: TextView = itemView.findViewById(R.id.app_name)
        var iconJob: Job? = null
    }

    interface InstalledAppListener {
        fun onInstalledAppClick(
            updatedModel: InstalledAppsData?,
            reloadAdapter: Boolean,
        )
    }

    private val scope = MainScope()

    /** Matches the `banner` view in installed_apps_viewholder.xml. */
    private val iconSizePx = (ICON_DP * appContext.resources.displayMetrics.density).toInt()

    private val specialPackages: MutableList<String> = ArrayList()
    private val copyAppsList: List<InstalledAppsData>
    private val installedAppListener: InstalledAppListener
    private var mAppsList: List<InstalledAppsData>?

    override fun getItemCount(): Int = mAppsList?.size ?: 0

    override fun onBindViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        i: Int,
    ) {
        val listViewHolder = viewHolder as InstalledAppsViewHolder
        val installedAppsData = mAppsList!![viewHolder.getAdapterPosition()]
        bindIcon(listViewHolder, installedAppsData.packageName)

        listViewHolder.tvAppName.text = installedAppsData.appName
        if (installedAppsData.isChecked) {
            listViewHolder.itemView.background =
                ResourcesCompat
                    .getDrawable(
                        listViewHolder.itemView.context.resources,
                        R.drawable.apps_background_selected,
                        appContext.theme,
                    )
            listViewHolder.imgCheck.visibility = View.VISIBLE
        } else {
            listViewHolder.itemView.background =
                ResourcesCompat
                    .getDrawable(
                        listViewHolder.itemView.context.resources,
                        R.drawable.apps_background,
                        appContext.theme,
                    )
            listViewHolder.imgCheck.visibility = View.INVISIBLE
        }
        listViewHolder.itemView.setOnClickListener {
            installedAppsData.isChecked = !installedAppsData.isChecked
            notifyItemChanged(listViewHolder.adapterPosition)
            installedAppListener.onInstalledAppClick(installedAppsData, false)
        }
    }

    /** Decoded off the main thread, and abandoned if the row is reused before it arrives. */
    private fun bindIcon(
        holder: InstalledAppsViewHolder,
        packageName: String,
    ) {
        holder.iconJob?.cancel()
        val cached = appIconCache.cached(packageName, iconSizePx)
        if (cached != null) {
            holder.imgAppLogo.setImageBitmap(cached)
            return
        }
        holder.imgAppLogo.setImageResource(android.R.drawable.sym_def_app_icon)
        holder.iconJob =
            scope.launch {
                appIconCache.icon(packageName, iconSizePx)?.let { holder.imgAppLogo.setImageBitmap(it) }
            }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? InstalledAppsViewHolder)?.iconJob?.cancel()
        super.onViewRecycled(holder)
    }

    /** Children only: the adapter is usable again if it is re-attached. */
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        scope.coroutineContext.cancelChildren()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        i: Int,
    ): RecyclerView.ViewHolder {
        val itemView =
            LayoutInflater
                .from(viewGroup.context)
                .inflate(R.layout.installed_apps_viewholder, viewGroup, false)
        return InstalledAppsViewHolder(itemView)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setFilterType(showSystemApps: Boolean) {
        mAppsList =
            if (showSystemApps) {
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
    fun updateApp(
        name: String,
        check: Boolean,
    ) {
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

    companion object {
        private const val ICON_DP = 60
    }
}
