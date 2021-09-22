package edu.ohsu.cmp.coach.model.recommendation;

import java.util.HashMap;
import java.util.Map;

public enum Audience {
    PATIENT("patient"),
    CARE_TEAM("careTeam");

    private static final Map<String, Audience> TAG_MAP = new HashMap<String, Audience>();
    static {
        for (Audience a : values()) {
            TAG_MAP.put(a.tag, a);
        }
    }

    public static Audience fromTag(String tag) {
        return TAG_MAP.get(tag);
    }

    private final String tag;

    Audience(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }
}
