/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.customview

import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import com.windscribe.tv.R

class ProgressFragment : Fragment() {
    @JvmField
    @BindView(R.id.progressLabel)
    var progressLabel: TextView? = null
    private var progressText = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateProgressStatus(progressText)
    }

    override fun onDestroyView() {
        progressText = ""
        super.onDestroyView()
    }

    fun add(activity: AppCompatActivity, container: Int, addToBackStack: Boolean) {
        enterTransition = Slide(Gravity.BOTTOM)
            .addTarget(R.id.progress_fragment_container)
        val transaction = activity.supportFragmentManager
            .beginTransaction()
            .add(container, this)
        if (addToBackStack) {
            transaction.addToBackStack(this.javaClass.name)
        }
        transaction.commit()
    }

    fun add(
        progressText: String,
        activity: AppCompatActivity,
        container: Int,
        addToBackStack: Boolean
    ) {
        this.progressText = progressText
        add(activity, container, addToBackStack)
    }

    fun finishProgress() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    fun updateProgressStatus(call: String) {
        if (progressLabel != null) {
            progressText = call
            progressLabel?.text = progressText
        }
    }

    companion object {
        @JvmStatic
        val instance: ProgressFragment
            get() = ProgressFragment()
    }
}
