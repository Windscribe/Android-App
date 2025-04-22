import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.windscribe.mobile.R
import com.windscribe.mobile.lipstick.LipstickActivity
import com.windscribe.mobile.lipstick.LipstickViewmodel
import com.windscribe.mobile.lipstick.LookAndFeelHelper
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.AppColors.homeBackground
import com.windscribe.mobile.view.theme.font12
import com.windscribe.mobile.view.theme.font16

@Composable
fun AppTheme(lipstickViewmodel: LipstickViewmodel?) {
    val expanded = remember { mutableStateOf(false) }
    val items = LookAndFeelHelper.getThemeOptions()
    val themeItem = lipstickViewmodel?.themeItem?.value ?: items.first()
    val activity = LocalContext.current as? LipstickActivity?
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .background(color = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .zIndex(10.0f)
                .background(color = Color(0XFF1E2937), shape = RoundedCornerShape(12.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Image(painterResource(R.drawable.ic_apple), contentDescription = "")
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    stringResource(R.string.theme),
                    style = font16,
                    color = AppColors.white
                )
                Spacer(modifier = Modifier.weight(1.0f))

                // Anchor view for dropdown
                Box(modifier = Modifier.clickable { expanded.value = !expanded.value }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(themeItem.title),
                            style = font12,
                            color = AppColors.white
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cm_icon),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AppColors.white
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                    ) {
                        DropdownMenu(
                            expanded = expanded.value,
                            onDismissRequest = { expanded.value = false },
                            modifier = Modifier.background(AppColors.white)
                        ) {
                            items.forEach {
                                DropdownMenuItem(onClick = {
                                    lipstickViewmodel?.onThemeItemSelected(it)
                                    expanded.value = false
                                    activity?.reloadApp()
                                }, text = {
                                    Text(
                                        stringResource(it.title),
                                        color = homeBackground,
                                        style = font16,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                })
                            }
                        }
                    }
                }
            }
        }

        // Bottom description box
        Box(
            modifier = Modifier
                .offset(y = (-16).dp)
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 0.8.dp)
                .border(
                    width = 1.dp,
                    color = AppColors.white5,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                )
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.appearance_description),
                    modifier = Modifier.padding(12.dp),
                    style = font12,
                    color = AppColors.white50
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewAppTheme() {
    AppTheme(null)
}