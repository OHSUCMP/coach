package edu.ohsu.cmp.coach.model;

import java.util.HashMap;
import java.util.Map;

public enum RedcapDataAccessGroup {
    OHSU("ohsu"),
    VUMC("vumc"),
    MU("mu");


    private static final Map<String, RedcapDataAccessGroup> TAG_MAP = new HashMap<>();
    static {
        for (RedcapDataAccessGroup dag : values()) {
            TAG_MAP.put(dag.tag, dag);
        }
    }

    public static RedcapDataAccessGroup fromTag(String tag) {
        return TAG_MAP.get(tag);
    }

    private final String tag;

    RedcapDataAccessGroup(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }
}
