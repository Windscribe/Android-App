/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import com.windscribe.vpn.R
import com.windscribe.vpn.commonutils.FlagIconResource
import java.util.HashMap

/**
 * Created by Mustafizur on 2018-03-05.
 */
object FlagIconResource {
    fun getFlag(countryCode: String?): Int {
        return flagIcons[countryCode]?:R.drawable.dummy_flag
    }

    val flagIcons: Map<String, Int>
        get() {
            val flagIcon: MutableMap<String, Int> = HashMap()
            flagIcon["CA"] = R.drawable.ca
            flagIcon["US"] = R.drawable.us
            flagIcon["FR"] = R.drawable.fr
            flagIcon["UK"] = R.drawable.uk
            flagIcon["AT"] = R.drawable.at
            flagIcon["DE"] = R.drawable.de
            flagIcon["BG"] = R.drawable.bg
            flagIcon["HU"] = R.drawable.hu
            flagIcon["AR"] = R.drawable.ar
            flagIcon["VN"] = R.drawable.vn
            flagIcon["CZ"] = R.drawable.cz
            flagIcon["BE"] = R.drawable.be
            flagIcon["AE"] = R.drawable.ae
            flagIcon["TH"] = R.drawable.th
            flagIcon["TW"] = R.drawable.tw
            flagIcon["KR"] = R.drawable.kr
            flagIcon["SG"] = R.drawable.sg
            flagIcon["MY"] = R.drawable.my
            flagIcon["JP"] = R.drawable.jp
            flagIcon["ID"] = R.drawable.id
            flagIcon["HK"] = R.drawable.hk
            flagIcon["NZ"] = R.drawable.nz
            flagIcon["AU"] = R.drawable.au
            flagIcon["UA"] = R.drawable.ua
            flagIcon["TR"] = R.drawable.tr
            flagIcon["ZA"] = R.drawable.za
            flagIcon["RU"] = R.drawable.ru
            flagIcon["LY"] = R.drawable.ly
            flagIcon["IN"] = R.drawable.`in`
            flagIcon["AZ"] = R.drawable.az
            flagIcon["GB"] = R.drawable.uk
            flagIcon["CH"] = R.drawable.ch
            flagIcon["SE"] = R.drawable.se
            flagIcon["ES"] = R.drawable.es
            flagIcon["GR"] = R.drawable.gr
            flagIcon["IS"] = R.drawable.`is`
            flagIcon["IE"] = R.drawable.ie
            flagIcon["IL"] = R.drawable.il
            flagIcon["IT"] = R.drawable.it
            flagIcon["LV"] = R.drawable.lv
            flagIcon["LT"] = R.drawable.lt
            flagIcon["LU"] = R.drawable.lu
            flagIcon["MD"] = R.drawable.md
            flagIcon["NL"] = R.drawable.nl
            flagIcon["NO"] = R.drawable.no
            flagIcon["PL"] = R.drawable.pl
            flagIcon["PT"] = R.drawable.pt
            flagIcon["RO"] = R.drawable.ro
            flagIcon["DK"] = R.drawable.dk
            flagIcon["FI"] = R.drawable.fi
            flagIcon["AL"] = R.drawable.al
            flagIcon["SK"] = R.drawable.sk
            flagIcon["SI"] = R.drawable.si
            flagIcon["EE"] = R.drawable.ee
            flagIcon["TN"] = R.drawable.tn
            flagIcon["PH"] = R.drawable.ph
            flagIcon["CO"] = R.drawable.co
            flagIcon["MX"] = R.drawable.mx
            flagIcon["RS"] = R.drawable.rs
            flagIcon["GE"] = R.drawable.ge
            flagIcon["CL"] = R.drawable.cl
            flagIcon["CR"] = R.drawable.cr
            flagIcon["CY"] = R.drawable.cy
            flagIcon["KE"] = R.drawable.ke
            flagIcon["MK"] = R.drawable.mk
            flagIcon["HR"] = R.drawable.hr
            flagIcon["BR"] = R.drawable.br
            flagIcon["SL"] = R.drawable.sl
            flagIcon["PA"] = R.drawable.pa
            flagIcon["VE"] = R.drawable.ve
            flagIcon["BS"] = R.drawable.bs
            flagIcon["ET"] = R.drawable.et
            flagIcon["DZ"] = R.drawable.dz
            flagIcon["MA"] = R.drawable.ma
            flagIcon["AM"] = R.drawable.am
            flagIcon["MC"] = R.drawable.mc
            flagIcon["PK"] = R.drawable.pk
            flagIcon["CN"] = R.drawable.cn
            flagIcon["AQ"] = R.drawable.aq
            flagIcon["BA"] = R.drawable.ba
            flagIcon["KH"] = R.drawable.kh
            flagIcon["EC"] = R.drawable.ec
            flagIcon["KZ"] = R.drawable.kz
            flagIcon["MT"] = R.drawable.mt
            flagIcon["PE"] = R.drawable.pe
            flagIcon["GH"] = R.drawable.gh
            return flagIcon
        }
}