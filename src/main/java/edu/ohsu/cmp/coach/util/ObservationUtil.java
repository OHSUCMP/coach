package edu.ohsu.cmp.coach.util;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.EncounterMatcher;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.ObservationSource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;

import java.util.Date;

public class ObservationUtil {

    public static ObservationSource getSourceByEncounter(Encounter encounter, FhirConfigManager fcm) {
        ObservationSource source = null;

        EncounterMatcher matcher = new EncounterMatcher(fcm);
        if      (matcher.isOfficeEncounter(encounter))  source = ObservationSource.OFFICE;
        else if (matcher.isHomeEncounter(encounter))    source = ObservationSource.HOME;    // this should remain generic HOME as a more specific source is unknown

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

            if (FhirUtil.hasCoding(code, fcm.getBpHomeCodings()) || FhirUtil.hasHomeSettingExtension(bpObservation)) {
                source = ObservationSource.HOME;    // this should remain generic HOME as a more specific source is unknown

            } else if (FhirUtil.hasCoding(code, fcm.getBpOfficeCodings())) {
                source = ObservationSource.OFFICE;

            } else if (FhirUtil.hasCoding(code, fcm.getBpPanelCodings()) && bpObservation.hasComponent()) {
                // okay, so we couldn't determine the context of this Observation from its code element.  perhaps
                // if this is a panel, there may be a component that can tell us
                for (Observation.ObservationComponentComponent component : bpObservation.getComponent()) {
                    if (component.hasCode()) {
                        if (FhirUtil.hasCoding(component.getCode(), fcm.getBpHomeCodings())) {
                            source = ObservationSource.HOME;
                            break;

                        } else if (FhirUtil.hasCoding(component.getCode(), fcm.getBpOfficeCodings())) {
                            source = ObservationSource.OFFICE;
                            break;
                        }
                    }
                }
            }
        }

        return source != null ?
                source :
                ObservationSource.UNKNOWN;
    }

    // todo : getPulseSource doesn't have an implementation that takes Encounter as a parameter, as getBPSource
    //        functions above do.  This means that the PulseModel constructor doesn't make use of a fallback strategy
    //        in the way that BloodPressureModel does, and this should be updated

    public static ObservationSource getPulseSource(Observation pulseObservation) {
        return FhirUtil.hasHomeSettingExtension(pulseObservation) ?
                ObservationSource.HOME :    // this should remain generic HOME as a more specific source is unknown
                ObservationSource.UNKNOWN;
    }

    public static Date getReadingDate(Observation observation) throws DataException {
        if (observation.getEffectiveDateTimeType() != null) {
            return observation.getEffectiveDateTimeType().getValue(); //.getTime();

        } else if (observation.getEffectiveInstantType() != null) {
            return observation.getEffectiveInstantType().getValue(); //.getTime();

        } else if (observation.getEffectivePeriod() != null) {
            return observation.getEffectivePeriod().getEnd(); //.getTime();

        } else {
            throw new DataException("missing timestamp");
        }
    }
}
