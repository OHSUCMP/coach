package edu.ohsu.cmp.coach.model;

import java.util.HashMap;
import java.util.Map;

public enum RecommendationSeverity {
    // these values taken from https://terminology.hl7.org/CodeSystem-cdshooks-indicator.html
    INFO("info"),
    WARNING("warning"),
    CRITICAL("critical");

    private static final Map<String, RecommendationSeverity> MAP = new HashMap<>();
    static {
        for (RecommendationSeverity r : values()) {
            MAP.put(r.indicator, r);
        }
    }

    public static RecommendationSeverity fromIndicator(String indicator) {
        return MAP.get(indicator);
    }

    private final String indicator;

    RecommendationSeverity(String indicator) {
        this.indicator = indicator;
    }

    public String getIndicator() {
        return indicator;
    }
}
