package edu.ohsu.cmp.coach.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class JWTUtil {

    private static final Logger logger = LoggerFactory.getLogger(JWTUtil.class);

    public static String createToken(String iss, String publicKeyFile, String privateKeyFile) throws ConfigurationException {
        if (StringUtils.isNotBlank(publicKeyFile) && StringUtils.isNotBlank(privateKeyFile)) {
            File x509CertificateFile = new File(publicKeyFile);
            File pkcs8PrivateKeyFile = new File(privateKeyFile);

            try {
                RSAPublicKey publicKey = (RSAPublicKey) CryptoUtil.readPublicKeyFromCertificate(x509CertificateFile);
                RSAPrivateKey privateKey = (RSAPrivateKey) CryptoUtil.readPrivateKey(pkcs8PrivateKeyFile);
                return createToken(iss, publicKey, privateKey);

            } catch (Exception e) {
                throw new ConfigurationException("could not instantiate object with iss=" + iss + ", x509CertificateFile=" + x509CertificateFile + ", pkcs8PrivateKeyFile=" + pkcs8PrivateKeyFile, e);
            }
        }
        return null;
    }

    public static String createToken(String iss, RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
        return JWT.create()
                .withIssuer(iss)
                .sign(algorithm);
    }

    public static boolean isTokenValid(String token, String iss, RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        try {
            Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(iss)
                    .build();
            verifier.verify(token);
            return true;

        } catch (JWTVerificationException e) {
            logger.warn("JWT failed verification: " + token);
            return false;
        }
    }

}
