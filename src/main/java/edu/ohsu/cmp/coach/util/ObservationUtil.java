package edu.ohsu.cmp.coach.util;

import edu.ohsu.cmp.coach.fhir.EncounterMatcher;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.ObservationSource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;

public class ObservationUtil {

    public static ObservationSource getSourceByEncounter(Encounter encounter, FhirConfigManager fcm) {
        ObservationSource source = null;

        EncounterMatcher matcher = new EncounterMatcher(fcm);
        if (matcher.isAmbEncounter(encounter))              source = ObservationSource.OFFICE;
        else if (matcher.isHomeHealthEncounter(encounter))  source = ObservationSource.HOME;

        return source != null ?
                source :
                ObservationSource.UNKNOWN;
    }

    public static ObservationSource getBPSource(Observation bpObservation, Encounter encounter, FhirConfigManager fcm) {
        ObservationSource source = getBPSource(bpObservation, fcm);
        if (source == ObservationSource.UNKNOWN) {
            source = getSourceByEncounter(encounter, fcm);
        }
        return source;
    }

    public static ObservationSource getBPSource(Observation bpObservation, FhirConfigManager fcm) {
        ObservationSource source = null;

        if (bpObservation.hasCode()) {
            CodeableConcept code = bpObservation.getCode();

            if (FhirUtil.hasCoding(code, fcm.getBpHomeBluetoothSystolicCoding()) ||
                    FhirUtil.hasCoding(code, fcm.getBpHomeBluetoothDiastolicCoding())) {
                source = ObservationSource.HOME_BLUETOOTH;

            } else if (FhirUtil.hasCoding(code, fcm.getBpHomeCodings()) || FhirUtil.hasHomeSettingExtension(bpObservation)) {
                source = ObservationSource.HOME;

            } else if (FhirUtil.hasCoding(code, fcm.getBpOfficeCodings())) {
                source = ObservationSource.OFFICE;
            }
        }

        return source != null ?
                source :
                ObservationSource.UNKNOWN;
    }

    public static ObservationSource getPulseSource(Observation pulseObservation) {
        return FhirUtil.hasHomeSettingExtension(pulseObservation) ?
                ObservationSource.HOME :
                ObservationSource.UNKNOWN;
    }
}
