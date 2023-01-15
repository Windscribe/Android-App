package com.windscribe.mobile.custom_view.preferences

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

open class BaseView(val view: View) {
    fun setVisibility(visibility: Int){
        view.visibility = visibility
    }
    fun showKeyboard(editText: EditText) {
        val keyboard = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        keyboard.showSoftInput(editText, 0)
    }
}