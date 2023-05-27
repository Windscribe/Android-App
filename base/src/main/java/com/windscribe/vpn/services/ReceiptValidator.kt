package com.windscribe.vpn.services

import android.content.Context
import android.content.pm.PackageManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.windscribe.vpn.workers.WindScribeWorkManager.Companion.PENDING_AMAZON_RECEIPT_WORKER_KEY
import com.windscribe.vpn.workers.WindScribeWorkManager.Companion.PENDING_GOGGLE_RECEIPT_WORKER_KEY

class ReceiptValidator(private val context: Context, private val amazon: OneTimeWorkRequest?= null, private val google: OneTimeWorkRequest? = null) {
    fun checkPendingAccountUpgrades() {
        if (amazon == null || google == null){
            return
        }
        val pkgManager: PackageManager = context.packageManager
        val installerPackageName = pkgManager.getInstallerPackageName(context.packageName)
        if (installerPackageName != null && installerPackageName.startsWith("com.amazon")) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                    PENDING_AMAZON_RECEIPT_WORKER_KEY,
                    ExistingWorkPolicy.REPLACE,
                    amazon
            )
        } else {
            WorkManager.getInstance(context).enqueueUniqueWork(
                    PENDING_GOGGLE_RECEIPT_WORKER_KEY,
                    ExistingWorkPolicy.REPLACE,
                    google
            )
        }
    }
}