package com.windscribe.mobile.ui.preferences.advance

import PreferencesNavBar
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceScreen(viewModel: AdvanceViewModel? = null) {
    val navController = LocalNavController.current
    val params by viewModel?.parameters?.collectAsState() ?: remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    PreferenceBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            PreferencesNavBar(title = stringResource(R.string.advance)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 473.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = params,
                        onValueChange = {
                            viewModel?.updateParams(it)
                        },
                        modifier = Modifier
                            .height(250.dp)
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryTextColor.copy(
                                    alpha = 0.05f
                                ), shape = RoundedCornerShape(12.dp)
                            ),
                        singleLine = false,
                        maxLines = Int.MAX_VALUE,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.enter_1_key_value_per_line),
                                style = font12.copy(textAlign = TextAlign.Start),
                            )
                        },
                        textStyle = font12.copy(textAlign = TextAlign.Start),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.primaryTextColor,
                            disabledContainerColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                            disabledBorderColor = Color.Transparent,
                            disabledLabelColor = MaterialTheme.colorScheme.primaryTextColor,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.primaryTextColor,
                            errorBorderColor = Color.Transparent,
                            errorContainerColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                            errorCursorColor = MaterialTheme.colorScheme.primaryTextColor,
                            errorLabelColor = MaterialTheme.colorScheme.primaryTextColor,
                            errorLeadingIconColor = MaterialTheme.colorScheme.primaryTextColor,
                            errorPlaceholderColor = MaterialTheme.colorScheme.primaryTextColor,
                            errorTrailingIconColor = MaterialTheme.colorScheme.primaryTextColor,
                            focusedLabelColor = MaterialTheme.colorScheme.primaryTextColor,
                            focusedLeadingIconColor = MaterialTheme.colorScheme.primaryTextColor,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.primaryTextColor,
                            focusedTrailingIconColor = MaterialTheme.colorScheme.primaryTextColor,
                            unfocusedLabelColor = MaterialTheme.colorScheme.primaryTextColor,
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.primaryTextColor,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.primaryTextColor,
                            unfocusedTrailingIconColor = MaterialTheme.colorScheme.primaryTextColor,
                            cursorColor = MaterialTheme.colorScheme.primaryTextColor,
                            selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                                handleColor = MaterialTheme.colorScheme.primaryTextColor,
                                backgroundColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.4f)
                            )
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NextButton(
                            text = stringResource(R.string.save),
                            enabled = true,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel?.saveAdvanceParams(params)
                            }
                        )
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel?.clearAdvanceParams()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.clear),
                                color = MaterialTheme.colorScheme.primaryTextColor
                            )
                        }
                    }
                }
            }
        }
        HandleToast(viewModel)
    }
}

@Composable
private fun HandleToast(viewModel: AdvanceViewModel?) {
    val context = LocalContext.current
    val toastMessage by viewModel?.toastMessage?.collectAsState(initial = "")
        ?: remember { mutableStateOf("") }
    LaunchedEffect(toastMessage) {
        if (toastMessage.isBlank()) {
            return@LaunchedEffect
        }
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }
}

@Composable
@MultiDevicePreview
private fun AdvanceScreenPreview() {
    PreviewWithNav {
        AdvanceScreen()
    }
}