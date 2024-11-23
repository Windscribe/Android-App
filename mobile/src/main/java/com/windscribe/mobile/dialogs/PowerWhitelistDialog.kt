package com.windscribe.mobile.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.windscribe.mobile.databinding.PowerWhiteListDialogBinding

interface PowerWhitelistDialogCallback {
    fun neverAskPowerWhiteListPermissionAgain()
    fun askPowerWhiteListPermissionLater()
    fun askForPowerWhiteListPermission()
}

class PowerWhitelistDialog : Fragment() {
    private var callback: PowerWhitelistDialogCallback? = null
    private var binding: PowerWhiteListDialogBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = activity as? PowerWhitelistDialogCallback
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
            closeDialog()
        }
        binding?.later?.setOnClickListener {
            callback?.askPowerWhiteListPermissionLater()
            closeDialog()
        }
        binding?.neverAskAgain?.setOnClickListener {
            callback?.neverAskPowerWhiteListPermissionAgain()
            closeDialog()
        }
    }

    private fun closeDialog() {
        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    override fun onStart() {
        super.onStart()
        activity?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    companion object {
        private const val TAG = "PowerWhitelistDialog"

        fun show(activity: AppCompatActivity) {
            activity.runOnUiThread {
                val fragmentManager = activity.supportFragmentManager
                if (fragmentManager.findFragmentByTag(TAG) == null) {
                    val transaction = fragmentManager.beginTransaction()
                    transaction.add(android.R.id.content, PowerWhitelistDialog(), TAG)
                    transaction.commitAllowingStateLoss()
                }
            }
        }
    }
}