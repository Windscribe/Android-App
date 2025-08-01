package com.windscribe.mobile.ui.common

import android.R.attr.description
import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.theme.backgroundColor
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import kotlin.collections.forEach

@Composable
fun CustomDropDown(
    @StringRes title: Int,
    items: List<DropDownStringItem>,
    selectedItemKey: String,
    shape: RoundedCornerShape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
    @StringRes description: Int? = null,
    onSelect: (DropDownStringItem) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    var selected by remember(selectedItemKey, items) {
        mutableStateOf(items.find { it.key == selectedItemKey })
    }
    Column(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = shape
            )
            .padding(14.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(title),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primaryTextColor
            )
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .clickable { expanded.value = !expanded.value }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selected?.label ?: "",
                        style = font16,
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_cm_icon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primaryTextColor
                    )
                }

                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryTextColor)
                ) {
                    items.forEach {
                        DropdownMenuItem(
                            onClick = {
                                expanded.value = false
                                selected = it
                                onSelect(it)
                            },
                            text = {
                                Text(
                                    text = it.label ?: "",
                                    color = MaterialTheme.colorScheme.backgroundColor,
                                    style = font16,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                }
            }
        }
        if (description != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Description(stringResource(description))
        }
    }
}