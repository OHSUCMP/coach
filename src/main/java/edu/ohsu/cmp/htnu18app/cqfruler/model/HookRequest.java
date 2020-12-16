package edu.ohsu.cmp.htnu18app.cqfruler.model;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import edu.ohsu.cmp.htnu18app.registry.model.FHIRCredentials;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.UUID;

public class HookRequest {
    private String hookInstanceUUID;
    private String fhirServerURL;
    private String bearerToken;
    private String userId;
    private String patientId;
    private String prefetch;
//    private Map<String, IBaseResource> prefetch;

    public HookRequest(FHIRCredentials credentials) {
        this(credentials, null);
    }

    public HookRequest(FHIRCredentials credentials, IBaseResource ... prefetchArr) {
        this.hookInstanceUUID = UUID.randomUUID().toString();
        this.fhirServerURL = credentials.getServerURL();
        this.bearerToken = credentials.getBearerToken();
        this.userId = credentials.getUserId();
        this.patientId = credentials.getPatientId();

        if (prefetchArr != null && prefetchArr.length > 0) {
            // need to build prefetch as a serialized string here, as we're creating multiple items
            // with additional attributes, and mustache templates just aren't complex enough to build
            // this out.  womp womp
            FhirContext ctx = FhirContext.forR4();
            IParser jsonParser = ctx.newJsonParser().setPrettyPrint(false);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < prefetchArr.length; i ++) {
                sb.append("\"item").append(i).append("\":{");

                sb.append("\"response\":{\"status\":\"200 OK\"},");

                IBaseResource item = prefetchArr[i];
                String s = jsonParser.encodeResourceToString(item);
                sb.append("\"resource\":").append(s);

                sb.append("}"); // close item

                if (i < prefetchArr.length - 1) {
                    sb.append(",\n");
                }
            }
            this.prefetch = sb.toString();
        }
    }

    public String getHookInstanceUUID() {
        return hookInstanceUUID;
    }

    public String getFhirServerURL() {
        return fhirServerURL;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPrefetch() {
        return prefetch;
    }
}
