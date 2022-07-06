/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings.fragment

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.leanback.widget.HorizontalGridView
import butterknife.BindView
import butterknife.ButterKnife
import com.windscribe.tv.R
import com.windscribe.tv.adapter.MenuAdapter
import com.windscribe.tv.adapter.MenuAdapter.MenuItemSelectListener
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.settings.SettingActivity
import java.lang.ClassCastException

class GeneralFragment : Fragment() {
    @JvmField
    @BindView(R.id.languageList)
    var languageView: HorizontalGridView? = null

    @JvmField
    @BindView(R.id.generalParent)
    var mainLayout: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.sortList)
    var sortView: HorizontalGridView? = null

    @JvmField
    @BindView(R.id.titleLanguage)
    var titleLanguageTextView: TextView? = null
    private var listener: SettingsFragmentListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity: SettingActivity
        if (context is SettingActivity) {
            activity = context
            try {
                listener = activity
            } catch (e: ClassCastException) {
                throw ClassCastException("$activity must implement OnCompleteListener")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_general, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
    }

    fun checkViewVisibility(scrollView: NestedScrollView) {
        listener?.onContainerHidden(!isViewVisible(scrollView, languageView))
    }

    fun resetTextResources() {
        titleLanguageTextView?.setText(R.string.preferred_language)
    }

    fun setLanguageAdapter(savedLanguage: String, languages: Array<String>) {
        val languageAdapter = MenuAdapter(listOf(*languages), savedLanguage)
        languageAdapter.setListener(object : MenuItemSelectListener {
            override fun onItemSelected(selectedItem: String?) {
                listener?.onLanguageSelect(
                    selectedItem
                )
            }
        })
        languageView?.setNumRows(1)
        languageView?.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        languageView?.adapter = languageAdapter
    }

    fun setSortAdapter(savedSort: String, sortTypes: Array<String>) {
        val sortAdapter = MenuAdapter(listOf(*sortTypes), savedSort)
        sortAdapter.setListener(object : MenuItemSelectListener {
            override fun onItemSelected(selectedItem: String?) {
                selectedItem?.let {
                    listener?.onSortSelect(
                        selectedItem
                    )
                }
            }
        })
        sortView?.setNumRows(1)
        sortView?.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        sortView?.adapter = sortAdapter
    }

    private fun isViewVisible(scrollView: NestedScrollView, view: View?): Boolean {
        val scrollBounds = Rect()
        scrollView.getDrawingRect(scrollBounds)
        view?.let {
            val top = view.y
            val bottom = top + view.height
            return scrollBounds.top < top && scrollBounds.bottom > bottom
        }
        return false
    }
}
