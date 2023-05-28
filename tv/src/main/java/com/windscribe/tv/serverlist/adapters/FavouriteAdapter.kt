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
import com.windscribe.tv.serverlist.listeners.NodeClickListener
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.ServerListData

class FavouriteAdapter(
    private val locations: MutableList<City>,
    serverListData: ServerListData,
    private val listener: NodeClickListener
) : RecyclerView.Adapter<FavouriteHolder>() {
    inner class FavouriteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnConnect: ConnectButtonView = itemView.findViewById(R.id.connect)
        private val btnFav: FavouriteButtonView = itemView.findViewById(R.id.fav)
        private val detailStar: ImageView = itemView.findViewById(R.id.pro_label)
        private val highlightTextView: TextView = itemView.findViewById(R.id.highlightedText)
        private val latencyView: TextView = itemView.findViewById(R.id.latency)
        private val nodeNameLabel: TextView = itemView.findViewById(R.id.nodeName)
        private val nodeNickNameLabel: TextView = itemView.findViewById(R.id.nodeNickName)
        fun bind(city: City) {
            btnFav.setState(2)
            nodeNameLabel.text = city.nodeName
            nodeNickNameLabel.text = city.nickName
            val pingTime = getPingTime(city)
            if (pingTime == -1) {
                latencyView.text = ""
            } else {
                latencyView.text = latencyView.resources.getString(R.string.ping_time, pingTime)
            }
            if (isPremiumUser) {
                btnFav.visibility = View.VISIBLE
                detailStar.visibility = View.INVISIBLE
            } else {
                if (city.pro == 1) {
                    detailStar.visibility = View.VISIBLE
                } else {
                    detailStar.visibility = View.INVISIBLE
                }
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
                    if (!city.nodesAvailable() && serverListData.isProUser) {
                        listener.onDisabledClick()
                    } else {
                        listener.onFavouriteNodeCLick(city)
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
                        if (!serverListData.isProUser && city.pro == 1) {
                            setHighlightText(appContext.getString(R.string.upgrade), hasFocus)
                        } else if (!city.nodesAvailable()) {
                            setHighlightText(appContext.getString(R.string.unavailable), hasFocus)
                        } else {
                            setHighlightText(appContext.getString(R.string.connect), hasFocus)
                        }
                    }
                btnFav.onFocusChangeListener =
                    View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                        selectedBackground(hasFocus)
                        setHighlightText(
                            appContext.getString(R.string.remove_it_from_favourites),
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
        return locations[position].getId().toLong()
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

    private fun getPingTime(city: City): Int {
        for (pingTime in serverListData.pingTimes) {
            if (city.getId() == pingTime.ping_id) {
                return pingTime.getPingTime()
            }
        }
        return -1
    }

    init {
        setHasStableIds(true)
        this.serverListData = serverListData
    }
}