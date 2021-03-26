package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.exception.DataException;
import org.hl7.fhir.r4.model.MedicationStatement;

public class MedicationModel {

    // see https://www.hl7.org/fhir/terminologies-systems.html
    public static final String SYSTEM = "http://www.nlm.nih.gov/research/umls/rxnorm";
    public static final String VALUE_SET_OID = "2.16.840.1.113762.1.4.1178.10";


    public MedicationModel(MedicationStatement ms) throws DataException {

    }
}
