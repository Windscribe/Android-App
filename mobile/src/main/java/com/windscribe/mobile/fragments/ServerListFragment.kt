/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.ConfigAdapter
import com.windscribe.mobile.custom_view.RefreshViewEg
import com.windscribe.mobile.custom_view.refresh.RecyclerRefreshLayout
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.di.DaggerActivityComponent
import com.windscribe.mobile.holder.ConfigViewHolder
import com.windscribe.mobile.holder.RemoveConfigHolder
import com.windscribe.mobile.windscribe.FragmentClickListener
import com.windscribe.mobile.windscribe.WindscribeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import java.lang.Exception
import kotlin.math.roundToInt

class ServerListFragment : Fragment() {

    @JvmField
    @BindView(R.id.cl_data_status)
    var upgradeLayout: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.recycler_view_server_list)
    var recyclerView: RecyclerView? = null
    private var lastPositionSnapped = 0

    @JvmField
    @BindView(R.id.tv_add_config_button)
    var addConfigButton: TextView? = null

    @JvmField
    @BindView(R.id.tv_deviceName)
    var deviceName: TextView? = null

    @JvmField
    @BindView(R.id.img_nothing_to_show)
    var imageViewBrokenHeart: ImageView? = null

    @JvmField
    @BindView(R.id.tv_reload)
    var reloadViewButton: TextView? = null

    @JvmField
    @BindView(R.id.tv_adapter_load_error)
    var textViewAdapterLoadError: TextView? = null

    @JvmField
    @BindView(R.id.tv_add_button)
    var textViewAddButton: TextView? = null

    @JvmField
    @BindView(R.id.data_left)
    var textViewDataRemaining: TextView? = null

    @JvmField
    @BindView(R.id.data_upgrade_label)
    var textViewDataUpgrade: TextView? = null

    @JvmField
    @BindView(R.id.cl_server_list_fragment)
    var serverListParentLayout: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.recycler_view_server_list_swipe)
    var swipeRefreshLayout: RecyclerRefreshLayout? = null

    private var fragmentClickListener: FragmentClickListener? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var mFragmentNumber = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerActivityComponent.builder().activityModule(ActivityModule())
            .applicationComponent(
                appContext
                    .applicationComponent
            ).build().inject(this)
        arguments?.let {
            mFragmentNumber = it.getInt("fragment_number", 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentView = inflater.inflate(R.layout.server_list_fragment_layout, container, false)
        ButterKnife.bind(this, fragmentView)
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager?.orientation = RecyclerView.VERTICAL
        recyclerView?.layoutManager = linearLayoutManager
        linearLayoutManager?.isItemPrefetchEnabled = false
        setSwipeRefreshLayout(fragmentView)
        setScrollHapticFeedback()
        return fragmentView
    }

    fun addSwipeListener() {
        ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    if (viewHolder is ConfigViewHolder) {
                        return ItemTouchHelper.LEFT
                    }
                    return if (viewHolder is RemoveConfigHolder) {
                        ItemTouchHelper.RIGHT
                    } else super.getSwipeDirs(recyclerView, viewHolder)
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    if (recyclerView?.isComputingLayout == true) {
                        return
                    }
                    val adapter = recyclerView?.adapter
                    if (adapter is ConfigAdapter) {
                        val configFiles = adapter.configFiles
                        if (direction == ItemTouchHelper.LEFT && configFiles.size > 0) {
                            for (configFile in configFiles) {
                                configFile.type = 1
                            }
                            val type = configFiles[viewHolder.adapterPosition].type
                            if (type == 1) {
                                configFiles[viewHolder.adapterPosition].type = 2
                            }
                            adapter.notifyDataSetChanged()
                        }
                        if (direction == ItemTouchHelper.RIGHT) {
                            for (configFile in configFiles) {
                                if (configFile.type == 2) {
                                    configFile.type = 1
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }).attachToRecyclerView(recyclerView)
    }

    fun clearErrors() {
        imageViewBrokenHeart?.visibility = View.GONE
        textViewAdapterLoadError?.visibility = View.GONE
        textViewAddButton?.visibility = View.GONE
        textViewAdapterLoadError?.text = ""
        reloadViewButton?.visibility = View.GONE
        addConfigButton?.visibility = View.GONE
    }

    fun hideUpgradeLayout() {
        upgradeLayout?.visibility = View.GONE
    }

    // Add static ip
    @OnClick(R.id.tv_add_button, R.id.tv_add_config_button)
    fun onAddClick() {
        if (fragmentClickListener != null) {
            if (mFragmentNumber == 4) {
                fragmentClickListener?.onAddConfigClick()
            } else {
                fragmentClickListener?.onStaticIpClick()
            }
        }
    }

    @OnClick(R.id.tv_reload)
    fun onReloadClick() {
        if (fragmentClickListener != null) {
            fragmentClickListener?.onReloadClick()
        }
    }

    @OnClick(R.id.data_upgrade_label)
    fun onUpgradeViewClick() {
        if (fragmentClickListener != null) {
            fragmentClickListener?.onUpgradeClicked()
        }
    }

    fun scrollTo(scrollTo: Int) {
        if (recyclerView?.adapter != null) {
            recyclerView?.smoothScrollToPosition(scrollTo)
        }
    }

    fun setAddMoreConfigLayout(error: String?, configCount: Int) {
        if (activity == null) {
            return
        }
        clearErrors()
        if (configCount == 0) {

            imageViewBrokenHeart
                ?.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_custom_config_icon
                    )
                )
            imageViewBrokenHeart?.visibility = View.VISIBLE
            textViewAdapterLoadError?.text = error
            textViewAdapterLoadError?.visibility = View.VISIBLE
            addConfigButton?.visibility = View.VISIBLE
        } else {
            textViewAddButton?.visibility = View.VISIBLE
            textViewAddButton?.setText(R.string.add_vpn_config)
        }
    }

    fun setErrorNoItems(errorNoItems: String?) {
        clearErrors()
        imageViewBrokenHeart?.visibility = View.VISIBLE
        textViewAdapterLoadError?.visibility = View.VISIBLE
        textViewAdapterLoadError?.text = errorNoItems
    }

    fun setErrorNoStaticIp(btnText: String?, error: String?, deviceName: String?) {
        clearErrors()
        textViewAdapterLoadError?.visibility = View.VISIBLE
        textViewAddButton?.visibility = View.VISIBLE
        textViewAdapterLoadError?.text = error
        textViewAddButton?.text = btnText
        if (deviceName != null && deviceName.isNotEmpty()) {
            this.deviceName?.visibility = View.VISIBLE
            this.deviceName?.text = deviceName
        }
    }

    fun setFragmentClickListener(fragmentClickListener: FragmentClickListener?) {
        this.fragmentClickListener = fragmentClickListener
    }

    fun setLoadRetry(message: String?) {
        clearErrors()
        textViewAdapterLoadError?.visibility = View.VISIBLE
        textViewAdapterLoadError?.text = message
        reloadViewButton?.text = resources.getString(R.string.retry)
        reloadViewButton?.visibility = View.VISIBLE
    }

    fun setRefreshingLayout(refreshing: Boolean) {
        swipeRefreshLayout?.setRefreshing(refreshing)
    }

    fun setSwipeRefreshLayoutEnabled(enabled: Boolean) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout?.isEnabled = enabled && canPullToRefresh()
        }
    }

    fun showUpgradeLayout(color: Int, upgradeLabel: String?, dataLeft: String?) {
        upgradeLayout?.visibility = View.VISIBLE
        textViewDataUpgrade?.text = upgradeLabel
        textViewDataRemaining?.text = dataLeft
        textViewDataRemaining?.setTextColor(color)
    }

    private fun canPullToRefresh(): Boolean {
        val firstViewPosition = linearLayoutManager?.findFirstVisibleItemPosition()
        return firstViewPosition == 0 && linearLayoutManager?.childCount!! > 0
    }

    private fun onRefreshForPing() {
        var fragmentIndex = 0
        arguments?.let {
            fragmentIndex = it.getInt("fragment_number", 0)
        }
        when (fragmentIndex) {
            0 -> fragmentClickListener?.onRefreshPingsForAllServers()
            1 -> fragmentClickListener?.onRefreshPingsForFavouritesServers()
            2 -> fragmentClickListener?.onRefreshPingsForStreamingServers()
            3 -> fragmentClickListener?.onRefreshPingsForStaticServers()
            4 -> fragmentClickListener?.onRefreshPingsForConfigServers()
        }
    }

    private fun setScrollHapticFeedback() {
        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val centerView = linearLayoutManager?.findFirstVisibleItemPosition()
                if (centerView != null) {
                    updateAdapterPosition(centerView)
                }
               /* fragmentClickListener!!.setServerListToolbarElevation(
                    if (centerView == 0) 0 else resources.getDimensionPixelSize(R.dimen.reg_2dp)
                )*/
                swipeRefreshLayout!!.isEnabled = canPullToRefresh()
            }
        })
    }

    private fun setSwipeRefreshLayout(view: View) {
        // Refresh layout
        val refreshViewEg = RefreshViewEg(activity)
        swipeRefreshLayout?.setRefreshView(refreshViewEg, view.layoutParams)
        refreshViewEg.layoutParams.height = resources.getDimension(R.dimen.reg_80dp).roundToInt()
        refreshViewEg.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        swipeRefreshLayout?.isEnabled = canPullToRefresh()
        swipeRefreshLayout?.setRefreshInitialOffset(
            -resources.getDimension(R.dimen.reg_68dp).roundToInt().toFloat()
        )
        swipeRefreshLayout?.setOnRefreshListener {
            if (fragmentClickListener != null) {
                if (activity is WindscribeActivity) {
                    (activity as WindscribeActivity?)?.performButtonClickHapticFeedback()
                }
                onRefreshForPing()
            }
        }
    }

    private fun updateAdapterPosition(position: Int) {
        val hapticFeedbackEnabled = appContext.preference.isHapticFeedbackEnabled
        if (position == lastPositionSnapped || !hapticFeedbackEnabled) {
            return
        }
        val vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (activity is WindscribeActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createOneShot(8, 255)
                if (vibrationEffect != null) {
                    try {
                        vibrator.vibrate(vibrationEffect)
                    } catch (ignored: Exception) {
                    }
                }
            } else {
                val audioAttributes = AudioAttributes.Builder().build()
                vibrator.vibrate(8, audioAttributes)
            }
        }
        lastPositionSnapped = position
    }

    companion object {
        fun newInstance(number: Int): ServerListFragment {
            val serverListFragment = ServerListFragment()
            val bundle = Bundle()
            bundle.putInt("fragment_number", number)
            serverListFragment.arguments = bundle
            return serverListFragment
        }
    }
}
