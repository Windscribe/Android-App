
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font28
import kotlinx.coroutines.delay

data class FeatureItem(val imageRes: Int, val text: String)

val featureList = listOf(
    FeatureItem(R.drawable.feature_servers, "Servers in over 69 countries and 134 cities."),
    FeatureItem(R.drawable.feature_secure, "Automatically Secure any Network"),
    FeatureItem(R.drawable.feature_logging, "Strict No-Logging Policy"),
    FeatureItem(R.drawable.feature_quick, "Works with Shortcuts & Quick Settings"),
)

@Composable
fun FeatureSection(modifier: Modifier = Modifier) {
    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(currentIndex) {
        while (true) {
            delay(3000)
            currentIndex = (currentIndex + 1) % featureList.size
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier
    ) {
        val currentFeature = featureList[currentIndex]

        Image(
            painter = painterResource(currentFeature.imageRes),
            contentDescription = "Feature Image",
            modifier = Modifier.size(Dimen.dp120)
        )
        Spacer(modifier = Modifier.height(Dimen.dp8))
        Text(
            text = currentFeature.text,
            style = font28,
            color = Color(0xFFFFFFFF)
        )
        Spacer(modifier = Modifier.height(Dimen.dp24))
        DotsIndicator(currentIndex)
    }
}

@Composable
fun DotsIndicator(currentIndex: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        featureList.indices.forEach { index ->
            val alpha = animateFloatAsState(
                targetValue = if (index == currentIndex) 1f else 0.4f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = alpha.value),
                modifier = Modifier.size(Dimen.dp8)
            ) {}
        }
    }
}

@Preview
@Composable
fun FeatureSectionPreview() {
    FeatureSection()
}

@Preview
@Composable
fun DotsIndicatorPreview() {
    DotsIndicator(1)
}