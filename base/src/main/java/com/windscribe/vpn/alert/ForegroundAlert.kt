package com.windscribe.vpn.alert

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext

const val TITLE = "Error"
const val POSITIVE_BUTTON_LABEL = "OK"
const val NEGATIVE_BUTTON_LABEL = "Cancel"
fun showRetryDialog(message: String, retryCallBack: () -> Unit, cancelCallBack: () -> Unit) {
    safeDialog {
        val builder = createDialogBuilder(it, message)
        val listener = { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
            if (which == AlertDialog.BUTTON_POSITIVE) {
                retryCallBack()
            }else if(which == AlertDialog.BUTTON_NEGATIVE){
                cancelCallBack()
            }
        }
        with(builder) {
            setPositiveButton(POSITIVE_BUTTON_LABEL, DialogInterface.OnClickListener(function = listener))
            setNegativeButton(NEGATIVE_BUTTON_LABEL, DialogInterface.OnClickListener(function = listener))
            setOnCancelListener { cancelCallBack() }
            setOnDismissListener { cancelCallBack() }
            show()
        }
    }
}

fun createDialogBuilder(activity: Activity, message: String, title: String = TITLE): AlertDialog.Builder {
    val builder = AlertDialog.Builder(activity, R.style.AlertDialog)
    val view: View = LayoutInflater.from(activity).inflate(R.layout.alert_dialog_view, null)
    view.findViewById<TextView>(R.id.message).text = message
    builder.setView(view)
    builder.setTitle(title)
    return builder
}

fun showAlertDialog(message: String, callBack: () -> Unit) {
    safeDialog {
        val builder = createDialogBuilder(it, message)
        val listener = { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
            if (which == AlertDialog.BUTTON_POSITIVE)
                callBack()
        }
        with(builder) {
            setPositiveButton(POSITIVE_BUTTON_LABEL, DialogInterface.OnClickListener(function = listener))
            show()
        }
    }
}

fun showErrorDialog(message: String) {
    safeDialog {
        val builder = createDialogBuilder(it, message)
        val listener = { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
        }
        with(builder) {
            setNeutralButton(POSITIVE_BUTTON_LABEL, DialogInterface.OnClickListener(function = listener))
            show()
        }
    }
}

fun showAlertDialog(title: String, message: String, positionButtonLabel: String = POSITIVE_BUTTON_LABEL, negativeButtonLabel: String = NEGATIVE_BUTTON_LABEL, retryCallBack: () -> Unit) {
    safeDialog {
        val builder = createDialogBuilder(it, message, title)
        val listener = { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
            if (which == AlertDialog.BUTTON_POSITIVE) {
                retryCallBack()
            }
        }
        with(builder) {
            setPositiveButton(positionButtonLabel, DialogInterface.OnClickListener(function = listener))
            setNegativeButton(negativeButtonLabel, DialogInterface.OnClickListener(function = listener))
            show()
        }
    }
}

fun safeDialog(block: (activity: Activity) -> Unit) {
    appContext.activeActivity?.let {
        it.runOnUiThread {
            block(it)
        }
    }
}
