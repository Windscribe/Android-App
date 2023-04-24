/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.mobile.databinding.UserAccountStatusLayoutBinding

interface AccountStatusDialogCallback {
    fun onRenewPlanClick()
}

data class AccountStatusDialogData(
    val title: String,
    val icon: Int,
    val description: String,
    val showSkipButton: Boolean,
    val skipText: String,
    val showUpgradeButton: Boolean,
    val upgradeText: String,
    val bannedLayout: Boolean = false
) : java.io.Serializable

class AccountStatusDialog : FullScreenDialog() {
    private var accountStatusDialogCallback: AccountStatusDialogCallback? = null
    private var binding: UserAccountStatusLayoutBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        accountStatusDialogCallback = context as? AccountStatusDialogCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = UserAccountStatusLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val accountStatusDialogData =
            arguments?.getSerializable(accountStatusDialogDataKey) as? AccountStatusDialogData
        accountStatusDialogData?.let {
            binding?.userAccountStatusIcon?.setImageResource(accountStatusDialogData.icon)
            binding?.userAccountStatusTitle?.text = accountStatusDialogData.title
            binding?.userAccountStatusDescription?.text = accountStatusDialogData.description
            binding?.userAccountStatusPrimaryButton?.text = accountStatusDialogData.upgradeText
            binding?.userAccountStatusSecondaryButton?.text = accountStatusDialogData.skipText
            binding?.userAccountStatusSecondaryButton?.visibility =
                if (accountStatusDialogData.showSkipButton) View.VISIBLE else View.GONE
            binding?.userAccountStatusPrimaryButton?.visibility =
                if (accountStatusDialogData.showUpgradeButton) View.VISIBLE else View.GONE
            binding?.userAccountStatusSecondaryButton?.setOnClickListener {
                dismiss()
            }
            binding?.userAccountStatusPrimaryButton?.setOnClickListener {
                if (accountStatusDialogData.bannedLayout) {
                    dismiss()
                } else {
                    accountStatusDialogCallback?.onRenewPlanClick()
                    dismiss()
                }
            }
        }
    }

    override fun dismiss() {
        val accountStatusDialogData =
            arguments?.getSerializable(accountStatusDialogDataKey) as? AccountStatusDialogData
        accountStatusDialogData?.let {
            if (accountStatusDialogData.bannedLayout) {
                activity?.finish()
            }
        }
        super.dismiss()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val tag = "AccountStatusDialog"
        private const val accountStatusDialogDataKey = "accountStatusDialogData"
        fun show(activity: AppCompatActivity, accountStatusDialogData: AccountStatusDialogData) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) return
            activity.runOnUiThread {
                kotlin.runCatching {
                    AccountStatusDialog().apply {
                        Bundle().apply {
                            putSerializable(accountStatusDialogDataKey, accountStatusDialogData)
                            arguments = this
                        }
                    }.showNow(activity.supportFragmentManager, tag)
                }
            }
        }

        fun hide(activity: AppCompatActivity) {
            activity.runOnUiThread {
                activity.supportFragmentManager.findFragmentByTag(tag)?.let {
                    (it as? AccountStatusDialog)?.dismiss()
                }
            }
        }
    }
}