package com.windscribe.mobile.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font18

@Composable
fun NextButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) AppColors.neonGreen else AppColors.white,
            contentColor = if (enabled) AppColors.deepBlue else AppColors.green,
            disabledContainerColor = AppColors.white,
            disabledContentColor = AppColors.green
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = text,
            style = font18,
        )
    }
}

@Preview
@Composable
fun NextButtonEnabledPreview() {
    NextButton(
        text = "Next",
        enabled = true
    ) { }
}

@Preview
@Composable
fun NextButtonDisabledPreview() {
    NextButton(
        text = "Next",
        enabled = false
    ) { }
}
