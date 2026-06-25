package com.windscribe.mobile.ui.preferences.excluded_ips

import PreferencesNavBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.localdatabase.tables.ExcludedIpDomain

@Composable
fun ExcludedIpDomainScreen(viewModel: ExcludedIpDomainViewModel = hiltViewModel<ExcludedIpDomainViewModelImpl>()) {
    val navController = LocalNavController.current
    val excludedList by viewModel.excludedList.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    PreferenceBackground {
        Column(
            modifier =
                Modifier
                    .padding(vertical = 16.dp, horizontal = 16.dp)
                    .navigationBarsPadding(),
        ) {
            PreferencesNavBar(stringResource(R.string.domains_ips)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.domains_ips_description),
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.70f),
                style = font14,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            InputSection(
                inputText = inputText,
                errorMessage = errorMessage,
                onInputChange = viewModel::onInputTextChange,
                onAdd = viewModel::onAddEntry,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ExcludedList(
                list = excludedList,
                onDelete = viewModel::onDeleteEntry,
            )
        }
    }
}

@Composable
private fun InputSection(
    inputText: String,
    errorMessage: String?,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                    ).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                textStyle = font16.copy(textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.primaryTextColor),
                placeholder = {
                    Text(
                        text = stringResource(R.string.enter_ip_domain_hint),
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.50f),
                        style = font16,
                    )
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primaryTextColor,
                        selectionColors =
                            androidx.compose.foundation.text.selection.TextSelectionColors(
                                handleColor = MaterialTheme.colorScheme.primaryTextColor,
                                backgroundColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.3f),
                            ),
                    ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onAdd()
                        },
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = painterResource(com.windscribe.mobile.R.drawable.ic_add),
                contentDescription = "Add",
                modifier =
                    Modifier
                        .size(32.dp)
                        .clickable { onAdd() },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor),
            )
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                style = font14,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ExcludedList(
    list: List<ExcludedIpDomain>,
    onDelete: (ExcludedIpDomain) -> Unit,
) {
    if (list.isEmpty()) {
        Text(
            text = "No excluded IPs or domains",
            color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.50f),
            style = font16,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
        )
    } else {
        LazyColumn {
            itemsIndexed(list) { index, entry ->
                val shape =
                    when {
                        list.size == 1 -> RoundedCornerShape(12.dp)
                        index == 0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        index == list.lastIndex -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                if (index > 0) {
                    Spacer(modifier = Modifier.height(1.dp))
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                                shape = shape,
                            ).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.value,
                            color = MaterialTheme.colorScheme.primaryTextColor,
                            style = font16.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Image(
                        painter = painterResource(android.R.drawable.ic_menu_delete),
                        contentDescription = "Delete",
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clickable { onDelete(entry) },
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor),
                    )
                }
            }
        }
    }
}
