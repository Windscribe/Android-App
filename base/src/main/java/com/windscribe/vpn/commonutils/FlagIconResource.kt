/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import com.windscribe.vpn.R

/**
 * Created by Mustafizur on 2018-03-05.
 */
object FlagIconResource {
    fun getFlag(countryCode: String?): Int {
        return flagIcons[countryCode] ?: R.drawable.dummy_flag
    }

    fun getSmallFlag(countryCode: String?): Int {
        return smallIcons[countryCode] ?: R.drawable.dummy_flag
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
            flagIcon["BY"] = R.drawable.by
            flagIcon["GT"] = R.drawable.gt
            flagIcon["LA"] = R.drawable.la
            flagIcon["NG"] = R.drawable.ng
            flagIcon["PY"] = R.drawable.py
            flagIcon["UY"] = R.drawable.uy
            return flagIcon
        }

    private val smallIcons: Map<String, Int>
        get() {
            val flagIcon: MutableMap<String, Int> = HashMap()
            flagIcon["AQ"] = R.drawable.aq_small
            flagIcon["CA"] = R.drawable.ca_small
            flagIcon["US"] = R.drawable.us_small
            flagIcon["FR"] = R.drawable.fr_small
            flagIcon["UK"] = R.drawable.uk_small
            flagIcon["AT"] = R.drawable.at_small
            flagIcon["DE"] = R.drawable.de_small
            flagIcon["BG"] = R.drawable.bg_small
            flagIcon["HU"] = R.drawable.hu_small
            flagIcon["AR"] = R.drawable.ar_small
            flagIcon["VN"] = R.drawable.vn_small
            flagIcon["CZ"] = R.drawable.cz_small
            flagIcon["BE"] = R.drawable.be_small
            flagIcon["AE"] = R.drawable.ae_small
            flagIcon["TH"] = R.drawable.th_small
            flagIcon["TW"] = R.drawable.tw_small
            flagIcon["KR"] = R.drawable.kr_small
            flagIcon["SG"] = R.drawable.sg_small
            flagIcon["MY"] = R.drawable.my_small
            flagIcon["JP"] = R.drawable.jp_small
            flagIcon["ID"] = R.drawable.id_small
            flagIcon["HK"] = R.drawable.hk_small
            flagIcon["NZ"] = R.drawable.nz_small
            flagIcon["AU"] = R.drawable.au_small
            flagIcon["UA"] = R.drawable.ua_small
            flagIcon["TR"] = R.drawable.tr_small
            flagIcon["ZA"] = R.drawable.za_small
            flagIcon["RU"] = R.drawable.ru_small
            flagIcon["LY"] = R.drawable.ly_small
            flagIcon["IN"] = R.drawable.in_small
            flagIcon["AZ"] = R.drawable.az_small
            flagIcon["GB"] = R.drawable.uk_small
            flagIcon["CH"] = R.drawable.ch_small
            flagIcon["SE"] = R.drawable.se_small
            flagIcon["ES"] = R.drawable.es_small
            flagIcon["GR"] = R.drawable.gr_small
            flagIcon["IS"] = R.drawable.is_small
            flagIcon["IE"] = R.drawable.ie_small
            flagIcon["IL"] = R.drawable.il_small
            flagIcon["IT"] = R.drawable.it_small
            flagIcon["LV"] = R.drawable.lv_small
            flagIcon["LT"] = R.drawable.lt_small
            flagIcon["LU"] = R.drawable.lu_small
            flagIcon["MD"] = R.drawable.md_small
            flagIcon["NL"] = R.drawable.nl_small
            flagIcon["NO"] = R.drawable.no_small
            flagIcon["PL"] = R.drawable.pl_small
            flagIcon["PT"] = R.drawable.pt_small
            flagIcon["RO"] = R.drawable.ro_small
            flagIcon["DK"] = R.drawable.dk_small
            flagIcon["FI"] = R.drawable.fi_small
            flagIcon["AL"] = R.drawable.al_small
            flagIcon["SK"] = R.drawable.sk_small
            flagIcon["SI"] = R.drawable.si_small
            flagIcon["EE"] = R.drawable.ee_small
            flagIcon["TN"] = R.drawable.tn_small
            flagIcon["PH"] = R.drawable.ph_small
            flagIcon["CO"] = R.drawable.co_small
            flagIcon["MX"] = R.drawable.mx_small
            flagIcon["RS"] = R.drawable.rs_small
            flagIcon["GE"] = R.drawable.ge_small
            flagIcon["CL"] = R.drawable.cl_small
            flagIcon["CR"] = R.drawable.cr_small
            flagIcon["CY"] = R.drawable.cy_small
            flagIcon["KE"] = R.drawable.ke_small
            flagIcon["MK"] = R.drawable.mk_small
            flagIcon["HR"] = R.drawable.hr_small
            flagIcon["BR"] = R.drawable.br_small
            flagIcon["PA"] = R.drawable.pa_small
            flagIcon["VE"] = R.drawable.ve_small
            flagIcon["BS"] = R.drawable.bs_small
            flagIcon["ET"] = R.drawable.et_small
            flagIcon["DZ"] = R.drawable.dz_small
            flagIcon["MA"] = R.drawable.ma_small
            flagIcon["AM"] = R.drawable.am_small
            flagIcon["MC"] = R.drawable.mc_small
            flagIcon["PK"] = R.drawable.pk_small
            flagIcon["CN"] = R.drawable.cn_small
            flagIcon["BA"] = R.drawable.ba_small
            flagIcon["KH"] = R.drawable.kh_small
            flagIcon["EC"] = R.drawable.ec_small
            flagIcon["KZ"] = R.drawable.kz_small
            flagIcon["MT"] = R.drawable.mt_small
            flagIcon["PE"] = R.drawable.pe_small
            flagIcon["GH"] = R.drawable.gh_small
            flagIcon["BY"] = R.drawable.by_small
            flagIcon["GT"] = R.drawable.gt_small
            flagIcon["LA"] = R.drawable.la_small
            flagIcon["NG"] = R.drawable.ng_small
            flagIcon["PY"] = R.drawable.py_small
            flagIcon["UY"] = R.drawable.uy_small

            return flagIcon
        }
}