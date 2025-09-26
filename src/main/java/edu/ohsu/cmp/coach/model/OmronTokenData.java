package edu.ohsu.cmp.coach.model;

import edu.ohsu.cmp.coach.model.omron.AccessTokenResponse;
import edu.ohsu.cmp.coach.model.omron.RefreshTokenResponse;

import java.util.Calendar;
import java.util.Date;

public class OmronTokenData {
    private String userIdToken;
    private String bearerToken;
    private Date expirationTimestamp;
    private String refreshToken;

    public OmronTokenData(AccessTokenResponse accessTokenResponse) {
        this.userIdToken = accessTokenResponse.getIdToken();
        this.bearerToken = accessTokenResponse.getAccessToken();

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.SECOND, accessTokenResponse.getExpiresIn());
        this.expirationTimestamp = cal.getTime();

        this.refreshToken = accessTokenResponse.getRefreshToken();
    }

    public void update(RefreshTokenResponse refreshTokenResponse) {
        this.bearerToken = refreshTokenResponse.getAccessToken();

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.SECOND, refreshTokenResponse.getExpiresIn());
        this.expirationTimestamp = cal.getTime();

        this.refreshToken = refreshTokenResponse.getRefreshToken();
    }

    public String getUserIdToken() {
        return userIdToken;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public Date getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
