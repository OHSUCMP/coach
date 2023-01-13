package edu.ohsu.cmp.coach.fhir;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("${fhirqueries.file}")
public class FhirQueryManager {
    @Value("${Patient.Lookup}")     private String patientLookup;
    @Value("${Encounter.Query}")    private String encounterQuery;
    @Value("${Observation.Category.Query}") private String observationCategoryQuery;
    @Value("${Observation.Code.Query}")     private String observationCodeQuery;
    @Value("${Condition.Query}")            private String conditionQuery;
    @Value("${Goal.Query}")                 private String goalQuery;
    @Value("${MedicationStatement.Query}")  private String medicationStatementQuery;
    @Value("${MedicationRequest.Query}")    private String medicationRequestQuery;
    @Value("${Procedure.Query}")    private String procedureQuery;

    public String getPatientLookup() {
        return patientLookup;
    }

    public String getEncounterQuery() {
        return encounterQuery;
    }

    public String getObservationCategoryQuery() {
        return observationCategoryQuery;
    }

    public String getObservationCodeQuery() {
        return observationCodeQuery;
    }

    public String getConditionQuery() {
        return conditionQuery;
    }

    public String getGoalQuery() {
        return goalQuery;
    }

    public String getMedicationStatementQuery() {
        return medicationStatementQuery;
    }

    public String getMedicationRequestQuery() {
        return medicationRequestQuery;
    }

    public String getProcedureQuery() {
        return procedureQuery;
    }
}
