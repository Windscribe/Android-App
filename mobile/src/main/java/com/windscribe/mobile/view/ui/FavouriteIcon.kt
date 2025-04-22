package com.windscribe.mobile.view.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.view.theme.AppColors

@Composable
fun FavouriteIcon(isFavorite: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = false,
                    radius = 16.dp,
                    color = AppColors.white
                )
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(if (isFavorite) R.drawable.ic_fav_selected else R.drawable.ic_fav),
            contentDescription = "Favourite",
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(AppColors.white70)
        )
    }
}