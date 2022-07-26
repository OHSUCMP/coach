package edu.ohsu.cmp.coach.model.cqfruler;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentials;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HookRequest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String hookInstanceUUID;
    private String fhirServerURL;
    private String bearerToken;
    private String userId;
    private String patientId;
    private String prefetch;
//    private Map<String, IBaseResource> prefetch;

    private boolean prefetchModified = false;

    public HookRequest(FHIRCredentials credentials) {
        this(credentials, null);
    }

    public HookRequest(FHIRCredentials credentials, List<IBaseResource> prefetchList) {
        this.hookInstanceUUID = UUID.randomUUID().toString();
        this.fhirServerURL = credentials.getServerURL();
        this.bearerToken = credentials.getBearerToken();
        this.userId = credentials.getUserId();
        this.patientId = credentials.getPatientId();

        if (prefetchList != null && prefetchList.size() > 0) {
            // need to build prefetch as a serialized string here, as we're creating multiple items
            // with additional attributes, and mustache templates just aren't complex enough to build
            // this out.  womp womp
            FhirContext ctx = FhirContext.forR4();
            IParser jsonParser = ctx.newJsonParser().setPrettyPrint(false);

            List<String> list = new ArrayList<>();
            int itemNo = 1;
            for (IBaseResource item : prefetchList) {
                if (item instanceof Bundle) {
                    Bundle bundle = (Bundle) item;
                    if ( ! bundle.hasEntry() || bundle.getEntry().isEmpty() ) {
                        continue;
                    }
                }

                SanitizeResponse sr = sanitize(jsonParser.encodeResourceToString(item), StandardCharsets.US_ASCII);
                this.prefetchModified = sr.isModified();

                String s = "\"item" + itemNo + "\":{" +
                        "\"response\":{\"status\":\"200 OK\"}," +
                        "\"resource\":" + sr.getJsonSanitized() + "}";

                list.add(s);

                itemNo ++;
            }

            this.prefetch = StringUtils.join(list, ",\n");
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

    public boolean isPrefetchModified() {
        return prefetchModified;
    }


//////////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private SanitizeResponse sanitize(String json, Charset charset) {
        ByteBuffer buffer = charset.encode(json);
        String jsonSanitized = charset.decode(buffer).toString();

        boolean modified = ! StringUtils.equals(json, jsonSanitized);
        if (modified) {
            logger.warn("JSON contains non-" + charset.name() + " characters.  Offending characters have been replaced with sentinels.");
            logger.debug("JSON = " + json);
            logger.debug("JSON-" + charset.name() + " = " + jsonSanitized);
        }

        return new SanitizeResponse(jsonSanitized, modified);
    }

    private static final class SanitizeResponse {
        private String jsonSanitized;
        private boolean modified;

        public SanitizeResponse(String jsonSanitized, boolean modified) {
            this.jsonSanitized = jsonSanitized;
            this.modified = modified;
        }

        public String getJsonSanitized() {
            return jsonSanitized;
        }

        public boolean isModified() {
            return modified;
        }
    }
}
