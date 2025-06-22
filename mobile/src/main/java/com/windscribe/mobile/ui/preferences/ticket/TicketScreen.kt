package com.windscribe.mobile.ui.preferences.ticket

import PreferencesNavBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.DropDownNoDescription
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.common.ScreenDescription
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.popup.FullScreenDialog
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.api.response.QueryType

@Composable
fun TicketScreen(viewModel: TicketViewModel? = null) {
    val navController = LocalNavController.current
    val selectedKey by remember { mutableStateOf(QueryType.Account) }
    val queryTypes = QueryType.entries.map {
        DropDownStringItem(it.name, it.name)
    }
    val submitTicketState by viewModel?.submitTicketState?.collectAsState()
        ?: remember { mutableStateOf(SubmitTicketState.Idle) }
    val isButtonEnabled by viewModel?.buttonEnabled?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val email by viewModel?.email?.collectAsState() ?: remember { mutableStateOf("") }

    PreferenceBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            PreferencesNavBar(stringResource(R.string.contact_humans)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            ScreenDescription(stringResource(R.string.how_to_send_ticket))
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                DropDownNoDescription(
                    R.string.category,
                    queryTypes,
                    selectedItemKey = selectedKey.name
                ) {
                    viewModel?.onQueryTypeSelected(QueryType.valueOf(it.key))
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    hint = stringResource(R.string.email),
                    modifier = Modifier.fillMaxWidth(),
                    defaultValue = email,
                    onValueChange = { viewModel?.onEmailChanged(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    hint = stringResource(R.string.subject),
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { viewModel?.onSubjectChanged(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    hint = stringResource(R.string.what_is_the_issue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    singleLine = false,
                    height = 120.dp,
                    onValueChange = { viewModel?.onMessageChanged(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                NextButton(
                    text = stringResource(R.string.send),
                    enabled = isButtonEnabled
                ) {
                    viewModel?.onSendTicketClicked()
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        if (submitTicketState is SubmitTicketState.Loading) {
            PreferenceProgressBar(true)
        }
        HandleState(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HandleState(viewModel: TicketViewModel?) {
    val submitTicketState by viewModel?.submitTicketState?.collectAsState()
        ?: remember { mutableStateOf(SubmitTicketState.Idle) }
    val showDialog = remember { mutableStateOf(false) }
    val message = when (submitTicketState) {
        is SubmitTicketState.Success -> {
            (submitTicketState as SubmitTicketState.Success).message
        }

        is SubmitTicketState.Error -> {
            (submitTicketState as SubmitTicketState.Error).message
        }

        else -> {
            ""
        }
    }
    LaunchedEffect(submitTicketState) {
        if (submitTicketState is SubmitTicketState.Success || submitTicketState is SubmitTicketState.Error) {
            showDialog.value = true
        }
    }
    if (showDialog.value) {
        FullScreenDialog(message, error = submitTicketState is SubmitTicketState.Error) {
            showDialog.value = false
        }
    }
}

@Composable
private fun TextField(
    modifier: Modifier = Modifier,
    hint: String,
    defaultValue: String = "",
    height: Dp = 52.dp,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit = {},
) {
    var text by remember { mutableStateOf(defaultValue) }
    val contentColor = MaterialTheme.colorScheme.primaryTextColor
    val containerColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f)
    Column(modifier = modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = hint,
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
            )
        }
        Box {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChange(it)
                },
                singleLine = singleLine,
                shape = RoundedCornerShape(9.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Done
                ),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor,
                    disabledTextColor = contentColor,
                    unfocusedContainerColor = containerColor,
                    focusedContainerColor = containerColor,
                    disabledContainerColor = containerColor,
                    errorContainerColor = containerColor,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = AppColors.white,
                    disabledIndicatorColor = Color.Transparent,
                ),
                visualTransformation = VisualTransformation.None,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                textStyle = font16.copy(color = contentColor, textAlign = TextAlign.Start),
            )
        }
    }
}


@Composable
@MultiDevicePreview
private fun TicketScreenPreview() {
    PreviewWithNav {
        TicketScreen()
    }
}