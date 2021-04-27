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
import edu.ohsu.cmp.htnu18app.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.http.HttpRequest;
import edu.ohsu.cmp.htnu18app.http.HttpResponse;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.model.recommendation.Card;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Service
public class CQFRulerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String cdsHooksEndpointURL;

    @Autowired
    private PatientService patientService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    public CQFRulerService(@Value("${cqfruler.cdshooks.endpoint.url}") String cdsHooksEndpointURL) {
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
    }

    public List<CDSHook> getCDSHooks() throws IOException {
        logger.info("getting " + cdsHooksEndpointURL);

        HttpResponse response = new HttpRequest().get(cdsHooksEndpointURL);
        String json = response.getResponseBody();

// json object for testing w/o CQF ruler call
//        String json = "{  \"services\": [    {      \"hook\": \"patient-view\",      \"name\": \"Hypertension\",      \"title\": \"OHSU Hypertension\",      \"description\": \"This PlanDefinition identifies hypertension\",      \"id\": \"plandefinition-Hypertension\",      \"prefetch\": {        \"item1\": \"Patient?_id\\u003d{{context.patientId}}\",        \"item2\": \"Observation?subject\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://loinc.org|55284-4\",        \"item3\": \"Encounter?patient\\u003dPatient/{{context.patientId}}\",        \"item4\": \"Condition?patient\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://snomed.info/sct|111438007,http://snomed.info/sct|123799005,http://snomed.info/sct|123800009,http://snomed.info/sct|14973001,http://snomed.info/sct|169465000,http://snomed.info/sct|194783001,http://snomed.info/sct|194785008,http://snomed.info/sct|194788005,http://snomed.info/sct|194791005,http://snomed.info/sct|199008003,http://snomed.info/sct|26078007,http://snomed.info/sct|28119000,http://snomed.info/sct|31992008,http://snomed.info/sct|39018007,http://snomed.info/sct|39727004,http://snomed.info/sct|427889009,http://snomed.info/sct|428575007,http://snomed.info/sct|48552006,http://snomed.info/sct|57684003,http://snomed.info/sct|73410007,http://snomed.info/sct|74451002,http://snomed.info/sct|89242004\",        \"item5\": \"Procedure?patient\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://snomed.info/sct|164783007,http://snomed.info/sct|413153004,http://snomed.info/sct|448489007,http://snomed.info/sct|448678005,http://www.ama-assn.org/go/cpt|93784,http://www.ama-assn.org/go/cpt|93786,http://www.ama-assn.org/go/cpt|93788,http://www.ama-assn.org/go/cpt|93790\"      }    }  ]}\n";

        Gson gson = new GsonBuilder().create();
        CDSServices services = gson.fromJson(json, new TypeToken<CDSServices>(){}.getType());

        return services.getHooks();
    }

    public List<Card> executeHook(String sessionId, String hookId) throws IOException {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        if ( ! cache.containsCards(hookId) ) {
            Patient p = patientService.getPatient(sessionId);

            Bundle bpBundle = patientService.getBloodPressureObservations(sessionId);

            // inject home blood pressure readings into Bundle for evaluation by CQF Ruler
            List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
            for (HomeBloodPressureReading item : hbprList) {
                String uuid = UUID.randomUUID().toString();

                Encounter e = buildEncounter(uuid, p.getId(), item.getReadingDate());
                bpBundle.addEntry().setFullUrl("http://hl7.org/fhir/Encounter/" + e.getId()).setResource(e);

                Observation o = buildBloodPressureObservation(uuid, p.getId(), e.getId(), item);

                // todo: should the URL be different?
                bpBundle.addEntry().setFullUrl("http://hl7.org/fhir/Observation/" + o.getId()).setResource(o);
            }

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();

            HookRequest request = new HookRequest(fcc.getCredentials(), p, bpBundle);

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, request).flush();

            logger.info("hookRequest = " + writer.toString());

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json; charset=UTF-8");

            HttpResponse httpResponse  = new HttpRequest().post(cdsHooksEndpointURL + "/" + hookId, null, headers, writer.toString());
            String json = httpResponse.getResponseBody();

// json object for testing w/o CQF ruler call
//            String json = "{ \"cards\": [ { \"summary\": \"Hypertension Diagnosis\", \"indicator\": \"info\", \"detail\": \"ConsiderHTNStage1 Patient\", \"source\": { \"label\": \"Info for those with normal blood pressure\", \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\" } }, { \"summary\": \"Recommend diagnosis of Stage 2 hypertension\", \"indicator\": \"warning\", \"detail\": \"You had a high blood pressure reading recently. Please see your provider to lower your blood pressure and reduce your risk of stroke or other adverse events. More severe hypertension, stage 2 hypertension is a systolic pressure of 140 mm Hg or higher or a diastolic pressure of 90 mm Hg or higher.;https://www.ahajournals.org/doi/10.1161/HYPERTENSIONAHA.120.15020;;at-most-one;https://www.heart.org/en/health-topics/high-blood-pressure/understanding-blood-pressure-readings\", \"source\": {} } ] }";
//            String json = "{ \"cards\": [ { \"summary\": \"Hypertension Diagnosis\", \"indicator\": \"info\", \"detail\": \"ConsiderHTNStage1 Patient\", \"source\": { \"label\": \"Info for those with normal blood pressure\", \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\" } }, { \"summary\": \"Recommend diagnosis of Stage 2 hypertension\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}Patient rationale{{/patient}}{{#careTeam}}care team rationale{{/careTeam}};{{#patient}}https://source.com/patient{{/patient}}{{#careTeam}}https://source.com/careTeam{{/careTeam}};{{#patient}}https://suggestions.com/patient{{/patient}}{{#careTeam}}https://suggestions.com/careTeam{{/careTeam}};at-most-one;<ol>{{#patient}}<li>https://links.com/patient</li>{{/patient}}<li>https://links.com/careTeamAndPatient</li></ol>\", \"source\": {} } ] }";
//            String json = "{ \"cards\": [ { \"summary\": \"Hypertension Diagnosis\", \"indicator\": \"info\", \"detail\": \"ConsiderHTNStage1 Patient\", \"source\": { \"label\": \"Info for those with normal blood pressure\", \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\" } }, { \"summary\": \"Recommend diagnosis of Stage 2 hypertension\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}Patient rationale{{/patient}}{{#careTeam}}care team rationale{{/careTeam}}|{{#patient}}https://source.com/patient{{/patient}}{{#careTeam}}https://source.com/careTeam{{/careTeam}}|[ { \\\"label\\\": \\\"Enter Blood Pressure\\\", \\\"actions\\\": [ \\\"ServiceRequest for High Blood Pressure Monitoring\\\", \\\"{{#patient}}<a href='/bp-readings'>Click here to go to the Home Blood Pressure entry page</a>{{/patient}}\\\" ] }, { \\\"label\\\": \\\"Diet\\\", \\\"actions\\\": [ \\\"Put the fork down!\\\", \\\"{{#patient}}<input type='checkbox' class='goal' data-goalid='dashDiet' /><label>Try the DASH Diet</label>{{/patient}}\\\" ] } ]|at-most-one|<ol>{{#patient}}<li>https://links.com/patient</li>{{/patient}}<li>https://links.com/careTeamAndPatient</li></ol>\", \"source\": {} } ] }";

            Gson gson = new GsonBuilder().create();
            try {
                CDSHookResponse response = gson.fromJson(json, new TypeToken<CDSHookResponse>() {
                }.getType());

                List<Card> cards = new ArrayList<Card>();
                for (CDSCard cdsCard : response.getCards()) {
                    cards.add(new Card(cache.getAudience(), cdsCard));
                }

                cache.setCards(hookId, cards);

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " processing response for hookId=" + hookId + " - " + e.getMessage(), e);
                logger.debug("JSON = " + json);
            }
        }

        return cache.getCards(hookId);
    }

