/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Constraints.Builder
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.worker.CredentialsWorker
import com.windscribe.vpn.workers.worker.NotificationWorker
import com.windscribe.vpn.workers.worker.RobertSyncWorker
import com.windscribe.vpn.workers.worker.ServerListWorker
import com.windscribe.vpn.workers.worker.SessionWorker
import com.windscribe.vpn.workers.worker.StaticIpWorker
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.ByteString.Companion.encodeUtf8
import org.slf4j.LoggerFactory

/**
 * Handles one off and periodic tasks for app.
 */
@Singleton
class WindScribeWorkManager(private val context: Context, private val scope: CoroutineScope, private val vpnConnectionStateManager: VPNConnectionStateManager, val preferencesHelper: PreferencesHelper) {
    var foregroundSessionUpdateJob: Job? = null
    private var logger = LoggerFactory.getLogger("work_manager")
    fun onAppStart() {
        if (preferencesHelper.sessionHash == null) return
        logger.debug("Starting one time work requests")
        // One time
        val data = Data.Builder().putBoolean("forceUpdate", true).build()
        updateSession(data)
        // Hourly
        logger.debug("Starting hourly work requests")
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOTIFICATION_HOURLY_WORKER_KEY, REPLACE, createPeriodicWorkerRequest(NotificationWorker::class.java, HOURS))
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(SESSION_HOURLY_WORKER_KEY, REPLACE, createPeriodicWorkerRequest(SessionWorker::class.java, HOURS))
        // Every day
        logger.debug("Starting every day work requests")
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOTIFICATION_DAY_WORKER_KEY, REPLACE, createPeriodicWorkerRequest(NotificationWorker::class.java, DAYS))
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(SESSION_DAY_WORKER_KEY, REPLACE, createPeriodicWorkerRequest(SessionWorker::class.java, DAYS))
    }

    fun onAppMovedToForeground() {
        if (preferencesHelper.sessionHash == null) return
        logger.debug("Starting foreground session update")
        WorkManager.getInstance(context).enqueueUniqueWork(SERVER_LIST_WORKER_KEY, ExistingWorkPolicy.REPLACE, createOneTimeWorkerRequest(SessionWorker::class.java))
        foregroundSessionUpdateJob = scope.launch {
            while (true) {
                delay(1000 * 60)
                logger.debug("Starting foreground session update")
                WorkManager.getInstance(context).enqueueUniqueWork(SERVER_LIST_WORKER_KEY, ExistingWorkPolicy.REPLACE, createOneTimeWorkerRequest(SessionWorker::class.java))
            }
        }
    }

    fun onAppMovedToBackground() {
        logger.debug("Removed foreground session update.")
        foregroundSessionUpdateJob?.cancel()
    }

    private fun createOneTimeWorkerRequest(workerClass: Class<out ListenableWorker>, data: Data = Data.EMPTY): OneTimeWorkRequest {
        return OneTimeWorkRequest.Builder(workerClass)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, SECONDS)
                .setInputData(data)
                .setConstraints(constraints)
                .build()
    }

    private fun createPeriodicWorkerRequest(workerClass: Class<out ListenableWorker>, timeUnit: TimeUnit, data: Data = Data.EMPTY): PeriodicWorkRequest {
        return PeriodicWorkRequest.Builder(workerClass, 1, timeUnit)
                .addTag(workerClass.name.encodeUtf8().hex())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, MINUTES)
                .setInputData(data)
                .setInitialDelay(1, timeUnit)
                .setConstraints(constraints)
                .build()
    }

    /**
     * Called when robert rules changes.
     */
    fun updateRobertRules() {
        val robertSyncRequest = createOneTimeWorkerRequest(RobertSyncWorker::class.java)
        WorkManager.getInstance(context).enqueue(robertSyncRequest)
    }

    fun updateServerList() {
        WorkManager.getInstance(context).enqueueUniqueWork(SERVER_LIST_WORKER_KEY, ExistingWorkPolicy.REPLACE, createOneTimeWorkerRequest(ServerListWorker::class.java))
    }

    fun updateCredentialsUpdate() {
        WorkManager.getInstance(context).enqueueUniqueWork(CREDENTIALS_WORKER_KEY, ExistingWorkPolicy.REPLACE, createOneTimeWorkerRequest(CredentialsWorker::class.java))
    }

    fun updateStaticIpList() {
        WorkManager.getInstance(context).enqueueUniqueWork(STATIC_IP_WORKER_KEY, ExistingWorkPolicy.REPLACE, createOneTimeWorkerRequest(StaticIpWorker::class.java))
    }

    fun updateNotifications() {
        WorkManager.getInstance(context).enqueueUniqueWork(NOTIFICATION_WORKER_KEY, ExistingWorkPolicy.REPLACE, createOneTimeWorkerRequest(NotificationWorker::class.java))
    }

    fun updateSession(inputData: Data = Data.EMPTY) {
        WorkManager.getInstance(context).enqueueUniqueWork(SESSION_WORKER_KEY, ExistingWorkPolicy.REPLACE, createOneTimeWorkerRequest(SessionWorker::class.java, inputData))
    }

    private val constraints: Constraints
        get() = Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

    companion object {
        const val NOTIFICATION_WORKER_KEY = "com.windscribe.vpn.notification_worker"
        const val NOTIFICATION_HOURLY_WORKER_KEY = "com.windscribe.vpn.notification_hourly_worker"
        const val NOTIFICATION_DAY_WORKER_KEY = "com.windscribe.vpn.notification_day_worker"
        const val SESSION_WORKER_KEY = "com.windscribe.vpn.session_worker"
        const val SESSION_HOURLY_WORKER_KEY = "com.windscribe.vpn.session_hourly_worker"
        const val SESSION_DAY_WORKER_KEY = "com.windscribe.vpn.session_day_worker"
        const val SERVER_LIST_WORKER_KEY = "com.windscribe.vpn.server_list"
        const val STATIC_IP_WORKER_KEY = "com.windscribe.vpn.static_ip"
        const val CREDENTIALS_WORKER_KEY = "com.windscribe.vpn.credentials"
    }
}
