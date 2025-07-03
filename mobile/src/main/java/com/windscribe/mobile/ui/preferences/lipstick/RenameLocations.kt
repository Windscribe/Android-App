package com.windscribe.mobile.ui.preferences.lipstick

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.Description
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor

@Composable
fun RenameLocations(viewmodel: LipstickViewmodel? = null) {
    val context = LocalContext.current
    val importPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewmodel?.loadServerListFile(context, it) } }

    val exportPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            run {
                if (uri != null) {
                    viewmodel?.exportServerListFile(context, uri)
                }
            }
        }

    Column{
        Header()
        Spacer(modifier = Modifier.height(1.dp))
        Section(com.windscribe.vpn.R.string.export_locations, "Export server list file.") {
            exportPickerLauncher.launch("locations.json")
        }
        Spacer(modifier = Modifier.height(1.dp))
        Section(com.windscribe.vpn.R.string.import_locations, "Import server list file.") {
            importPickerLauncher.launch(arrayOf("*/*"))
        }
        Spacer(modifier = Modifier.height(1.dp))
        Section(com.windscribe.vpn.R.string.reset, "Reset custom server list file.", shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)) {
            viewmodel?.onResetClick()
        }
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.fillMaxWidth().background(
        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,) {
            Image(
                painterResource(R.drawable.ic_sound),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                stringResource(com.windscribe.vpn.R.string.renamed_location),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primaryTextColor
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Description(stringResource(com.windscribe.vpn.R.string.renamed_location_description))
    }
}

@Composable
private fun Section(title: Int, description: String, shape: RoundedCornerShape = RoundedCornerShape(0.dp), onClick: () -> Unit) {
    Row(modifier =  Modifier.background(
        MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
        shape = shape
    ).hapticClickable { onClick() }.padding(16.dp)) {
        Text(
            stringResource(title),
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primaryTextColor
        )
        Spacer(modifier = Modifier.weight(1.0f))
        Icon(
            painter = painterResource(R.drawable.ic_forward_arrow_white),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.40f)
        )
    }
}

@Composable
@MultiDevicePreview
private fun AppCustomBackgroundPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        RenameLocations()
    }
}