package edu.ohsu.cmp.coach.fhir;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@PropertySource("${fhirqueries.file}")
public class FhirQueryManager {
    private static final String TOKEN_ID = "\\{id}";
    private static final String TOKEN_SUBJECT = "\\{subject}";
    private static final String TOKEN_ENCOUNTER = "\\{encounter}";
    private static final String TOKEN_CODE = "\\{code}";
    private static final String TOKEN_CATEGORY = "\\{category}";

    private static final String TOKEN_RELATIVE_DATE = "\\{now([-+])([mMdDyY0-9]+)}"; // "\\{now[-+][mMdDyY0-9]+}";
    private static final Pattern PATTERN_RELATIVE_DATE = Pattern.compile("now([-+])([mMdDyY0-9]+)");
    private static final Pattern PATTERN_RELATIVE_DATE_PART = Pattern.compile("([0-9]+)([mMdDyY])");
    private static final DateFormat FHIR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Value("${Patient.Lookup}")                     private String patientLookup;

//    @Value("${Base.Query}")                         private String baseQuery;

    @Value("${Encounter.Query}")            private String encounterQuery;
    @Value("${Observation.Query}")          private String observationQuery;
    @Value("${Condition.Query}")            private String conditionQuery;
    @Value("${Goal.Query}")                 private String goalQuery;
    @Value("${MedicationStatement.Query}")  private String medicationStatementQuery;
    @Value("${MedicationRequest.Query}")    private String medicationRequestQuery;

    public String getPatientLookup(String id) {
        return buildQuery(patientLookup, params()
                .add(TOKEN_ID, id)
        );
    }

    public boolean hasEncounterQuery() {
        return StringUtils.isNotEmpty(encounterQuery);
    }

    public String getEncounterQuery(String patientId) {
        return buildQuery(encounterQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    public boolean hasObservationQuery() {
        return StringUtils.isNotEmpty(observationQuery);
    }

    public String getObservationQuery(String patientId, String category) {
        return buildQuery(observationQuery, params()
                .add(TOKEN_SUBJECT, patientId)
                .add(TOKEN_CATEGORY, category)
        );
    }

    public boolean hasConditionQuery() {
        return StringUtils.isNotEmpty(conditionQuery);
    }

    public String getConditionQuery(String patientId, String category) {
        return buildQuery(conditionQuery, params()
                .add(TOKEN_SUBJECT, patientId)
                .add(TOKEN_CATEGORY, category)
        );
    }

    public boolean hasGoalQuery() {
        return StringUtils.isNotEmpty(goalQuery);
    }

    public String getGoalQuery(String patientId) {
        return buildQuery(goalQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    public boolean hasMedicationStatementQuery() {
        return StringUtils.isNotEmpty(medicationStatementQuery);
    }

    public String getMedicationStatementQuery(String patientId) {
        return buildQuery(medicationStatementQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }

    public boolean hasMedicationRequestQuery() {
        return StringUtils.isNotEmpty(medicationRequestQuery);
    }

    public String getMedicationRequestQuery(String patientId) {
        return buildQuery(medicationRequestQuery, params()
                .add(TOKEN_SUBJECT, patientId)
        );
    }


//////////////////////////////////////////////////////////////////////
// private methods
//

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
        for (Map.Entry<String, String> entry : params.entrySet()) {
            template = template.replaceAll(entry.getKey(), entry.getValue());
        }
        return template.replaceAll(TOKEN_RELATIVE_DATE, buildRelativeDate(extract(TOKEN_RELATIVE_DATE, template)));
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
