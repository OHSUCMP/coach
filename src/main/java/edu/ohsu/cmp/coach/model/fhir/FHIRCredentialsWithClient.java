package edu.ohsu.cmp.coach.model.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.CapabilityStatement;

public class FHIRCredentialsWithClient {
    private FHIRCredentials credentials;
    private IGenericClient client;
    private CapabilityStatement metadata = null;

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

    public CapabilityStatement getMetadata() {
        if (metadata == null) {
            metadata = client.capabilities()
                    .ofType(CapabilityStatement.class)
                    .execute();
        }
        return metadata;
    }
}
