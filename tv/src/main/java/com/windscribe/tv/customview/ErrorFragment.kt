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
import butterknife.OnClick
import com.windscribe.tv.R

class ErrorFragment : Fragment() {
    @JvmField
    @BindView(R.id.error)
    var errorView: TextView? = null
    private var error: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        error = requireArguments().getString("error")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_error, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        errorView?.text = error
    }

    fun add(error: String?, activity: AppCompatActivity, container: Int, addToBackStack: Boolean) {
        val bundle = Bundle()
        bundle.putString("error", error)
        arguments = bundle
        enterTransition = Slide(Gravity.BOTTOM).addTarget(R.id.error_fragment_container)
        val transaction = activity.supportFragmentManager
            .beginTransaction()
            .add(container, this)
        if (addToBackStack) {
            transaction.addToBackStack(this.javaClass.name)
        }
        transaction.commit()
    }

    @OnClick(R.id.close_btn)
    fun onCloseButtonClick() {
        requireActivity().onBackPressed()
    }

    companion object {
        @JvmStatic
        val instance: ErrorFragment
            get() = ErrorFragment()
    }
}
