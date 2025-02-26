package edu.ohsu.cmp.coach.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.nimbusds.jose.util.Base64;
import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.MyHttpException;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.fhir.jwt.AccessToken;
import edu.ohsu.cmp.coach.model.fhir.jwt.WebKey;
import edu.ohsu.cmp.coach.model.fhir.jwt.WebKeySet;
import edu.ohsu.cmp.coach.util.CryptoUtil;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class AccessTokenService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${smart.backend.clientId}")
    private String clientId;

    @Value("${smart.backend.scope}")
    private String scope;

    @Value("${smart.backend.iss}")
    private String backendIss;

    @Value("${smart.backend.jwt.x509-certificate-file:}")
    private String x509CertificateFilename;

    @Value("${smart.backend.jwt.pkcs8-private-key-file:}")
    private String pkcs8PrivateKeyFilename;

    @Value("${smart.backend.override-token-auth-url}")
    private String overrideTokenAuthUrl;

    @Value("${smart.backend.basic-auth.secret}")
    private String basicAuthSecret;

    private final Map<String, String> tokenAuthUrlMap = new HashMap<>();

    public boolean isAccessTokenEnabled() {
        return isJWTEnabled() || isBasicAuthEnabled();
    }

    public WebKeySet getWebKeySet() throws ConfigurationException {
        if ( ! isJWTEnabled() ) return null;

        WebKeySet webKeySet = new WebKeySet();
        File x509CertificateFile = new File(x509CertificateFilename);

        try {
            X509Certificate certificate = CryptoUtil.readCertificate(x509CertificateFile);
            webKeySet.add(new WebKey(certificate));

        } catch (Exception e) {
            throw new ConfigurationException("could not read x509CertificateFile=" + x509CertificateFilename, e);
        }

        return webKeySet;
    }

    public boolean isTokenValid(String token, String iss) {
        if ( ! isAccessTokenEnabled() ) return false;

        File x509CertificateFile = new File(x509CertificateFilename);
        File pkcs8PrivateKeyFile = new File(pkcs8PrivateKeyFilename);

        try {
            RSAPublicKey publicKey = (RSAPublicKey) CryptoUtil.readPublicKeyFromCertificate(x509CertificateFile);
            RSAPrivateKey privateKey = (RSAPrivateKey) CryptoUtil.readPrivateKey(pkcs8PrivateKeyFile);
            Algorithm algorithm = Algorithm.RSA384(publicKey, privateKey);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(iss)
                    .build();
            verifier.verify(token);
            return true;

        } catch (Exception e) {
            logger.warn("caught " + e.getClass().getName() + " validating JWT - " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * generate an Epic access token per specifications documented at
     * https://apporchard.epic.com/Article?docId=oauth2&section=Backend-Oauth2_Getting-Access-Token
     * @param fcc
     * @return
     */
    public AccessToken getAccessToken(FHIRCredentialsWithClient fcc) throws IOException, DataException, ConfigurationException {
        if ( ! isAccessTokenEnabled() ) return null;

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        requestHeaders.put("cache-control", "no-cache");
        requestHeaders.put("Accept", "application/json");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));

        String tokenAuthUrl = getTokenAuthUrl(fcc);

        if (isJWTEnabled()) {
            String jwt = createJWT(tokenAuthUrl);

            logger.debug("requesting access token from tokenAuthUrl=" + tokenAuthUrl + " with jwt=" + jwt);
            if (logger.isDebugEnabled()) {
                logger.debug("jwt is valid for tokenAuthUrl=" + tokenAuthUrl + "? " + isTokenValid(jwt, tokenAuthUrl));
            }

            params.add(new BasicNameValuePair("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
            params.add(new BasicNameValuePair("client_assertion", jwt));

        } else if (isBasicAuthEnabled()) {
            String authorization = "Basic " + Base64.encode(clientId + ":" + basicAuthSecret);

            logger.debug("requesting access token from tokenAuthUrl=" + tokenAuthUrl + " with authorization=" + authorization);

            requestHeaders.put("Authorization", authorization);

        } else {
            throw new CaseNotHandledException("unhandled case where both JWT and Basic authorization are disabled");
        }

        // I think this is required here for Oracle?
        if (StringUtils.isNotBlank(scope)) {
            params.add(new BasicNameValuePair("scope", scope));
        }

        String requestBody = URLEncodedUtils.format(params, StandardCharsets.UTF_8);

        HttpResponse httpResponse = new HttpRequest().post(tokenAuthUrl, null, requestHeaders, requestBody);

        int code = httpResponse.getResponseCode();
        String responseBody = httpResponse.getResponseBody();

        if (code < 200 || code > 299) {
            logger.error("received non-successful response to request for a access token from  url=" + tokenAuthUrl +
                    " with code " + code);
            logger.debug("requestBody=" + requestBody);
            logger.debug("responseBody=" + responseBody);
            throw new MyHttpException(code, responseBody);

        } else {
            Gson gson = new GsonBuilder().create();
            AccessToken accessToken = gson.fromJson(responseBody, new TypeToken<AccessToken>() {}.getType());
            logger.debug("received access token " + accessToken);
            return accessToken;
        }
    }


////////////////////////////////////////////////////////////////////////////
// private methods
//

    private boolean isJWTEnabled() {
        return StringUtils.isNotBlank(clientId) &&
                StringUtils.isNotBlank(x509CertificateFilename) &&
                StringUtils.isNotBlank(pkcs8PrivateKeyFilename);
    }

    private boolean isBasicAuthEnabled() {
        return StringUtils.isNotBlank(clientId) &&
                StringUtils.isNotBlank(basicAuthSecret);
    }

    private String getBackendServerURL(FHIRCredentialsWithClient fcc) {
        return StringUtils.isNotBlank(backendIss) ?
                backendIss :
                fcc.getCredentials().getServerURL();
    }

    private String createJWT(String tokenAuthUrl) throws ConfigurationException {
        if ( ! isAccessTokenEnabled() ) return null;

        // iss: clientId
        // sub: clientId (same as iss)
        // aud: tokenAuthUrl
        // kid: key ID, for JWKS association
        // jti: JWT ID, max 151 chars
        // exp: 5 minutes in the future, expressed as an integer 5 minutes in the future
        // scope: additional claim to specify requested scope

        File x509CertificateFile = new File(x509CertificateFilename);
        File pkcs8PrivateKeyFile = new File(pkcs8PrivateKeyFilename);

        try {
            X509Certificate certificate = CryptoUtil.readCertificate(x509CertificateFile);

            RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
            RSAPrivateKey privateKey = (RSAPrivateKey) CryptoUtil.readPrivateKey(pkcs8PrivateKeyFile);
            Algorithm algorithm = Algorithm.RSA384(publicKey, privateKey);

            String keyId = Base64.encode(DigestUtils.sha256(publicKey.getEncoded())).toString();
            String jwtId = Base64.encode(DigestUtils.sha256(CryptoUtil.randomBytes(32))).toString();

            // see: https://vendorservices.epic.com/Article?docId=oauth2&section=Creating-JWTs

            JWTCreator.Builder builder = JWT.create()
                    .withIssuer(clientId)
                    .withSubject(clientId)
                    .withAudience(tokenAuthUrl)
                    .withKeyId(keyId)
                    .withJWTId(jwtId)
                    .withExpiresAt(buildExpiresAt());

            // todo : also set "jku" header that contains a URL to the active COACH /jwt/jwks endpoint
            //        see https://www.rfc-editor.org/rfc/rfc7515#section-4.1.2 for details

            if (StringUtils.isNotBlank(scope)) {
                builder = builder.withClaim("scope", scope);
            }

            return builder.sign(algorithm);

        } catch (Exception e) {
            throw new ConfigurationException("could not instantiate object with iss=" + tokenAuthUrl +
                    ", x509CertificateFile=" + x509CertificateFile +
                    ", pkcs8PrivateKeyFile=" + pkcs8PrivateKeyFile, e);
        }
    }

    private String getTokenAuthUrl(FHIRCredentialsWithClient fcc) throws DataException, IOException {
        if (StringUtils.isNotBlank(overrideTokenAuthUrl)) {
            return overrideTokenAuthUrl;

        } else {
            String wellKnownTokenAuthUrl = getWellKnownTokenAuthUrl(fcc);
            if (StringUtils.isNotBlank(wellKnownTokenAuthUrl)) {
                return wellKnownTokenAuthUrl;

            } else {
                return FhirUtil.getTokenAuthenticationURL(fcc.getMetadata());
            }
        }
    }

    private String getWellKnownTokenAuthUrl(FHIRCredentialsWithClient fcc) throws IOException {
        final String backendServerUrl = getBackendServerURL(fcc);

        if ( ! tokenAuthUrlMap.containsKey(backendServerUrl) ) {
            String smartConfigurationUrl = backendServerUrl.endsWith("/") ?
                    backendServerUrl + ".well-known/smart-configuration" :
                    backendServerUrl + "/.well-known/smart-configuration";

            Map<String, String> requestHeaders = new LinkedHashMap<>();
            requestHeaders.put("Accept", "application/json");

            HttpResponse httpResponse = new HttpRequest().get(smartConfigurationUrl, null, requestHeaders);

            int code = httpResponse.getResponseCode();
            String responseBody = httpResponse.getResponseBody();

            if (code < 200 || code > 299) {
                logger.error("received non-successful response to request for SMART configuration for url=" + smartConfigurationUrl + " with code " + code);
                logger.debug("responseBody=" + responseBody);
                throw new MyHttpException(code, responseBody);

            } else {
                Gson gson = new GsonBuilder().create();
                JsonObject obj = gson.fromJson(responseBody, new TypeToken<JsonObject>() {}.getType());
                if (obj.has("token_endpoint")) {
                    String tokenAuthUrl = obj.get("token_endpoint").getAsString();
                    logger.debug("found token endpoint " + tokenAuthUrl);
                    tokenAuthUrlMap.put(backendServerUrl, tokenAuthUrl);
                }
            }
        }

        return tokenAuthUrlMap.get(backendServerUrl);
    }

    private Instant buildExpiresAt() {
        return LocalDateTime.now()
                .plusSeconds(30)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }
}
