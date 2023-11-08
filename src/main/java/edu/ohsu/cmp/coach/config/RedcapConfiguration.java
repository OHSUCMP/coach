package edu.ohsu.cmp.coach.config;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix="redcap")
@Configuration
public class RedcapConfiguration {

    @NotNull
    private Boolean enabled;

    private String apiUrl;

    private String apiToken;

    private String dataAccessGroup;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getDataAccessGroup() {
        return dataAccessGroup;
    }

    public void setDataAccessGroup(String dataAccessGroup) {
        this.dataAccessGroup = dataAccessGroup;
    }

}