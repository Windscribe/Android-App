/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.tv.R
import com.windscribe.tv.adapter.MenuAdapter.PortHolder
import com.windscribe.tv.serverlist.customviews.PreferenceItem
import com.windscribe.tv.serverlist.customviews.State

class MenuAdapter(localiseValues: List<String>, selectedKey: String, keys: List<String>? = null) :
    RecyclerView.Adapter<PortHolder>() {
    inner class PortHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelView: PreferenceItem = itemView.findViewById(R.id.label)
        fun bind(item: String) {
            labelView.text = item
            var itemKey = item
            if (keys != null) {
                itemKey = keys[localiseValues.indexOf(item)]
            }
            if (selectedItemKey == itemKey) {
                labelView.setState(State.MenuButtonState.NotSelected)
                labelView.setState(State.MenuButtonState.Selected)
            } else {
                labelView.setState(State.MenuButtonState.NotSelected)
            }
            labelView.setOnClickListener { setCurrentlySelectItem(itemKey) }
        }
    }

    interface MenuItemSelectListener {
        fun onItemSelected(selectedItemKey: String?)
    }

    private var selectedItemKey: String
    private var listener: MenuItemSelectListener? = null
    private val localiseValues: List<String>
    private val keys: List<String>?
    override fun getItemCount(): Int {
        return localiseValues.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(portHolder: PortHolder, i: Int) {
        portHolder.bind(localiseValues[i])
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): PortHolder {
        val view =
            LayoutInflater.from(viewGroup.context).inflate(R.layout.menu_item, viewGroup, false)
        return PortHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCurrentlySelectItem(selectedKey: String) {
        this.selectedItemKey = selectedKey
        notifyDataSetChanged()
        listener?.onItemSelected(selectedKey)
    }

    fun setListener(listener: MenuItemSelectListener?) {
        this.listener = listener
    }

    init {
        setHasStableIds(true)
        this.localiseValues = localiseValues
        this.keys = keys
        this.selectedItemKey = selectedKey
    }
}
