package com.windscribe.mobile.ui.popup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.mobile.ui.DialogData
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.theme
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font22

/**
 * Stateful entry point. The [AppStartActivityViewModel] is activity-scoped (it carries the dialog
 * data and callback set by the hosting activity), so it is passed in rather than resolved via
 * `hiltViewModel()`. The dialog data is read here and rendering is delegated to
 * [OverlayDialogContent].
 */
@Composable
fun OverlayDialogScreen(appStartActivityViewModel: AppStartActivityViewModel) {
    val dialogData = appStartActivityViewModel.dialogData
    OverlayDialogContent(
        dialogData = dialogData,
        onConfirm = { appStartActivityViewModel.dialogCallback?.onConfirm() },
        onDismiss = { appStartActivityViewModel.dialogCallback?.onDismiss() },
    )
}

/**
 * Stateless UI. Everything it needs is passed in, so it renders identically in the app and in
 * `@Preview`. This is the composable previews target.
 */
@Composable
fun OverlayDialogContent(
    dialogData: DialogData?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isIconAtBottom = dialogData?.iconAtBottom == true
    val dialogIcon = dialogData?.icon ?: R.drawable.ic_warning_icon

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(theme(R.attr.wdPrimaryInvertedColor))
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        // Close button positioned absolutely at top-right
        Image(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            colorFilter = ColorFilter.tint(theme(R.attr.wdPrimaryColor)),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
                    .size(24.dp)
                    .clickable { onDismiss() },
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =
                if (isIconAtBottom) {
                    Arrangement.Top
                } else {
                    Arrangement.Center
                },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, end = 32.dp, bottom = 32.dp, top = 72.dp),
        ) {
            if (!isIconAtBottom) {
                Spacer(modifier = Modifier.weight(0.5f))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Show icon at top if not iconAtBottom
            if (!isIconAtBottom) {
                Image(
                    painter = painterResource(dialogIcon),
                    contentDescription = "Attention",
                    colorFilter = ColorFilter.tint(theme(R.attr.wdPrimaryColor)),
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(153.dp)
                            .padding(vertical = 16.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = dialogData?.title ?: "",
                style = font22,
                color = theme(R.attr.wdPrimaryColor),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Scrollable description container
            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = dialogData?.description ?: "",
                    style = font14,
                    color = theme(R.attr.wdPrimaryColor),
                    textAlign = if (isIconAtBottom) TextAlign.Start else TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show icon at bottom if iconAtBottom
            if (isIconAtBottom) {
                Image(
                    painter = painterResource(dialogIcon),
                    contentDescription = "Attention",
                    colorFilter = ColorFilter.tint(theme(R.attr.wdPrimaryColor)),
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height(180.dp)
                            .padding(vertical = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            NextButton(
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                text = dialogData?.okLabel ?: "",
                enabled = true,
                onClick = onConfirm,
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = theme(R.attr.wdPrimaryColor),
                )
            }

            if (!isIconAtBottom) {
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

private class IconTopDialogProvider : PreviewParameterProvider<DialogData> {
    override val values =
        sequenceOf(
            DialogData(
                icon = R.drawable.ic_attention_icon,
                title = "Missing Location Permission",
                description =
                    "Location permission is required to use this feature. " +
                        "Go to App Settings > Permissions > Location and select \"Allow all the time\".",
                okLabel = "Open Settings",
                iconAtBottom = false,
            ),
        )
}

private class IconBottomDialogProvider : PreviewParameterProvider<DialogData> {
    override val values =
        sequenceOf(
            DialogData(
                icon = R.drawable.location_instruction_icon,
                title = "Location Permission Disclosure",
                description =
                    "Before granting background location permission, please understand:\n\n" +
                        "What we use it for:\n" +
                        "• Access WiFi network names while app runs in background\n" +
                        "• Enable Network Whitelist feature\n" +
                        "• GPS spoofing functionality\n\n" +
                        "What we DON'T do:\n" +
                        "• Track your physical location\n" +
                        "• Collect GPS coordinates\n" +
                        "• Share data with third parties\n" +
                        "• Send data off your device\n\n" +
                        "Your location data stays on your device and is used solely for the features above.",
                okLabel = "Grant Permission",
                iconAtBottom = true,
            ),
        )
}

@Preview(name = "Icon at Top (Missing Permission)", showBackground = true, showSystemUi = true)
@Composable
fun OverlayDialogIconTopPreview(
    @PreviewParameter(IconTopDialogProvider::class) dialogData: DialogData,
) {
    OverlayDialogContent(dialogData = dialogData, onConfirm = {}, onDismiss = {})
}

@Preview(name = "Icon at Bottom - Phone", showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp")
@Composable
fun OverlayDialogIconBottomPhonePreview(
    @PreviewParameter(IconBottomDialogProvider::class) dialogData: DialogData,
) {
    OverlayDialogContent(dialogData = dialogData, onConfirm = {}, onDismiss = {})
}

@Preview(name = "Icon at Bottom - Small Phone", showBackground = true, showSystemUi = true, device = "spec:width=360dp,height=640dp")
@Composable
fun OverlayDialogIconBottomSmallPhonePreview(
    @PreviewParameter(IconBottomDialogProvider::class) dialogData: DialogData,
) {
    OverlayDialogContent(dialogData = dialogData, onConfirm = {}, onDismiss = {})
}

@Preview(name = "Icon at Bottom - Large Phone", showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
fun OverlayDialogIconBottomLargePhonePreview(
    @PreviewParameter(IconBottomDialogProvider::class) dialogData: DialogData,
) {
    OverlayDialogContent(dialogData = dialogData, onConfirm = {}, onDismiss = {})
}

@Preview(name = "Icon at Bottom - Tablet", showBackground = true, showSystemUi = true, device = "spec:width=800dp,height=1280dp,dpi=240")
@Composable
fun OverlayDialogIconBottomTabletPreview(
    @PreviewParameter(IconBottomDialogProvider::class) dialogData: DialogData,
) {
    OverlayDialogContent(dialogData = dialogData, onConfirm = {}, onDismiss = {})
}
