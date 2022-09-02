@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package io.kanro.mediator.netty.ssl

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.util.Date

fun generateKeyPair(): KeyPair {
    return KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.genKeyPair()
}

fun generateCertificate(keyPair: KeyPair): X509Certificate {
    val name = X500Name("CN=Mediator Proxy Root, OU=Mediator, O=ButterCam Open Source").apply {
    }
    val publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
    val holder = X509v3CertificateBuilder(
        name,
        BigInteger.probablePrime(160, java.util.Random()),
        Date(),
        Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10),
        name,
        publicKeyInfo
    ).addExtension(
        Extension.basicConstraints, true, BasicConstraints(true)
    ).addExtension(
        Extension.keyUsage, true, KeyUsage(KeyUsage.cRLSign or KeyUsage.keyCertSign)
    ).build(JcaContentSignerBuilder("SHA256WithRSA").setProvider(BouncyCastleProvider()).build(keyPair.private))
    return JcaX509CertificateConverter().setProvider(BouncyCastleProvider()).getCertificate(holder)
}

fun generateServerCertificate(
    cn: String,
    hosts: Collection<String>,
    keyPair: KeyPair,
    ca: X509Certificate,
    caKey: KeyPair
): X509Certificate {
    val name = X500Name("CN=$cn")
    val holder = JcaX509v3CertificateBuilder(
        ca,
        BigInteger.valueOf(1),
        Date(),
        Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10),
        name,
        keyPair.public
    ).addExtension(
        Extension.subjectAlternativeName, false,
        GeneralNames(
            hosts.map {
                GeneralName(GeneralName.dNSName, it)
            }.toTypedArray()
        )
    ).addExtension(
        Extension.basicConstraints, true, BasicConstraints(false)
    ).addExtension(
        Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature)
    ).addExtension(
        Extension.extendedKeyUsage, false, ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_serverAuth))
    ).build(JcaContentSignerBuilder("SHA256WithRSA").setProvider(BouncyCastleProvider()).build(caKey.private))
    return JcaX509CertificateConverter().setProvider(BouncyCastleProvider()).getCertificate(holder)
}
