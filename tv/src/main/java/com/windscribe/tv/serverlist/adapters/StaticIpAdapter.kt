/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.adapters

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.tv.R
import com.windscribe.tv.serverlist.adapters.StaticIpAdapter.StaticHolder
import com.windscribe.tv.serverlist.customviews.ConnectButtonView
import com.windscribe.tv.serverlist.listeners.NodeClickListener
import com.windscribe.vpn.serverlist.entity.ServerListData
import com.windscribe.vpn.serverlist.entity.StaticRegion

class StaticIpAdapter(
    private val locations: List<StaticRegion>,
    dataDetails: ServerListData,
    private val listener: NodeClickListener
) : RecyclerView.Adapter<StaticHolder>() {
    inner class StaticHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnConnect: ConnectButtonView = itemView.findViewById(R.id.connect)
        private val detailStar: ImageView = itemView.findViewById(R.id.pro_label)
        private val extraView: TextView = itemView.findViewById(R.id.extra)
        private val highlightTextView: TextView = itemView.findViewById(R.id.highlightedText)
        private val latencyView: TextView = itemView.findViewById(R.id.latency)
        private val nodeNameLabel: TextView = itemView.findViewById(R.id.nodeName)
        private val nodeNickNameLabel: TextView = itemView.findViewById(R.id.nodeNickName)
        fun bind(region: StaticRegion) {
            nodeNameLabel.text = region.cityName
            nodeNickNameLabel.text = region.countryCode
            val pingTime = getPingTime(region.id)
            if (pingTime == -1) {
                latencyView.text = ""
            } else {
                latencyView.text =
                    latencyView.resources.getString(R.string.ping_time, pingTime)
            }
            extraView.text = region.staticIp
            btnConnect.setColorFilter(
                ContextCompat.getColor(itemView.context, R.color.colorWhite40),
                PorterDuff.Mode.MULTIPLY
            )
            btnConnect.setOnClickListener { listener.onStaticIpClick(region) }
            itemView.onFocusChangeListener =
                View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                    selectedBackground(hasFocus)
                    setHighlightText(hasFocus)
                }
            btnConnect.onFocusChangeListener =
                View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                    selectedBackground(hasFocus)
                    setHighlightText(hasFocus)
                }
        }

        private fun selectedBackground(selected: Boolean) {
            nodeNameLabel.alpha = if (selected) 1.0f else 0.40f
            nodeNickNameLabel.alpha = if (selected) 1.0f else 0.40f
            detailStar.alpha = if (selected) 1.0f else 0.40f
            latencyView.alpha = if (selected) 1.0f else 0.40f
            extraView.alpha = if (selected) 1.0f else 0.40f
            if (selected) {
                btnConnect.clearColorFilter()
            } else {
                btnConnect.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.colorWhite40),
                    PorterDuff.Mode.MULTIPLY
                )
            }
        }

        private fun setHighlightText(hasFocus: Boolean) {
            if (hasFocus) {
                highlightTextView.text = highlightTextView.resources.getString(R.string.connect)
                highlightTextView.visibility = View.VISIBLE
            } else {
                highlightTextView.text =
                    highlightTextView.resources.getString(R.string.connect)
                highlightTextView.visibility = View.INVISIBLE
            }
        }
    }

    private val dataDetails: ServerListData
    override fun getItemCount(): Int {
        return locations.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(staticHolder: StaticHolder, i: Int) {
        val region = locations[i]
        staticHolder.bind(region)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): StaticHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.static_item_view, viewGroup, false)
        return StaticHolder(view)
    }

    private fun getPingTime(id: Int): Int {
        for (pingTime in dataDetails.pingTimes) {
            if (id == pingTime.ping_id) {
                return pingTime.getPingTime()
            }
        }
        return -1
    }

    init {
        setHasStableIds(true)
        this.dataDetails = dataDetails
    }
}