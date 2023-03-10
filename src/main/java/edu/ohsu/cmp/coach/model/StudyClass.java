package edu.ohsu.cmp.coach.model;

import java.util.HashMap;
import java.util.Map;

public enum StudyClass {
    CONTROL("control"),
    INTERVENTION("intervention");

    private static final Map<String, StudyClass> MAP = new HashMap<>();
    static {
        for (StudyClass sc : values()) {
            MAP.put(sc.label, sc);
        }
    }

    public static StudyClass fromString(String string) {
        return MAP.get(string);
    }

    private final String label;

    StudyClass(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
