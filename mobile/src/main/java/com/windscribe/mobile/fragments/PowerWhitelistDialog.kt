package com.windscribe.mobile.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialogFragment
import com.windscribe.mobile.R

class PowerWhitelistDialog(private val activity: AppCompatActivity, val callback: (Boolean) -> Unit) : AppCompatDialogFragment() {

    @SuppressLint("BatteryLife")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity, R.style.AlertDialog)
            .setTitle(R.string.power_whitelist_title)
            .setMessage(R.string.power_whitelist_summary)
            .setPositiveButton(
                android.R.string.ok
            ) { _: DialogInterface?, _: Int ->
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + activity.packageName)
                )
                addToPowerWhitelist.launch(intent)
            }.create()
    }

    private val addToPowerWhitelist = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _: ActivityResult? ->
        callback(isIgnoringBatteryOptimizations(activity))
    }

    override fun onCancel(dialog: DialogInterface) {
        callback(isIgnoringBatteryOptimizations(activity))
    }

    companion object {
        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val name = context.applicationContext.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return manager.isIgnoringBatteryOptimizations(name)
            }
            return true
        }
    }
}