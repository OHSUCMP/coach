package edu.ohsu.cmp.coach.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.entity.ClinicContact;
import edu.ohsu.cmp.coach.entity.Counseling;
import edu.ohsu.cmp.coach.entity.MyGoal;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.transform.BaseVendorTransformer;
import edu.ohsu.cmp.coach.fhir.transform.DefaultVendorTransformer;
import edu.ohsu.cmp.coach.fhir.transform.VendorTransformer;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.model.cqfruler.CDSCard;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHook;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHookResponse;
import edu.ohsu.cmp.coach.model.cqfruler.HookRequest;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Action;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import edu.ohsu.cmp.coach.model.recommendation.Suggestion;
import edu.ohsu.cmp.coach.model.redcap.RandomizationGroup;
import edu.ohsu.cmp.coach.util.CDSHooksUtil;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.util.MustacheUtil;
import edu.ohsu.cmp.coach.util.UUIDUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import io.micrometer.common.util.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.ConditionCategory;
import org.hl7.fhir.r4.model.codesystems.ConditionClinical;
import org.hl7.fhir.r4.model.codesystems.ConditionVerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class RecommendationService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final boolean TESTING = false;   // true: use hard-coded 'canned' responses from CQF Ruler (fast, cheap)
                                                    // false: make CQF Ruler calls (slow, expensive)

    private static final String GENERIC_ERROR_MESSAGE = "ERROR: An error was encountered processing this recommendation.  See server logs for details.";
    private static final String COACH_SYSTEM = "https://coach.ohsu.edu";

    @Value("${cqfruler.cdshooks.endpoint.url}")
    private String cdsHooksEndpointURL;

    @Value("#{new Boolean('${security.show-dev-errors}')}")
    private Boolean showDevErrors;

    private List<String> cdsHookOrder;

    private List<String> basicGroupAllowFilter;

    @Autowired
    private BloodPressureService bpService;

    @Autowired
    private PulseService pulseService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private CounselingService counselingService;

    @Autowired
    private AdverseEventService adverseEventService;

    @Autowired
    private ClinicContactService clinicContactService;

    @Value("${contact.clinic}")
    private String clinicContact;

    @Value("${contact.after-hours}")
    private String clinicAfterHours;

    public RecommendationService(@Value("${cqfruler.cdshooks.order.csv}") String cdsHookOrderStr,
                                 @Value("${cqfruler.cdshooks.basic-group.allow-filter.csv}") String basicGroupAllowFilterStr) {
        this.cdsHookOrder = Arrays.asList(cdsHookOrderStr.split("\\s*,\\s*"));
        this.basicGroupAllowFilter = Arrays.asList(basicGroupAllowFilterStr.split("\\s*,\\s*"));
    }

    public List<CDSHook> getOrderedCDSHooks(String sessionId) throws IOException {
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

        // for users belonging to the "basic" randomization group, filter hooks to only those permitted for that cohort
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        if (workspace.getActiveRandomizationGroup() == RandomizationGroup.BASIC && basicGroupAllowFilter.size() > 0) {
            Iterator<CDSHook> iter = list.iterator();
            while (iter.hasNext()) {
                CDSHook item = iter.next();
                if ( ! basicGroupAllowFilter.contains(item.getId()) ) {
                    iter.remove();
                }
            }
        }

        return list;
    }

    public List<Card> getCards(String sessionId, String hookId) throws IOException {
        logger.debug("BEGIN getting cards for session=" + sessionId + ", hookId=" + hookId);

        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient();
        Audience audience = workspace.getAudience();

        // force Default context when preparing resources for transmission to CQF-Ruler
        DefaultVendorTransformer transformer = new DefaultVendorTransformer(workspace);

        List<Card> cards = new ArrayList<>();
        boolean prefetchModified = false;

        try {
            CompositeBundle compositeBundle = new CompositeBundle();
            Patient p = workspace.getPatient().getSourcePatient();
            compositeBundle.consume(p);
            compositeBundle.consume(buildBPBundle(sessionId, transformer));
//            compositeBundle.consume(buildPulseBundle(sessionId, transformer));      // do we care about pulses in recommendations?
            compositeBundle.consume(buildLocalCounselingBundle(sessionId, p.getId()));
            compositeBundle.consume(buildGoalsBundle(sessionId, transformer));
            compositeBundle.consume(buildAdverseEventsBundle(sessionId, p.getId()));
            compositeBundle.consume(buildConditionsBundle(sessionId, p.getId()));
            compositeBundle.consume(buildMedicationsBundle(sessionId));
            compositeBundle.consume(buildNormalizedSmokingObservations(sessionId));
            compositeBundle.consume(buildNormalizedDrinkingObservations(sessionId));
            compositeBundle.consume(workspace.getOtherSupplementalResources());

            HookRequest hookRequest = new HookRequest(fcc.getCredentials(), compositeBundle.getBundle());

            prefetchModified = hookRequest.isPrefetchModified();

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, hookRequest).flush();

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
                logger.error("CQF-RULER ERROR: code=" + code + ", body=" + body);

                auditService.doAudit(sessionId, AuditSeverity.ERROR, "recommendation engine error", "received HTTP " + code +
                        " from recommendation engine for " + hookId + " - see logs for details");

                Card card = showDevErrors ?
                        new Card(body, prefetchModified) :
                        new Card(GENERIC_ERROR_MESSAGE, prefetchModified);
                cards.add(card);

            } else {
                Gson gson = new GsonBuilder().create();
                try {
                    body = MustacheUtil.compileMustache(audience, body);
                    CDSHookResponse response = gson.fromJson(body, new TypeToken<CDSHookResponse>() {}.getType());

                    List<String> filterGoalIds = goalService.getExtGoalIdList(sessionId);

                    for (CDSCard cdsCard : response.getCards()) {
                        Card card = new Card(cdsCard, prefetchModified);

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
                                if (s.getType().equals(Suggestion.TYPE_CLINIC_CONTACT)) {
                                    List<Action> actions = new ArrayList<>();

                                    List<ClinicContact> ccList = clinicContactService.getClinicContactList();
                                    if ( ! ccList.isEmpty() ) {
                                        // pull clinic contact info from clinic_contact table, if any present
                                        for (ClinicContact cc : ccList) {
                                            // NOTE: the "|" delimiter is used in recommendations.js to identify
                                            //       that the clinic contacts should be treated differently
                                            String label = cc.getName() + "|" + cc.getPrimaryPhone();
                                            if (StringUtils.isNotBlank(cc.getAfterHoursPhone())) {
                                                label += " (after hours, call " + cc.getAfterHoursPhone() + ")";
                                            }
                                            Action action = new Action();
                                            action.setLabel(label);
                                            actions.add(action);
                                        }

                                    } else {
                                        // pull generic contact info from application.properties, if DB not populated
                                        Action callClinic = new Action();
                                        callClinic.setLabel(clinicContact);
                                        actions.add(callClinic);

                                        if (StringUtils.isNotBlank(clinicAfterHours)) {
                                            Action callAfterHours = new Action();
                                            callAfterHours.setLabel(clinicAfterHours);
                                            actions.add(callAfterHours);
                                        }
                                    }

                                    s.setActions(actions);
                                }
                            }
                        }

                        cards.add(card);
                    }

                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() + " processing response for hookId=" + hookId + " - " + e.getMessage(), e);
                    logger.debug("\n\nBODY =\n" + body + "\n\n");
                    throw e;
                }
            }

        } catch (Exception e) {
            String msg = "caught " + e.getClass().getName() + " processing hookId=" + hookId + " - " + e.getMessage();
            logger.error(msg, e);

            if (e instanceof IOException) {
                throw (IOException) e;

            } else {
                auditService.doAudit(sessionId, AuditSeverity.ERROR, "recommendation exception", "caught " + e.getClass().getSimpleName() +
                        " processing " + hookId + " - " + e.getMessage());

                Card card = showDevErrors ?
                        new Card(msg, prefetchModified) :
                        new Card(GENERIC_ERROR_MESSAGE, prefetchModified);
                cards.add(card);
            }

        } finally {
            logger.debug("DONE getting cards for session=" + sessionId + ", hookId=" + hookId);
        }

        return cards;
    }

    private Bundle buildConditionsBundle(String sessionId, String patientId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        CompositeBundle compositeBundle = new CompositeBundle();
        compositeBundle.consume(workspace.getEncounterDiagnosisConditions());
        compositeBundle.consume(workspace.getProblemListConditions());

        if (compositeBundle.size() == 0) {
            compositeBundle.consume(buildFakeConditionHACK(patientId));
        }

        return compositeBundle.getBundle();
    }

    private Condition buildFakeConditionHACK(String patientId) {
        Condition c = new Condition();
        c.setId("condition-FAKE-HACK");
        c.setSubject(new Reference().setReference(patientId));
        c.getClinicalStatus().addCoding(new Coding()
                .setCode(ConditionClinical.INACTIVE.toCode())
                .setSystem(ConditionClinical.INACTIVE.getSystem())
                .setDisplay(ConditionClinical.INACTIVE.getDisplay())
        );
        c.getVerificationStatus().addCoding(new Coding()
                .setCode(ConditionVerStatus.ENTEREDINERROR.toCode())
                .setSystem(ConditionVerStatus.ENTEREDINERROR.getSystem())
                .setDisplay(ConditionVerStatus.ENTEREDINERROR.getDisplay())
        );

        CodeableConcept category = new CodeableConcept();
        category.addCoding(new Coding()
                .setCode(ConditionCategory.ENCOUNTERDIAGNOSIS.toCode())
                .setSystem(ConditionCategory.ENCOUNTERDIAGNOSIS.getSystem())
                .setDisplay(ConditionCategory.ENCOUNTERDIAGNOSIS.getDisplay())
        );
        category.addCoding(new Coding()
                .setCode("coach-fake-condition")
                .setSystem(COACH_SYSTEM)
                .setDisplay("***FAKE*** Condition generated by COACH to prevent CQF-Ruler from querying the FHIR server")
        );
        c.getCategory().add(category);

        return c;
    }

    private Bundle buildLocalCounselingBundle(String sessionId, String patientId) {
        CompositeBundle bundle = new CompositeBundle();

        List<Counseling> list = counselingService.getLocalCounselingList(sessionId);
        if (list.size() > 0) {
            for (Counseling c : list) {
                bundle.consume(buildLocalCounselingProcedure(patientId, c));
            }
        } else {
            bundle.consume(buildFakeLocalCounselingProcedureHACK(patientId));
        }

        return bundle.getBundle();
    }

    private Procedure buildLocalCounselingProcedure(String patientId, Counseling c) {
        Procedure p = new Procedure();

        p.setId(c.getExtCounselingId());
        p.setSubject(new Reference().setReference(patientId));
        p.setStatus(Procedure.ProcedureStatus.COMPLETED);

        // set counseling category.  see https://www.hl7.org/fhir/valueset-procedure-category.html
        p.getCategory().addCoding(fcm.getProcedureCounselingCoding());

        p.getCode().addCoding(new Coding()
                .setCode(c.getReferenceCode())
                .setSystem(c.getReferenceSystem())
        );

        p.getPerformedDateTimeType().setValue(c.getCreatedDate());

        return p;
    }

    private Procedure buildFakeLocalCounselingProcedureHACK(String patientId) {
        Procedure p = new Procedure();
        p.setId("counseling-procedure-FAKE-HACK");
        p.setSubject(new Reference().setReference(patientId));
        p.setStatus(Procedure.ProcedureStatus.UNKNOWN);

        // set counseling category.  see https://www.hl7.org/fhir/valueset-procedure-category.html
        p.getCategory().addCoding(fcm.getProcedureCounselingCoding());

        p.getCode().addCoding(new Coding()
                .setCode("coach-fake-procedure")
                .setSystem(COACH_SYSTEM)
                .setDisplay("***FAKE*** Counseling Procedure generated by COACH to prevent CQF-Ruler from querying the FHIR server")
        );

        p.getPerformedDateTimeType().setValue(new Date());

        return p;
    }

    private Bundle buildGoalsBundle(String sessionId, VendorTransformer transformer) throws DataException {
        CompositeBundle bundle = new CompositeBundle();
        List<GoalModel> list = goalService.getGoals(sessionId);
        if (list.size() > 0) {
            for (GoalModel gm : list) {
                bundle.consume(transformer.transformOutgoingGoal(gm));
            }
        } else {
            bundle.consume(buildFakeGoalHACK(sessionId));
        }
        return bundle.getBundle();
    }

    private Goal buildFakeGoalHACK(String sessionId) {
        Goal g = new Goal();
        g.setId("goal-FAKE-HACK");
        Patient p = userWorkspaceService.get(sessionId).getPatient().getSourcePatient();
        g.setSubject(new Reference().setReference(p.getId()));
        g.setLifecycleStatus(Goal.GoalLifecycleStatus.REJECTED);
        g.getDescription().setText("***FAKE*** Goal generated by COACH to prevent CQF-Ruler from querying the FHIR server");
        return g;
    }

    private Bundle buildBPBundle(String sessionId, VendorTransformer transformer) throws DataException {
        CompositeBundle bundle = new CompositeBundle();
        List<BloodPressureModel> list = bpService.getBloodPressureReadings(sessionId);
        if (list.size() > 0) {
            for (BloodPressureModel bpm : list) {
                bundle.consume(transformer.transformOutgoingBloodPressureReading(bpm));
            }
        } else {
            bundle.consume(buildFakeObservationHACK("bp-observation-FAKE-HACK", sessionId));
        }
        return bundle.getBundle();
    }

    private Bundle buildPulseBundle(String sessionId, VendorTransformer transformer) throws DataException {
        CompositeBundle bundle = new CompositeBundle();
        List<PulseModel> list = pulseService.getPulseReadings(sessionId);
        if (list.size() > 0) {
            for (PulseModel pm : list) {
                bundle.consume(transformer.transformOutgoingPulseReading(pm));
            }
        } else {
            bundle.consume(buildFakeObservationHACK("pulse-observation-FAKE-HACK", sessionId));
        }
        return bundle.getBundle();
    }


    private Observation buildFakeObservationHACK(String id, String sessionId) {
        Observation o = new Observation();

        o.setId(id);

        Patient p = userWorkspaceService.get(sessionId).getPatient().getSourcePatient();
        o.setSubject(new Reference().setReference(p.getId()));
        o.setStatus(Observation.ObservationStatus.UNKNOWN);

        o.addCategory().addCoding()
                .setCode(BaseVendorTransformer.OBSERVATION_CATEGORY_CODE)
                .setSystem(BaseVendorTransformer.OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        o.getCode().addCoding(new Coding()
                .setCode("coach-fake-observation")
                .setSystem(COACH_SYSTEM)
                .setDisplay("***FAKE*** Observation generated by COACH to prevent CQF-Ruler from querying the FHIR server")
        );

        return o;
    }

    private Bundle buildAdverseEventsBundle(String sessionId, String patientId) throws DataException {
        CompositeBundle bundle = new CompositeBundle();
        List<AdverseEventModel> adverseEvents = adverseEventService.getAdverseEvents(sessionId);
        if (adverseEvents.size() > 0) {
            for (AdverseEventModel adverseEvent : adverseEvents) {
                bundle.consume(adverseEvent.toBundle(patientId, fcm));
            }
        } else {
            bundle.consume(buildFakeAdverseEventHACK(patientId));
        }
        return bundle.getBundle();
    }

    /**
     * *** HACK ***
     *
     * build a FAKE AdverseEvent resource to trick CQF Ruler into not querying the FHIR server for
     * AdverseEvent resources.  this is gross but sadly necessary for the time being.
     * @param patientId
     * @return
     */
    private AdverseEvent buildFakeAdverseEventHACK(String patientId) {
        AdverseEvent ae = new AdverseEvent();
        ae.setId("adverseevent-FAKE-HACK");
        ae.setSubject(new Reference().setReference(patientId));
        ae.getEvent().addCoding(new Coding()
                .setCode("coach-fake-adverse-event")
                .setSystem(COACH_SYSTEM)
                .setDisplay("***FAKE*** Adverse Event generated by COACH to prevent CQF-Ruler from querying the FHIR server")
        );
        ae.setDate(new Date());
        return ae;
    }

    private Bundle buildMedicationsBundle(String sessionId) {
        CompositeBundle bundle = new CompositeBundle();
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        List<MedicationModel> list = workspace.getMedications();
        if (list.size() > 0) {
            for (MedicationModel m : list) {
                if (m.hasSourceMedicationStatement()) {
                    bundle.consume(m.getSourceMedicationStatement());
                } else if (m.hasSourceMedicationRequest()) {
                    bundle.consume(m.getSourceMedicationRequest());
                    if (m.hasSourceMedicationRequestMedication()) {
                        bundle.consume(m.getSourceMedicationRequestMedication());
                    }
                }
            }
        } else {
            bundle.consume(buildFakeMedicationRequest(workspace.getPatient().getSourcePatient().getId()));
        }
        return bundle.getBundle();
    }

    private MedicationRequest buildFakeMedicationRequest(String patientId) {
        MedicationRequest mr = new MedicationRequest();

        mr.setId("medication-request-FAKE-HACK");
        mr.setStatus(MedicationRequest.MedicationRequestStatus.UNKNOWN);
        mr.setIntent(MedicationRequest.MedicationRequestIntent.PROPOSAL);
        mr.getMedicationCodeableConcept().addCoding(new Coding()
                .setCode("coach-fake-medication")
                .setSystem(COACH_SYSTEM)
                .setDisplay("***FAKE*** medication generated by COACH to prevent CQF-Ruler from querying the FHIR server")
        );
        mr.setSubject(new Reference().setReference(patientId));

        return mr;
    }

    private Bundle buildNormalizedSmokingObservations(String sessionId) {
        CompositeBundle bundle = new CompositeBundle();
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        Bundle sourceBundle = workspace.getSmokingObservations();
        if (sourceBundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : sourceBundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    Observation o = (Observation) entry.getResource();
                    if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), fcm.getSmokingCodings())) {
                        try {
                            bundle.consume(normalizeSmokingObservation(o));

                        } catch (Exception e) {
                            logger.error("caught " + e.getClass().getSimpleName() + " building normalized smoking Observation from resource with id=" + o.getId(), e);
                        }
                    }
                }
            }
        }

        return bundle.getBundle();
    }

    private static final Coding SMOKING_OBSERVATION_CODING = new Coding("http://loinc.org", "72166-2", "Tobacco smoking status");
    private static final Coding TOBACCO_USER_CODING = new Coding("http://snomed.info/sct", "110483000", "Tobacco user (finding)");
    private static final Coding SMOKING_COMPONENT_CODING = new Coding("http://loinc.org", "8663-7", "Cigarettes smoked current (pack per day) - Reported");
    private static final Coding NEVER_SMOKED_TOBACCO_CODING = new Coding("http://snomed.info/sct", "266919005", "Never smoked tobacco");
    private static final String PACKS_PER_DAY = "Packs/Day";

    private Observation normalizeSmokingObservation(Observation o) throws DataException {
        Observation normalized = new Observation();
        normalized.setId(UUIDUtil.getRandomUUID());
        if ( ! normalized.hasMeta() ) normalized.setMeta(new Meta());
        normalized.getMeta().setSource(o.getId());
        normalized.setSubject(o.getSubject());
        normalized.setStatus(o.getStatus());
        normalized.setEffective(o.getEffective());
        normalized.setIssued(o.getIssued());

        normalized.getCode().addCoding(SMOKING_OBSERVATION_CODING);

        boolean isTobaccoUser;
        if (o.hasValueCodeableConcept()) {
            normalized.setValue(o.getValueCodeableConcept());
            isTobaccoUser = ! FhirUtil.hasCoding(o.getValueCodeableConcept(), NEVER_SMOKED_TOBACCO_CODING);
        } else {
            normalized.setValue(new CodeableConcept().addCoding(TOBACCO_USER_CODING));
            isTobaccoUser = true;
        }

        Quantity valueQuantity = null;
        if (fcm.isSmokingGetValueFromComponent()) {
            if (o.hasComponent()) {
                for (Observation.ObservationComponentComponent component : o.getComponent()) {
                    if (component.hasCode() && FhirUtil.hasCoding(component.getCode(), fcm.getSmokingComponentCoding())) {
                        if (component.hasValueQuantity()) {
                            valueQuantity = component.getValueQuantity();
                            break;
                        } else {
                            throw new DataException("expected component to have a valueQuantity, but none found");
                        }
                    }
                }
            } else {
                throw new DataException("expected observation to have a component, but none found");
            }

        } else if (o.hasValueQuantity()) {
            valueQuantity = o.getValueQuantity();

        } else {
            throw new DataException("expected observation to have a valueQuantity, but none found");
        }

        if (isTobaccoUser) {
            // only tobacco users will have a quantity
            if (valueQuantity != null && valueQuantity.hasValue()) {
                Observation.ObservationComponentComponent component = new Observation.ObservationComponentComponent();
                component.setCode(new CodeableConcept().addCoding(SMOKING_COMPONENT_CODING));
                component.setValue(new Quantity()
                        .setValue(valueQuantity.getValue())
                        .setUnit(PACKS_PER_DAY)
                );
                normalized.addComponent(component);

            } else {
                throw new DataException("valueQuantity not found or does not contain a value");
            }
        }

        return normalized;
    }

    private Bundle buildNormalizedDrinkingObservations(String sessionId) {
        CompositeBundle bundle = new CompositeBundle();
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        Bundle sourceBundle = workspace.getDrinkingObservations();
        if (sourceBundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : sourceBundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    Observation o = (Observation) entry.getResource();
                    if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), fcm.getDrinkingCodings())) {
                        try {
                            bundle.consume(normalizeDrinkingObservation(o));

                        } catch (Exception e) {
                            logger.error("caught " + e.getClass().getSimpleName() + " building normalized drinking Observation from resource with id=" + o.getId(), e);
                        }
                    }
                }
            }
        }

        return bundle.getBundle();
    }

    private static final Coding DRINKS_PER_DAY_CODING = new Coding("http://loinc.org", "11287-0", "Alcoholic drinks per drinking day - Reported");
    private static final Coding DRINKS_PER_WEEK_CODING = new Coding("http://loinc.org", "44940-5", "Alcoholic drinks per week - Reported");
    private static final Coding DRINKS_SHX_1_TO_2_PER_DAY = new Coding("http://loinc.org", "LA15694-5", "SAMHSA-drinks a day - 1 or 2");
    private static final Coding DRINKS_SHX_3_OR_4_PER_DAY = new Coding("http://loinc.org", "LA15695-2", "SAMHSA-drinks a day - 3 or 4");
    private static final Coding DRINKS_SHX_5_OR_6_PER_DAY = new Coding("http://loinc.org", "LA18930-0", "SAMHSA-drinks a day - 5 or 6");
    private static final Coding DRINKS_SHX_7_TO_9_PER_DAY = new Coding("http://loinc.org", "LA18931-8", "SAMHSA-drinks a day - 7 to 9");
    private static final Coding DRINKS_SHX_10_OR_MORE_PER_DAY = new Coding("http://loinc.org", "LA18932-6", "SAMHSA-drinks a day - 10 or more");
    private static final String DRINKS_PER_DAY = "Drinks/Day";

    private Observation normalizeDrinkingObservation(Observation o) throws DataException {
        Observation normalized = new Observation();
        normalized.setId(UUIDUtil.getRandomUUID());
        if (!normalized.hasMeta()) normalized.setMeta(new Meta());
        normalized.getMeta().setSource(o.getId());
        normalized.setSubject(o.getSubject());
        normalized.setStatus(o.getStatus());
        if      (o.hasEffective())  normalized.setEffective(o.getEffective());
        else if (o.hasIssued())     normalized.setEffective(new DateTimeType(o.getIssued()));
        normalized.setIssued(o.getIssued());

        Quantity valueQuantity = null;
        if (fcm.isDrinkingGetValueFromComponent()) {
            if (o.hasComponent()) {
                for (Observation.ObservationComponentComponent component : o.getComponent()) {
                    if (component.hasCode() && FhirUtil.hasCoding(component.getCode(), fcm.getDrinkingComponentCoding())) {
                        if (component.hasValueQuantity()) {
                            valueQuantity = component.getValueQuantity();
                            break;
                        } else {
                            throw new DataException("expected component to have a valueQuantity, but none found");
                        }
                    }
                }
            } else {
                throw new DataException("expected observation to have a component, but none found");
            }

        } else if (o.hasValueQuantity()) {
            valueQuantity = o.getValueQuantity();
        }

        BigDecimal perDrinkingDayValue;
        if (valueQuantity != null && valueQuantity.hasValue()) {
            if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), DRINKS_PER_DAY_CODING)) {
                perDrinkingDayValue = valueQuantity.getValue();

            } else if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), DRINKS_PER_WEEK_CODING)) {
                perDrinkingDayValue = valueQuantity.getValue().divide(new BigDecimal(7), 1, RoundingMode.HALF_UP);

            } else if (valueQuantity.hasUnit() && valueQuantity.getUnit().toLowerCase().endsWith("/day")) {
                perDrinkingDayValue = valueQuantity.getValue();

            } else if (valueQuantity.hasUnit() && valueQuantity.getUnit().toLowerCase().endsWith("/week")) {
                perDrinkingDayValue = valueQuantity.getValue().divide(new BigDecimal(7), 1, RoundingMode.HALF_UP);

            } else {
                throw new DataException("expected a per-day or per-week code, or for the unit to end with '/day' or '/week', but found " +
                        valueQuantity.getUnit() + " on Observation with id=" + o.getId());
            }

        } else if (o.hasValueCodeableConcept()) {
            if (FhirUtil.hasCoding(o.getValueCodeableConcept(), DRINKS_SHX_1_TO_2_PER_DAY)) {
                perDrinkingDayValue = new BigDecimal("1.5");
            } else if (FhirUtil.hasCoding(o.getValueCodeableConcept(), DRINKS_SHX_3_OR_4_PER_DAY)) {
                perDrinkingDayValue = new BigDecimal("3.5");
            } else if (FhirUtil.hasCoding(o.getValueCodeableConcept(), DRINKS_SHX_5_OR_6_PER_DAY)) {
                perDrinkingDayValue = new BigDecimal("5.5");
            } else if (FhirUtil.hasCoding(o.getValueCodeableConcept(), DRINKS_SHX_7_TO_9_PER_DAY)) {
                perDrinkingDayValue = new BigDecimal(8);
            } else if (FhirUtil.hasCoding(o.getValueCodeableConcept(), DRINKS_SHX_10_OR_MORE_PER_DAY)) {
                perDrinkingDayValue = new BigDecimal(10);
            } else {
                throw new DataException("expected Observation with id=" + o.getId() + " to have a valueCodeableConcept with a coding, but none found");
            }

        } else {
            throw new DataException("expected Observation with id=" + o.getId() + " to have a valueQuantity or valueCodeableConcept, but none found");
        }

        normalized.getCode().addCoding(DRINKS_PER_DAY_CODING);
        normalized.setValue(new Quantity()
                .setValue(perDrinkingDayValue)
                .setUnit(DRINKS_PER_DAY)
        );

        return normalized;
    }
}
