package com.windscribe.mobile.lipstick

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.windscribe.mobile.R
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.font12
import com.windscribe.mobile.view.theme.font16

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

    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .background(color = Color(0XFF1E2937), shape = RoundedCornerShape(16.dp))
            .zIndex(10.0f)
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Image(
                    painterResource(R.drawable.ic_rename_location),
                    contentDescription = "Rename server locations."
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    stringResource(R.string.renamed_location),
                    style = font16,
                    color = AppColors.white
                )
                Spacer(modifier = Modifier.weight(1.0f))
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = AppColors.white5
        )
        Section(R.string.export_locations, "Export server list file.") {
            exportPickerLauncher.launch("locations.json")
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = AppColors.white5
        )
        Section(R.string.import_locations, "Import server list file.") {
            importPickerLauncher.launch(arrayOf("*/*"))
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = AppColors.white5
        )
        Section(R.string.reset, "Reset custom server list file.") {
            viewmodel?.onResetClick()
        }
    }
    Box(
        modifier = Modifier
            .offset(y = (-16).dp)
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 0.8.dp)
            .border(
                width = 1.dp,
                color = AppColors.white5,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.renamed_location_description),
                modifier = Modifier.padding(12.dp),
                style = font12,
                color = AppColors.white50
            )
        }
    }
}

@Composable
private fun Section(title: Int, description: String, onClick: () -> Unit) {
    Row(modifier = Modifier
        .padding(16.dp)
        .clickable { onClick() }) {
        Text(stringResource(title), style = font12, color = AppColors.white)
        Spacer(modifier = Modifier.weight(1.0f))
        Icon(
            painter = painterResource(R.drawable.ic_forward_arrow_white),
            contentDescription = description,
            tint = Color.White
        )
    }
}

@Composable
@Preview(showBackground = true)
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