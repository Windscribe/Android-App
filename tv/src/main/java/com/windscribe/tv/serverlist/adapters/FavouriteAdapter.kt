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
import com.windscribe.tv.serverlist.adapters.FavouriteAdapter.FavouriteHolder
import com.windscribe.tv.serverlist.customviews.ConnectButtonView
import com.windscribe.tv.serverlist.customviews.FavouriteButtonView
import com.windscribe.tv.serverlist.listeners.DatacenterClickListener
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.DatacenterStatus
import com.windscribe.vpn.serverlist.entity.DatacenterStatusHelper
import com.windscribe.vpn.serverlist.entity.ServerListData

class FavouriteAdapter(
    private val locations: MutableList<Datacenter>,
    serverListData: ServerListData,
    private val listener: DatacenterClickListener
) : RecyclerView.Adapter<FavouriteHolder>() {
    inner class FavouriteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnConnect: ConnectButtonView = itemView.findViewById(R.id.connect)
        private val btnFav: FavouriteButtonView = itemView.findViewById(R.id.fav)
        private val detailStar: ImageView = itemView.findViewById(R.id.pro_label)
        private val highlightTextView: TextView = itemView.findViewById(R.id.highlightedText)
        private val latencyView: TextView = itemView.findViewById(R.id.latency)
        private val nodeNameLabel: TextView = itemView.findViewById(R.id.nodeName)
        private val nodeNickNameLabel: TextView = itemView.findViewById(R.id.nodeNickName)
        fun bind(city: Datacenter) {
            btnFav.setState(2)
            nodeNameLabel.text = city.nodeName
            nodeNickNameLabel.text = city.nickName
            val pingTime = getPingTime(city)
            if (pingTime == -1) {
                latencyView.text = ""
            } else {
                latencyView.text = latencyView.resources.getString(com.windscribe.vpn.R.string.ping_time, pingTime)
            }
            // Determine datacenter status
            val serverCount = serverListData.serverCountMap[city.id] ?: 0
            val status = DatacenterStatusHelper.getStatus(city, serverCount, isPremiumUser)
            val requiresPro = DatacenterStatusHelper.requiresPro(city, serverCount, isPremiumUser)

            // Show pro icon if user is not premium and datacenter requires Pro
            if (isPremiumUser) {
                btnFav.visibility = View.VISIBLE
                detailStar.visibility = View.INVISIBLE
            } else {
                detailStar.visibility = if (requiresPro) View.VISIBLE else View.INVISIBLE
            }

                btnFav.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.colorWhite40),
                    PorterDuff.Mode.MULTIPLY
                )
                btnConnect.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.colorWhite40),
                    PorterDuff.Mode.MULTIPLY
                )
                btnConnect.setOnClickListener {
                    when (status) {
                        DatacenterStatus.UnderMaintenance -> {
                            listener.onDisabledClick()
                        }
                        DatacenterStatus.Pro, DatacenterStatus.Available -> {
                            // Pro locations go through onConnectClick → attemptConnection → upgrade logic
                            listener.onFavouriteDatacenterClick(city)
                        }
                    }
                }
                btnFav.setOnClickListener {
                    locations.remove(city)
                    listener.onFavouriteButtonClick(city, btnFav.getState())
                }
                itemView.onFocusChangeListener =
                    View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                        selectedBackground(hasFocus)
                    }
                itemView.onFocusChangeListener =
                    View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                        selectedBackground(hasFocus)
                    }
                btnConnect.onFocusChangeListener =
                    View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                        selectedBackground(hasFocus)
                        val text = when (status) {
                            DatacenterStatus.Pro -> appContext.getString(com.windscribe.vpn.R.string.upgrade)
                            DatacenterStatus.UnderMaintenance -> appContext.getString(com.windscribe.vpn.R.string.unavailable)
                            DatacenterStatus.Available -> appContext.getString(com.windscribe.vpn.R.string.connect)
                        }
                        setHighlightText(text, hasFocus)
                    }
                btnFav.onFocusChangeListener =
                    View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                        selectedBackground(hasFocus)
                        setHighlightText(
                            appContext.getString(com.windscribe.vpn.R.string.remove_it_from_favourites),
                            hasFocus
                        )
                    }
        }

        private fun selectedBackground(selected: Boolean) {
            nodeNameLabel.alpha = if (selected) 1.0f else 0.40f
            nodeNickNameLabel.alpha = if (selected) 1.0f else 0.40f
            detailStar.alpha = if (selected) 1.0f else 0.40f
            latencyView.alpha = if (selected) 1.0f else 0.40f
            if (selected) {
                btnConnect.clearColorFilter()
                btnFav.clearColorFilter()
            } else {
                btnFav.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.colorWhite40),
                    PorterDuff.Mode.MULTIPLY
                )
                btnConnect.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.colorWhite40),
                    PorterDuff.Mode.MULTIPLY
                )
            }
        }

        private fun setHighlightText(text: String, hasFocus: Boolean) {
            if (hasFocus) {
                highlightTextView.text = text
                highlightTextView.visibility = View.VISIBLE
            } else {
                highlightTextView.text = text
                highlightTextView.visibility = View.INVISIBLE
            }
        }
    }

    private val serverListData: ServerListData
    private var isPremiumUser = false
    override fun getItemCount(): Int {
        return locations.size
    }

    override fun getItemId(position: Int): Long {
        return locations[position].id.toLong()
    }

    override fun onBindViewHolder(favouriteHolder: FavouriteHolder, i: Int) {
        val city = locations[i]
        favouriteHolder.bind(city)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): FavouriteHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.favourite_item_view, viewGroup, false)
        return FavouriteHolder(view)
    }

    fun setPremiumUser(isPremiumUser: Boolean) {
        this.isPremiumUser = isPremiumUser
    }

    private fun getPingTime(city: Datacenter): Int {
        for (pingTime in serverListData.pingTimes) {
            if (city.id == pingTime.ping_id) {
                return pingTime.pingTime
            }
        }
        return -1
    }

    private fun hasServersForCity(city: Datacenter): Boolean {
        // Check if there are servers available for this city (datacenter)
        val serverCount = serverListData.serverCountMap[city.id] ?: 0
        return serverCount > 0
    }

    init {
        setHasStableIds(true)
        this.serverListData = serverListData
    }
}