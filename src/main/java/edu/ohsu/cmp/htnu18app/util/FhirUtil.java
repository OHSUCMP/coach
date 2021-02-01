package edu.ohsu.cmp.htnu18app.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;

public class FhirUtil {
    public static IGenericClient buildClient(String serverUrl, String bearerToken) {
        FhirContext ctx = FhirContext.forR4();
        IGenericClient client = ctx.newRestfulGenericClient(serverUrl);

        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(bearerToken);
        client.registerInterceptor(authInterceptor);

        return client;
    }

}
