/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.adapters

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.windscribe.tv.R
import com.windscribe.tv.serverlist.listeners.NodeClickListener
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.constants.AnimConstants
import com.windscribe.vpn.serverlist.entity.RegionAndCities
import com.windscribe.vpn.serverlist.entity.ServerListData

class ServerAdapter(
    private val groups: List<RegionAndCities>,
    private val serverListData: ServerListData,
    private val listener: NodeClickListener?,
    private val isTypeStreaming: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    internal inner class BestLocationHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.label)
        fun bind() {
            textView.text = itemView.resources.getText(R.string.best_location)
            itemView.setOnClickListener {
                serverListData.bestLocation?.let {
                    listener?.onBestLocationClick(it.city.getId())
                }
            }
            itemView.onFocusChangeListener =
                View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                    val valueAnimator: ValueAnimator
                    if (hasFocus) {
                        valueAnimator = ValueAnimator.ofFloat(0.4f, 1.0f)
                            .setDuration(AnimConstants.FLAG_ITEM_ANIM_DURATION)
                        valueAnimator.addUpdateListener { animation: ValueAnimator ->
                            itemView.alpha = (animation.animatedValue as Float)
                        }
                    } else {
                        valueAnimator = ValueAnimator.ofFloat(1.0f, 0.4f)
                            .setDuration(AnimConstants.FLAG_ITEM_ANIM_DURATION)
                        valueAnimator.addUpdateListener { animation: ValueAnimator ->
                            itemView.alpha = (animation.animatedValue as Float)
                        }
                    }
                    valueAnimator.start()
                }
        }
    }

    internal inner class ServerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageBackground: ImageView = itemView.findViewById(R.id.imageBackground)
        private val imageView: AppCompatImageView = itemView.findViewById(R.id.image)
        private val textView: TextView = itemView.findViewById(R.id.label)
        private var group: RegionAndCities? = null
        fun bind(group: RegionAndCities) {
            this.group = group
            textView.text = group.region.name
            imageBackground.visibility = View.VISIBLE
            val countryCode = group.region.countryCode
            Glide.with(itemView).load(FlagIconResource.getFlag(countryCode)).into(imageView)
        }

        private fun locationBackground(focus: Boolean, imageView: ImageView) {
            val valueAnimator: ValueAnimator
            if (focus) {
                valueAnimator = ValueAnimator.ofFloat(0.4f, 1.0f)
                    .setDuration(AnimConstants.FLAG_ITEM_ANIM_DURATION)
                valueAnimator
                    .addUpdateListener { animation: ValueAnimator ->
                        imageView.alpha = (animation.animatedValue as Float)
                    }
            } else {
                valueAnimator = ValueAnimator.ofFloat(1.0f, 0.4f)
                    .setDuration(AnimConstants.FLAG_ITEM_ANIM_DURATION)
                valueAnimator
                    .addUpdateListener { animation: ValueAnimator ->
                        imageView.alpha = (animation.animatedValue as Float)
                    }
            }
            valueAnimator.start()
        }

        private fun setItemViewAlpha(focus: Boolean, itemView: View) {
            val valueAnimator: ValueAnimator
            if (focus) {
                valueAnimator = ValueAnimator.ofFloat(0.4f, 1.0f)
                    .setDuration(AnimConstants.FLAG_ITEM_ANIM_DURATION)
                valueAnimator.addUpdateListener { animation: ValueAnimator ->
                    itemView.alpha = (animation.animatedValue as Float)
                }
            } else {
                valueAnimator = ValueAnimator.ofFloat(1.0f, 0.4f)
                    .setDuration(AnimConstants.FLAG_ITEM_ANIM_DURATION)
                valueAnimator.addUpdateListener { animation: ValueAnimator ->
                    itemView.alpha = (animation.animatedValue as Float)
                }
            }
            valueAnimator.start()
        }

        init {
            itemView.setOnClickListener {
                group?.let {
                    listener?.onGroupSelected(it.region)
                }
            }
            itemView.onFocusChangeListener =
                View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                    setItemViewAlpha(hasFocus, itemView)
                    locationBackground(hasFocus, imageBackground)
                }
        }
    }

    override fun getItemCount(): Int {
        return groups.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 && !isTypeStreaming) {
            0
        } else {
            1
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        val type = getItemViewType(i)
        val group = groups[i]
        if (type == 0) {
            (holder as BestLocationHolder).bind()
        } else {
            (holder as ServerHolder).bind(group)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        return if (i == 0) {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.server_item_view_best_location, viewGroup, false)
            BestLocationHolder(view)
        } else {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.server_item_view, viewGroup, false)
            ServerHolder(view)
        }
    }
}