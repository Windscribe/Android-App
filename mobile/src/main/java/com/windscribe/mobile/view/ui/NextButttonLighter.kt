package com.windscribe.mobile.view.ui

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
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font16

@Composable
fun NextButtonLighter(modifier: Modifier = Modifier,
                      text: String,
                      onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimen.dp48)
            .padding(horizontal = Dimen.dp24),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.white10,
            contentColor = AppColors.white,
            disabledContainerColor = AppColors.white10,
            disabledContentColor = AppColors.white25
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(Dimen.dp24),
    ) {
        Text(
            text = text,
            style = font16,
        )
    }
}