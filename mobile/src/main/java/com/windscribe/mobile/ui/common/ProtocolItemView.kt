package com.windscribe.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util

@Composable
fun ProtocolItemView(timeleft: Int = 0, procolInfo: ProtocolInformation, onSelected: () -> Unit = {}
) {
    val showBorder = procolInfo.type == ProtocolConnectionStatus.Connected || procolInfo.type == ProtocolConnectionStatus.NextUp
    val color = AppColors.white
    val borderColor = if (showBorder) {
        AppColors.neonGreen.copy(alpha = 0.3f)
    } else {
        color.copy(alpha = 0.0f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp ,color = borderColor, shape = RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
            .clickable{
               onSelected()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Util.getProtocolLabel(procolInfo.protocol),
                style = font16,
                color = color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "|",
                style = font16,
                color = color.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = procolInfo.port,
                style = font16,
                color = color.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (procolInfo.type == ProtocolConnectionStatus.Connected) {
                Text(
                    text = "Connected to",
                    style = font12,
                    color = borderColor.copy(1.0f),
                    modifier = Modifier
                        .offset(x = (15).dp, y = (-13).dp)
                        .background(color = borderColor.copy(alpha = 0.10f), shape = RoundedCornerShape(topEnd = 6.dp, bottomStart = 6.dp))
                        .padding(top = 6.dp, bottom = 6.dp, start = 24.dp, end = 24.dp)
                )
            }
            if (procolInfo.type == ProtocolConnectionStatus.NextUp) {
                Text(
                    text = "Next up in $timeleft",
                    style = font12,
                    color = color,
                    modifier = Modifier
                        .offset(x = (13).dp, y = (-11).dp)
                        .background(color = borderColor.copy(alpha = 0.10f), shape = RoundedCornerShape(topEnd = 6.dp, bottomStart = 6.dp))
                        .padding(all = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = procolInfo.description,
                style = font12,
                color = color.copy(alpha = 0.5f),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1.0f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (procolInfo.type == ProtocolConnectionStatus.Connected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = AppColors.neonGreen,
                )
            } else if (procolInfo.type == ProtocolConnectionStatus.Disconnected) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward_arrow_white),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .alpha(0.5f)
                        .then(Modifier.size(16.dp)) // Optional
                )
            } else if (procolInfo.type == ProtocolConnectionStatus.Failed && procolInfo.error.isNotBlank()) {
                Text(
                    text = procolInfo.error,
                    style = font12,
                    color = AppColors.red,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}