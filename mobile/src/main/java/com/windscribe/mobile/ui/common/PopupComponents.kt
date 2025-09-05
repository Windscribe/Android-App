package com.windscribe.mobile.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font18
import com.windscribe.mobile.ui.theme.font22
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.mobile.ui.theme.serverListBackgroundColor

@Composable
fun PopupContainer(content: @Composable (ColumnScope.() -> Unit)) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.serverListBackgroundColor)
            .clickable { },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .width(274.dp)
                .verticalScroll(scrollState),
            content = content
        )
    }
}

@Composable
fun PopupPrimaryActionButton(
    modifier: Modifier = Modifier,
    resourceId: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = true,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.actionGreen,
            contentColor = AppColors.midnightNavy,
            disabledContainerColor = AppColors.white,
            disabledContentColor = AppColors.green
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = stringResource(resourceId),
            style = font18,
        )
    }
}

@Composable
fun PopupSecondaryActionButton(
    modifier: Modifier = Modifier,
    resourceId: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = true,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = .10f),
            contentColor = MaterialTheme.colorScheme.primaryTextColor,
            disabledContainerColor = AppColors.white,
            disabledContentColor = AppColors.green
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = stringResource(resourceId),
            style = font18,
        )
    }
}

@Composable
fun PopupHeroImage(resourceId: Int) {
    Image(
        painter = painterResource(id = resourceId),
        contentDescription = "Garry Image",
        modifier = Modifier
            .size(width = 274.dp, height = 217.dp)
    )
}

@Composable
fun PopupTitle(resourceId: Int) {
    Text(
        text = stringResource(resourceId),
        style = font22,
        color = MaterialTheme.colorScheme.primaryTextColor,
        textAlign = TextAlign.Center
    )
}

@Composable
fun PopupDescription(resourceId: Int) {
    Text(
        text = stringResource(resourceId),
        style = font16,
        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = .50f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Preview
@Composable
fun PopupHeroImagePreview() {
    PopupHeroImage(resourceId = R.drawable.garry_location_under_maintence)
}

@Preview(
    name = "Light Theme",
    uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(
    name = "Dark Theme",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PopupTitlePreview() {
    PopupTitle(resourceId = com.windscribe.vpn.R.string.under_maintenance)
}

@Preview(
    name = "Light Theme",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Dark Theme",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PopupDescriptionPreview() {
    PopupDescription(resourceId = com.windscribe.vpn.R.string.check_status_description)
}

@Preview(
    name = "Light Theme",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Dark Theme",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PopupPrimaryActionButtonPreview() {
    PopupPrimaryActionButton(
        resourceId = com.windscribe.vpn.R.string.check_status,
        onClick = {}
    )
}


@Preview(
    name = "Light Theme",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Dark Theme",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PopupSecondaryActionButtonPreview() {
    PopupSecondaryActionButton(
        resourceId = com.windscribe.vpn.R.string.back,
        onClick = {}
    )
}
