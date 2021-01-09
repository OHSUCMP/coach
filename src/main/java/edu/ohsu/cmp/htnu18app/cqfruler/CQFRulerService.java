package edu.ohsu.cmp.htnu18app.cqfruler;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.cqfruler.model.*;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import edu.ohsu.cmp.htnu18app.util.HttpUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CQFRulerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String cdsHooksEndpointURL;

    @Autowired
    private PatientService patientService;

    public CQFRulerService(@Value("${cqfruler.cdshooks.endpoint.url}") String cdsHooksEndpointURL) {
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
    }

    public List<CDSHook> getCDSHooks() throws IOException {
        logger.info("getting " + cdsHooksEndpointURL);

        String json = HttpUtil.get(cdsHooksEndpointURL);
        logger.info("response: " + json);

        Gson gson = new GsonBuilder().create();
        CDSServices services = gson.fromJson(json, new TypeToken<CDSServices>(){}.getType());

        return services.getHooks();
    }

    public List<Card> executeHook(String sessionId, String hookId) throws IOException {
        Patient p = patientService.getPatient(sessionId);
        Bundle bpBundle = patientService.getBloodPressureObservations(sessionId);

        CacheData cache = SessionCache.getInstance().get(sessionId);
        FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();

        HookRequest request = new HookRequest(fcc.getCredentials(), p, bpBundle);

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
        StringWriter writer = new StringWriter();
        mustache.execute(writer, request).flush();

        logger.info("hookRequest = " + writer.toString());

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json; charset=UTF-8");

        String json = HttpUtil.post(cdsHooksEndpointURL + "/" + hookId, headers, writer.toString());

// json object for testing w/o CQF ruler call
//        String json = "{  \"cards\": [    {      \"summary\": \"Hypertension Diagnosis\",      \"indicator\": \"info\",      \"source\": {        \"label\": \"Info for those with normal blood pressure\",        \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\"      }    },    {      \"summary\": \"Patient may have Stage 1 Hypertension.\",      \"indicator\": \"warning\",      \"detail\": \"Consider diagnosis of stage 1 HTN.\",      \"source\": {}    }  ]}";

        Gson gson = new GsonBuilder().create();
        CDSHookResponse response = gson.fromJson(json, new TypeToken<CDSHookResponse>(){}.getType());

        return response.getCards();
    }
}
