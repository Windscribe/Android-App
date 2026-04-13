package com.windscribe.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

/**
 * A styled text field component with three visual states: Default, Active (focused), and Error.
 * Based on Figma design with specific styling for each state.
 *
 * States:
 * - Default: Gray border (white 10% opacity), gray placeholder
 * - Active: White border, gray placeholder (focused state)
 * - Error: Red border (#ff7f7f), red text
 *
 * @param modifier Modifier to be applied to the text field
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param placeholder Placeholder text to display when empty
 * @param isError Whether the field is in error state
 * @param keyboardType Type of keyboard to show
 * @param imeAction IME action for the keyboard
 */
@Composable
fun StyledTextField(
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Define colors based on state
    val borderColor = when {
        isError -> Color(0xFFFF7F7F) // Red border for error state
        isFocused -> AppColors.white // White border for active/focused state
        else -> AppColors.white.copy(alpha = 0.1f) // Gray border for default state
    }

    val textColor = if (isError) Color(0xFFFF7F7F) else AppColors.white
    val placeholderColor = if (isError) Color(0xFFFF7F7F) else Color(0xFF898F9D)

    val customTextSelectionColors = TextSelectionColors(
        handleColor = AppColors.white,
        backgroundColor = AppColors.white.copy(alpha = 0.3f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = font16.copy(
                color = textColor,
                textAlign = TextAlign.Start,
                lineHeight = 20.sp
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = imeAction,
                keyboardType = keyboardType
            ),
            cursorBrush = SolidColor(AppColors.white),
            interactionSource = interactionSource,
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.white.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(9.dp)
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(9.dp)
                )
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .height(48.dp),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = font16,
                            color = placeholderColor,
                            textAlign = TextAlign.Start
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF090E19)
fun StyledTextFieldPreview() {
    var defaultText by remember { mutableStateOf("") }
    var activeText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.deepBlue)
            .padding(16.dp)
    ) {
        // Default state
        Text(
            text = "Default State",
            style = font16,
            color = AppColors.white,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        StyledTextField(
            value = defaultText,
            onValueChange = { defaultText = it },
            placeholder = "Enter username"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Active/Focused state - will show white border when clicked
        Text(
            text = "Active State (click to focus)",
            style = font16,
            color = AppColors.white,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        StyledTextField(
            value = activeText,
            onValueChange = { activeText = it },
            placeholder = "Enter usernameygggg"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error state
        Text(
            text = "Error State",
            style = font16,
            color = AppColors.white,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        StyledTextField(
            value = errorText,
            onValueChange = { errorText = it },
            placeholder = "Enter 2FA",
            isError = true
        )
    }
}