package edu.ohsu.cmp.coach.model;

public enum ObservationSource {
    OFFICE,
    HOME,           // generic HOME source, used usually when coming from the FHIR server
    COACH_UI,       // when we know the source came from the COACH UI
    OMRON,          // when we know the source came from Omron
    UNKNOWN
}
