/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.ProtocolAdapter.ProtocolViewHolder
import com.windscribe.mobile.listeners.ProtocolClickListener
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.utils.ProtocolConfig

class ProtocolAdapter(
    private val listener: ProtocolClickListener
) : RecyclerView.Adapter<ProtocolViewHolder>() {
    var protocolConfigs: List<ProtocolConfig> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
        field = value
        notifyDataSetChanged()
    }
    class ProtocolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mProtocolTextView: TextView = itemView.findViewById(R.id.protocol)
        fun bind(protocolConfig: ProtocolConfig) {
            mProtocolTextView.text = protocolConfig.heading
            if (adapterPosition == 0) {
                mProtocolTextView.background = ResourcesCompat.getDrawable(
                    itemView.context.resources,
                    R.drawable.capsule_background_small, appContext.theme
                )
            } else {
                mProtocolTextView.background = null
            }
        }
    }

    override fun getItemCount(): Int {
        return protocolConfigs.size
    }

    override fun onBindViewHolder(holder: ProtocolViewHolder, position: Int) {
        val protocolConfig = protocolConfigs[holder.adapterPosition]
        holder.bind(protocolConfig)
        holder.itemView.setOnClickListener {
            holder.mProtocolTextView.background = ResourcesCompat.getDrawable(
                holder.itemView.context.resources,
                R.drawable.capsule_background_small,
                appContext.theme
            )
            listener.onProtocolSelected(protocolConfig)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProtocolViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.protocol_layout, parent, false)
        return ProtocolViewHolder(view)
    }
}