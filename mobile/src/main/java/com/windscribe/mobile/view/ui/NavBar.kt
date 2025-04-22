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
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font24

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
            .statusBarsPadding(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_back_arrow),
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
                .padding(horizontal = Dimen.dp8)
                .align(Alignment.Center)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NavBarPreview() {
    NavBar(title = "Preferences") {}
}
