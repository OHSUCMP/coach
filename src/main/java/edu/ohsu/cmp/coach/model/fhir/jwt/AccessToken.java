package edu.ohsu.cmp.coach.model.fhir.jwt;

import com.google.gson.annotations.SerializedName;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessToken {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_in")
    private Integer expiresIn;

    private String scope;

    @Override
    public String toString() {
        return "AccessToken{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", scope='" + scope + '\'' +
                '}';
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean providesWriteAccess(Class<? extends Resource> clazz) {
        String key = "system/" + clazz.getName() + ".write";
        if (logger.isDebugEnabled()) {
            logger.debug("checking scope.contains(" + key + ")? " + scope.contains(key));
        }
        return scope != null && scope.contains(key);
    }
}
