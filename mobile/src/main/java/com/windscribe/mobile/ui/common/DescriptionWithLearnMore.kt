package com.windscribe.mobile.ui.common

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor

@Composable
fun DescriptionWithLearnMore(description: String, path: String) {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        append("$description ")
        pushStringAnnotation(tag = "LEARN_MORE", annotation = "learn_more")
        withStyle(
            style = SpanStyle(
                color = AppColors.mediumBlue,
                fontWeight = FontWeight.Medium
            )
        ) {
            append("Learn more")
        }
        pop()
    }

    ClickableText(
        text = annotatedString,
        style = font14.copy(
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Normal
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations("LEARN_MORE", start = offset, end = offset)
                .firstOrNull()?.let {
                    context.openUrl(path)
                }
        }
    )
}