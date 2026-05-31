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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Callbacks the advance-parameters UI can raise. Hoisted out of the composables so the stateless
 * [AdvanceContent] never needs to know about [AdvanceViewModel] — previews supply no-op lambdas.
 */
class AdvanceActions(
    val onParamsChange: (String) -> Unit = {},
    val onSaveClick: (String) -> Unit = {},
    val onClearClick: () -> Unit = {},
)

/**
 * Stateful entry point. Owns the [AdvanceViewModel], collects its flows and wires the toast
 * side effect, then delegates rendering to [AdvanceContent].
 */
@Composable
fun AdvanceScreen(viewModel: AdvanceViewModel = hiltViewModel<AdvanceViewModelImpl>()) {
    val params by viewModel.parameters.collectAsState()
    AdvanceContent(
        params = params,
        toastMessage = viewModel.toastMessage,
        actions =
            AdvanceActions(
                onParamsChange = viewModel::updateParams,
                onSaveClick = viewModel::saveAdvanceParams,
                onClearClick = viewModel::clearAdvanceParams,
            ),
    )
}

/**
 * Stateless advance-parameters UI. Everything it needs is passed in, so it renders identically
 * in the app and in `@Preview`. This is the composable previews target.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceContent(
    params: String,
    toastMessage: Flow<String>,
    actions: AdvanceActions,
) {
    val navController = LocalNavController.current
    val focusManager = LocalFocusManager.current
    PreferenceBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            PreferencesNavBar(title = stringResource(R.string.advance)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier =
                        Modifier
                            .widthIn(max = 473.dp)
                            .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OutlinedTextField(
                        value = params,
                        onValueChange = {
                            actions.onParamsChange(it)
                        },
                        modifier =
                            Modifier
                                .height(250.dp)
                                .fillMaxWidth()
                                .background(
                                    color =
                                        MaterialTheme.colorScheme.primaryTextColor.copy(
                                            alpha = 0.05f,
                                        ),
                                    shape = RoundedCornerShape(12.dp),
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
                        colors =
                            OutlinedTextFieldDefaults.colors(
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
                                selectionColors =
                                    androidx.compose.foundation.text.selection.TextSelectionColors(
                                        handleColor = MaterialTheme.colorScheme.primaryTextColor,
                                        backgroundColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.4f),
                                    ),
                            ),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NextButton(
                            text = stringResource(R.string.save),
                            enabled = true,
                            onClick = {
                                focusManager.clearFocus()
                                actions.onSaveClick(params)
                            },
                        )
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                actions.onClearClick()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.clear),
                                color = MaterialTheme.colorScheme.primaryTextColor,
                            )
                        }
                    }
                }
            }
        }
        HandleToast(toastMessage)
    }
}

@Composable
private fun HandleToast(toastMessage: Flow<String>) {
    val context = LocalContext.current
    val message by toastMessage.collectAsState(initial = "")
    LaunchedEffect(message) {
        if (message.isBlank()) {
            return@LaunchedEffect
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Feeds representative parameter text into the preview so the renderer draws [AdvanceContent]
 * without a VM.
 */
private class AdvanceStateProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf("", "key=value")
}

@Composable
@MultiDevicePreview
private fun AdvanceContentPreview(
    @PreviewParameter(AdvanceStateProvider::class) params: String,
) {
    PreviewWithNav {
        AdvanceContent(
            params = params,
            toastMessage = emptyFlow(),
            actions = AdvanceActions(),
        )
    }
}
