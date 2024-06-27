package com.windscribe.vpn.state

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.windscribe.vpn.R
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.VPNPermissionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class DynamicShortcutManager(private val context: Context, private val scope: CoroutineScope, private val vpnStateManager: VPNConnectionStateManager) {
    companion object {
        const val QUICK_CONNECT_ID = "ws_quick_connect"
        const val QUICK_DISCONNECT_ID = "ws_quick_disconnect"
        const val QUICK_CONNECT_ACTION = "ws_quick_connect_action"
        const val QUICK_DISCONNECT_ACTION = "ws_quick_disconnect_action"
    }
    private val logger = LoggerFactory.getLogger("shortcut_m")
    init {
        listenForVPNState()
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
                      intent.putExtra("action", QUICK_DISCONNECT_ACTION)
                      addShortcut(QUICK_CONNECT_ID, context.getString(R.string.quick_connect), R.drawable.quick_connect_connecting, intent, rank = 1)
                      addShortcut(QUICK_DISCONNECT_ID, context.getString(R.string.disconnect), R.drawable.quick_disconnect, intent, rank = 1)
                  }
                  VPNState.Status.Connected -> {
                      intent.putExtra("action", QUICK_DISCONNECT_ACTION)
                      addShortcut(QUICK_CONNECT_ID, context.getString(R.string.quick_connect), R.drawable.quick_connect_connected, intent, rank = 1)
                      addShortcut(QUICK_DISCONNECT_ID, context.getString(R.string.disconnect), R.drawable.quick_disconnect, intent)
                  }
                  else -> {
                      intent.putExtra("action", QUICK_CONNECT_ACTION)
                      addShortcut(QUICK_CONNECT_ID, context.getString(R.string.quick_connect), R.drawable.quick_connect_disconnected, intent, rank = 1)
                      removeShortCut(QUICK_DISCONNECT_ID)
                  }
              }
            }
        }
    }

    private fun addShortcut(id: String, text: String, icon: Int, intent: Intent?, rank: Int = 2){

        val shortcutBuilder = ShortcutInfoCompat.Builder(context, id)
                .setShortLabel(text)
                .setIcon(IconCompat.createWithResource(context, icon))
                .setRank(rank)
        if(intent != null) {
            shortcutBuilder.setIntent(intent)
        }
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcutBuilder.build())
    }

    private fun removeShortCut(id: String) {
       ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
    }
}