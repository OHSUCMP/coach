package edu.ohsu.cmp.htnu18app.cqfruler;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSCard;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHookResponse;
import edu.ohsu.cmp.htnu18app.cqfruler.model.HookRequest;
import edu.ohsu.cmp.htnu18app.entity.app.Counseling;
import edu.ohsu.cmp.htnu18app.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.entity.app.MyGoal;
import edu.ohsu.cmp.htnu18app.exception.SessionMissingException;
import edu.ohsu.cmp.htnu18app.fhir.FhirConfigManager;
import edu.ohsu.cmp.htnu18app.http.HttpRequest;
import edu.ohsu.cmp.htnu18app.http.HttpResponse;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.model.recommendation.Audience;
import edu.ohsu.cmp.htnu18app.model.recommendation.Card;
import edu.ohsu.cmp.htnu18app.model.recommendation.Suggestion;
import edu.ohsu.cmp.htnu18app.service.CounselingService;
import edu.ohsu.cmp.htnu18app.service.EHRService;
import edu.ohsu.cmp.htnu18app.service.GoalService;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import edu.ohsu.cmp.htnu18app.util.MustacheUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class CDSHookExecutor implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean testing;
    private String sessionId;
    private String cdsHooksEndpointURL;
    private EHRService ehrService;
    private HomeBloodPressureReadingService hbprService;
    private GoalService goalService;
    private CounselingService counselingService;
    private FhirConfigManager fcm;

    public CDSHookExecutor(boolean testing, String sessionId,
                           String cdsHooksEndpointURL,
                           EHRService ehrService,
                           HomeBloodPressureReadingService hbprService,
                           GoalService goalService,
                           CounselingService counselingService,
                           FhirConfigManager fcm) {
        this.testing = testing;
        this.sessionId = sessionId;
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
        this.ehrService = ehrService;
        this.hbprService = hbprService;
        this.goalService = goalService;
        this.counselingService = counselingService;
        this.fcm = fcm;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return "CDSHookExecutor{" +
                "testing=" + testing +
                ", sessionId='" + sessionId + '\'' +
                ", cdsHooksEndpointURL='" + cdsHooksEndpointURL + '\'' +
                ", ehrService=" + ehrService +
                ", hbprService=" + hbprService +
                ", goalService=" + goalService +
                ", counselingService=" + counselingService +
                ", fcm=" + fcm +
                '}';
    }

    @Override
    public void run() {
        try {
            CacheData cache = SessionCache.getInstance().get(sessionId);

            cache.deleteAllCards();

            List<CDSHook> hooks = null;
            try {
                hooks = CDSHooksUtil.getCDSHooks(testing, cdsHooksEndpointURL);

            } catch (IOException e) {
                logger.error("caught " + e.getClass().getName() + " getting CDS Hooks - " + e.getMessage(), e);
            }

            if (hooks != null) {
                for (CDSHook hook : hooks) {
                    try {
                        List<Card> cards = getCardsForHook(sessionId, hook.getId(),
                                cache.getFhirCredentialsWithClient(),
                                cache.getAudience());

                        cache.setCards(hook.getId(), cards);

                        logger.info("cards generated for sessionId=" + sessionId + ", hookId=" + hook.getId());

                    } catch (IOException e) {
                        logger.error("caught " + e.getClass().getName() + " executing hook '" + hook.getId() + "' - " + e.getMessage(), e);
                    }
                }
            }

        } catch (SessionMissingException sme) {
            logger.error("caught " + sme.getClass().getName() + " attempting to execute CDS Hooks - aborting", sme);
        }
    }

