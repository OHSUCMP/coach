package edu.ohsu.cmp.htnu18app.model.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;

public class FHIRCredentialsWithClient {
    private FHIRCredentials credentials;
    private IGenericClient client;

    public FHIRCredentialsWithClient(FHIRCredentials credentials, IGenericClient client) {
        this.credentials = credentials;
        this.client = client;
    }

    public FHIRCredentials getCredentials() {
        return credentials;
    }

    public IGenericClient getClient() {
        return client;
    }
}
