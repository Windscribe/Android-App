package com.windscribe.mobile.dialogs


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.mobile.databinding.PowerWhiteListDialogBinding

interface PowerWhitelistDialogCallback {
    fun neverAskPowerWhiteListPermissionAgain()
    fun askPowerWhiteListPermissionLater()
    fun askForPowerWhiteListPermission()
}

class PowerWhitelistDialog : FullScreenDialog() {
    private var callback: PowerWhitelistDialogCallback? = null
    private var binding: PowerWhiteListDialogBinding? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = activity as PowerWhitelistDialogCallback?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = PowerWhiteListDialogBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.ok?.setOnClickListener {
            callback?.askForPowerWhiteListPermission()
            dismiss()
        }
        binding?.later?.setOnClickListener {
            callback?.askPowerWhiteListPermissionLater()
            dismiss()
        }
        binding?.neverAskAgain?.setOnClickListener {
            callback?.neverAskPowerWhiteListPermissionAgain()
            dismiss()
        }
    }

    companion object {
        const val tag = "PowerWhitelistDialog"
        fun show(activity: AppCompatActivity) {
            activity.runOnUiThread {
                kotlin.runCatching {
                    PowerWhitelistDialog().showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}