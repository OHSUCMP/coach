package edu.ohsu.cmp.coach.session;

import edu.ohsu.cmp.coach.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.coach.model.Audience;

public class ProvisionalSessionCacheData {
    private FHIRCredentials credentials;
    private Audience audience;

    public ProvisionalSessionCacheData(FHIRCredentials credentials, Audience audience) {
        this.credentials = credentials;
        this.audience = audience;
    }

    public FHIRCredentials getCredentials() {
        return credentials;
    }

    public Audience getAudience() {
        return audience;
    }
}
