/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.RobertSettingsAdapter.RobertSettingsViewHolder
import com.windscribe.vpn.commonutils.ThemeUtils

data class RobertSetting(var filter: String, var enabled: Boolean)

interface RobertAdapterListener {

    fun settingChanged(originalList: List<RobertSetting>, data: List<String>, position: Int)
}

class RobertSettingsAdapter(private val robertAdapterListener: RobertAdapterListener) :
    RecyclerView.Adapter<RobertSettingsViewHolder>() {

    var data: List<RobertSetting> = mutableListOf()
    var settingUpdateInProgress = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RobertSettingsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.robert_setting_item_view, parent, false)
        return RobertSettingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: RobertSettingsViewHolder, position: Int) {
        holder.toggle.setOnClickListener {
            if (settingUpdateInProgress) return@setOnClickListener
            val originalList = ArrayList(data.map { it.copy() })
            data[holder.adapterPosition].enabled = !data[holder.adapterPosition].enabled
            val selected = data.filter {
                it.enabled
            }.map {
                it.filter
            }.toList()
            settingUpdateInProgress = true
            robertAdapterListener.settingChanged(originalList, selected, holder.adapterPosition)
            notifyItemChanged(holder.adapterPosition)
        }
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class RobertSettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var iconMap = mapOf(
            Pair("malware", R.drawable.ic_malware),
            Pair("ads", R.drawable.ic_ads),
            Pair("social", R.drawable.ic_social),
            Pair("porn", R.drawable.ic_porn),
            Pair("gambling", R.drawable.ic_gambling),
            Pair("fakenews", R.drawable.ic_fake_news),
            Pair("competitors", R.drawable.ic_other_vpn),
            Pair("cryptominers", R.drawable.ic_crypto)
        )
        var labelMap = mapOf(
            Pair("malware", "Malware"),
            Pair("ads", "Ads + Trackers"),
            Pair("social", "Social Networks"),
            Pair("porn", "Porn"),
            Pair("gambling", "Gambling"),
            Pair("fakenews", "Fake News + Clickbait"),
            Pair("competitors", "Other VPNs"),
            Pair("cryptominers", "Cryptominers")
        )

        var toggle: ImageView = itemView.findViewById(R.id.toggle)
        var icon: ImageView = itemView.findViewById(R.id.icon)
        var filter: TextView = itemView.findViewById(R.id.filter)
        var allow: TextView = itemView.findViewById(R.id.allow)
        fun bind(robertSetting: RobertSetting) {
            if (robertSetting.enabled) {
                toggle.setImageResource(R.drawable.ic_toggle_button_on)
                allow.setText(R.string.blocking)
                allow.setTextColor(itemView.context.resources.getColor(R.color.colorNeonGreen))
            } else {
                toggle.setImageResource(R.drawable.ic_toggle_button_off)
                allow.setText(R.string.allowing)
                allow.setTextColor(
                    ThemeUtils.getColor(
                        itemView.context,
                        R.attr.wdSecondaryColor,
                        R.color.colorWhite50
                    )
                )
            }
            iconMap[robertSetting.filter]?.let {
                icon.setImageResource(it)
            }
            labelMap[robertSetting.filter]?.let {
                filter.text = it
            } ?: run {
                filter.text = robertSetting.filter.replaceFirstChar { it.uppercase() }
            }
        }
    }
}
