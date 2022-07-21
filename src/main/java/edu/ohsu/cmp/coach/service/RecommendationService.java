package edu.ohsu.cmp.coach.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.entity.app.Counseling;
import edu.ohsu.cmp.coach.entity.app.MyGoal;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.AdverseEventModel;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.model.cqfruler.CDSCard;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHook;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHookResponse;
import edu.ohsu.cmp.coach.model.cqfruler.HookRequest;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import edu.ohsu.cmp.coach.model.recommendation.Suggestion;
import edu.ohsu.cmp.coach.util.CDSHooksUtil;
import edu.ohsu.cmp.coach.util.MustacheUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
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
public class RecommendationService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final boolean TESTING = false;   // true: use hard-coded 'canned' responses from CQF Ruler (fast, cheap)
                                                    // false: make CQF Ruler calls (slow, expensive)

    private static final String GENERIC_ERROR_MESSAGE = "ERROR: An error was encountered processing this recommendation.  See server logs for details.";

    private String cdsHooksEndpointURL;
    private Boolean showDevErrors;
    private List<String> cdsHookOrder;


    @Autowired
    private BloodPressureService bpService;

    @Autowired
    private PulseService pulseService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private CounselingService counselingService;

    public RecommendationService(@Value("${cqfruler.cdshooks.endpoint.url}") String cdsHooksEndpointURL,
                                 @Value("#{new Boolean('${security.show-dev-errors}')}") Boolean showDevErrors,
                                 @Value("${cqfruler.cdshooks.order.csv}") String cdsHookOrder) {
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
        this.showDevErrors = showDevErrors;
        this.cdsHookOrder = Arrays.asList(cdsHookOrder.split("\\s*,\\s*"));
    }

    public List<CDSHook> getOrderedCDSHooks() throws IOException {
        Map<String, CDSHook> map = new LinkedHashMap<>();
        for (CDSHook cdsHook : CDSHooksUtil.getCDSHooks(TESTING, cdsHooksEndpointURL)) {
            map.put(cdsHook.getId(), cdsHook);
        }

        List<CDSHook> list = new ArrayList<>();
        for (String hookId : cdsHookOrder) {
            CDSHook cdsHook = map.remove(hookId);
            if (cdsHook != null) {
                list.add(cdsHook);
            }
        }

        list.addAll(map.values());

        return list;
    }

    public List<Card> getCards(String sessionId, String hookId) throws IOException {
        logger.debug("BEGIN getting cards for session=" + sessionId + ", hookId=" + hookId);

        UserWorkspace workspace = workspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        Audience audience = workspace.getAudience();

        List<Card> cards = new ArrayList<>();

        try {
            Patient p = workspace.getPatient().getSourcePatient();

            CompositeBundle compositeBundle = new CompositeBundle();

            compositeBundle.consume(p);

            compositeBundle.consume(buildBPBundle(sessionId, p.getId()));
//            compositeBundle.consume(buildPulseBundle(sessionId, p.getId()));      // do we care about pulses in recommendations?
            compositeBundle.consume(buildLocalCounselingBundle(sessionId, p.getId()));
            compositeBundle.consume(buildGoalsBundle(sessionId, p.getId()));

            // HACK: if the patient has no Adverse Events, we must construct a fake one to send to
            //       CQF-Ruler to prevent it from querying the FHIR server for them and consequently
            //       blowing up
            List<AdverseEventModel> adverseEvents = workspace.getAdverseEvents();
            if (adverseEvents.size() > 0) {
                for (AdverseEventModel adverseEvent : workspace.getAdverseEvents()) {
                    compositeBundle.consume(adverseEvent.toBundle(p.getId(), fcm));
                }
            } else {
                compositeBundle.consume(buildFakeAdverseEventHACK(sessionId));
            }

            compositeBundle.consume(workspace.getEncounterDiagnosisConditions());
            compositeBundle.consume(workspace.getSupplementalResources());

            HookRequest request = new HookRequest(fcc.getCredentials(), compositeBundle.getBundle());

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, request).flush();

            logger.debug("hookRequest = " + writer.toString());

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json; charset=UTF-8");

            int code;
            String body;
            if (TESTING) {
//                int x = (int) Math.round(Math.random());
//                if (x == 1) {
//                    code = 500;
//                    body = showDevErrors ?
//                        "ERROR: Exception in CQL Execution.Invalid Interval - the ending boundary must be greater than or equal to the starting boundary.org.opencds.cqf.cql.engine.exception.InvalidInterval: Invalid Interval - the ending boundary must be greater than or equal to the starting boundary.\tat org.opencds.cqf.cql.engine.runtime.Interval.<init>(Interval.java:55)\tat org.opencds.cqf.cql.engine.elm.execution.IntervalEvaluator.internalEvaluate(IntervalEvaluator.java:20)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.FunctionRefEvaluator.internalEvaluate(FunctionRefEvaluator.java:33)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.EndEvaluator.internalEvaluate(EndEvaluator.java:32)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.GreaterOrEqualEvaluator.internalEvaluate(GreaterOrEqualEvaluator.java:80)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.AndEvaluator.internalEvaluate(AndEvaluator.java:50)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.QueryEvaluator.evaluateWhere(QueryEvaluator.java:75)\tat org.opencds.cqf.cql.engine.elm.execution.QueryEvaluator.internalEvaluate(QueryEvaluator.java:198)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.ExpressionDefEvaluator.internalEvaluate(ExpressionDefEvaluator.java:19)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.ExpressionRefEvaluator.internalEvaluate(ExpressionRefEvaluator.java:11)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.ExistsEvaluator.internalEvaluate(ExistsEvaluator.java:28)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.NotEvaluator.internalEvaluate(NotEvaluator.java:31)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.ExpressionDefEvaluator.internalEvaluate(ExpressionDefEvaluator.java:19)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.ExpressionRefEvaluator.internalEvaluate(ExpressionRefEvaluator.java:11)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cql.engine.elm.execution.AndEvaluator.internalEvaluate(AndEvaluator.java:49)\tat org.opencds.cqf.cql.engine.elm.execution.Executable.evaluate(Executable.java:18)\tat org.opencds.cqf.cds.hooks.R4HookEvaluator.resolveActions(R4HookEvaluator.java:75)\tat org.opencds.cqf.cds.hooks.R4HookEvaluator.evaluateCdsHooksPlanDefinition(R4HookEvaluator.java:53)\tat org.opencds.cqf.cds.hooks.R4HookEvaluator.evaluateCdsHooksPlanDefinition(R4HookEvaluator.java:19)\tat org.opencds.cqf.cds.hooks.BaseHookEvaluator.evaluate(BaseHookEvaluator.java:73)\tat org.opencds.cqf.r4.servlet.CdsHooksServlet.doPost(CdsHooksServlet.java:206)\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:707)\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:790)\tat org.eclipse.jetty.servlet.ServletHolder$NotAsync.service(ServletHolder.java:1459)\t..." :
//                        GENERIC_ERROR_MESSAGE;
//
//                } else {
                code = 200;
                body = "{ \"cards\": [{ \"summary\": \"Blood pressure goal not reached. Discuss treatment options\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}Your recent blood pressure is above your goal. Changing your behaviors may help - see the additional options. In addition, please consult your care team to consider additional treatment options. Please review the home blood pressure measurement protocol presented on the blood pressure entry page to make sure that the tool is accurately measuring your blood pressure.{{/patient}}{{#careTeam}}BP not at goal. Consider initiating antihypertensive drug therapy with a single antihypertinsive drug with dosage titration and sequential addition of other agents to achieve the target BP.{{/careTeam}}|[ { \\\"id\\\": \\\"contact-suggestion\\\", \\\"label\\\": \\\"Contact care team BP Treatment\\\", \\\"type\\\": \\\"suggestion-link\\\", \\\"actions\\\": [{\\\"label\\\":\\\"Contact your care team about options to control your high blood pressure\\\", \\\"url\\\":\\\"/contact\\\"}] } ]|at-most-one|\", \"source\": {} }, { \"summary\": \"Discuss Smoking Cessation\", \"indicator\": \"info\", \"detail\": \"{{#patient}}You have a hypertension (high blood pressure) diagnosis and reported smoking. Reducing smoking will help lower blood pressure, the risk of stroke, and other harmful events; talk to your care team about quitting smoking.{{/patient}}{{#careTeam}}Patient reports they smoke. Counsel about quitting according to your local protocol.{{/careTeam}}| [ {\\\"id\\\": \\\"smoking-counseling\\\", \\\"type\\\":\\\"counseling-link\\\", \\\"references\\\": {\\\"system\\\":\\\"http://snomed.info/sct\\\", \\\"code\\\":\\\"225323000\\\"},\\\"label\\\": \\\"Smoking Cessation Counseling\\\",\\\"actions\\\": [{\\\"url\\\":\\\"/SmokingCessation\\\", \\\"label\\\":\\\"Click here to learn more about tobacco cessation.\\\"}]}, { \\\"id\\\": \\\"smoking-freetext-goal\\\", \\\"type\\\":\\\"goal\\\", \\\"references\\\":{\\\"system\\\":\\\"https//coach-dev.ohsu.edu\\\", \\\"code\\\":\\\"smoking-cessation\\\"}, \\\"label\\\": \\\"Set a Tobacco Cessation goal (freetext):\\\", \\\"actions\\\": [] }, { \\\"id\\\": \\\"radio-smoking-goal\\\", \\\"type\\\": \\\"goal\\\", \\\"references\\\": {\\\"system\\\":\\\"https//coach-dev.ohsu.edu\\\", \\\"code\\\":\\\"smoking-cessation\\\"}, \\\"label\\\": \\\"Set a Tobacco Cessation goal (choice):\\\", \\\"actions\\\": [ {\\\"label\\\":\\\"Reduce my smoking by [quantity:] cigarettes per [time period:day]\\\"}, {\\\"label\\\":\\\"Quit smoking completely\\\"} ] } ]|at-most-one|\", \"source\": {} }, { \"summary\": \"Discuss target blood pressure and set a blood pressure goal\", \"indicator\": \"warning\", \"detail\": \"{{#patient}}You recently received a hypertension (high blood pressure) diagnosis. Setting goals for lowering your blood pressure has been proven to help overall health and reduce your chance of stroke or other conditions.{{/patient}}{{#careTeam}}No BP Goal set: Setting a blood pressure goal can help engage patients and improve outcomes. For most patients, choosing a target between \\u003c120-140/80-90 is recommended; lower targets may be for ASCVD, ASCVD risk \\u003e10%, multimorbidity (CKD and diabetes), or preference; higher targets may be for age, adverse events, or frailty.{{/careTeam}}|[ { \\\"id\\\": \\\"bp-radio-goal\\\", \\\"label\\\": \\\"BP Goal\\\", \\\"type\\\": \\\"bp-goal\\\", \\\"references\\\":{\\\"system\\\":\\\"https//coach-dev.ohsu.edu\\\", \\\"code\\\":\\\"blood-pressure\\\"}, \\\"actions\\\": [{\\\"label\\\":\\\"140/90\\\"}, {\\\"label\\\":\\\"130/80\\\"}, {\\\"label\\\":\\\"120/80\\\"}]}]|at-most-one|\", \"source\": {} } ] }";
//                }

            } else {
                HttpResponse httpResponse = new HttpRequest().post(cdsHooksEndpointURL + "/" + hookId, null, headers, writer.toString());
                code = httpResponse.getResponseCode();
                body = httpResponse.getResponseBody();
            }

            logger.debug("got response code=" + code + ", body=" + body);

            if (code < 200 || code > 299) {
                logger.error("CQF-RULER ERROR: " + body);
                Card card = showDevErrors ?
                        new Card(body) :
                        new Card(GENERIC_ERROR_MESSAGE);
                cards.add(card);

            } else {
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
                                    MyGoal myGoal = goalService.getLocalGoal(sessionId, s.getId());
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
            }

        } catch (Exception e) {
            String msg = "caught " + e.getClass().getName() + " processing hookId=" + hookId + " - " + e.getMessage();
            logger.error(msg, e);

            if (e instanceof IOException) {
                throw (IOException) e;

            } else {
                Card card = showDevErrors ?
                        new Card(msg) :
                        new Card(GENERIC_ERROR_MESSAGE);
                cards.add(card);
            }

        } finally {
            logger.debug("DONE getting cards for session=" + sessionId + ", hookId=" + hookId);
        }

        return cards;
    }

    private Bundle buildLocalCounselingBundle(String sessionId, String patientId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        List<Counseling> counselingList = counselingService.getCounselingList(sessionId);
        for (Counseling c : counselingList) {
            String uuid = UUID.randomUUID().toString();

            Encounter e = buildEncounter(uuid, patientId, c.getCreatedDate());
            bundle.addEntry().setFullUrl("http://hl7.org/fhir/Encounter/" + e.getId()).setResource(e);

            Procedure p = buildLocalCounselingProcedure(patientId, e.getId(), c);
            bundle.addEntry().setFullUrl("http://hl7.org/fhir/Procedure/" + p.getId()).setResource(p);
        }

        return bundle;
    }

    private Procedure buildLocalCounselingProcedure(String patientId, String encounterId, Counseling c) {
        Procedure p = new Procedure();

        p.setId(c.getExtCounselingId());
        p.setSubject(new Reference().setReference(patientId));
        p.setEncounter(new Reference().setReference(encounterId));
        p.setStatus(Procedure.ProcedureStatus.COMPLETED);

        // set counseling category.  see https://www.hl7.org/fhir/valueset-procedure-category.html
        p.getCategory().addCoding()
                .setCode(fcm.getProcedureCounselingCode())
                .setSystem(fcm.getProcedureCounselingSystem());

        p.getCode().addCoding().setCode(c.getReferenceCode()).setSystem(c.getReferenceSystem());

        p.getPerformedDateTimeType().setValue(c.getCreatedDate());

        return p;
    }

    private Bundle buildGoalsBundle(String sessionId, String patientId) {
        CompositeBundle bundle = new CompositeBundle();
        for (GoalModel gm : goalService.getGoals(sessionId)) {
            bundle.consume(gm.toBundle(patientId, fcm));
        }
        return bundle.getBundle();
    }

    private Bundle buildBPBundle(String sessionId, String patientId) {
        CompositeBundle bundle = new CompositeBundle();
        for (BloodPressureModel bpm : bpService.getBloodPressureReadings(sessionId)) {
            bundle.consume(bpm.toBundle(patientId, fcm));
        }
        return bundle.getBundle();
    }

    private Bundle buildPulseBundle(String sessionId, String patientId) {
        CompositeBundle bundle = new CompositeBundle();
        for (PulseModel pm : pulseService.getPulseReadings(sessionId)) {
            bundle.consume(pm.toBundle(patientId, fcm));
        }
        return bundle.getBundle();
    }

    private Encounter buildEncounter(String uuid, String patientId, Date date) {
        Encounter e = new Encounter();

        e.setId("encounter-" + uuid);
        e.setStatus(Encounter.EncounterStatus.FINISHED);
        e.getClass_().setSystem(fcm.getEncounterClassSystem())
                .setCode(fcm.getEncounterClassAMBCode())
                .setDisplay(fcm.getEncounterClassAMBDisplay());

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

    /**
     * *** HACK ***
     *
     * build a FAKE AdverseEvent resource to trick CQF Ruler into not querying the FHIR server for
     * AdverseEvent resources.  this is gross but sadly necessary for the time being.
     * @param sessionId
     * @return
     */
    private AdverseEvent buildFakeAdverseEventHACK(String sessionId) {
        String aeid = "adverseevent-FAKE-HACK";

        AdverseEvent ae = new AdverseEvent();
        ae.setId(aeid);

        Patient p = workspaceService.get(sessionId).getPatient().getSourcePatient();

        ae.setSubject(new Reference().setReference(p.getId()));

        ae.getEvent().addCoding(new Coding()
                .setCode("coach-fake-adverse-event")
                .setSystem("https://coach.ohsu.edu")
                .setDisplay("***FAKE*** Adverse Event generated by COACH to prevent CQF-Ruler from querying the FHIR server"));

        ae.setDate(new Date());

        return ae;
    }
}
