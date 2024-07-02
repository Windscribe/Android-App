package com.windscribe.vpn.state

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.windscribe.vpn.R
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.backend.utils.VPNPermissionActivity
import com.windscribe.vpn.commonutils.Ext.toResult
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class DynamicShortcutManager(private val context: Context, private val scope: CoroutineScope, private val vpnStateManager: VPNConnectionStateManager, private val locationRepository: LocationRepository) {
    companion object {
        const val QUICK_CONNECT_ID = "ws_quick_connect"
        const val QUICK_DISCONNECT_ID = "ws_quick_disconnect"
        const val QUICK_CONNECT_ACTION = "ws_quick_connect_action"
        const val QUICK_DISCONNECT_ACTION = "ws_quick_disconnect_action"
        const val QUICK_CONNECT_ACTION_KEY = "ws_quick_connect_action_key"
        const val RECENT_CONNECT_ACTION = "ws_recent_connect_action"
        const val RECENT_CONNECT_ID = "ws_recent_connect_id"
        const val RECENT_COUNTRY_CODE_KEY = "ws_country_code"
    }

    private val logger = LoggerFactory.getLogger("shortcut_m")

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
                        addShortcut(QUICK_CONNECT_ID, context.getString(R.string.disconnect), R.drawable.quick_disconnect, intent)
                    }

                    VPNState.Status.Connected -> {
                        intent.putExtra(QUICK_CONNECT_ACTION_KEY, QUICK_DISCONNECT_ACTION)
                        addShortcut(QUICK_CONNECT_ID, context.getString(R.string.disconnect), R.drawable.quick_disconnect, intent)
                    }

                    else -> {
                        intent.putExtra(QUICK_CONNECT_ACTION_KEY, QUICK_CONNECT_ACTION)
                        addShortcut(QUICK_CONNECT_ID, context.getString(R.string.quick_connect), R.drawable.quick_connect_disconnected, intent)
                    }
                }
            }
        }
    }

    private fun addShortcut(id: String, text: String, icon: Int, intent: Intent?) {
        val shortcutBuilder = ShortcutInfoCompat.Builder(context, id)
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
            locationRepository.selectedCity.onEach { delay(1000) }.mapLatest {
                Util.getSavedLocation().toResult()
            }.mapNotNull { it }.collectLatest {
                if (it.isSuccess) {
                    addRecentShortcut(it.getOrThrow())
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun addRecentShortcut(selectedLocation: LastSelectedLocation) {
        val recentShortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
                .filter { it.id.startsWith("ws_recent") }
        val shortcutID = "ws_recent_${selectedLocation.cityId}"
        val intent = Intent(context, VPNPermissionActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(QUICK_CONNECT_ACTION_KEY, RECENT_CONNECT_ACTION)
            putExtra(RECENT_CONNECT_ID, selectedLocation.cityId)
        }
        val latestShortcut = ShortcutInfoCompat.Builder(context, shortcutID)
                .setShortLabel("${selectedLocation.nodeName} ${selectedLocation.nickName}")
                .setIcon(IconCompat.createWithResource(context, FlagIconResource.getSmallFlag(selectedLocation.countryCode)))
                .setExtras(PersistableBundle().apply { this.putString(RECENT_COUNTRY_CODE_KEY, selectedLocation.countryCode) })
                .setIntent(intent)
                .build()
        val shortcutsToAdd = recentShortcuts.toMutableList()
        shortcutsToAdd.add(0, latestShortcut)
        shortcutsToAdd.forEachIndexed { index, v ->
            val countryCode = v.extras?.getString(RECENT_COUNTRY_CODE_KEY)
            if (index > 2) {
                Log.i("stu", "reMOVING $countryCode")
                removeShortCut(v.id)
                return
            }
            Log.i("stu", "$countryCode ${index + 2}")
            val updated = ShortcutInfoCompat.Builder(context, v.id)
                    .setShortLabel("${v.shortLabel}")
                    .setIcon(IconCompat.createWithResource(context, FlagIconResource.getSmallFlag(countryCode)))
                    .setIntent(v.intent)
                    .setExtras(v.extras ?: PersistableBundle.EMPTY)
                    .setRank(index + 2)
                    .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, updated)
        }
    }


    private fun removeShortCut(id: String) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
    }
}