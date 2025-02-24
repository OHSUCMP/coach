package edu.ohsu.cmp.coach.util;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;


public class CryptoUtil {
    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";


    public static X509Certificate readCertificate(File certFile) throws IOException, CertificateException {
        try (FileInputStream fis = new FileInputStream(certFile)) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(fis);
        }
    }

    public static PublicKey readPublicKeyFromCertificate(File certFile) throws IOException, CertificateException {
        return readCertificate(certFile).getPublicKey();
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

    public static byte[] randomBytes(int length) {
        byte[] b = new byte[length];
        new SecureRandom().nextBytes(b);
        return b;
    }
}
