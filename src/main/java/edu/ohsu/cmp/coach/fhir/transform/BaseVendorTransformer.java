package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseVendorTransformer implements VendorTransformer {
    private static final String TOKEN_ID = "\\{id}";
    private static final String TOKEN_SUBJECT = "\\{subject}";
    private static final String TOKEN_ENCOUNTER = "\\{encounter}";
    private static final String TOKEN_CODE = "\\{code}";
    private static final String TOKEN_CATEGORY = "\\{category}";
    private static final String TOKEN_RELATIVE_DATE = "\\{now([-+])([mMdDyY0-9]+)}"; // "\\{now[-+][mMdDyY0-9]+}";
    private static final Pattern PATTERN_RELATIVE_DATE = Pattern.compile("now([-+])([mMdDyY0-9]+)");
    private static final Pattern PATTERN_RELATIVE_DATE_PART = Pattern.compile("([0-9]+)([mMdDyY])");
    private static final DateFormat FHIR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    protected static final String NO_ENCOUNTERS_KEY = null; // intentionally instantiated with null value

    public static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
    public static final String OBSERVATION_CATEGORY_CODE = "vital-signs";

    protected static final String UUID_NOTE_TAG = "COACH_OBSERVATION_GROUP_UUID::";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected UserWorkspace workspace;

    public BaseVendorTransformer(UserWorkspace workspace) {
        this.workspace = workspace;
    }

    protected abstract BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation bpObservation, Observation protocolObservation) throws DataException;
    protected abstract BloodPressureModel buildBloodPressureModel(Encounter encounter, Observation systolicObservation, Observation diastolicObservation, Observation protocolObservation) throws DataException;
    protected abstract BloodPressureModel buildBloodPressureModel(Observation o) throws DataException;
    protected abstract BloodPressureModel buildBloodPressureModel(Observation systolicObservation, Observation diastolicObservation) throws DataException;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String getPatientLookup(String id) {
        String patientLookup = workspace.getFhirQueryManager().getPatientLookup();
        return buildQuery(patientLookup, params()
                .add(TOKEN_ID, id)
        );
    }

    @Override
    public String getEncounterQuery(String patientId) {
        return getEncounterQuery(patientId, null);
    }

    @Override
    public String getEncounterQuery(String patientId, String lookbackPeriod) {
        String encounterQuery = workspace.getFhirQueryManager().getEncounterQuery();

        if (StringUtils.isBlank(encounterQuery)) return null;

        String query = lookbackPeriod != null ?
                addLookbackPeriodParam(encounterQuery, lookbackPeriod) :
                encounterQuery;

        return buildQuery(query, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    @Override
    public String getObservationCategoryQuery(String patientId, String category) {
        return getObservationCategoryQuery(patientId, category, null);
    }

    @Override
    public String getObservationCategoryQuery(String patientId, String category, String lookbackPeriod) {
        String observationCategoryQuery = workspace.getFhirQueryManager().getObservationCategoryQuery();
        String query = lookbackPeriod != null ?
                addLookbackPeriodParam(observationCategoryQuery, lookbackPeriod) :
                observationCategoryQuery;

        return buildQuery(query, params()
                .add(TOKEN_SUBJECT, patientId)
                .add(TOKEN_CATEGORY, category)
        );
    }

    @Override
    public String getObservationCodeQuery(String patientId, String code) {
        return getObservationCodeQuery(patientId, code, null);
    }

    @Override
    public String getObservationCodeQuery(String patientId, String code, String lookbackPeriod) {
        String observationCodeQuery = workspace.getFhirQueryManager().getObservationCodeQuery();
        String query = StringUtils.isNotBlank(lookbackPeriod) ?
                addLookbackPeriodParam(observationCodeQuery, lookbackPeriod) :
                observationCodeQuery;

        return buildQuery(query, params()
                .add(TOKEN_SUBJECT, patientId)
                .add(TOKEN_CODE, code)
        );
    }

    @Override
    public String getConditionQuery(String patientId, String category) {
        String conditionQuery = workspace.getFhirQueryManager().getConditionQuery();
        return buildQuery(conditionQuery, params()
                .add(TOKEN_SUBJECT, patientId)
                .add(TOKEN_CATEGORY, category)
        );
    }

    @Override
    public String getGoalQuery(String patientId) {
        String goalQuery = workspace.getFhirQueryManager().getGoalQuery();
        return buildQuery(goalQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    @Override
    public String getMedicationStatementQuery(String patientId) {
        String medicationStatementQuery = workspace.getFhirQueryManager().getMedicationStatementQuery();
        return buildQuery(medicationStatementQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    @Override
    public String getMedicationRequestQuery(String patientId) {
        String medicationRequestQuery = workspace.getFhirQueryManager().getMedicationRequestQuery();
        return buildQuery(medicationRequestQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    @Override
    public String getProcedureQuery(String patientId) {
        String procedureQuery = workspace.getFhirQueryManager().getProcedureQuery();
        return buildQuery(procedureQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    private static Params params() {
        return new Params();
    }

    private static final class Params extends HashMap<String, String> {
        public Params add(String key, String value) {
            put(key, value);
            return this;
        }
    }

    private String buildQuery(String template, Map<String, String> params) {
        return buildQuery(template, params, null);
    }

    private String buildQuery(String template, Map<String, String> params, Integer limit) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            template = template.replaceAll(entry.getKey(), entry.getValue());
        }
        template = template.replaceAll(TOKEN_RELATIVE_DATE, buildRelativeDate(extract(TOKEN_RELATIVE_DATE, template)));
        return limit != null ?
                template + "&_count=" + limit + "&_total=" + limit :
                template;
    }

    private String buildRelativeDate(String s) {
        Matcher m1 = PATTERN_RELATIVE_DATE.matcher(s);

        if (m1.matches()) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            int multiplier = m1.group(1).equals("-") ? -1 : 1;

            Matcher m2 = PATTERN_RELATIVE_DATE_PART.matcher(m1.group(2));
            while (m2.find()) {
                int i = Integer.parseInt(m2.group(1));
                String datePart = m2.group(2);

                if (datePart.equalsIgnoreCase("y")) {
                    cal.add(Calendar.YEAR, multiplier * i);

                } else if (datePart.equalsIgnoreCase("m")) {
                    cal.add(Calendar.MONTH, multiplier * i);

                } else if (datePart.equalsIgnoreCase("d")) {
                    cal.add(Calendar.DAY_OF_MONTH, multiplier * i);
                }
            }

            return getFhirDateFormat().format(cal.getTime());
        }

        return "";
    }

    protected DateFormat getFhirDateFormat() {
        return FHIR_DATE_FORMAT;
    }

    private String extract(String token, String s) {
        Pattern p = Pattern.compile(".*(" + token + ").*");
        Matcher m = p.matcher(s);
        if (m.matches()) {
            String s2 = m.group(1);
            return s2.substring(1, s2.length() - 1);
        }
        return "";
    }

    private String addLookbackPeriodParam(String query, String lookbackPeriod) {
        return query + "&date=ge{now-" + lookbackPeriod + "}";
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public List<BloodPressureModel> transformIncomingBloodPressureReadings(Bundle bundle) throws DataException {
        if (bundle == null) return null;

        Map<String, List<Observation>> encounterObservationsMap = buildEncounterObservationsMap(bundle);
        FhirConfigManager fcm = workspace.getFhirConfigManager();

        List<Coding> bpPanelCodings = fcm.getBpPanelCodings();
        List<Coding> systolicCodings = fcm.getBpSystolicCodings();
        List<Coding> diastolicCodings = fcm.getBpDiastolicCodings();

        List<BloodPressureModel> list = new ArrayList<>();

        for (Encounter encounter : getAllEncounters(bundle)) {
            logger.debug("processing Encounter: " + encounter.getId());

            // these Observations get popped / removed from the map
            List<Observation> encounterObservations = getObservationsFromMap(encounter, encounterObservationsMap);

            if (encounterObservations == null) {
                logger.debug("no Observations found for Encounter " + encounter.getId() + " - skipping -");
                continue;
            }

            logger.debug("building Observations for Encounter " + encounter.getId());

            List<Observation> bpObservationList = new ArrayList<>();            // potentially many per encounter
            Map<String, SystolicDiastolicPair> map = new LinkedHashMap<>();     // potentially many per encounter
            Observation protocol = null;

            for (Observation o : encounterObservations) {
                if ( ! o.hasCode() ) {
                    logger.warn("observation " + o.getId() + " missing code - skipping -");

                } else if (FhirUtil.hasCoding(o.getCode(), bpPanelCodings)) {
                    bpObservationList.add(o);

                } else if (FhirUtil.hasCoding(o.getCode(), systolicCodings)) {
                    String key = getObservationMatchKey(o);
                    if ( ! map.containsKey(key) ) {
                        map.put(key, new SystolicDiastolicPair());
                    }
                    map.get(key).setSystolicObservation(o);

                } else if (FhirUtil.hasCoding(o.getCode(), diastolicCodings)) {
                    String key = getObservationMatchKey(o);
                    if ( ! map.containsKey(key) ) {
                        map.put(key, new SystolicDiastolicPair());
                    }
                    map.get(key).setDiastolicObservation(o);

                } else if (protocol == null && FhirUtil.hasCoding(o.getCode(), fcm.getProtocolCoding())) {
                    logger.debug("protocolObservation = " + o.getId() + " (encounter=" + encounter.getId() +
                            ") (effectiveDateTime=" + o.getEffectiveDateTimeType().getValueAsString() + ")");
                    protocol = o;
                }
            }

            // process BP panel Observations

            for (Observation bp : bpObservationList) {
                logger.debug("bpObservation = " + bp.getId() + " (encounter=" + encounter.getId() +
                        ") (effectiveDateTime=" + bp.getEffectiveDateTimeType().getValueAsString() + ")");
                try {
                    list.add(buildBloodPressureModel(encounter, bp, protocol));

                } catch (DataException e) {
                    logger.warn("caught " + e.getClass().getSimpleName() +
                            " building BloodPressureModel from Observation with id=" + bp.getId() + " - " +
                            e.getMessage() + " - skipping -");
                }
            }

            // process systolic and diastolic Observation pairs

            for (Map.Entry<String, SystolicDiastolicPair> entry : map.entrySet()) {
                SystolicDiastolicPair sdp = entry.getValue();
                if (sdp.isValid()) {
                    Observation systolic = sdp.getSystolicObservation();
                    Observation diastolic = sdp.getDiastolicObservation();

                    logger.debug("systolicObservation = " + systolic.getId() + " (effectiveDateTime=" +
                            systolic.getEffectiveDateTimeType().getValueAsString() + ")");
                    logger.debug("diastolicObservation = " + diastolic.getId() + " (effectiveDateTime=" +
                            diastolic.getEffectiveDateTimeType().getValueAsString() + ")");

                    try {
                        list.add(buildBloodPressureModel(encounter, systolic, diastolic, protocol));

                    } catch (DataException e) {
                        logger.warn("caught " + e.getClass().getSimpleName() +
                                " building BloodPressureModel from (systolic, diastolic) Observations with systolic.id=" +
                                systolic.getId() + ", diastolic.id=" + diastolic.getId() + " - " +
                                e.getMessage() + " - skipping -");
                    }

                } else {
                    logger.warn("found incomplete systolic-diastolic pair for readingDate=" + entry.getKey() + " - skipping -");
                }
            }
        }

        // there may be BP observations in the system that aren't tied to any encounters.  we still want to capture these
        // of course, we can't associate any other observations with them (e.g. protocol), but whatever.  better than nothing

        // these observations without Encounters that also have identical timestamps are presumed to be related.
        // these need to be combined into a single BloodPresureModel object for any pair of (systolic, diastolic) that
        // have the same timestamp
        // alternatively, Observations may have a specially-crafted note element that contains a UUID that can be used to
        // recombine independent systolic and diastolic readings

        // storer 12/7: Observations may have Encounter references, but if we can't get at those Encounter records,
        // we probably shouldn't be processing them.

        logger.debug("processing BP Observations that do not reference Encounters -");

        if (encounterObservationsMap.containsKey(NO_ENCOUNTERS_KEY)) {
            Map<String, SystolicDiastolicPair> map = new LinkedHashMap<>();

            for (Observation o : encounterObservationsMap.remove(NO_ENCOUNTERS_KEY)) {
                if (o.hasCode()) {
                    if (FhirUtil.hasCoding(o.getCode(), bpPanelCodings)) {
                        logger.debug("bpObservation = " + o.getId() + " (no encounter) (effectiveDateTime=" +
                                o.getEffectiveDateTimeType().getValueAsString() + ")");

                        try {
                            list.add(buildBloodPressureModel(o));

                        } catch (DataException e) {
                            logger.warn("caught " + e.getClass().getSimpleName() +
                                    " building BloodPressureModel from Observation with id=" + o.getId() + " - " +
                                    e.getMessage() + " - skipping -");
                        }

                    } else if (FhirUtil.hasCoding(o.getCode(), systolicCodings)) {
                        String key = getObservationMatchKey(o);
                        if ( ! map.containsKey(key) ) {
                            map.put(key, new SystolicDiastolicPair());
                        }
                        map.get(key).setSystolicObservation(o);

                    } else if (FhirUtil.hasCoding(o.getCode(), diastolicCodings)) {
                        String key = getObservationMatchKey(o);
                        if ( ! map.containsKey(key) ) {
                            map.put(key, new SystolicDiastolicPair());
                        }
                        map.get(key).setDiastolicObservation(o);

                    } else {
                        logger.debug("did not process Observation " + o.getId() + " - invalid coding");
                    }

                } else {
                    logger.debug("did not process Observation " + o.getId() + " - no coding");
                }
            }

            // now process dateObservationsMap, which should only include individual systolic and diastolic readings

            for (Map.Entry<String, SystolicDiastolicPair> entry : map.entrySet()) {
                SystolicDiastolicPair sdp = entry.getValue();
                if (sdp.isValid()) {
                    Observation systolic = sdp.getSystolicObservation();
                    Observation diastolic = sdp.getDiastolicObservation();

                    logger.debug("systolicObservation = " + systolic.getId() + " (effectiveDateTime=" +
                            systolic.getEffectiveDateTimeType().getValueAsString() + ")");
                    logger.debug("diastolicObservation = " + diastolic.getId() + " (effectiveDateTime=" +
                            diastolic.getEffectiveDateTimeType().getValueAsString() + ")");

                    try {
                        list.add(buildBloodPressureModel(systolic, diastolic));

                    } catch (DataException e) {
                        logger.warn("caught " + e.getClass().getSimpleName() +
                                " building BloodPressureModel from (systolic, diastolic) Observations with systolic.id=" +
                                systolic.getId() + ", diastolic.id=" + diastolic.getId() + " - " +
                                e.getMessage() + " - skipping -");
                    }

                } else {
                    logger.warn("found incomplete systolic-diastolic pair for readingDate=" + entry.getKey() + " - skipping -");
                }
            }
        }

        // finally, strip any item from the list where we can't determine the source

        Iterator<BloodPressureModel> iter = list.iterator();
        while (iter.hasNext()) {
            BloodPressureModel bpm = iter.next();
            if (bpm.getSource() == ObservationSource.UNKNOWN) {
                logger.warn("removing BloodPressureModel " + bpm + " - source is UNKNOWN");
                iter.remove();
            }
        }

        return list;
    }

    // helper class for organizing working objects
    private static final class SystolicDiastolicPair {
        private Observation systolicObservation = null;
        private Observation diastolicObservation = null;

        public boolean isValid() {
            return systolicObservation != null && diastolicObservation != null;
        }

        public Observation getSystolicObservation() {
            return systolicObservation;
        }

        public void setSystolicObservation(Observation systolicObservation) {
            this.systolicObservation = systolicObservation;
        }

        public Observation getDiastolicObservation() {
            return diastolicObservation;
        }

        public void setDiastolicObservation(Observation diastolicObservation) {
            this.diastolicObservation = diastolicObservation;
        }
    }

    protected Map<String, List<Observation>> buildEncounterObservationsMap(Bundle bundle) {
        Map<String, List<Observation>> map = new HashMap<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    Observation observation = (Observation) entry.getResource();
                    if (observation.hasEncounter()) {
                        List<String> keys = buildKeys(observation.getEncounter());

                        // we want to associate THE SAME list with each key, NOT separate instances of identical lists

                        List<Observation> list = null;
                        for (String key : keys) {
                            if (map.containsKey(key)) {
                                list = map.get(key);
                                break;
                            }
                        }
                        if (list == null) {
                            list = new ArrayList<>();
                            for (String key : keys) {
                                map.put(key, list);
                            }
                        }

                        map.get(keys.get(0)).add(observation);

                    } else {
                        List<Observation> list = map.get(NO_ENCOUNTERS_KEY);
                        if (list == null) {
                            list = new ArrayList<>();
                            map.put(NO_ENCOUNTERS_KEY, list);
                        }
                        list.add(observation);
                    }
                }
            }
        }
        return map;
    }

    /**
     * pops all Observations from the provided Map associated with the specified Encounter.
     * @param encounter
     * @param map
     * @return
     */
    protected List<Observation> getObservationsFromMap(Encounter encounter, Map<String, List<Observation>> map) {
        List<Observation> list = null;
        for (String key : buildKeys(encounter.getId(), encounter.getIdentifier())) {
            if (map.containsKey(key)) {     // the same exact list may be represented multiple times for different keys.
                if (list == null) {         // we only care about the first
                    list = map.remove(key);
                } else {
                    map.remove(key);
                }
            }
        }
        return list;
    }

    protected List<String> buildKeys(Reference reference) {
        return FhirUtil.buildKeys(reference);
    }

    protected List<String> buildKeys(String id, Identifier identifier) {
        return FhirUtil.buildKeys(id, identifier);
    }

    protected List<String> buildKeys(String id, List<Identifier> identifiers) {
        return FhirUtil.buildKeys(id, identifiers);
    }

    protected String genTemporaryId() {
        return UUID.randomUUID().toString();
    }

//    adapted from CDSHooksExecutor.buildHomeBloodPressureObservation()
//    used when creating new Home Health (HH) Blood Pressure Observations

//    protected Observation buildHomeHealthBloodPressureObservation(BloodPressureModel model, String patientId, FhirConfigManager fcm) throws DataException {
//        return buildHomeHealthBloodPressureObservation(model, null, patientId, fcm);
//    }

    protected Observation buildProtocolObservation(AbstractVitalsModel model, String patientId, FhirConfigManager fcm) throws DataException {
        return buildProtocolObservation(model, null, patientId, fcm);
    }

    protected Observation buildProtocolObservation(AbstractVitalsModel model, Encounter encounter, String patientId, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        if (encounter != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(encounter)));
        } else if (model.getSourceEncounter() != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(model.getSourceEncounter())));
        }

        o.setStatus(Observation.ObservationStatus.FINAL);
        o.getCode().addCoding(fcm.getProtocolCoding());

        FhirUtil.addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(model.getReadingDate()));

        String answerValue = model.getFollowedProtocol() ?
                fcm.getProtocolAnswerYes() :
                fcm.getProtocolAnswerNo();

        o.setValue(new CodeableConcept());
        o.getValueCodeableConcept()
                .setText(answerValue)
                .addCoding(fcm.getProtocolAnswerCoding());

        return o;
    }

    protected Goal buildGoal(GoalModel model, String patientId, FhirConfigManager fcm) {

        // this is only used when building local goals, for which sourceGoal == null

        Goal g = new Goal();

        g.setId(model.getExtGoalId());
        g.setSubject(new Reference().setReference(patientId));
        g.setLifecycleStatus(model.getLifecycleStatus().getFhirValue());
        g.getAchievementStatus().addCoding().setCode(model.getAchievementStatus().getFhirValue())
                .setSystem("http://terminology.hl7.org/CodeSystem/goal-achievement");
        g.getCategoryFirstRep().addCoding().setCode(model.getReferenceCode()).setSystem(model.getReferenceSystem());
        g.getDescription().setText(model.getGoalText());
        g.setStatusDate(model.getStatusDate());
        g.getTarget().add(new Goal.GoalTargetComponent()
                .setDue(new DateType().setValue(model.getTargetDate())));

        if (model.isBPGoal()) {
            Goal.GoalTargetComponent systolic = new Goal.GoalTargetComponent();
            systolic.getMeasure().addCoding(fcm.getBpSystolicCommonCoding());
            systolic.setDetail(new Quantity());
            systolic.getDetailQuantity().setCode(fcm.getBpValueCode());
            systolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
            systolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
            systolic.getDetailQuantity().setValue(model.getSystolicTarget());
            g.getTarget().add(systolic);

            Goal.GoalTargetComponent diastolic = new Goal.GoalTargetComponent();
            diastolic.getMeasure().addCoding(fcm.getBpDiastolicCommonCoding());
            diastolic.setDetail(new Quantity());
            diastolic.getDetailQuantity().setCode(fcm.getBpValueCode());
            diastolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
            diastolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
            diastolic.getDetailQuantity().setValue(model.getDiastolicTarget());
            g.getTarget().add(diastolic);
        }

        return g;
    }

    // most Encounters will be in the workspace cache, but newly created ones will not be in there yet,
    // although they *will* be in the bundle passed in as a parameter.  so consolidate those into one list
    protected List<Encounter> getAllEncounters(Bundle bundle) {
        List<Encounter> list = new ArrayList<>();

        list.addAll(workspace.getEncounters());

        if (bundle != null && bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource()) {
                    if (entry.getResource() instanceof Encounter) {
                        list.add((Encounter) entry.getResource());
                    }
                }
            }
        }

        return list;
    }

    private String getObservationMatchKey(Observation observation) {
        String uuid = getUUIDFromNote(observation); // used by Epic, but Default should still understand and use this if it exists
        return uuid != null ?
                uuid :
                observation.getEffectiveDateTimeType().getValueAsString();
    }

    private String getUUIDFromNote(Observation observation) {
        if (observation != null && observation.hasNote()) {
            for (Annotation annotation : observation.getNote()) {
                if (annotation.hasText() && annotation.getText().startsWith(UUID_NOTE_TAG)) {
                    return annotation.getText().substring(UUID_NOTE_TAG.length() + 1);
                }
            }
        }
        return null;
    }
}
