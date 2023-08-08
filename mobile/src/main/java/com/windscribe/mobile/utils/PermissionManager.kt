package com.windscribe.mobile.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResultListener
import com.windscribe.mobile.dialogs.BackgroundLocationPermissionDialog
import com.windscribe.mobile.dialogs.ForegroundLocationPermissionDialog
import com.windscribe.mobile.dialogs.LocationPermissionMissingDialog
import com.windscribe.mobile.utils.PermissionManagerImpl.Companion.disabledFeatureTag
import com.windscribe.mobile.utils.PermissionManagerImpl.Companion.okButtonKey
import com.windscribe.mobile.utils.PermissionManagerImpl.Companion.rationaleTag
import com.windscribe.mobile.utils.PermissionManagerImpl.Companion.resultKey

/**
 * Build permission request for each permission
 * and register in onCreate of activity.
 */
data class PermissionRequest(val activity: AppCompatActivity, val permission: String, val rationaleDialog: DialogFragment, val disabledFeatureDialog: DialogFragment) {
    var callback: ((Boolean) -> Unit)? = null
    var launcher: ActivityResultLauncher<String>? = null
    fun isGranted(context: Context): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Requests given permission and shows rational dialog if required.
     */
    fun request() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            showRationale(activity) { userAccepted ->
                if (userAccepted) {
                    launcher?.launch(permission)
                } else {
                    callback?.let { it -> it(false) }
                }
            }
        } else {
            launcher?.launch(permission)
        }
    }

    /**
     * called when Permission result is returned.
     * shows disabled feature dialog when denied.
     */
    fun permissionResultReceived(granted: Boolean) {
        if (granted) {
            callback?.let { it -> it(true) }
        } else {
            showDisabledFeatureDialog(activity) { goToSettings ->
                if (goToSettings) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    if (intent.resolveActivity(activity.packageManager) != null) {
                        activity.startActivity(intent)
                    }
                } else {
                    callback?.let { it(false) }
                }
            }
        }
    }

    private fun showRationale(activity: AppCompatActivity, callback: (Boolean) -> Unit) {
        if (activity.supportFragmentManager.findFragmentByTag(rationaleTag) != null) {
            callback(false)
            return
        }
        activity.runOnUiThread {
            kotlin.runCatching {
                rationaleDialog.showNow(activity.supportFragmentManager, rationaleTag)
                rationaleDialog.setFragmentResultListener(resultKey) { _, bundle ->
                    callback(bundle.containsKey(okButtonKey))
                }
            }
        }
    }

    private fun showDisabledFeatureDialog(activity: AppCompatActivity, callback: (Boolean) -> Unit) {
        if (activity.supportFragmentManager.findFragmentByTag(disabledFeatureTag) != null) {
            callback(false)
            return
        }
        activity.runOnUiThread {
            kotlin.runCatching {
                disabledFeatureDialog.showNow(activity.supportFragmentManager, rationaleTag)
                disabledFeatureDialog.setFragmentResultListener(resultKey) { _, bundle ->
                    callback(bundle.containsKey(okButtonKey))
                }
            }
        }
    }
}

interface PermissionManager {
    fun register(activity: AppCompatActivity)
    fun withForegroundLocationPermission(callback: (error: String?) -> Unit)
    fun withBackgroundLocationPermission(callback: (error: String?) -> Unit)
    fun isBackgroundPermissionGranted(): Boolean
}

class PermissionManagerImpl(private val activity: AppCompatActivity) : PermissionManager {
    companion object {
        const val resultKey = "resultKey"
        const val okButtonKey = "okButtonKey"
        const val rationaleTag = "RationalTag"
        const val disabledFeatureTag = "DisabledFeatureTag"
    }

    private lateinit var foregroundPermissionRequest: PermissionRequest
    private lateinit var backgroundPermissionRequest: PermissionRequest

    override fun isBackgroundPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ::backgroundPermissionRequest.isInitialized && backgroundPermissionRequest.isGranted(activity)
        }
        return true
    }

    /**
     * Register activity for result callbacks for each permission. Must be called from onCreate of activity.
     */
    override fun register(activity: AppCompatActivity) {
        foregroundPermissionRequest = PermissionRequest(activity, Manifest.permission.ACCESS_FINE_LOCATION, ForegroundLocationPermissionDialog(), LocationPermissionMissingDialog())
        val foregroundLocationPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            foregroundPermissionRequest.permissionResultReceived(permission)
        }
        foregroundPermissionRequest.launcher = foregroundLocationPermissionLauncher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionRequest = PermissionRequest(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION, BackgroundLocationPermissionDialog(), LocationPermissionMissingDialog())
            val backgroundLocationPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
                backgroundPermissionRequest.permissionResultReceived(permission)
            }
            backgroundPermissionRequest.launcher = backgroundLocationPermissionLauncher
        }
    }

    /**
     * Requests background location permission.
     * if granted callback is called with no error.
     */
    private fun askForBackgroundLocationPermission(callback: (error: String?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionRequest.callback = { granted ->
                if (granted) {
                    callback(null)
                } else {
                    callback("Background location permission denied")
                }
            }
            backgroundPermissionRequest.request()
        } else {
            callback(null)
        }
    }

    /**
     * Requests foreground location permission.
     * if granted callback is called with no error.
     */
    override fun withForegroundLocationPermission(callback: (error: String?) -> Unit) {
        foregroundPermissionRequest.callback = { granted ->
            if (granted) {
                callback(null)
            } else {
                callback("Fine location permission denied.")
            }
        }
        foregroundPermissionRequest.request()
    }

    /**
     * Requests foreground and background location permission.
     * if granted callback is called with no error.
     */
    override fun withBackgroundLocationPermission(callback: (error: String?) -> Unit) {
        val backgroundPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionRequest.isGranted(activity)
        } else {
            true
        }
        if (foregroundPermissionRequest.isGranted(activity) && backgroundPermissionGranted) {
            callback(null)
        } else if (foregroundPermissionRequest.isGranted(activity)) {
            askForBackgroundLocationPermission(callback)
        } else {
            withForegroundLocationPermission {
                if (it == null) {
                    askForBackgroundLocationPermission(callback)
                } else {
                    callback(it)
                }
            }
        }
    }

}

