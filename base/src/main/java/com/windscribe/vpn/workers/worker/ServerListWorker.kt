package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import org.slf4j.LoggerFactory

class ServerListWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val logger = LoggerFactory.getLogger("worker")

    @Inject
    lateinit var serverListRepository: ServerListRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var mainScope: CoroutineScope

    init {
        appContext.applicationComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        if (!userRepository.loggedIn()) return Result.failure()

        return runCatching {
            // First update
            serverListRepository.update()
            // Check if we need to reload with different country code
            val reloadServerList = serverListRepository.globalServerList &&
                appContext.appLifeCycleObserver.overriddenCountryCode != null &&
                appContext.appLifeCycleObserver.overriddenCountryCode != "ZZ"
            if (reloadServerList) {
                // Reload server list with country override
                serverListRepository.update()
            }
            // Load the updated server list
            serverListRepository.load()
            logger.debug("Successfully updated server list. Global Server list: ${serverListRepository.globalServerList} CountryOverride: ${appContext.appLifeCycleObserver.overriddenCountryCode}")
            mainScope.launch {
                handleLocationUpdate()
            }
            Result.success()
        }.getOrElse { error ->
            logger.debug("Failed to update server list: $error")
            Result.failure()
        }
    }

    private suspend fun handleLocationUpdate() {
        try {
            val previousLocation = locationRepository.selectedCity.value
            val updatedLocation = locationRepository.updateLocation()
            if (updatedLocation != previousLocation && preferencesHelper.globalUserConnectionPreference) {
                logger.debug("Last selected location is changed Now Reconnecting")
                locationRepository.setSelectedCity(updatedLocation)
                vpnController.connectAsync()
            } else if (preferencesHelper.globalUserConnectionPreference && !locationRepository.isNodeAvailable()) {
                logger.debug("Missing currently connected node Now Reconnecting to same location.")
                vpnController.connectAsync()
            }
        } catch (e: Exception) {
            logger.debug("Failed to update last selected location.")
        }
    }
}