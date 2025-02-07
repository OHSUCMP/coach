package edu.ohsu.cmp.coach.model;

import com.nimbusds.jose.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

// JSON Web Key (JWK) Object, see https://datatracker.ietf.org/doc/html/rfc7517 for details
public class WebKey {
    private String kid;
    private String kty;
    private String use;
    private String e;
    private String n;
    private String x5t;

    public WebKey(X509Certificate certificate) throws CertificateEncodingException {
        this.kid = Base64.encode(DigestUtils.sha256(certificate.getEncoded())).toString();

        RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
        this.kty = publicKey.getAlgorithm();
        this.use = "sig";
        this.e = Base64.encode(publicKey.getPublicExponent()).toString();
        this.n = Base64.encode(publicKey.getModulus()).toString();

        this.x5t = Base64.encode(DigestUtils.sha1(certificate.getEncoded())).toString();
    }

    public String getKid() {
        return kid;
    }

    public String getKty() {
        return kty;
    }

    public String getUse() {
        return use;
    }

    public String getE() {
        return e;
    }

    public String getN() {
        return n;
    }

    public String getX5t() {
        return x5t;
    }
}