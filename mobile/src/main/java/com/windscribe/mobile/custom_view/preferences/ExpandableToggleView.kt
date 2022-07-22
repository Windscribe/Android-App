package com.windscribe.mobile.custom_view.preferences

import android.content.Context
import android.content.res.TypedArray
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.transition.Visibility
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.getResourceIdOrThrow
import com.windscribe.mobile.R


class ExpandableToggleView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr){

    interface Delegate {
        fun onToggleClick()
        fun onExplainClick()
    }

    enum class ChildType {
        DecoyTraffic, PreferredProtocol, SplitTunnelMode
    }

    var delegate: Delegate? = null
    private var toggle: ImageView? = null
    private val attributes: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ExpandableToggleView)
    private val view: View = inflate(context, R.layout.expandable_toggle_view, this)
    var childView: BaseView? = null

    init {
        view.findViewById<TextView>(R.id.description).text = attributes.getString(R.styleable.ExpandableToggleView_ExpandableToggleViewDescription)
        toggle = view.findViewById(R.id.toggle)
        view.findViewById<TextView>(R.id.label).text = attributes.getString(R.styleable.ExpandableToggleView_ExpandableToggleViewTitle)
        val leftIcon = attributes.getResourceIdOrThrow(R.styleable.ExpandableToggleView_ExpandableToggleViewLeftIcon)
        view.findViewById<ImageView>(R.id.left_icon).setImageResource(leftIcon)
        view.findViewById<ImageView>(R.id.right_icon).setOnClickListener {
            delegate?.onExplainClick()
        }
        toggle?.setOnClickListener {
            delegate?.onToggleClick()
        }
        if (attributes.getBoolean(R.styleable.ExpandableToggleView_ExpandableToggleShowRightIcon, true).not()) {
            view.findViewById<ImageView>(R.id.right_icon).visibility = INVISIBLE
        }
        attachChildView()
    }

    private fun animateVisibilityChange(active: Boolean) {
        val hideExplainViewOnCollapse = attributes.getBoolean(R.styleable.ExpandableToggleView_ExpandableToggleHideExplainViewOnCollapse, false)
        val visibility = if (active) {
            VISIBLE
        } else {
            GONE
        }
        val transition: Transition = Fade()
        transition.duration = if (hideExplainViewOnCollapse) {
            0
        } else {
            300
        }
        TransitionManager.beginDelayedTransition(parent as ViewGroup?, transition)
        childView?.setVisibility(visibility)
        if (hideExplainViewOnCollapse) {
            setExplainViewVisibility(visibility, active)
            if (attributes.getBoolean(R.styleable.ExpandableToggleView_ExpandableToggleShowRightIcon, true).not()) {
                view.findViewById<ImageView>(R.id.right_icon).visibility = INVISIBLE
            }
        }
    }

    private fun setExplainViewVisibility(visibility: Int, active: Boolean){
        view.findViewById<TextView>(R.id.description).visibility = visibility
        view.findViewById<ImageView>(R.id.right_icon).visibility = visibility
        view.findViewById<ImageView>(R.id.bottom_background).visibility = visibility
        view.findViewById<ImageView>(R.id.top_background).visibility = visibility
        view.findViewById<ImageView>(R.id.clip_corner_background).visibility = visibility
        view.findViewById<ImageView>(R.id.background).visibility = if (active) {
            GONE
        } else {
            VISIBLE
        }
    }

    fun setToggleImage(resourceId: Int) {
        toggle?.setImageResource(resourceId)
        animateVisibilityChange(resourceId == R.drawable.ic_toggle_button_on)
    }

    fun setDescription(resourceId: Int) {
        view.findViewById<TextView>(R.id.description).text = context.getString(resourceId)
    }

    private fun attachChildView() {
        val placeHolder = view.findViewById<FrameLayout>(R.id.place_holder_view)
        val type = attributes.getString(R.styleable.ExpandableToggleView_ExpandableToggleViewChildType)?.let { ChildType.valueOf(it) }
                ?: ChildType.DecoyTraffic
        childView = when (type) {
            ChildType.DecoyTraffic -> {
                DecoyTrafficView(inflate(context, R.layout.connection_decoy_traffic_tab, placeHolder))
            }
            ChildType.PreferredProtocol -> {
                ConnectionModeView(inflate(context, R.layout.auto_manual_mode_view, placeHolder))
            }
            else -> {
                SplitRoutingModeView(inflate(context, R.layout.split_routing_mode_view, placeHolder))
            }
        }
    }
}