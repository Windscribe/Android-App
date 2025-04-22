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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.windscribe.mobile.R
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.AppColors.homeBackground
import com.windscribe.mobile.view.theme.font12
import com.windscribe.mobile.view.theme.font16

@Composable
fun AppCustomSound(viewmodel: LipstickViewmodel? = null) {
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
                    painterResource(R.drawable.ic_sound),
                    contentDescription = "App sound notifications."
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    stringResource(R.string.sound_notifications),
                    style = font16,
                    color = AppColors.white
                )
                Spacer(modifier = Modifier.weight(1.0f))
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = AppColors.white5)
        WhenDisconnectedSection(viewmodel)
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = AppColors.white5)
        WhenConnectedSection(viewmodel)
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
                stringResource(R.string.sound_notifications_description),
                modifier = Modifier.padding(12.dp),
                style = font12,
                color = AppColors.white50
            )
        }
    }
}

@Composable
private fun WhenDisconnectedSection(viewmodel: LipstickViewmodel?) {
    val item = viewmodel?.whenDisconnectedSoundItem?.collectAsState()
    val bundledItem = viewmodel?.bundledDisconnectedSoundItem?.collectAsState()
    val customItem = viewmodel?.customDisconnectedSoundItem?.collectAsState()
    val context = LocalContext.current
    val expandedMain = remember { mutableStateOf(false) }
    val expandedBundled = remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewmodel?.loadDisconnectedCustomSound(context, it) } }
    if (bundledItem == null) return
    DropdownSection(
        title = stringResource(R.string.when_disconnected),
        displayValue = stringResource(item?.value?.title ?: R.string.None),
        isDropdownExpanded = expandedMain,
        onDropdownClick = { expandedMain.value = !expandedMain.value },
        dropdownItems = LookAndFeelHelper.getSoundOptions(),
        onItemSelected = { viewmodel.onWhenDisconnectedSoundItemSelected(it) },
        extraContent = {
            val title = bundledItem.value.label.ifBlank {
                stringResource(bundledItem.value.title)
            }
            if (item?.value?.id == 2) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { expandedBundled.value = !expandedBundled.value },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = font12,
                        color = AppColors.white50
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cm_icon),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AppColors.white
                    )
                }
                if (expandedBundled.value) {
                    DropDownItems(
                        expanded = expandedBundled,
                        items = LookAndFeelHelper.getBundledSoundOptions()
                    ) {
                        viewmodel.onDisconnectedBundledSoundItemSelected(it)
                        expandedBundled.value = false
                    }
                }
            }

            if (item?.value?.id == 3) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { filePickerLauncher.launch(arrayOf("*/*")) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        customItem?.value ?: "No selection",
                        style = font12,
                        color = AppColors.white50
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_forward_arrow_white),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AppColors.white
                    )
                }
            }
        }
    )
}

@Composable
private fun WhenConnectedSection(viewmodel: LipstickViewmodel?) {
    val item = viewmodel?.whenConnectedSoundItem?.collectAsState()
    val bundledItem = viewmodel?.bundledConnectedSoundItem?.collectAsState()
    val customItem = viewmodel?.customConnectedSoundItem?.collectAsState()
    val context = LocalContext.current
    val expandedMain = remember { mutableStateOf(false) }
    val expandedBundled = remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewmodel?.loadConnectedCustomSound(context, it) } }
    if (bundledItem == null) return
    DropdownSection(
        title = stringResource(R.string.when_connected),
        displayValue = stringResource(item?.value?.title ?: R.string.None),
        isDropdownExpanded = expandedMain,
        onDropdownClick = { expandedMain.value = !expandedMain.value },
        dropdownItems = LookAndFeelHelper.getSoundOptions(),
        onItemSelected = { viewmodel.onWhenConnectedSoundItemSelected(it) },
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        extraContent = {
            val title = bundledItem.value.label.ifBlank {
                stringResource(bundledItem.value.title)
            }
            if (item?.value?.id == 2) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { expandedBundled.value = !expandedBundled.value },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = font12,
                        color = AppColors.white50
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cm_icon),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AppColors.white
                    )
                }
                if (expandedBundled.value) {
                    DropDownItems(
                        expanded = expandedBundled,
                        items = LookAndFeelHelper.getBundledSoundOptions()
                    ) {
                        viewmodel.onConnectedBundledSoundItemSelected(it)
                        expandedBundled.value = false
                    }
                }
            }

            if (item?.value?.id == 3) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { filePickerLauncher.launch(arrayOf("*/*")) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        customItem?.value ?: "No selection",
                        style = font12,
                        color = AppColors.white50
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_forward_arrow_white),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AppColors.white
                    )
                }
            }
        }
    )
}

@Composable
private fun DropdownSection(
    title: String,
    displayValue: String,
    isDropdownExpanded: MutableState<Boolean>,
    onDropdownClick: () -> Unit,
    dropdownItems: List<DropDownItem>,
    onItemSelected: (DropDownItem) -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(0.dp),
    extraContent: (@Composable () -> Unit)? = null
) {
    Box(Modifier.background(Color(0XFF1E2937), shape = shape)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column {
                Text(title, style = font12, color = AppColors.white)
                extraContent?.invoke()
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(24.dp)
                    .clickable {
                        onDropdownClick()
                    }) {
                Text(displayValue, style = font12, color = AppColors.white)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_cm_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp),
                    tint = AppColors.white
                )
            }
        }

        if (isDropdownExpanded.value) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp)
            ) {
                DropDownItems(
                    expanded = isDropdownExpanded,
                    items = dropdownItems,
                    onItemClick = {
                        onItemSelected(it)
                        isDropdownExpanded.value = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DropDownItems(
    expanded: MutableState<Boolean>,
    items: List<DropDownItem>,
    onItemClick: (DropDownItem) -> Unit
) {
    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false },
        modifier = Modifier.background(AppColors.white)
    ) {
        items.forEach {
            val title = it.label.ifBlank {
                stringResource(it.title)
            }
            DropdownMenuItem(onClick = {
                onItemClick(it)
            }, text = {
                Text(
                    title,
                    color = homeBackground,
                    style = font16,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            })
        }
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
        AppCustomSound()
    }
}