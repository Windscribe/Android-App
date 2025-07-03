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
import com.windscribe.tv.databinding.FragmentErrorPrimaryBinding

class ErrorPrimaryFragment : Fragment() {
    private lateinit var binding: FragmentErrorPrimaryBinding
    private var error: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        error = arguments?.getString("error")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentErrorPrimaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.error.text = error
        binding.close.requestFocus()
        binding.close.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    fun add(error: String?, activity: AppCompatActivity, container: Int, addToBackStack: Boolean) {
        val bundle = Bundle()
        bundle.putString("error", error)
        arguments = bundle
        enterTransition = Slide(Gravity.BOTTOM).addTarget(R.id.error_fragment_container)
        val transaction = activity.supportFragmentManager.beginTransaction().add(container, this)
        if (addToBackStack) {
            transaction.addToBackStack(this.javaClass.name)
        }
        transaction.commit()
    }

    companion object {
        val instance: ErrorPrimaryFragment
            get() = ErrorPrimaryFragment()
    }
}
