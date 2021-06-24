package edu.ohsu.cmp.htnu18app.cqfruler;

import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import edu.ohsu.cmp.htnu18app.service.GoalService;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class CQFRulerService {
    private static final boolean TESTING = false; // true: use hard-coded 'canned' responses from CQF Ruler (fast, cheap)
                                                  // false: make CQF Ruler calls (slow, expensive)

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String cdsHooksEndpointURL;

    @Autowired
    private PatientService patientService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    @Autowired
    private GoalService goalService;

    public CQFRulerService(@Value("${cqfruler.cdshooks.endpoint.url}") String cdsHooksEndpointURL) {
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
    }

    public void executeHooksDetached(String sessionId) {
        CDSHookExecutor executor = new CDSHookExecutor(TESTING, sessionId, cdsHooksEndpointURL, patientService, hbprService, goalService);
        logger.info("created " + executor);

        Thread t = new Thread(executor);
        t.start();
    }

    public List<CDSHook> getCDSHooks() throws IOException {
        return CDSHooksUtil.getCDSHooks(TESTING, cdsHooksEndpointURL);
    }

//    public List<Card> executeHook(String sessionId, String hookId) throws IOException {
//        CacheData cache = SessionCache.getInstance().get(sessionId);
//        if ( ! cache.containsCards(hookId) ) {
//            cache.setCards(hookId, null); // placeholder so subsequent calls don't re-trigger the call
//
//            try {
//                Patient p = patientService.getPatient(sessionId);
//
//                Bundle bpBundle = patientService.getBloodPressureObservations(sessionId);
//
//                // inject home blood pressure readings into Bundle for evaluation by CQF Ruler
//                List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
//                for (HomeBloodPressureReading item : hbprList) {
//                    String uuid = UUID.randomUUID().toString();
//
//                    Encounter e = buildEncounter(uuid, p.getId(), item.getReadingDate());
//                    bpBundle.addEntry().setFullUrl("http://hl7.org/fhir/Encounter/" + e.getId()).setResource(e);
//
//                    Observation o = buildBloodPressureObservation(uuid, p.getId(), e.getId(), item);
//
//                    // todo: should the URL be different?
//                    bpBundle.addEntry().setFullUrl("http://hl7.org/fhir/Observation/" + o.getId()).setResource(o);
//                }
//
//                FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
//
//                HookRequest request = new HookRequest(fcc.getCredentials(), p, bpBundle);
//
//                MustacheFactory mf = new DefaultMustacheFactory();
//                Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
//                StringWriter writer = new StringWriter();
//                mustache.execute(writer, request).flush();
//
//                logger.info("hookRequest = " + writer.toString());
//
//                Map<String, String> headers = new HashMap<>();
//                headers.put("Content-Type", "application/json; charset=UTF-8");
//
//                String json;
//                if (TESTING) {
////                json = "{ \"cards\": [ { \"summary\": \"Hypertension Diagnosis\", \"indicator\": \"info\", \"detail\": \"ConsiderHTNStage1 Patient\", \"source\": { \"label\": \"Info for those with normal blood pressure\", \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\" } }, { \"summary\": \"Recommend diagnosis of Stage 2 hypertension\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}Patient rationale{{/patient}}{{#careTeam}}care team rationale{{/careTeam}}|{{#patient}}https://source.com/patient{{/patient}}{{#careTeam}}https://source.com/careTeam{{/careTeam}}|[ { \\\"label\\\": \\\"Enter Blood Pressure\\\", \\\"actions\\\": [ \\\"ServiceRequest for High Blood Pressure Monitoring\\\", \\\"{{#patient}}<a href='/bp-readings'>Click here to go to the Home Blood Pressure entry page</a>{{/patient}}\\\" ] }, { \\\"label\\\": \\\"Diet\\\", \\\"actions\\\": [ \\\"Put the fork down!\\\", \\\"{{#patient}}<input type='checkbox' class='goal' data-goalid='dashDiet' /><label>Try the DASH Diet</label>{{/patient}}\\\" ] } ]|at-most-one|<ol>{{#patient}}<li>https://links.com/patient</li>{{/patient}}<li>https://links.com/careTeamAndPatient</li></ol>\", \"source\": {} } ] }";
//                    json = "{ \"cards\": [ { \"summary\": \"Hypertension Diagnosis\", \"indicator\": \"info\", \"detail\": \"ConsiderHTNStage1 Patient\", \"source\": { \"label\": \"Info for those with normal blood pressure\", \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\" } }, { \"summary\": \"Recommend diagnosis of Stage 2 hypertension\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}Patient rationale{{/patient}}{{#careTeam}}care team rationale{{/careTeam}}|{{#patient}}https://source.com/patient{{/patient}}{{#careTeam}}https://source.com/careTeam{{/careTeam}}|[ { \\\"label\\\": \\\"Enter Blood Pressure\\\", \\\"actions\\\": [ \\\"ServiceRequest for High Blood Pressure Monitoring\\\", \\\"{{#patient}}<a href='/bp-readings'>Click here to go to the Home Blood Pressure entry page</a>{{/patient}}\\\" ] }, { \\\"label\\\": \\\"Diet\\\", \\\"actions\\\": [ \\\"Put the fork down!\\\", \\\"{{#patient}}<input type='checkbox' class='goal' data-goalid='dashDiet' /><label>Try the DASH Diet</label>{{/patient}}\\\" ] } ]|at-most-one|[{{#patient}}{ \\\"label\\\": \\\"Patient Link\\\", \\\"url\\\": \\\"https://links.com/patient\\\" },{{/patient}} { \\\"label\\\": \\\"Care Team and Patient Link\\\", \\\"url\\\": \\\"https://links.com/careTeamAndPatient\\\" }]\", \"source\": {} } ] }";
//
//                } else {
//                    HttpResponse httpResponse  = new HttpRequest().post(cdsHooksEndpointURL + "/" + hookId, null, headers, writer.toString());
//                    json = httpResponse.getResponseBody();
//                }
//
//                Gson gson = new GsonBuilder().create();
//                try {
//                    json = MustacheUtil.compileMustache(cache.getAudience(), json);
//                    CDSHookResponse response = gson.fromJson(json, new TypeToken<CDSHookResponse>() {}.getType());
//
//                    List<Card> cards = new ArrayList<>();
//                    for (CDSCard cdsCard : response.getCards()) {
//                        cards.add(new Card(cdsCard));
//                    }
//
//                    cache.setCards(hookId, cards);
//
//                } catch (Exception e) {
//                    logger.error("caught " + e.getClass().getName() + " processing response for hookId=" + hookId + " - " + e.getMessage(), e);
//                    logger.error("\n\nJSON =\n" + json + "\n\n");
//                    throw e;
//                }
//
//            } catch (Exception e) {
//                logger.error("caught " + e.getClass().getName() + " processing hookId=" + hookId + " - " + e.getMessage(), e);
//                cache.deleteCards(hookId);
//
//                if (e instanceof IOException) {
//                    throw (IOException) e;
//                }
//            }
//        }
//
//        return cache.getCards(hookId);
//    }

////////////////////////////////////////////////////////////////////////
// private methods
//

//    private Encounter buildEncounter(String uuid, String patientId, Date date) {
//        Encounter e = new Encounter();
//
//        e.setId("encounter-" + uuid);
//        e.setStatus(Encounter.EncounterStatus.FINISHED);
//        e.getClass_().setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode").setCode("AMB").setDisplay("ambulatory");
//
//        e.setSubject(new Reference().setReference(patientId));
//
//        Calendar start = Calendar.getInstance();
//        start.setTime(date);
//        start.add(Calendar.MINUTE, -1);
//
//        Calendar end = Calendar.getInstance();
//        end.setTime(date);
//        end.add(Calendar.MINUTE, 1);
//
//        e.getPeriod().setStart(start.getTime()).setEnd(end.getTime());
//
//        return e;
//    }

//    private Observation buildBloodPressureObservation(String uuid, String patientId, String encounterId, HomeBloodPressureReading item) {
//        // adapted from https://www.programcreek.com/java-api-examples/?api=org.hl7.fhir.dstu3.model.Observation
//
//        Observation o = new Observation();
//
//        o.setId("observation-bp-" + uuid);
//        o.setSubject(new Reference().setReference(patientId));
//        o.setEncounter(new Reference().setReference(encounterId));
//        o.setStatus(Observation.ObservationStatus.FINAL);
//        o.getCode().addCoding().setCode(BloodPressureModel.CODE).setSystem(BloodPressureModel.SYSTEM);
//        o.setEffective(new DateTimeType(item.getReadingDate()));
//
//        Observation.ObservationComponentComponent systolic = new Observation.ObservationComponentComponent();
//        systolic.getCode().addCoding().setCode(BloodPressureModel.SYSTOLIC_CODE).setSystem(BloodPressureModel.SYSTEM);
//        systolic.setValue(new Quantity());
//        systolic.getValueQuantity().setCode(BloodPressureModel.VALUE_CODE);
//        systolic.getValueQuantity().setSystem(BloodPressureModel.VALUE_SYSTEM);
//        systolic.getValueQuantity().setUnit(BloodPressureModel.VALUE_UNIT);
//        systolic.getValueQuantity().setValue(item.getSystolic());
//        o.getComponent().add(systolic);
//
//        Observation.ObservationComponentComponent diastolic = new Observation.ObservationComponentComponent();
//        diastolic.getCode().addCoding().setCode(BloodPressureModel.DIASTOLIC_CODE).setSystem(BloodPressureModel.SYSTEM);
//        diastolic.setValue(new Quantity());
//        diastolic.getValueQuantity().setCode(BloodPressureModel.VALUE_CODE);
//        diastolic.getValueQuantity().setSystem(BloodPressureModel.VALUE_SYSTEM);
//        diastolic.getValueQuantity().setUnit(BloodPressureModel.VALUE_UNIT);
//        diastolic.getValueQuantity().setValue(item.getDiastolic());
//        o.getComponent().add(diastolic);
//
//        return o;
//    }
}
