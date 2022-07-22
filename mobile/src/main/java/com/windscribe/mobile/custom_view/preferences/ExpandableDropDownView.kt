package com.windscribe.mobile.custom_view.preferences

import android.content.Context
import android.content.res.TypedArray
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.getResourceIdOrThrow
import com.windscribe.mobile.R


class ExpandableDropDownView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), AdapterView.OnItemSelectedListener {

    interface Delegate {
        fun onItemSelect(position: Int)
        fun onExplainClick()
    }
    enum class ChildType {
        ConnectionMode, PacketSize, KeepAliveMode
    }
    var delegate: Delegate? = null
    private var spinner: Spinner? = null
    private var current: TextView? = null
    private val attributes: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ExpandableDropDownView)
    private val view: View = View.inflate(context, R.layout.expandable_dropdown_view, this)
    var childView: BaseView? = null

    init {
        spinner = view.findViewById(R.id.spinner)
        current = view.findViewById(R.id.current)
        attributes.getString(R.styleable.ExpandableDropDownView_ExpandableDropDownDescription)?.let {
            view.findViewById<TextView>(R.id.description).text = it
        }
        view.findViewById<TextView>(R.id.label).text = attributes.getString(R.styleable.ExpandableDropDownView_ExpandableDropDownTitle)
        val leftIcon = attributes.getResourceIdOrThrow(R.styleable.ExpandableDropDownView_ExpandableDropDownLeftIcon)
        view.findViewById<ImageView>(R.id.left_icon).setImageResource(leftIcon)
        view.findViewById<ImageView>(R.id.right_icon).setOnClickListener { delegate?.onExplainClick() }
        view.findViewById<ImageView>(R.id.clickable_area).setOnClickListener { spinner?.performClick() }
        spinner?.onItemSelectedListener = this
        attachChildView()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        view?.findViewById<TextView>(R.id.tv_drop_down)?. text = ""
        spinner?.selectedItem.toString().let {
            current?.text = it
            delegate?.onItemSelect(position)
            animateVisibilityChange(position)
        }
    }

    private fun animateVisibilityChange(position: Int){
        val visibility = if (position == 0){
            GONE
        } else {
            VISIBLE
        }
        val transition: Transition = Fade()
        transition.duration = 300
        TransitionManager.beginDelayedTransition(parent as ViewGroup?, transition)
        childView?.setVisibility(visibility)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun setAdapter(savedSelection: String, selections: Array<String>) {
        val selectionAdapter: ArrayAdapter<String> = ArrayAdapter<String>(context, R.layout.drop_down_layout,
                R.id.tv_drop_down, selections)
        spinner?.adapter = selectionAdapter
        spinner?.isSelected = false
        spinner?.setSelection(selectionAdapter.getPosition(savedSelection))
        current?.text = savedSelection
    }



    private fun attachChildView(){
        val childType = attributes.getString(R.styleable.ExpandableDropDownView_ExpandableDropDownChildType)?.let { ChildType.valueOf(it) }
                ?:ChildType. ConnectionMode
        val placeHolder = view.findViewById<FrameLayout>(R.id.place_holder_view)
        childView = when (childType) {
            ChildType.PacketSize -> {
                PacketSizeView(inflate(context, R.layout.packet_size_view, placeHolder))
            }
            ChildType.ConnectionMode -> {
                ConnectionModeView(inflate(context, R.layout.auto_manual_mode_view, placeHolder))
            }
            else -> {
                KeepAliveView(inflate(context, R.layout.connection_keep_alive_tab, placeHolder))
            }
        }
    }
}