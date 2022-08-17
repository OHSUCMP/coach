package edu.ohsu.cmp.coach.util;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.*;


public class CryptoUtil {
    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";


    public static PublicKey readPublicKeyFromCertificate(File certFile) throws IOException, CertificateException {
        try (FileInputStream fis = new FileInputStream(certFile)) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(fis);
            return certificate.getPublicKey();
        }
    }

    public static PrivateKey readPrivateKey(File pemFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        String s = Files.readString(pemFile.toPath(), Charset.defaultCharset());
        String s2 = s.replace(PKCS8_HEADER, "")
                .replaceAll("\\s+", "")
                .replace(PKCS8_FOOTER, "");
        byte[] content = Base64.decodeBase64(s2);
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
        return factory.generatePrivate(privKeySpec);
    }
}
