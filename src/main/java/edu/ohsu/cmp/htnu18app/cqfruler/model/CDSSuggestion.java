package edu.ohsu.cmp.htnu18app.cqfruler.model;

import java.util.List;

public class CDSSuggestion {
    private String label;
    private List<String> actions;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}
