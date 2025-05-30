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
import com.windscribe.mobile.ui.theme.font12

@Composable
fun ActionButtonLighter(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.neonGreen5,
            contentColor = AppColors.neonGreen,
            disabledContainerColor = AppColors.neonGreen5,
            disabledContentColor = AppColors.neonGreen
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = text,
            style = font12,
        )
    }
}