package edu.ohsu.cmp.htnu18app.cqfruler;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSServices;
import edu.ohsu.cmp.htnu18app.cqfruler.model.Card;
import edu.ohsu.cmp.htnu18app.cqfruler.model.HookRequest;
import edu.ohsu.cmp.htnu18app.registry.model.FHIRCredentials;
import edu.ohsu.cmp.htnu18app.registry.model.FHIRCredentialsWithClient;
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
import java.util.ArrayList;
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

    public List<Card> executeHook(FHIRCredentialsWithClient credentialsWithClient, String hookId) throws IOException {
        FHIRCredentials credentials = credentialsWithClient.getCredentials();
        IGenericClient client = credentialsWithClient.getClient();

        Patient p = patientService.getPatient(client, credentials.getPatientId());
        Bundle bpBundle = patientService.getBloodPressureObservations(client, credentials.getPatientId());

        HookRequest request = new HookRequest(credentials, p, bpBundle);

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
        StringWriter writer = new StringWriter();
        mustache.execute(writer, request).flush();

        logger.info("hookRequest = " + writer.toString());

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json; charset=UTF-8");

        String response = HttpUtil.post(cdsHooksEndpointURL + "/" + hookId, headers, writer.toString());

        logger.info("got response: " + response);

        List<Card> list = new ArrayList<Card>();

        // todo : populate cards

        return list;
    }
}
