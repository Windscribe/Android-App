package com.windscribe.vpn.state

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.windscribe.vpn.R
import com.windscribe.vpn.backend.VPNState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class DynamicShortcutManager(private val context: Context, private val scope: CoroutineScope, private val vpnStateManager: VPNConnectionStateManager) {
    companion object {
        const val QUICK_CONNECT_ID = "ws_quick_connect"
        const val QUICK_DISCONNECT_ID = "ws_quick_disconnect"
    }
    private val logger = LoggerFactory.getLogger("shortcut_m")
    init {
        listenForVPNState()
    }

    private fun listenForVPNState() {
        scope.launch {
            vpnStateManager.state.collectLatest {
              when (it.status) {
                  VPNState.Status.Connecting -> {
                      addShortcut(QUICK_CONNECT_ID, context.getString(R.string.quick_connect), R.drawable.quick_connect_connecting, rank = 1)
                      addShortcut(QUICK_DISCONNECT_ID, context.getString(R.string.disconnect), R.drawable.quick_disconnect, rank = 1)
                  }
                  VPNState.Status.Connected -> {
                      addShortcut(QUICK_CONNECT_ID, context.getString(R.string.quick_connect), R.drawable.quick_connect_connected, rank = 1)
                      addShortcut(QUICK_DISCONNECT_ID, context.getString(R.string.disconnect), R.drawable.quick_disconnect)
                  }
                  else -> {
                      addShortcut(QUICK_CONNECT_ID, context.getString(R.string.quick_connect), R.drawable.quick_connect_disconnected, rank = 1)
                      removeShortCut(QUICK_DISCONNECT_ID)
                  }
              }
            }
        }
    }

    private fun addShortcut(id: String, text: String, icon: Int, rank: Int = 2){
        val shortcut = ShortcutInfoCompat.Builder(context, id)
                .setShortLabel(text)
                .setIcon(IconCompat.createWithResource(context, icon))
                .setRank(rank)
                .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mysite.example.com/")))
                .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    private fun removeShortCut(id: String) {
       ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
    }
}