////////////////////////////////////////////////////////////////////////
// private methods
//
    private List<Card> getCardsForHook(String sessionId, String hookId,
                                       FHIRCredentialsWithClient fcc, Audience audience) throws IOException {

        List<Card> cards = new ArrayList<>();

        try {
            Patient p = ehrService.getPatient(sessionId);

            Bundle bpBundle = buildBPBundle(p.getId());
            Bundle counselingBundle = buildCounselingBundle(p.getId());
            Bundle goalsBundle = buildGoalsBundle(p.getId());
//            Bundle conditionsBundle = buildConditionsBundle(p.getId());

            HookRequest request = new HookRequest(fcc.getCredentials(), p, bpBundle, counselingBundle, goalsBundle);

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, request).flush();

            logger.debug("hookRequest = " + writer.toString());

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json; charset=UTF-8");

            int code;
            String body;
            if (testing) {
                code = 200;
                body = "{ \"cards\": [{ \"summary\": \"Blood pressure goal not reached. Discuss treatment options\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}Your recent blood pressure is above your goal. Changing your behaviors may help - see the additional options. In addition, please consult your care team to consider additional treatment options. Please review the home blood pressure measurement protocol presented on the blood pressure entry page to make sure that the tool is accurately measuring your blood pressure.{{/patient}}{{#careTeam}}BP not at goal. Consider initiating antihypertensive drug therapy with a single antihypertinsive drug with dosage titration and sequential addition of other agents to achieve the target BP.{{/careTeam}}|[ { \\\"id\\\": \\\"contact-suggestion\\\", \\\"label\\\": \\\"Contact care team BP Treatment\\\", \\\"type\\\": \\\"suggestion-link\\\", \\\"actions\\\": [{\\\"label\\\":\\\"Contact your care team about options to control your high blood pressure\\\", \\\"url\\\":\\\"/contact\\\"}] } ]|at-most-one|\", \"source\": {} }, { \"summary\": \"Discuss Smoking Cessation\", \"indicator\": \"info\", \"detail\": \"{{#patient}}You have a hypertension (high blood pressure) diagnosis and reported smoking. Reducing smoking will help lower blood pressure, the risk of stroke, and other harmful events; talk to your care team about quitting smoking.{{/patient}}{{#careTeam}}Patient reports they smoke. Counsel about quitting according to your local protocol.{{/careTeam}}| [ {\\\"id\\\": \\\"smoking-counseling\\\", \\\"type\\\":\\\"counseling-link\\\", \\\"references\\\": {\\\"system\\\":\\\"http://snomed.info/sct\\\", \\\"code\\\":\\\"225323000\\\"},\\\"label\\\": \\\"Smoking Cessation Counseling\\\",\\\"actions\\\": [{\\\"url\\\":\\\"/SmokingCessation\\\", \\\"label\\\":\\\"Click here to learn more about tobacco cessation.\\\"}]}, { \\\"id\\\": \\\"smoking-freetext-goal\\\", \\\"type\\\":\\\"goal\\\", \\\"references\\\":{\\\"system\\\":\\\"https//coach-dev.ohsu.edu\\\", \\\"code\\\":\\\"smoking-cessation\\\"}, \\\"label\\\": \\\"Set a Tobacco Cessation goal (freetext):\\\", \\\"actions\\\": [] }, { \\\"id\\\": \\\"radio-smoking-goal\\\", \\\"type\\\": \\\"goal\\\", \\\"references\\\": {\\\"system\\\":\\\"https//coach-dev.ohsu.edu\\\", \\\"code\\\":\\\"smoking-cessation\\\"}, \\\"label\\\": \\\"Set a Tobacco Cessation goal (choice):\\\", \\\"actions\\\": [ {\\\"label\\\":\\\"Reduce my smoking by [quantity:number] cigarettes per [time period:text]\\\"}, {\\\"label\\\":\\\"Quit smoking completely\\\"} ] } ]|at-most-one|\", \"source\": {} }, { \"summary\": \"Discuss target blood pressure and set a blood pressure goal\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}You recently received a hypertension (high blood pressure) diagnosis. Setting goals for lowering your blood pressure has been proven to help overall health and reduce your chance of stroke or other conditions.{{/patient}}{{#careTeam}}No BP Goal set: Setting a blood pressure goal can help engage patients and improve outcomes. For most patients, choosing a target between \\u003c120-140/80-90 is recommended; lower targets may be for ASCVD, ASCVD risk \\u003e10%, multimorbidity (CKD and diabetes), or preference; higher targets may be for age, adverse events, or frailty.{{/careTeam}}|[ { \\\"id\\\": \\\"bp-radio-goal\\\", \\\"label\\\": \\\"BP Goal\\\", \\\"type\\\": \\\"bp-goal\\\", \\\"references\\\":{\\\"system\\\":\\\"https//coach-dev.ohsu.edu\\\", \\\"code\\\":\\\"blood-pressure\\\"}, \\\"actions\\\": [{\\\"label\\\":\\\"140/90\\\"}, {\\\"label\\\":\\\"130/80\\\"}, {\\\"label\\\":\\\"120/80\\\"}]}]|at-most-one|\", \"source\": {} } ] }";

            } else {
                HttpResponse httpResponse = new HttpRequest().post(cdsHooksEndpointURL + "/" + hookId, null, headers, writer.toString());
                code = httpResponse.getResponseCode();
                body = httpResponse.getResponseBody();
            }

            logger.debug("got response code=" + code + ", body=" + body);

            Gson gson = new GsonBuilder().create();
            try {
                body = MustacheUtil.compileMustache(audience, body);
                CDSHookResponse response = gson.fromJson(body, new TypeToken<CDSHookResponse>() {}.getType());

                List<String> filterGoalIds = goalService.getExtGoalIdList(sessionId);

                for (CDSCard cdsCard : response.getCards()) {
                    Card card = new Card(cdsCard);

                    if (card.getSuggestions() != null) {
                        Iterator<Suggestion> iter = card.getSuggestions().iterator();
                        while (iter.hasNext()) {
                            Suggestion s = iter.next();
                            if (s.getType().equals(Suggestion.TYPE_GOAL) && filterGoalIds.contains(s.getId())) {
                                iter.remove();
                            }
                        }

                        for (Suggestion s : card.getSuggestions()) {
                            if (s.getType().equals(Suggestion.TYPE_UPDATE_GOAL)) {
                                MyGoal myGoal = goalService.getGoal(sessionId, s.getId());
                                s.setGoal(myGoal);
                            }
                        }
                    }

                    cards.add(card);
                }

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " processing response for hookId=" + hookId + " - " + e.getMessage(), e);
                logger.error("\n\nBODY =\n" + body + "\n\n");
                throw e;
            }

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " processing hookId=" + hookId + " - " + e.getMessage(), e);

            if (e instanceof IOException) {
                throw (IOException) e;
            }
        }

        return cards;
    }

    private Bundle buildCounselingBundle(String patientId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        List<Counseling> counselingList = counselingService.getCounselingList(sessionId);
        for (Counseling c : counselingList) {
            String uuid = UUID.randomUUID().toString();

            Encounter e = buildEncounter(uuid, patientId, c.getCreatedDate());
            bundle.addEntry().setFullUrl("http://hl7.org/fhir/Encounter/" + e.getId()).setResource(e);

            Procedure p = buildCounselingProcedure(patientId, e.getId(), c);
            bundle.addEntry().setFullUrl("http://hl7.org/fhir/Procedure/" + p.getId()).setResource(p);
        }

        return bundle;
    }

    private Procedure buildCounselingProcedure(String patientId, String encounterId, Counseling c) {
        Procedure p = new Procedure();

        p.setId(c.getExtCounselingId());
        p.setSubject(new Reference().setReference(patientId));
        p.setEncounter(new Reference().setReference(encounterId));
        p.setStatus(Procedure.ProcedureStatus.COMPLETED);

        // set counseling category.  see https://www.hl7.org/fhir/valueset-procedure-category.html
        p.getCategory().addCoding()
                .setCode("409063005")
                .setSystem("http://snomed.info/sct");

        p.getCode().addCoding().setCode(c.getReferenceCode()).setSystem(c.getReferenceSystem());

        p.getPerformedDateTimeType().setValue(c.getCreatedDate());

        return p;
    }

    private Bundle buildGoalsBundle(String patientId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        // add to new bundle so as to not modify what's in the cache
        for (Bundle.BundleEntryComponent bec : ehrService.getCurrentGoals(sessionId).getEntry()) {
            bundle.addEntry(bec);
        }

        List<MyGoal> myGoalList = goalService.getGoalList(sessionId);
        for (MyGoal g : myGoalList) {
            Goal goal = buildGoal(patientId, g);
            bundle.addEntry().setFullUrl("http://hl7.org/fhir/Goal/" + goal.getId()).setResource(goal);
        }

        return bundle;
    }

    private Goal buildGoal(String patientId, MyGoal myGoal) {
        Goal g = new Goal();

        g.setId(myGoal.getExtGoalId());
        g.setSubject(new Reference().setReference(patientId));
        g.setLifecycleStatus(myGoal.getLifecycleStatus().toGoalLifecycleStatus());
        g.getAchievementStatus().addCoding().setCode(myGoal.getAchievementStatus().getFhirValue())
                .setSystem("http://terminology.hl7.org/CodeSystem/goal-achievement");
        g.getCategoryFirstRep().addCoding().setCode(myGoal.getReferenceCode()).setSystem(myGoal.getReferenceSystem());
        g.getDescription().setText(myGoal.getGoalText());
        g.setStatusDate(myGoal.getStatusDate());

        if (myGoal.isBloodPressureGoal()) {
            Goal.GoalTargetComponent systolic = new Goal.GoalTargetComponent();
            systolic.getMeasure().addCoding().setCode(fcm.getBpSystolicCode()).setSystem(fcm.getBpSystem());
            systolic.setDetail(new Quantity());
            systolic.getDetailQuantity().setCode(fcm.getBpValueCode());
            systolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
            systolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
            systolic.getDetailQuantity().setValue(myGoal.getSystolicTarget());
            g.getTarget().add(systolic);

            Goal.GoalTargetComponent diastolic = new Goal.GoalTargetComponent();
            diastolic.getMeasure().addCoding().setCode(fcm.getBpDiastolicCode()).setSystem(fcm.getBpSystem());
            diastolic.setDetail(new Quantity());
            diastolic.getDetailQuantity().setCode(fcm.getBpValueCode());
            diastolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
            diastolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
            diastolic.getDetailQuantity().setValue(myGoal.getDiastolicTarget());
            g.getTarget().add(diastolic);
        }

        return g;
    }

    private Bundle buildBPBundle(String patientId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        // add to new bundle so as to not modify what's in the cache
        for (Bundle.BundleEntryComponent bec : ehrService.getBloodPressureObservations(sessionId).getEntry()) {
            bundle.addEntry(bec);
        }

        // inject home blood pressure readings into Bundle for evaluation by CQF Ruler
        List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
        for (HomeBloodPressureReading item : hbprList) {
//            String uuid = UUID.randomUUID().toString();
//            Encounter e = buildEncounter(uuid, patientId, item.getReadingDate());
//            bundle.addEntry().setFullUrl("http://hl7.org/fhir/Encounter/" + e.getId()).setResource(e);

            Observation o = buildHomeBloodPressureObservation(patientId, item);

            // todo: should the URL be different?
            bundle.addEntry().setFullUrl("http://hl7.org/fhir/Observation/" + o.getId()).setResource(o);
        }

        return bundle;
    }

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

    private Observation buildHomeBloodPressureObservation(String patientId, HomeBloodPressureReading item) {
        // adapted from https://www.programcreek.com/java-api-examples/?api=org.hl7.fhir.dstu3.model.Observation
        String uuid = UUID.randomUUID().toString();

        Observation o = new Observation();

        o.setId("observation-bp-" + uuid);
        o.setSubject(new Reference().setReference(patientId));
//        o.setEncounter(new Reference().setReference(encounterId));
        o.setStatus(Observation.ObservationStatus.FINAL);
        o.getCode().addCoding()
                .setCode(fcm.getBpCode())
                .setSystem(fcm.getBpSystem());

        if (StringUtils.isNotEmpty(fcm.getBpHomeSystem()) && StringUtils.isNotEmpty(fcm.getBpHomeCode())) {
            o.getCode().addCoding()
                    .setCode(fcm.getBpHomeCode())
                    .setSystem(fcm.getBpHomeSystem())
                    .setDisplay(fcm.getBpHomeDisplay());
        }

        // setting MeasurementSettingExt to indicate taken in a "home" setting
        // see https://browser.ihtsdotools.org/?perspective=full&conceptId1=264362003&edition=MAIN/SNOMEDCT-US/2021-09-01&release=&languages=en
        o.addExtension(new Extension()
                .setUrl("http://hl7.org/fhir/us/vitals/StructureDefinition/MeasurementSettingExt")
                .setValue(new Coding()
                        .setCode("264362003")
                        .setSystem("http://snomed.info/sct")));

        o.setEffective(new DateTimeType(item.getReadingDate()));

        Observation.ObservationComponentComponent systolic = new Observation.ObservationComponentComponent();
        systolic.getCode().addCoding().setCode(fcm.getBpSystolicCode()).setSystem(fcm.getBpSystem());
        systolic.setValue(new Quantity());
        systolic.getValueQuantity().setCode(fcm.getBpValueCode());
        systolic.getValueQuantity().setSystem(fcm.getBpValueSystem());
        systolic.getValueQuantity().setUnit(fcm.getBpValueUnit());
        systolic.getValueQuantity().setValue(item.getSystolic());
        o.getComponent().add(systolic);

        Observation.ObservationComponentComponent diastolic = new Observation.ObservationComponentComponent();
        diastolic.getCode().addCoding().setCode(fcm.getBpDiastolicCode()).setSystem(fcm.getBpSystem());
        diastolic.setValue(new Quantity());
        diastolic.getValueQuantity().setCode(fcm.getBpValueCode());
        diastolic.getValueQuantity().setSystem(fcm.getBpValueSystem());
        diastolic.getValueQuantity().setUnit(fcm.getBpValueUnit());
        diastolic.getValueQuantity().setValue(item.getDiastolic());
        o.getComponent().add(diastolic);

        return o;
    }
}
