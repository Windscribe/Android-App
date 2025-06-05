import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font18
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.mobile.ui.theme.primaryTextColor

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun NavBar(
    title: String,
    onNavClick: () -> Unit
) {
    val interactionSource = MutableInteractionSource()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp, bottom = 8.dp)
        ,
    ) {
        Image(
            painter = painterResource(id = R.drawable.arrow_left),
            contentDescription = stringResource(id = R.string.image_description),
            colorFilter = ColorFilter.tint(AppColors.white),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, color = AppColors.white),
                    onClick = onNavClick
                )
                .padding(4.dp)
                .align(Alignment.CenterStart)
        )

        Text(
            text = title,
            style = font24,
            color = AppColors.white,
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.Center)
        )
    }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun PreferencesNavBar(
    title: String,
    onNavClick: () -> Unit
) {
    val interactionSource = MutableInteractionSource()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.arrow_left),
            contentDescription = stringResource(id = R.string.image_description),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, color = MaterialTheme.colorScheme.primaryTextColor),
                    onClick = onNavClick
                )
                .padding(4.dp)
                .align(Alignment.CenterStart)
        )

        Text(
            text = title,
            style = font18,
            color = MaterialTheme.colorScheme.primaryTextColor,
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.Center)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NavBarPreview() {
    NavBar(title = "Preferences") {}
}
