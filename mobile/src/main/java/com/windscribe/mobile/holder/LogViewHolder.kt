package com.windscribe.mobile.holder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.mobile.R


class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(log: String){
        val view = itemView.findViewById<TextView>(R.id.debugViewLabel)
        view.text = log
    }
}