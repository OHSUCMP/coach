package edu.ohsu.cmp.coach.fhir;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("${fhirqueries.file}")
public class FhirQueryManager {
    @Value("${Patient.Lookup}")                 private String patientLookup;
    @Value("${Patient.Strategy}")               private FhirStrategy patientStrategy;
    @Value("${Encounter.Query}")                private String encounterQuery;
    @Value("${Encounter.Strategy}")             private FhirStrategy encounterStrategy;
//    @Value("${Observation.Category.Query}")     private String observationCategoryQuery;
//    @Value("${Observation.Category.Strategy}")  private FhirStrategy observationCategoryStrategy;
    @Value("${Observation.Query}")              private String observationQuery;
    @Value("${Observation.Strategy}")           private FhirStrategy observationStrategy;
    @Value("${Condition.Query}")                private String conditionQuery;
    @Value("${Condition.Strategy}")             private FhirStrategy conditionStrategy;
    @Value("${Goal.Query}")                     private String goalQuery;
    @Value("${Goal.Strategy}")                  private FhirStrategy goalStrategy;
    @Value("${MedicationStatement.Query}")      private String medicationStatementQuery;
    @Value("${MedicationStatement.Strategy}")   private FhirStrategy medicationStatementStrategy;
    @Value("${MedicationRequest.Query}")        private String medicationRequestQuery;
    @Value("${MedicationRequest.Strategy}")     private FhirStrategy medicationRequestStrategy;
    @Value("${Medication.Strategy}")            private FhirStrategy medicationStrategy;
    @Value("${Procedure.Query}")                private String procedureQuery;
    @Value("${Procedure.Strategy}")             private FhirStrategy procedureStrategy;
    @Value("${ServiceRequest.Query}")           private String serviceRequestQuery;
    @Value("${ServiceRequest.Strategy}")        private FhirStrategy serviceRequestStrategy;


    public String getPatientLookup() {
        return patientLookup;
    }

    public FhirStrategy getPatientStrategy() {
        return patientStrategy;
    }

    public String getEncounterQuery() {
        return encounterQuery;
    }

    public FhirStrategy getEncounterStrategy() {
        return encounterStrategy;
    }

//    public String getObservationCategoryQuery() {
//        return observationCategoryQuery;
//    }
//
//    public FhirStrategy getObservationCategoryStrategy() {
//        return observationCategoryStrategy;
//    }

    public String getObservationQuery() {
        return observationQuery;
    }

    public FhirStrategy getObservationStrategy() {
        return observationStrategy;
    }

    public String getConditionQuery() {
        return conditionQuery;
    }

    public FhirStrategy getConditionStrategy() {
        return conditionStrategy;
    }

    public String getGoalQuery() {
        return goalQuery;
    }

    public FhirStrategy getGoalStrategy() {
        return goalStrategy;
    }

    public String getMedicationStatementQuery() {
        return medicationStatementQuery;
    }

    public FhirStrategy getMedicationStatementStrategy() {
        return medicationStatementStrategy;
    }

    public String getMedicationRequestQuery() {
        return medicationRequestQuery;
    }

    public FhirStrategy getMedicationRequestStrategy() {
        return medicationRequestStrategy;
    }

    public FhirStrategy getMedicationStrategy() {
        return medicationStrategy;
    }

    public String getProcedureQuery() {
        return procedureQuery;
    }

    public FhirStrategy getProcedureStrategy() {
        return procedureStrategy;
    }

    public String getServiceRequestQuery() {
        return serviceRequestQuery;
    }

    public FhirStrategy getServiceRequestStrategy() {
        return serviceRequestStrategy;
    }
}
