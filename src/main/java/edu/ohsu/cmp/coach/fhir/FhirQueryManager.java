package edu.ohsu.cmp.coach.fhir;

import edu.ohsu.cmp.coach.util.FhirUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@PropertySource("${fhirqueries.file}")
public class FhirQueryManager {
    private static final String TOKEN_ID = "\\{id}";
    private static final String TOKEN_SUBJECT = "\\{subject}";
    private static final String TOKEN_ENCOUNTER = "\\{encounter}";
    private static final String TOKEN_CODE = "\\{code}";

    private static final String TOKEN_RELATIVE_DATE = "\\{now([-+])([mMdDyY0-9]+)}"; // "\\{now[-+][mMdDyY0-9]+}";
    private static final Pattern PATTERN_RELATIVE_DATE = Pattern.compile("now([-+])([mMdDyY0-9]+)");
    private static final Pattern PATTERN_RELATIVE_DATE_PART = Pattern.compile("([0-9]+)([mMdDyY])");
    private static final DateFormat FHIR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Value("${Patient.Lookup}")                     private String patientLookup;
    @Value("${Observation.Query.code}")             private String observationQueryCode;
    @Value("${Observation.ByEncounterQuery.code}")  private String observationByEncounterQueryCode;
    @Value("${Condition.Query}")                    private String conditionQuery;
    @Value("${Goal.Query}")                         private String goalQuery;
    @Value("${MedicationStatement.Query}")          private String medicationStatementQuery;
    @Value("${MedicationRequest.Query}")            private String medicationRequestQuery;
    @Value("${AdverseEvent.Query}")                 private String adverseEventQuery;

    public String getPatientLookup(String id) {
        return buildQuery(patientLookup, params()
                .add(TOKEN_ID, id)
        );
    }

    public String getObservationQueryCode(String patientId, String system, String code) {
        return buildQuery(observationQueryCode, params()
                .add(TOKEN_SUBJECT, patientId)
                .add(TOKEN_CODE, system + '|' + code)
        );
    }

    public String getObservationByEncounterQueryCode(String patientId, String encounterRef, String system, String code) {
        return buildQuery(observationByEncounterQueryCode, params()
                .add(TOKEN_SUBJECT, patientId)
                .add(TOKEN_ENCOUNTER, FhirUtil.toRelativeReference(encounterRef))
                .add(TOKEN_CODE, system + '|' + code)
        );
    }

    public String getConditionQuery(String patientId) {
        return buildQuery(conditionQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    public String getGoalQuery(String patientId) {
        return buildQuery(goalQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    public String getMedicationStatementQuery(String patientId) {
        return buildQuery(medicationStatementQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    public String getMedicationRequestQuery(String patientId) {
        return buildQuery(medicationRequestQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    public String getAdverseEventQuery(String patientId) {
        return buildQuery(adverseEventQuery, params()
                .add(TOKEN_SUBJECT, patientId)
                .add(TOKEN_RELATIVE_DATE, buildRelativeDate(extract(TOKEN_RELATIVE_DATE, adverseEventQuery)))
        );
    }

    public static Params params() {
        return new Params();
    }

    public static final class Params extends HashMap<String, String> {
        public Params add(String key, String value) {
            put(key, value);
            return this;
        }
    }

//////////////////////////////////////////////////////////////////////
// private methods
//

    private String buildQuery(String template, Map<String, String> params) {
        return template.replaceAll(TOKEN_ID, params.get(TOKEN_ID))
                .replaceAll(TOKEN_SUBJECT, params.get(TOKEN_SUBJECT))
                .replaceAll(TOKEN_CODE, params.get(TOKEN_CODE))
                .replaceAll(TOKEN_ENCOUNTER, params.get(TOKEN_ENCOUNTER))
                .replaceAll(TOKEN_RELATIVE_DATE, buildRelativeDate(extract(TOKEN_RELATIVE_DATE, template)));
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

            return FHIR_DATE_FORMAT.format(cal.getTime());
        }

        return "";
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
}