////////////////////////////////////////////////////////////////////////
// private methods
//

    private Encounter buildEncounter(String uuid, String patientId, Date date) {
        Encounter e = new Encounter();

        e.setId("encounter-" + uuid);
        e.setStatus(Encounter.EncounterStatus.FINISHED);
        e.getClass_().setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode").setCode("AMB").setDisplay("ambulatory");

        e.setSubject(new Reference().setReference(patientId));

        Calendar start = Calendar.getInstance();
        start.setTime(date);
        start.add(Calendar.MINUTE, -1);

        Calendar end = Calendar.getInstance();
        end.setTime(date);
        end.add(Calendar.MINUTE, 1);

        e.getPeriod().setStart(start.getTime()).setEnd(end.getTime());

        return e;
    }

    private Observation buildBloodPressureObservation(String uuid, String patientId, String encounterId, HomeBloodPressureReading item) {
        // adapted from https://www.programcreek.com/java-api-examples/?api=org.hl7.fhir.dstu3.model.Observation

        Observation o = new Observation();

        o.setId("observation-bp-" + uuid);
        o.setSubject(new Reference().setReference(patientId));
        o.setEncounter(new Reference().setReference(encounterId));
        o.setStatus(Observation.ObservationStatus.FINAL);
        o.getCode().addCoding().setCode(BloodPressureModel.CODE).setSystem(BloodPressureModel.SYSTEM);
        o.setEffective(new DateTimeType(item.getReadingDate()));

        Observation.ObservationComponentComponent systolic = new Observation.ObservationComponentComponent();
        systolic.getCode().addCoding().setCode(BloodPressureModel.SYSTOLIC_CODE).setSystem(BloodPressureModel.SYSTEM);
        systolic.setValue(new Quantity());
        systolic.getValueQuantity().setCode(BloodPressureModel.VALUE_CODE);
        systolic.getValueQuantity().setSystem(BloodPressureModel.VALUE_SYSTEM);
        systolic.getValueQuantity().setUnit(BloodPressureModel.VALUE_UNIT);
        systolic.getValueQuantity().setValue(item.getSystolic());
        o.getComponent().add(systolic);

        Observation.ObservationComponentComponent diastolic = new Observation.ObservationComponentComponent();
        diastolic.getCode().addCoding().setCode(BloodPressureModel.DIASTOLIC_CODE).setSystem(BloodPressureModel.SYSTEM);
        diastolic.setValue(new Quantity());
        diastolic.getValueQuantity().setCode(BloodPressureModel.VALUE_CODE);
        diastolic.getValueQuantity().setSystem(BloodPressureModel.VALUE_SYSTEM);
        diastolic.getValueQuantity().setUnit(BloodPressureModel.VALUE_UNIT);
        diastolic.getValueQuantity().setValue(item.getDiastolic());
        o.getComponent().add(diastolic);

        return o;
    }
}
