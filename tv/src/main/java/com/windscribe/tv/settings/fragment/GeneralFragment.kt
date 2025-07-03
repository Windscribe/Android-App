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
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.windscribe.tv.adapter.MenuAdapter
import com.windscribe.tv.adapter.MenuAdapter.MenuItemSelectListener
import com.windscribe.tv.databinding.FragmentGeneralBinding
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.settings.SettingActivity

class GeneralFragment : Fragment() {
    private lateinit var binding: FragmentGeneralBinding
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
    ): View {
        binding = FragmentGeneralBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
    }

    fun checkViewVisibility(scrollView: NestedScrollView) {
        listener?.onContainerHidden(!isViewVisible(scrollView, binding.languageList))
    }

    fun resetTextResources() {
        binding.titleLanguage.setText(com.windscribe.vpn.R.string.preferred_language)
    }

    fun setLanguageAdapter(savedLanguage: String, languages: Array<String>) {
        val languageAdapter = MenuAdapter(listOf(*languages), savedLanguage)
        languageAdapter.setListener(object : MenuItemSelectListener {
            override fun onItemSelected(selectedItemKey: String?) {
                listener?.onLanguageSelect(
                    selectedItemKey
                )
            }
        })
        binding.languageList.setNumRows(1)
        binding.languageList.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.languageList.adapter = languageAdapter
    }

    fun setSortAdapter(localiseItems: Array<String>, selectedKey: String, keys: Array<String>) {
        val sortAdapter = MenuAdapter(listOf(*localiseItems), selectedKey, listOf(*keys))
        sortAdapter.setListener(object : MenuItemSelectListener {
            override fun onItemSelected(selectedItemKey: String?) {
                selectedItemKey?.let {
                    listener?.onSortSelect(
                        selectedItemKey
                    )
                }
            }
        })
        binding.sortList.setNumRows(1)
        binding.sortList.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.sortList.adapter = sortAdapter
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
