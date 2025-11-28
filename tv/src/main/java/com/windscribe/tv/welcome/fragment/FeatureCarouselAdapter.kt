/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.tv.R

data class FeatureItem(val imageRes: Int, val text: String)

class FeatureCarouselAdapter(private val features: List<FeatureItem>) :
    RecyclerView.Adapter<FeatureCarouselAdapter.FeatureViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feature_carousel, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(features[position])
    }

    override fun getItemCount(): Int = features.size

    class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val featureImage: ImageView = itemView.findViewById(R.id.feature_image)
        private val featureText: TextView = itemView.findViewById(R.id.feature_text)

        fun bind(feature: FeatureItem) {
            featureImage.setImageResource(feature.imageRes)
            featureText.text = feature.text
        }
    }
}