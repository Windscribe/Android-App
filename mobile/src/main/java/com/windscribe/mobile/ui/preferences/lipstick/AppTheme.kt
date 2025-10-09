import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.Description
import com.windscribe.mobile.ui.common.DescriptionWithLearnMore
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.preferences.lipstick.LipstickViewmodel
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.backgroundColor
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.constants.FeatureExplainer
import com.windscribe.vpn.constants.NetworkKeyConstants

@Composable
fun AppTheme(lipstickViewmodel: LipstickViewmodel?) {
    val expanded = remember { mutableStateOf(false) }
    val items = LookAndFeelHelper.getThemeOptions()
    val themeItem = lipstickViewmodel?.themeItem?.value ?: items.first()
    val activity = LocalContext.current as? AppStartActivity
    Column(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Image(painter = painterResource(R.drawable.ic_appearance), contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(com.windscribe.vpn.R.string.theme),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primaryTextColor
            )
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .clickable { expanded.value = !expanded.value }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = themeItem.id,
                        style = font16,
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cm_icon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primaryTextColor
                    )
                }

                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryTextColor)
                ) {
                    items.forEach {
                        DropdownMenuItem(
                            onClick = {
                                expanded.value = false
                                lipstickViewmodel?.onThemeItemSelected(it)
                                activity?.recreate()
                            },
                            text = {
                                Text(
                                    text = it.id,
                                    color = MaterialTheme.colorScheme.backgroundColor,
                                    style = font16,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Description(stringResource(com.windscribe.vpn.R.string.appearance_description))
    }
}

@Composable
@MultiDevicePreview
private fun PreviewAppTheme() {
    AppTheme(null)
}