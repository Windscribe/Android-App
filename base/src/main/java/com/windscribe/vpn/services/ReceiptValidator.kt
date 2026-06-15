package com.windscribe.vpn.services

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.workers.WindScribeWorkManager.Companion.PENDING_AMAZON_RECEIPT_WORKER_KEY
import com.windscribe.vpn.workers.WindScribeWorkManager.Companion.PENDING_GOGGLE_RECEIPT_WORKER_KEY

class ReceiptValidator(
    private val context: Context,
    private val preferencesHelper: PreferencesHelper,
    private val amazon: OneTimeWorkRequest? = null,
    private val google: OneTimeWorkRequest? = null,
) {
    fun checkPendingAccountUpgrades() {
        if (amazon == null || google == null) {
            return
        }
        val hasPendingAmazon = !preferencesHelper.amazonPurchasedItem.isNullOrEmpty()
        val hasPendingGoogle = !preferencesHelper.purchasedItem.isNullOrEmpty()
        if (!hasPendingAmazon && !hasPendingGoogle) {
            return
        }
        val pkgManager: PackageManager = context.packageManager
        val installerPackageName =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pkgManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pkgManager.getInstallerPackageName(context.packageName)
            }
        if (installerPackageName != null && installerPackageName.startsWith("com.amazon")) {
            if (hasPendingAmazon) {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    PENDING_AMAZON_RECEIPT_WORKER_KEY,
                    ExistingWorkPolicy.REPLACE,
                    amazon,
                )
            }
        } else {
            if (hasPendingGoogle) {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    PENDING_GOGGLE_RECEIPT_WORKER_KEY,
                    ExistingWorkPolicy.REPLACE,
                    google,
                )
            }
        }
    }
}
