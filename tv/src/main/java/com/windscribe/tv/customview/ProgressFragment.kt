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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentProgressBinding

class ProgressFragment : Fragment() {
    private var progressText = ""
    private lateinit var binding: FragmentProgressBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
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

    fun updateProgressStatus(call: String) {
        progressText = call
        binding.progressLabel.text = progressText
    }

    companion object {
        @JvmStatic
        val instance: ProgressFragment
            get() = ProgressFragment()
    }
}
