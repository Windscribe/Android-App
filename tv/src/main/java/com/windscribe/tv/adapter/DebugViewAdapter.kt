/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.tv.R
import com.windscribe.tv.adapter.DebugViewAdapter.DebugHolder

class DebugViewAdapter(private val logs: List<String>) : RecyclerView.Adapter<DebugHolder>() {
    class DebugHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val debugLabelView: AppCompatTextView = itemView.findViewById(R.id.debug_text)
        fun bind(log: String?) {
            debugLabelView.text = log
        }

        init {
            itemView.onFocusChangeListener =
                View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                    if (hasFocus) {
                        debugLabelView.setBackgroundColor(
                            itemView.context.resources.getColor(R.color.colorWhite50)
                        )
                    } else {
                        debugLabelView.background = null
                    }
                }
        }
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    override fun onBindViewHolder(holder: DebugHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebugHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.debug_item_view, parent, false)
        return DebugHolder(view)
    }
}
