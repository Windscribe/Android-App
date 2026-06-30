package com.windscribe.mobile.ui.preferences.excluded_ips

import PreferencesNavBar
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
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
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.localdatabase.tables.ExcludedIpDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedIpDomainScreen(viewModel: ExcludedIpDomainViewModel = hiltViewModel<ExcludedIpDomainViewModelImpl>()) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val excludedList by viewModel.excludedList.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let { viewModel.onImportFromFile(it) }
        }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearToastMessage()
        }
    }

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
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                style = font14.copy(fontWeight = FontWeight.Normal, textAlign = TextAlign.Start),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            InputSection(
                inputText = inputText,
                onInputChange = viewModel::onInputTextChange,
                onAdd = viewModel::onAddEntry,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ActionButtons(
                onImport = { filePickerLauncher.launch("text/*") },
                onDeleteAll = { showDeleteAllDialog = true },
                hasItems = excludedList.isNotEmpty(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.onRefreshHostnames() },
                modifier = Modifier.fillMaxSize(),
            ) {
                ExcludedList(
                    list = excludedList,
                    onDelete = viewModel::onDeleteEntry,
                )
            }
        }
    }

    if (showDeleteAllDialog) {
        DeleteAllConfirmDialog(
            onConfirm = {
                viewModel.onDeleteAll()
                showDeleteAllDialog = false
            },
            onDismiss = { showDeleteAllDialog = false },
        )
    }
}

@Composable
private fun DeleteAllConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        shape = RoundedCornerShape(16.dp),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
        modifier =
            Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
            ),
        title = {
            Text(
                text = "Delete All?",
                style = font16.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primaryTextColor,
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete all entries? This action cannot be undone.",
                style = font12.copy(textAlign = TextAlign.Left),
                color = MaterialTheme.colorScheme.primaryTextColor,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Delete",
                    style = font16.copy(fontWeight = FontWeight.Medium),
                    color = Color.Red,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    style = font16,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                )
            }
        },
    )
}

@Composable
private fun InputSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

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
}

@Composable
private fun ActionButtons(
    onImport: () -> Unit,
    onDeleteAll: () -> Unit,
    hasItems: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Import button
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                    ).clickable { onImport() }
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(com.windscribe.mobile.R.drawable.ic_import),
                contentDescription = "Import",
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Import",
                color = MaterialTheme.colorScheme.primaryTextColor,
                style = font16.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Start,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Delete All button
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(
                        color =
                            if (hasItems) {
                                Color.Red.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.primaryTextColor.copy(
                                    alpha = 0.05f,
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                    ).clickable(enabled = hasItems) { onDeleteAll() }
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(android.R.drawable.ic_menu_delete),
                contentDescription = "Delete All",
                modifier = Modifier.size(20.dp),
                colorFilter =
                    ColorFilter.tint(
                        if (hasItems) Color.Red else MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.3f),
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Delete All",
                color = if (hasItems) Color.Red else MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.3f),
                style = font16.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Start,
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
