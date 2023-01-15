/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.customview

import android.annotation.SuppressLint
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.tv.R

class ErrorPrimaryFragment : Fragment() {
    @JvmField
    @BindView(R.id.close)
    var closeBtn: Button? = null

    @JvmField
    @BindView(R.id.error)
    var errorView: TextView? = null
    private var error: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        error = arguments?.getString("error")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_error_primary, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        errorView?.text = error
        closeBtn?.requestFocus()
    }

    fun add(error: String?, activity: AppCompatActivity, container: Int, addToBackStack: Boolean) {
        val bundle = Bundle()
        bundle.putString("error", error)
        arguments = bundle
        enterTransition = Slide(Gravity.BOTTOM)
            .addTarget(R.id.error_fragment_container)
        val transaction = activity.supportFragmentManager
            .beginTransaction()
            .add(container, this)
        if (addToBackStack) {
            transaction.addToBackStack(this.javaClass.name)
        }
        transaction.commit()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.close)
    fun onCloseButtonClick() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {
        val instance: ErrorPrimaryFragment
            get() = ErrorPrimaryFragment()
    }
}
