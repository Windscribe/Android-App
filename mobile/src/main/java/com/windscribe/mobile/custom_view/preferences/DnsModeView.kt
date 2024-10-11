package com.windscribe.mobile.custom_view.preferences

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.windscribe.mobile.R
import com.windscribe.vpn.commonutils.InputFilterMinMax

class DnsModeView(childView: View) : BaseView(childView) {
    private val customDns: EditText = childView.findViewById(R.id.custom_dns)
    private val editCustomDns: ImageView = childView.findViewById(R.id.custom_dns_edit)
    private val submitCustomDns: ImageView = childView.findViewById(R.id.custom_dns_check)
    private val cancelCustomDns: ImageView = childView.findViewById(R.id.custom_dns_cancel)
    interface Delegate {
        fun onCustomDnsChanged(dns: String)
    }
    var delegate: Delegate? = null

    init {
        editCustomDns.setOnClickListener {
            customDns.isEnabled = true
            customDns.requestFocus()
            customDns.setSelection(customDns.text.length)
            showKeyboard(customDns)
            editCustomDns.visibility = View.GONE
            submitCustomDns.visibility = View.VISIBLE
            cancelCustomDns.visibility = View.VISIBLE
        }
        cancelCustomDns.setOnClickListener {
            submitCustomDns.visibility = View.GONE
            cancelCustomDns.visibility = View.GONE
            editCustomDns.visibility = View.VISIBLE
            customDns.clearFocus()
            customDns.isEnabled = false
        }
        submitCustomDns.setOnClickListener {
            val dnsAddress = customDns.text.toString()
            if (!isValid(dnsAddress)) {
                Toast.makeText(it.context, it.context.getString(R.string.enter_valid_ip_or_url), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitCustomDns.visibility = View.INVISIBLE
            cancelCustomDns.visibility = View.INVISIBLE
            editCustomDns.visibility = View.VISIBLE
            customDns.clearFocus()
            customDns.isEnabled = false
            delegate?.onCustomDnsChanged(dnsAddress)
        }
    }
    fun setCustomDns(customDNS: String) {
        customDns.setText(customDNS)
    }

    private fun isValid(customDNS: String): Boolean {
        if (customDNS.isBlank() || customDNS.isEmpty() || customDNS.length < 7) {
            return false
        }
        val ipAddressRegex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".toRegex()
        val urlOrDomainRegex = """^(https?|h3)://([\w-]+(\.[\w-]+)+)(/[\w- ./?%&=]*)?$|^([\w-]+(\.[\w-]+)+)$""".toRegex()
        return customDNS.matches(ipAddressRegex) || customDNS.matches(urlOrDomainRegex)
    }
}