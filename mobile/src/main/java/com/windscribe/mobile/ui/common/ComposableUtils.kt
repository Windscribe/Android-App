package com.windscribe.mobile.ui.common

import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.windscribe.mobile.ui.AppStartActivity

@Composable
fun FragmentView(fragment: Fragment, activity: AppStartActivity) {
    val fragmentId = remember { View.generateViewId() }

    AndroidView(
        factory = {
            val container = FragmentContainerView(it).apply { id = fragmentId }

            val fm = activity.supportFragmentManager
            if (fm.findFragmentById(fragmentId) == null) {
                fm.beginTransaction()
                    .replace(fragmentId, fragment)
                    .commit()
            }

            container
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun theme(resource: Int): Color {
    val context = LocalContext.current
    val typedValue = remember { TypedValue() }
    context.theme.resolveAttribute(resource, typedValue, true)
    return colorResource(id = typedValue.resourceId)
}
