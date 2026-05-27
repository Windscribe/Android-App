package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

@HiltWorker
class ServerListWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val serverListRepository: ServerListRepository,
        private val userRepository: UserRepository,
        private val locationRepository: LocationRepository,
        private val preferencesHelper: PreferencesHelper,
        private val vpnController: WindVpnController,
        private val mainScope: CoroutineScope,
    ) : CoroutineWorker(context, workerParams) {
        private val logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result {
            if (!userRepository.loggedIn()) return Result.failure()

            return runCatching {
                serverListRepository.update()
                logger.debug("Successfully updated server list.")
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
