package com.windscribe.mobile.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.R

private val IbmPlex = FontFamily(
    Font(com.windscribe.vpn.R.font.ibm_plex_sans_light, FontWeight.W300),//w300
    Font(com.windscribe.vpn.R.font.ibm_plex_sans_regular, FontWeight.Normal),//w400
    Font(com.windscribe.vpn.R.font.ibm_plex_sans_medium, FontWeight.Medium),//w500
    Font(com.windscribe.vpn.R.font.ibm_plex_sans_semi_bold, FontWeight.SemiBold),//w600
    Font(com.windscribe.vpn.R.font.ibm_plex_sans_bold, FontWeight.Bold),//w700
)
val font9 = TextStyle(
    fontSize = 9.sp,
    fontFamily = IbmPlex,
    fontWeight = FontWeight.Medium,
    color = AppColors.white,
    textAlign = TextAlign.Center,
)
val font12 = TextStyle(
    fontFamily = IbmPlex,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    textAlign = TextAlign.Center
)
val font16 = TextStyle(
    fontFamily = IbmPlex,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    textAlign = TextAlign.Center
)
val font14 = TextStyle(
    fontFamily = IbmPlex,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    textAlign = TextAlign.Center
)
val font18 = TextStyle(
    fontFamily = IbmPlex,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    textAlign = TextAlign.Center
)
val font24 = TextStyle(
    fontFamily = IbmPlex,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    textAlign = TextAlign.Center
)
val font26 = TextStyle(
    fontSize = 26.sp,
    fontFamily = IbmPlex,
    fontWeight = FontWeight.SemiBold,
    color = AppColors.white
)

val font28 = TextStyle(
    fontFamily = IbmPlex,
    fontWeight = FontWeight.W300,
    fontSize = 28.sp,
    textAlign = TextAlign.Center
)