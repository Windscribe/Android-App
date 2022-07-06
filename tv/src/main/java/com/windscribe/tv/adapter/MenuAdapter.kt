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

class MenuAdapter(menuItems: List<String>, currentlySelectItem: String) :
    RecyclerView.Adapter<PortHolder>() {
    inner class PortHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelView: PreferenceItem = itemView.findViewById(R.id.label)
        fun bind(item: String) {
            labelView.text = item
            if (currentlySelectItem == item) {
                labelView.setState(State.MenuButtonState.NotSelected)
                labelView.setState(State.MenuButtonState.Selected)
            } else {
                labelView.setState(State.MenuButtonState.NotSelected)
            }
            labelView.setOnClickListener { setCurrentlySelectItem(item) }
        }
    }

    interface MenuItemSelectListener {
        fun onItemSelected(selectedItem: String?)
    }

    private var currentlySelectItem: String
    private var listener: MenuItemSelectListener? = null
    private val menuItems: List<String>
    override fun getItemCount(): Int {
        return menuItems.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(portHolder: PortHolder, i: Int) {
        portHolder.bind(menuItems[i])
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): PortHolder {
        val view =
            LayoutInflater.from(viewGroup.context).inflate(R.layout.menu_item, viewGroup, false)
        return PortHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCurrentlySelectItem(currentlySelectItem: String) {
        this.currentlySelectItem = currentlySelectItem
        notifyDataSetChanged()
        listener!!.onItemSelected(currentlySelectItem)
    }

    fun setListener(listener: MenuItemSelectListener?) {
        this.listener = listener
    }

    init {
        setHasStableIds(true)
        this.menuItems = menuItems
        this.currentlySelectItem = currentlySelectItem
    }
}
