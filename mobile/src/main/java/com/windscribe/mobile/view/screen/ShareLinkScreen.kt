package com.windscribe.mobile.view.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.R
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.ui.NextButton
import com.windscribe.mobile.viewmodel.SharedLinkViewmodel

@Composable
fun ShareLinkScreen(viewmodel: SharedLinkViewmodel?) {
 //   val navController = LocalNavController.current
    val shouldExit by viewmodel?.shouldExit?.collectAsState() ?: remember { mutableStateOf(false) }
    LaunchedEffect(shouldExit) {
        if (shouldExit) {
         //   navController.popBackStack()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppColors.darkBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_share_favourite),
                contentDescription = null,
                modifier = Modifier
                    .size(86.dp)
                    .padding(top = 32.dp)
            )
            Text(
                text = stringResource(id = R.string.share_windscribe_with_a_friend),
                fontFamily = FontFamily(Font(R.font.ibm_font_family)),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
            )
            Column() {
                Spacer(modifier = Modifier.height(16.dp))
                ReferralFeature(stringResource(R.string.first_reason_to_use_referral))
                Spacer(modifier = Modifier.height(Dimen.dp16))
                ReferralFeature(stringResource(R.string.if_you_go_pro_they_ll_go_pro_too))
            }
            NextButton(
                text = stringResource(R.string.share_invite_link), enabled = true, onClick = {

                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            )
        }
    }
}

@Composable
@Preview
fun ShareLinkScreenPreview() {
    ShareLinkScreen(null)
}