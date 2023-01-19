package com.react.musicServer.data

import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory


/**
 * Stream-lined api for loading certificates into the default ssl context
 */
class CertChain {
    private val cf = CertificateFactory.getInstance("X.509")
    private val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

    init {
        // load the built-in certs!
        val ksPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts")
        keyStore.load(Files.newInputStream(ksPath), null)
    }

    @Throws(Exception::class)
    fun load(filename: String): CertChain {
        CertChain::class.java.getResourceAsStream("/certs/$filename").use { cert ->
            assert(cert != null)
            val caInput: InputStream = BufferedInputStream(cert)
            val crt = cf.generateCertificate(caInput)
            keyStore.setCertificateEntry(filename, crt)
        }
        return this
    }

    @Throws(Exception::class)
    fun done() {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, tmf.trustManagers, null)
        SSLContext.setDefault(sslContext)
    }
}