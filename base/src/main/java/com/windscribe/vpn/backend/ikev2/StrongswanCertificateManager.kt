/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.ikev2

import android.content.Context
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.strongswan.android.logic.TrustedCertificateManager

object StrongswanCertificateManager {

    fun init(context: Context) {
        storeCertificate(parseCertificate(context))
    }

    /**
     * Load the file from the given URI and try to parse it as X.509 certificate.
     *
     * @return certificate or null
     */
    fun parseCertificate(context: Context): X509Certificate? =
            try {
                val factory: CertificateFactory = CertificateFactory.getInstance("X.509")
                val input = context.assets.open("pro-root.der")
                factory.generateCertificate(input) as X509Certificate
                /* we don't check whether it's actually a CA certificate or not */
            } catch (e: CertificateException) {
                e.printStackTrace()
                null
            } catch (e: IOException) {
                e.printStackTrace()
                null
            } catch (e: KeyStoreException) {
                e.printStackTrace()
                null
            }

    /**
     * Try to store the given certificate in the KeyStore.
     *
     * @param certificate
     * @return whether it was successfully stored
     */
    fun storeCertificate(certificate: X509Certificate?) =
            try {
                val store = KeyStore.getInstance("LocalCertificateStore")
                store.load(null, null)
                store.setCertificateEntry(null, certificate)
                TrustedCertificateManager.getInstance().reset()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
}
