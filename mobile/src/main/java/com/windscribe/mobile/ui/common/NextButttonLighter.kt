package com.windscribe.mobile.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

@Composable
fun NextButtonLighter(modifier: Modifier = Modifier,
                      text: String,
                      onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.white.copy(alpha = 0.10f),
            contentColor = AppColors.white,
            disabledContainerColor = AppColors.white.copy(alpha = 0.10f),
            disabledContentColor = AppColors.white.copy(alpha = 0.25f)
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = text,
            style = font16,
        )
    }
}