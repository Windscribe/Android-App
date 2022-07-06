/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.adapter

import android.annotation.SuppressLint
import android.os.Build
import android.text.Html
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.tv.R
import com.windscribe.tv.serverlist.customviews.PreferenceHeaderItemMain
import com.windscribe.tv.serverlist.customviews.State
import com.windscribe.vpn.localdatabase.tables.WindNotification

class NewsFeedAdapter(private val mNotificationList: List<WindNotification>?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private inner class NewsFeedViewHolder @SuppressLint("NotifyDataSetChanged") constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: PreferenceHeaderItemMain = itemView.findViewById(R.id.newsFeedItem)
        private var windNotification: WindNotification? = null
        fun bind(windNotification: WindNotification) {
            this.windNotification = windNotification
            tvTitle.text = windNotification.notificationTitle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tvTitle.text = Html.fromHtml(
                    windNotification.notificationTitle,
                    Html.FROM_HTML_MODE_LEGACY
                )
            } else {
                tvTitle.text = Html.fromHtml(windNotification.notificationTitle)
            }
            val state = stateArray[windNotification.notificationId, -1]
            if (state == 1) {
                stateArray.clear()
                tvTitle.setState(State.TwoState.SELECTED)
                tvTitle.requestFocus()
                listener?.onNewsFeedItemClick(windNotification)
            } else {
                tvTitle.setState(State.TwoState.NOT_SELECTED)
            }
        }

        init {
            tvTitle.setOnClickListener {
                stateArray.clear()
                stateArray.put(windNotification!!.notificationId, 1)
                notifyDataSetChanged()
            }
        }
    }

    interface NewsFeedListener {
        fun onNewsFeedItemClick(windNotification: WindNotification)
    }

    private var listener: NewsFeedListener? = null
    private val stateArray = SparseIntArray()
    override fun getItemCount(): Int {
        return mNotificationList?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val windNotification = mNotificationList?.get(position)
        if (windNotification != null) {
            (holder as NewsFeedViewHolder).bind(windNotification)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return NewsFeedViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.news_feed_view, parent, false)
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItemSelected(notificationId: Int) {
        if (mNotificationList?.isEmpty() == true) {
            return
        }
        stateArray.clear()
        stateArray.put(notificationId, 1)
        notifyDataSetChanged()
    }

    fun setListener(listener: NewsFeedListener?) {
        this.listener = listener
    }
}
