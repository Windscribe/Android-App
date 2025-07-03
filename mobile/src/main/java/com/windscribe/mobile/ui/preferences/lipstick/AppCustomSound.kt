package com.windscribe.mobile.ui.preferences.lipstick

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter.State.Empty.painter
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.Description
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownItem
import com.windscribe.mobile.ui.theme.backgroundColor
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor

@Composable
fun AppCustomSound(viewmodel: LipstickViewmodel? = null) {
    Column{
        Header()
        Spacer(modifier = Modifier.height(1.dp))
        WhenDisconnectedSection(viewmodel)
        Spacer(modifier = Modifier.height(1.dp))
        WhenConnectedSection(viewmodel)
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
                stringResource(com.windscribe.vpn.R.string.sound_notifications),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primaryTextColor
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Description(stringResource(com.windscribe.vpn.R.string.sound_notifications_description))
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
    DropdownSection(
        title = stringResource(com.windscribe.vpn.R.string.when_disconnected),
        displayValue = stringResource(item?.value?.title ?: com.windscribe.vpn.R.string.None),
        isDropdownExpanded = expandedMain,
        onDropdownClick = { expandedMain.value = !expandedMain.value },
        dropdownItems = LookAndFeelHelper.getSoundOptions(),
        onItemSelected = { viewmodel?.onWhenDisconnectedSoundItemSelected(it) },
        shape = RoundedCornerShape(0.dp),
        extraContent = {
            val title = bundledItem?.value?.label?.ifBlank {
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
                        title ?: "",
                        style = font16.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cm_icon),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primaryTextColor
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
                        style = font16,
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_forward_arrow_white),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primaryTextColor
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
    DropdownSection(
        title = stringResource(com.windscribe.vpn.R.string.when_connected),
        displayValue = stringResource(item?.value?.title ?: com.windscribe.vpn.R.string.None),
        isDropdownExpanded = expandedMain,
        onDropdownClick = { expandedMain.value = !expandedMain.value },
        dropdownItems = LookAndFeelHelper.getSoundOptions(),
        onItemSelected = { viewmodel?.onWhenConnectedSoundItemSelected(it) },
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        extraContent = {
            val title = bundledItem?.value?.label?.ifBlank {
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
                        title ?: "",
                        style = font16,
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cm_icon),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primaryTextColor
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
                        style = font16,
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_forward_arrow_white),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primaryTextColor
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
    Box(
        Modifier.background(
            MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
            shape = shape
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column {
                Text(title, style = font16.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.primaryTextColor)
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
                Text(
                    displayValue,
                    style = font16,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_cm_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.primaryTextColor
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
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryTextColor)
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
                    color = MaterialTheme.colorScheme.backgroundColor,
                    style = font16,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            })
        }
    }
}

@Composable
@MultiDevicePreview
private fun AppCustomSoundPreview() {
    PreviewWithNav {
        AppCustomSound()
    }
}