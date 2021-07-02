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
import edu.ohsu.cmp.htnu18app.http.HttpRequest;
import edu.ohsu.cmp.htnu18app.http.HttpResponse;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.model.recommendation.Audience;
import edu.ohsu.cmp.htnu18app.model.recommendation.Card;
import edu.ohsu.cmp.htnu18app.model.recommendation.Suggestion;
import edu.ohsu.cmp.htnu18app.service.CounselingService;
import edu.ohsu.cmp.htnu18app.service.GoalService;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import edu.ohsu.cmp.htnu18app.util.MustacheUtil;
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
    private PatientService patientService;
    private HomeBloodPressureReadingService hbprService;
    private GoalService goalService;
    private CounselingService counselingService;

    public CDSHookExecutor(boolean testing, String sessionId,
                           String cdsHooksEndpointURL,
                           PatientService patientService,
                           HomeBloodPressureReadingService hbprService,
                           GoalService goalService,
                           CounselingService counselingService) {
        this.testing = testing;
        this.sessionId = sessionId;
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
        this.patientService = patientService;
        this.hbprService = hbprService;
        this.goalService = goalService;
        this.counselingService = counselingService;
    }

    @Override
    public String toString() {
        return "CDSHookExecutor{" +
                "testing=" + testing +
                ", sessionId='" + sessionId + '\'' +
                ", cdsHooksEndpointURL='" + cdsHooksEndpointURL + '\'' +
                ", patientService=" + patientService +
                ", hbprService=" + hbprService +
                ", goalService=" + goalService +
                ", counselingService=" + counselingService +
                '}';
    }

    @Override
    public void run() {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        cache.deleteAllCards();

        List<CDSHook> hooks = null;
        try {
            hooks = CDSHooksUtil.getCDSHooks(testing, cdsHooksEndpointURL);

        } catch (Exception e) {
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

                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() + " executing hook '" + hook.getId() + "' - " + e.getMessage(), e);
                }
            }
        }
    }

