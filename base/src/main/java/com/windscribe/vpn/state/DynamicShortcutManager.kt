package com.windscribe.vpn.state

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.windscribe.vpn.R
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.backend.utils.VPNPermissionActivity
import com.windscribe.vpn.commonutils.Ext.toResult
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

enum class LocationTypeInt() {
    City, Static, Custom
}
class DynamicShortcutManager(private val context: Context, private val scope: CoroutineScope, private val vpnStateManager: VPNConnectionStateManager, private val locationRepository: LocationRepository, private val db: LocalDbInterface) {
    companion object {
        const val QUICK_CONNECT_ID = "ws_quick_connect"
        const val QUICK_CONNECT_ACTION = "ws_quick_connect_action"
        const val QUICK_DISCONNECT_ACTION = "ws_quick_disconnect_action"
        const val QUICK_CONNECT_ACTION_KEY = "ws_quick_connect_action_key"
        const val RECENT_CONNECT_ACTION = "ws_recent_connect_action"
        const val RECENT_CONNECT_ID = "ws_recent_connect_id"
        const val RECENT_COUNTRY_CODE_KEY = "ws_country_code"
        const val RECENT_LOCATION_TYPE_INT = "ws_location_type_int"
        const val RECENT_DATE_KEY = "ws_recent_date_key"
    }

    private val logger = LoggerFactory.getLogger("shortcut")

    init {
        listenForVPNState()
        listenForSelectedLocationChange()
    }

    private fun listenForVPNState() {
        scope.launch {
            vpnStateManager.state.collectLatest {
                val intent = Intent(context, VPNPermissionActivity::class.java)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                when (it.status) {
                    VPNState.Status.Connecting -> {
                        intent.putExtra(QUICK_CONNECT_ACTION_KEY, QUICK_DISCONNECT_ACTION)
                        addShortcut(context.getString(R.string.disconnect), R.drawable.quick_disconnect, intent)
                    }

                    VPNState.Status.Connected -> {
                        intent.putExtra(QUICK_CONNECT_ACTION_KEY, QUICK_DISCONNECT_ACTION)
                        addShortcut(context.getString(R.string.disconnect), R.drawable.quick_disconnect, intent)
                    }

                    else -> {
                        intent.putExtra(QUICK_CONNECT_ACTION_KEY, QUICK_CONNECT_ACTION)
                        addShortcut(context.getString(R.string.quick_connect), R.drawable.quick_connect_disconnected, intent)
                    }
                }
            }
        }
    }

    private fun addShortcut(text: String, icon: Int, intent: Intent?) {
        val shortcutBuilder = ShortcutInfoCompat.Builder(context, QUICK_CONNECT_ID)
                .setShortLabel(text)
                .setIcon(IconCompat.createWithResource(context, icon))
                .setRank(1)
        if (intent != null) {
            shortcutBuilder.setIntent(intent)
        }
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcutBuilder.build())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun listenForSelectedLocationChange() {
        scope.launch {
            locationRepository.selectedCity.mapLatest { id ->
                return@mapLatest getLastSelectedLocation(id)
            }.mapNotNull { it }.collectLatest {
                if (it.isSuccess) {
                    addRecentShortcut(it.getOrThrow().first, it.getOrThrow().second)
                }
            }
        }
    }

    private suspend fun getLastSelectedLocation(id: Int): Result<Pair<LastSelectedLocation, LocationTypeInt>> {
        when (WindUtilities.getSourceTypeBlocking()) {
            SelectedLocationType.CityLocation -> {
                return db.getCityAndRegionByID(id).map { cityAndRegion ->
                    Pair(LastSelectedLocation(
                            cityAndRegion.city.id,
                            cityAndRegion.city.nodeName,
                            cityAndRegion.city.nickName,
                            cityAndRegion.region.countryCode,
                    ), LocationTypeInt.City)
                }.toResult()
            }

            SelectedLocationType.StaticIp -> {
                return db.getStaticRegionByID(id).map { staticRegion ->
                    Pair(LastSelectedLocation(id, staticRegion.cityName, staticRegion.staticIp, staticRegion.countryCode), LocationTypeInt.Static)
                }.toResult()
            }

            SelectedLocationType.CustomConfiguredProfile -> {
                return db.getConfigFile(id).map {
                    Pair(LastSelectedLocation(id, nickName = ""), LocationTypeInt.Custom)
                }.toResult()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun addRecentShortcut(selectedLocation: LastSelectedLocation, locationTypeInt: LocationTypeInt) {
        val recentShortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
                .filter { it.id.startsWith("ws_recent") }
        val shortcutID = "ws_recent_${selectedLocation.cityId}"
        val intent = Intent(context, VPNPermissionActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(QUICK_CONNECT_ACTION_KEY, RECENT_CONNECT_ACTION)
            putExtra(RECENT_CONNECT_ID, selectedLocation.cityId)
            putExtra(RECENT_LOCATION_TYPE_INT, locationTypeInt.ordinal)
        }
        val latestShortcut = ShortcutInfoCompat.Builder(context, shortcutID)
                .setShortLabel("${selectedLocation.nodeName} ${selectedLocation.nickName}")
                .setIcon(IconCompat.createWithResource(context, FlagIconResource.getSmallFlag(selectedLocation.countryCode)))
                .setExtras(PersistableBundle().apply {
                    this.putString(RECENT_COUNTRY_CODE_KEY, selectedLocation.countryCode)
                    this.putLong(RECENT_DATE_KEY, System.currentTimeMillis())
                    this.putInt(RECENT_LOCATION_TYPE_INT, locationTypeInt.ordinal)
                })
                .setIntent(intent)
                .build()
        val shortcutsToAdd = recentShortcuts.toMutableList()
        shortcutsToAdd.add(0, latestShortcut)
        shortcutsToAdd.distinctBy { it.shortLabel }.sortedByDescending {
            it.extras?.getLong(RECENT_DATE_KEY) ?: System.currentTimeMillis()
        }.forEachIndexed { index, v ->
            val countryCode = v.extras?.getString(RECENT_COUNTRY_CODE_KEY)
            if (index > 2) {
                logger.debug("Removing shortcut - ${v.shortLabel} at index $index")
                removeShortCut(v.id)
                return
            }
            val updated = ShortcutInfoCompat.Builder(context, v.id)
                    .setShortLabel("${v.shortLabel}")
                    .setIcon(IconCompat.createWithResource(context, FlagIconResource.getSmallFlag(countryCode)))
                    .setIntent(v.intent)
                    .setExtras(v.extras ?: PersistableBundle.EMPTY)
                    .setRank(index + 2)
                    .build()
            val ignored = ShortcutManagerCompat.pushDynamicShortcut(context, updated)
        }
    }


    private fun removeShortCut(id: String) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
    }
}