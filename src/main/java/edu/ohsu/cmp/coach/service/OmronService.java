package edu.ohsu.cmp.coach.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.omron.AccessTokenResponse;
import edu.ohsu.cmp.coach.model.omron.OmronBloodPressureModel;
import edu.ohsu.cmp.coach.model.omron.RefreshTokenResponse;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OmronService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // todo : build out services for retrieving vitals data from Omron here

    @Value("${omron.application-id}")
    private String clientId;

    @Value("${omron.secret-key}")
    private String secretKey;

    @Value("${omron.scope}")
    private String scope;

    @Value("${omron.host.url}")
    private String hostUrl;

    @Value("${omron.redirect.url}")
    private String redirectUrl;

    public String getAuthorizationRequestUrl() throws DataException {
        try {
            URLCodec urlCodec = new URLCodec();
            return hostUrl + "/connect/authorize?client_id=" + urlCodec.encode(clientId) +
                    "&response_type=code&scope=" + urlCodec.encode(scope) +
                    "&redirect_uri=" + urlCodec.encode(redirectUrl);

        } catch (EncoderException e) {
            throw new DataException("caught " + e.getClass().getName() + " encoding Omron Authorization Request URL params - " + e.getMessage(), e);
        }
    }

    public AccessTokenResponse requestAccessToken(String authorizationCode) throws IOException, EncoderException {
        logger.debug("requesting access token for authorizationCode=" + authorizationCode);

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("client_id", clientId);
        bodyParams.put("client_secret", secretKey);
        bodyParams.put("grant_type", "authorization_code");
        bodyParams.put("scope", scope);
        bodyParams.put("redirect_uri", redirectUrl);
        bodyParams.put("code", authorizationCode);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Cache-Control", "no-cache");

        HttpResponse httpResponse = new HttpRequest().post(hostUrl + "/connect/token", null, headers, bodyParams);
        int code = httpResponse.getResponseCode();
        String body = httpResponse.getResponseBody();

        logger.debug("got response code=" + code + ", body=" + body);

        if (code < 200 || code > 299) {
            logger.error("Omron access token request error: " + body);
            return null;

        } else {
            Gson gson = new GsonBuilder().create();
            AccessTokenResponse accessTokenResponse = gson.fromJson(body, new TypeToken<AccessTokenResponse>() {}.getType());
            return accessTokenResponse;
        }
    }

    public RefreshTokenResponse refreshAccessToken(String refreshToken) throws IOException, EncoderException {
        logger.debug("refreshing access token for refreshToken=" + refreshToken);

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("client_id", clientId);
        bodyParams.put("client_secret", secretKey);
        bodyParams.put("grant_type", "refresh_token");
        bodyParams.put("redirect_uri", redirectUrl);
        bodyParams.put("refresh_token", refreshToken);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Cache-Control", "no-cache");

        HttpResponse httpResponse = new HttpRequest().post(hostUrl + "/connect/token", null, headers, bodyParams);
        int code = httpResponse.getResponseCode();
        String body = httpResponse.getResponseBody();

        logger.debug("got response code=" + code + ", body=" + body);

        if (code < 200 || code > 299) {
            logger.error("Omron access token refresh error: " + body);
            return null;

        } else {
            Gson gson = new GsonBuilder().create();
            RefreshTokenResponse refreshTokenResponse = gson.fromJson(body, new TypeToken<RefreshTokenResponse>() {}.getType());
            return refreshTokenResponse;
        }
    }

    public boolean revokeAccessToken(String accessToken) throws EncoderException, IOException {
        logger.debug("revoking accessToken=" + accessToken);

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("client_id", clientId);
        bodyParams.put("client_secret", secretKey);
        bodyParams.put("token", accessToken);
        bodyParams.put("token_type_hint", "access_token");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Cache-Control", "no-cache");

        HttpResponse httpResponse = new HttpRequest().post(hostUrl + "/connect/revocation", null, headers, bodyParams);
        int code = httpResponse.getResponseCode();

        logger.debug("got response code=" + code);

        if (code == 200) {
            return true;
        } else {
            logger.error("Omron access token revocation error: " + code);
            return false;
        }
    }

    public List<OmronBloodPressureModel> getBloodPressureMeasurements(String sessionId) throws EncoderException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        String accessToken = null; // todo : pull this from the user's workspace

        Map<String, String> bodyParams = new LinkedHashMap<>();
//        bodyParams.put("since", sinceTimestamp);  // todo : implement this.  Values: Valid JSON Date (12-01-2015, 12/01/2015, 2015-12-01)
//        bodyParams.put("limit", limit);           // optional
        bodyParams.put("type", "bloodpressure");
        bodyParams.put("includeHourlyActivity", "false");
//        bodyParams.put("sortOrder", "desc");        // optional

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        HttpResponse httpResponse = new HttpRequest().post(hostUrl + "/connect/revocation", null, headers, bodyParams);
        int code = httpResponse.getResponseCode();
        String body = httpResponse.getResponseBody();

        if (code < 200 || code > 299) {
            logger.error("Omron access token refresh error: " + body);
            return null;

        } else {
            logger.debug("got body (PARSE THIS WITH GSON): " + body);
//            Gson gson = new GsonBuilder().create();
//            RefreshTokenResponse refreshTokenResponse = gson.fromJson(body, new TypeToken<RefreshTokenResponse>() {}.getType());
//            return refreshTokenResponse;
            return null;
        }
    }
}
