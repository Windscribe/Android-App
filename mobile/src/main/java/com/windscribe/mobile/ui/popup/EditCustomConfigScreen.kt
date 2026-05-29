package com.windscribe.mobile.ui.popup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

/** State the edit-custom-config UI renders. */
data class EditCustomConfigState(
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val isOpenVPN: Boolean = true,
    val isRemember: Boolean = false,
    val connect: Boolean = true,
)

/** Callbacks the edit-custom-config UI can raise; defaults are no-ops for previews. */
class EditCustomConfigActions(
    val onNameChange: (String) -> Unit = {},
    val onUsernameChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onToggleIsRemember: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
)

@Composable
fun EditCustomConfigScreen(viewmodel: EditCustomConfigViewmodel = hiltViewModel<EditCustomConfigViewmodelImpl>()) {
    val navController = LocalNavController.current
    val shouldExit by viewmodel.shouldExit.collectAsState()
    LaunchedEffect(shouldExit) {
        if (shouldExit) {
            navController.popBackStack()
        }
    }
    val name by viewmodel.name.collectAsState()
    val username by viewmodel.username.collectAsState()
    val password by viewmodel.password.collectAsState()
    val isOpenVPN by viewmodel.isOpenVPN.collectAsState()
    val isRemember by viewmodel.isRemember.collectAsState()
    val connect by viewmodel.connect.collectAsState()
    EditCustomConfigContent(
        state =
            EditCustomConfigState(
                name = name,
                username = username,
                password = password,
                isOpenVPN = isOpenVPN,
                isRemember = isRemember,
                connect = connect,
            ),
        actions =
            EditCustomConfigActions(
                onNameChange = viewmodel::onNameChange,
                onUsernameChange = viewmodel::onUsernameChange,
                onPasswordChange = viewmodel::onPasswordChange,
                onToggleIsRemember = viewmodel::onToggleIsRemember,
                onSaveClick = viewmodel::onSaveClick,
            ),
    )
}

@Composable
fun EditCustomConfigContent(
    state: EditCustomConfigState,
    actions: EditCustomConfigActions,
) {
    val name = state.name
    val username = state.username
    val password = state.password
    val isOpenVPN = state.isOpenVPN
    val isRemember = state.isRemember
    val connect = state.connect
    val tintColor =
        if (isRemember) {
            AppColors.neonGreen
        } else {
            AppColors.white
        }
    val scrollState = rememberScrollState()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = AppColors.deepBlue),
    ) {
        Column(
            modifier =
                Modifier
                    .width(400.dp)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .statusBarsPadding()
                    .padding(horizontal = 32.dp)
                    .align(Alignment.Center),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1.0f))
            Image(
                painter = painterResource(id = R.drawable.ic_network_security_feature_icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(AppColors.white),
            )
            Text(
                text = stringResource(id = com.windscribe.vpn.R.string.edit_config_file),
                fontFamily = FontFamily(Font(com.windscribe.vpn.R.font.ibm_font_family)),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            CustomTextField(
                onValueChange = actions.onNameChange,
                modifier = Modifier.fillMaxWidth(),
                hint = stringResource(com.windscribe.vpn.R.string.config_title),
                value = name,
            )
            if (isOpenVPN) {
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    onValueChange = actions.onUsernameChange,
                    modifier = Modifier.fillMaxWidth(),
                    hint = stringResource(com.windscribe.vpn.R.string.username),
                    value = username,
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    onValueChange = actions.onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    hint = stringResource(com.windscribe.vpn.R.string.password),
                    value = password,
                    isPassword = true,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Save credentials", style = font16, color = Color.White)
                    Spacer(modifier = Modifier.weight(1.0f))
                    Image(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tintColor),
                        modifier =
                            Modifier
                                .clickable {
                                    actions.onToggleIsRemember()
                                }.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            NextButton(
                text = stringResource(if (connect) com.windscribe.vpn.R.string.connect else com.windscribe.vpn.R.string.update),
                enabled = true,
                onClick = {
                    actions.onSaveClick()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
            )
            Spacer(modifier = Modifier.weight(1.0f))
        }
    }
}

@Composable
private fun CustomTextField(
    modifier: Modifier = Modifier,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    isPassword: Boolean = false,
) {
    var text by remember { mutableStateOf(value) }
    var passwordVisible by remember { mutableStateOf(false) }

    // ADD THIS
    LaunchedEffect(value) {
        text = value
    }

    Column(modifier = modifier) {
        Text(
            text = hint,
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = AppColors.white,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        Box {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChange(it)
                },
                isError = false,
                singleLine = true,
                shape = RoundedCornerShape(9.dp),
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                    ),
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    if (isPassword) {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        id = if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye,
                                    ),
                                contentDescription = "Toggle password visibility",
                                tint = AppColors.white.copy(alpha = 0.50f),
                            )
                        }
                    }
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedTextColor = AppColors.white,
                        unfocusedTextColor = AppColors.white,
                        disabledTextColor = AppColors.white,
                        unfocusedContainerColor = AppColors.white.copy(0.05f),
                        focusedContainerColor = AppColors.white.copy(0.05f),
                        disabledContainerColor = AppColors.white.copy(0.05f),
                        errorContainerColor = AppColors.white.copy(0.05f),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        cursorColor = AppColors.white,
                        selectionColors =
                            androidx.compose.foundation.text.selection.TextSelectionColors(
                                handleColor = AppColors.white,
                                backgroundColor = AppColors.white.copy(alpha = 0.3f),
                            ),
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                textStyle =
                    font16.copy(
                        color = AppColors.white,
                        textAlign = TextAlign.Start,
                    ),
            )
        }
    }
}

@Composable
@MultiDevicePreview
fun EditCustomConfigScreenPreview() {
    PreviewWithNav {
        EditCustomConfigContent(
            state = EditCustomConfigState(name = "My Config", isOpenVPN = true),
            actions = EditCustomConfigActions(),
        )
    }
}