////////////////////////////////////////////////////////////////////////
// private methods
//
    private List<Card> getCardsForHook(String sessionId, String hookId,
                                       FHIRCredentialsWithClient fcc, Audience audience) throws IOException {

        List<Card> cards = new ArrayList<>();

        try {
            Patient p = patientService.getPatient(sessionId);

            Bundle bpBundle = buildBPBundle(p.getId());
            Bundle counselingBundle = buildCounselingBundle(p.getId());
            Bundle goalsBundle = buildGoalsBundle(p.getId());
            Bundle conditionsBundle = buildConditionsBundle(p.getId());

            HookRequest request = new HookRequest(fcc.getCredentials(), p, bpBundle, counselingBundle, goalsBundle, conditionsBundle);

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, request).flush();

            logger.info("hookRequest = " + writer.toString());

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json; charset=UTF-8");

            String json;
            if (testing) {
                json = "{ \"cards\": [{ \"summary\": \"Discuss Smoking Cessation\", \"indicator\": \"info\", \"detail\": \"{{#patient}}You have a hypertension (high blood pressure) diagnosis and reported smoking. Reducing smoking will help lower blood pressure, the risk of stroke, and other harmful events; talk to your care team about quitting smoking.{{/patient}}{{#careTeam}}Patient reports they smoke. Counsel about quitting according to your local protocol.{{/careTeam}}| [ {\\\"id\\\": \\\"smoking-counseling\\\", \\\"type\\\":\\\"counseling\\\", \\\"references\\\": {\\\"system\\\":\\\"http://snomed.info/sct\\\", \\\"code\\\":\\\"225323000\\\"},\\\"label\\\": \\\"Suggested Reading\\\",\\\"actions\\\": [{\\\"url\\\":\\\"/SmokingCessation\\\", \\\"label\\\":\\\"Click here to learn more about tobacco cessation.\\\"}]}, { \\\"id\\\": \\\"smoking-freetext-goal\\\", \\\"type\\\":\\\"goal\\\", \\\"references\\\":{\\\"system\\\":\\\"https//coach-dev.ohsu.edu\\\", \\\"code\\\":\\\"smoking-cessation\\\"}, \\\"label\\\": \\\"Set a Tobacco Cessation goal (freetext):\\\", \\\"actions\\\": [] }, { \\\"id\\\": \\\"radio-smoking-goal\\\", \\\"type\\\": \\\"goal\\\", \\\"references\\\": {\\\"system\\\":\\\"https//coach-dev.ohsu.edu\\\", \\\"code\\\":\\\"smoking-cessation\\\"}, \\\"label\\\": \\\"Set a Tobacco Cessation goal (choice):\\\", \\\"actions\\\": [ {\\\"label\\\":\\\"Reduce my smoking by half\\\"}, {\\\"label\\\":\\\"Quit smoking completely\\\"} ] }, { \\\"id\\\": \\\"smoking-goal-checkbox1\\\", \\\"type\\\": \\\"goal\\\", \\\"references\\\": {\\\"system\\\":\\\"https//coach-dev.ohsu.edu\\\", \\\"code\\\":\\\"smoking-cessation\\\"}, \\\"label\\\": \\\"Set a Tobacco Cessation goal (prescribed):\\\", \\\"actions\\\": [ {\\\"label\\\":\\\"Reduce/quit smoking.\\\"} ] } ]|at-most-one|\", \"source\": {} }] }";

            } else {
                HttpResponse httpResponse = new HttpRequest().post(cdsHooksEndpointURL + "/" + hookId, null, headers, writer.toString());
                json = httpResponse.getResponseBody();
            }

            logger.info("got JSON=" + json);

            Gson gson = new GsonBuilder().create();
            try {
                json = MustacheUtil.compileMustache(audience, json);
                CDSHookResponse response = gson.fromJson(json, new TypeToken<CDSHookResponse>() {}.getType());

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
                                edu.ohsu.cmp.htnu18app.entity.app.Goal goal = goalService.getGoal(sessionId, s.getId());
                                s.setGoal(goal);
                            }
                        }
                    }

                    cards.add(card);
                }

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " processing response for hookId=" + hookId + " - " + e.getMessage(), e);
                logger.error("\n\nJSON =\n" + json + "\n\n");
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

    private Bundle buildConditionsBundle(String patientId) {
        return patientService.getConditions(sessionId);
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
        p.getCategory().addCoding().setCode("409063005").setSystem("http://snomed.info/sct");

        p.getCode().addCoding().setCode(c.getReferenceCode()).setSystem(c.getReferenceSystem());

        p.getPerformedDateTimeType().setValue(c.getCreatedDate());

        return p;
    }

    private Bundle buildGoalsBundle(String patientId) {
        Bundle bundle = patientService.getGoals(sessionId);

        List<edu.ohsu.cmp.htnu18app.entity.app.Goal> goalList = goalService.getGoalList(sessionId);
        for (edu.ohsu.cmp.htnu18app.entity.app.Goal g : goalList) {
            Goal goal = buildGoal(patientId, g);
            bundle.addEntry().setFullUrl("http://hl7.org/fhir/Goal/" + goal.getId()).setResource(goal);
        }

        return bundle;
    }

    private Goal buildGoal(String patientId, edu.ohsu.cmp.htnu18app.entity.app.Goal goal) {
        Goal g = new Goal();

        g.setId(goal.getExtGoalId());
        g.setSubject(new Reference().setReference(patientId));
        g.setLifecycleStatus(goal.getLifecycleStatus().toGoalLifecycleStatus());
        g.getAchievementStatus().addCoding().setCode(goal.getAchievementStatus().getFhirValue())
                .setSystem("http://terminology.hl7.org/CodeSystem/goal-achievement");
        g.getCategoryFirstRep().addCoding().setCode(goal.getReferenceCode()).setSystem(goal.getReferenceSystem());
        g.getDescription().setText(goal.getGoalText());
        g.setStatusDate(goal.getStatusDate());

        return g;
    }

    private Bundle buildBPBundle(String patientId) {
        Bundle bundle = patientService.getObservations(sessionId);

        // inject home blood pressure readings into Bundle for evaluation by CQF Ruler
        List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
        for (HomeBloodPressureReading item : hbprList) {
            String uuid = UUID.randomUUID().toString();

            Encounter e = buildEncounter(uuid, patientId, item.getReadingDate());
            bundle.addEntry().setFullUrl("http://hl7.org/fhir/Encounter/" + e.getId()).setResource(e);

            Observation o = buildBloodPressureObservation(uuid, patientId, e.getId(), item);

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
