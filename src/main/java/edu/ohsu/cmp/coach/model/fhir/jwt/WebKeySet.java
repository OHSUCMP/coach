package edu.ohsu.cmp.coach.model.fhir.jwt;

import java.util.HashSet;
import java.util.Set;

public class WebKeySet {
    private Set<WebKey> keys;

    public WebKeySet() {
        keys = new HashSet<>();
    }

    public void add(WebKey webKey) {
        keys.add(webKey);
    }

    public Set<WebKey> getKeys() {
        return keys;
    }
}