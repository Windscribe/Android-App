package com.windscribe.mobile.custom_view.preferences

import android.view.View
import android.widget.*
import com.windscribe.mobile.R

class DecoyTrafficView(private val childView: View) : BaseView(childView), AdapterView.OnItemSelectedListener {
    private val potentialTrafficVolume = childView.findViewById<TextView>(R.id.tv_current_potential_traffic)
    private val spinner = childView.findViewById<Spinner>(R.id.spinner_fake_traffic_volume)
    private val current = childView.findViewById<TextView>(R.id.tv_current_fake_traffic_volume)
    private val clickableArea = childView.findViewById<ImageView>(R.id.clickable_area)

    interface Delegate {
        fun onDecoyTrafficVolumeChanged(volume: String)
    }

    var delegate: Delegate? = null

    init {
        clickableArea.setOnClickListener { spinner?.performClick() }
        spinner?.onItemSelectedListener = this
    }

    fun setPotentialTraffic(trafficVolume: String) {
        potentialTrafficVolume.text = trafficVolume
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        view?.findViewById<TextView>(R.id.tv_drop_down)?.text = ""
        spinner?.selectedItem.toString().let {
            current?.text = it
            delegate?.onDecoyTrafficVolumeChanged(it)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun setAdapter(savedSelection: String, selections: Array<String>) {
        val selectionAdapter: ArrayAdapter<String> = ArrayAdapter<String>(childView.context, R.layout.drop_down_layout,
                R.id.tv_drop_down, selections)
        spinner?.adapter = selectionAdapter
        spinner?.isSelected = false
        spinner?.setSelection(selectionAdapter.getPosition(savedSelection))
        current?.text = savedSelection
    }
}