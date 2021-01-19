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

//        String json = HttpUtil.get(cdsHooksEndpointURL);

// json object for testing w/o CQF ruler call
        String json = "{  \"services\": [    {      \"hook\": \"patient-view\",      \"name\": \"Hypertension\",      \"title\": \"OHSU Hypertension\",      \"description\": \"This PlanDefinition identifies hypertension\",      \"id\": \"plandefinition-Hypertension\",      \"prefetch\": {        \"item1\": \"Patient?_id\\u003d{{context.patientId}}\",        \"item2\": \"Observation?subject\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://loinc.org|55284-4\",        \"item3\": \"Encounter?patient\\u003dPatient/{{context.patientId}}\",        \"item4\": \"Condition?patient\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://snomed.info/sct|111438007,http://snomed.info/sct|123799005,http://snomed.info/sct|123800009,http://snomed.info/sct|14973001,http://snomed.info/sct|169465000,http://snomed.info/sct|194783001,http://snomed.info/sct|194785008,http://snomed.info/sct|194788005,http://snomed.info/sct|194791005,http://snomed.info/sct|199008003,http://snomed.info/sct|26078007,http://snomed.info/sct|28119000,http://snomed.info/sct|31992008,http://snomed.info/sct|39018007,http://snomed.info/sct|39727004,http://snomed.info/sct|427889009,http://snomed.info/sct|428575007,http://snomed.info/sct|48552006,http://snomed.info/sct|57684003,http://snomed.info/sct|73410007,http://snomed.info/sct|74451002,http://snomed.info/sct|89242004\",        \"item5\": \"Procedure?patient\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://snomed.info/sct|164783007,http://snomed.info/sct|413153004,http://snomed.info/sct|448489007,http://snomed.info/sct|448678005,http://www.ama-assn.org/go/cpt|93784,http://www.ama-assn.org/go/cpt|93786,http://www.ama-assn.org/go/cpt|93788,http://www.ama-assn.org/go/cpt|93790\"      }    }  ]}\n";

        Gson gson = new GsonBuilder().create();
        CDSServices services = gson.fromJson(json, new TypeToken<CDSServices>(){}.getType());

        return services.getHooks();
    }

    public List<Card> executeHook(String sessionId, String hookId) throws IOException {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        if ( ! cache.containsCards(hookId) ) {
            Patient p = patientService.getPatient(sessionId);
            Bundle bpBundle = patientService.getBloodPressureObservations(sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();

            HookRequest request = new HookRequest(fcc.getCredentials(), p, bpBundle);

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, request).flush();

            logger.info("hookRequest = " + writer.toString());

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json; charset=UTF-8");

//        String json = HttpUtil.post(cdsHooksEndpointURL + "/" + hookId, headers, writer.toString());

// json object for testing w/o CQF ruler call
            String json = "{  \"cards\": [    {      \"summary\": \"Hypertension Diagnosis\",      \"indicator\": \"info\",      \"source\": {        \"label\": \"Info for those with normal blood pressure\",        \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\"      }    },    {      \"summary\": \"Patient may have Stage 1 Hypertension.\",      \"indicator\": \"warning\",      \"detail\": \"Consider diagnosis of stage 1 HTN.\",      \"source\": {}    }  ]}";

            Gson gson = new GsonBuilder().create();
            CDSHookResponse response = gson.fromJson(json, new TypeToken<CDSHookResponse>(){}.getType());

            cache.setCards(hookId, response.getCards());
        }

        return cache.getCards(hookId);
    }
}
