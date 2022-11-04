package com.windscribe.mobile.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.mobile.R
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.commonutils.Ext.toPx
import com.windscribe.vpn.commonutils.ThemeUtils

class ProtocolInformationAdapter(
    val data: MutableList<ProtocolInformation>,
    private val listener: ItemSelectListener
) :
    RecyclerView.Adapter<ProtocolInformationViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProtocolInformationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.protocol_information, parent, false)
        return ProtocolInformationViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: ProtocolInformationViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun update(updatedData: List<ProtocolInformation>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return data.size
            }

            override fun getNewListSize(): Int {
                return updatedData.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return data[oldItemPosition].protocol == updatedData[newItemPosition].protocol
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = data[oldItemPosition]
                val newItem = updatedData[newItemPosition]
                return oldItem.protocol == newItem.protocol && oldItem.description == newItem.description
                        && oldItem.type == newItem.type && oldItem.port == newItem.port
            }

        })
        data.clear()
        data.addAll(updatedData)
        diff.dispatchUpdatesTo(this)
    }
}


interface ItemSelectListener {
    fun onItemSelect(protocolInformation: ProtocolInformation)
}


class ProtocolInformationViewHolder(val itemView: View, val listener: ItemSelectListener) :
    RecyclerView.ViewHolder(itemView) {
    private val protocolView: TextView = itemView.findViewById(R.id.protocol)
    private val portView: TextView = itemView.findViewById(R.id.port)
    private val descriptionView: TextView = itemView.findViewById(R.id.description)
    private val dividerView: TextView = itemView.findViewById(R.id.divider)
    private val statusView: TextView = itemView.findViewById(R.id.status)
    private val checkView: ImageView = itemView.findViewById(R.id.check)
    private val actionArrowView: ImageView = itemView.findViewById(R.id.action_arrow)
    private val errorView: TextView = itemView.findViewById(R.id.error)
    private val containerView: ConstraintLayout = itemView.findViewById(R.id.container)
    private var protocolInformation: ProtocolInformation? = null

    init {
        itemView.setOnClickListener { view ->
            protocolInformation?.let {
                if (it.type != ProtocolConnectionStatus.Failed && it.type != ProtocolConnectionStatus.Connected) {
                    listener.onItemSelect(it)
                }
            }
        }
    }

    fun bind(protocolInformation: ProtocolInformation) {
        this.protocolInformation = protocolInformation
        protocolInformation.apply {
            protocolView.text = Util.getProtocolLabel(protocol)
            portView.text = port
            descriptionView.text = description
        }
        itemView.apply {
            when (protocolInformation.type) {
                ProtocolConnectionStatus.Connected -> {
                    containerView.background = AppCompatResources.getDrawable(
                        context,
                        R.drawable.protocol_connected_background
                    )
                    protocolView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdActionColor,
                            R.color.colorNeonGreen
                        )
                    )
                    portView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdActionColor50,
                            R.color.colorNeonGreen50
                        )
                    )
                    descriptionView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdActionColor50,
                            R.color.colorNeonGreen50
                        )
                    )
                    dividerView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdActionColor25,
                            R.color.colorNeonGreen25
                        )
                    )
                    statusView.text = "Connected to"
                    statusView.visibility = View.VISIBLE
                    checkView.visibility = View.VISIBLE
                    actionArrowView.visibility = View.GONE
                    errorView.visibility = View.GONE
                    val background = statusView.background as GradientDrawable
                    //The corners are ordered top-left, top-right, bottom-right, bottom-left
                    val radius = context.toPx(8F)
                    //   background.cornerRadii = floatArrayOf(0F, 0F,context.toPx(7F),context.toPx(7F),radius, 0F, 0F,radius,radius)
                    val param = statusView.layoutParams as ViewGroup.MarginLayoutParams
                    val margin = context.toPx(2F).toInt()
                    //   param.setMargins(0,0,margin,0)
                    statusView.layoutParams = param
                }
                ProtocolConnectionStatus.Disconnected -> {
                    containerView.background = AppCompatResources.getDrawable(
                        context,
                        R.drawable.protocol_disconnected_background
                    )
                    protocolView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdPrimaryColor,
                            R.color.colorWhite
                        )
                    )
                    portView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdSecondaryColor,
                            R.color.colorWhite50
                        )
                    )
                    descriptionView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdSecondaryColor,
                            R.color.colorWhite50
                        )
                    )
                    dividerView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdPrimaryColor25,
                            R.color.colorWhite25
                        )
                    )
                    statusView.visibility = View.GONE
                    checkView.visibility = View.GONE
                    actionArrowView.visibility = View.VISIBLE
                    errorView.visibility = View.GONE
                }
                ProtocolConnectionStatus.Failed -> {
                    containerView.background = AppCompatResources.getDrawable(
                        context,
                        R.drawable.protocol_failed_background
                    )
                    protocolView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdSecondaryColor,
                            R.color.colorWhite50
                        )
                    )
                    portView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdSecondaryColor,
                            R.color.colorWhite50
                        )
                    )
                    descriptionView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdSecondaryColor,
                            R.color.colorWhite50
                        )
                    )
                    dividerView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdPrimaryColor25,
                            R.color.colorWhite25
                        )
                    )
                    statusView.visibility = View.GONE
                    checkView.visibility = View.GONE
                    actionArrowView.visibility = View.GONE
                    errorView.visibility = View.VISIBLE
                }
                ProtocolConnectionStatus.NextUp -> {
                    containerView.background = AppCompatResources.getDrawable(
                        context,
                        R.drawable.protocol_disconnected_background
                    )
                    protocolView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdPrimaryColor,
                            R.color.colorWhite
                        )
                    )
                    portView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdSecondaryColor,
                            R.color.colorWhite50
                        )
                    )
                    descriptionView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdSecondaryColor,
                            R.color.colorWhite50
                        )
                    )
                    dividerView.setTextColor(
                        ThemeUtils.getColor(
                            context,
                            R.attr.wdPrimaryColor25,
                            R.color.colorWhite25
                        )
                    )
                    statusView.text = "NEXT UP IN ${protocolInformation.autoConnectTimeLeft}s"
                    statusView.visibility = View.VISIBLE
                    checkView.visibility = View.GONE
                    actionArrowView.visibility = View.VISIBLE
                    errorView.visibility = View.GONE
                    val background = statusView.background as GradientDrawable
                    //The corners are ordered top-left, top-right, bottom-right, bottom-left
                    val radius = context.toPx(8F)
                    background.cornerRadii =
                        floatArrayOf(0F, 0F, radius, radius, 0F, 0F, radius, radius)
                    val param = statusView.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(0, 0, 0, 0)
                    statusView.layoutParams = param
                }
            }
        }
    }
}