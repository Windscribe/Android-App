package com.windscribe.mobile.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor

@Composable
fun DescriptionWithLearnMore(
    description: String,
    path: String,
) {
    val context = LocalContext.current
    val annotatedString =
        buildAnnotatedString {
            append("$description ")
            withLink(
                LinkAnnotation.Clickable(
                    tag = "LEARN_MORE",
                    styles =
                        TextLinkStyles(
                            style =
                                SpanStyle(
                                    color = AppColors.mediumBlue,
                                    fontWeight = FontWeight.Medium,
                                ),
                        ),
                    linkInteractionListener = { context.openUrl(path) },
                ),
            ) {
                append("Learn more")
            }
        }

    Text(
        text = annotatedString,
        style =
            font14.copy(
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Normal,
            ),
    )
}
