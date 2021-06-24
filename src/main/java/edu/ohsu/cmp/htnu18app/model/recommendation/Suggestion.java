package edu.ohsu.cmp.htnu18app.model.recommendation;

import java.util.List;

public class Suggestion {
    public static final String TYPE_GOAL = "goal";
    public static final String TYPE_COUNSELING = "counseling";

    private String id;
    private String type; // counseling, goal
    private Reference references;
    private String label;
    private List<Action> actions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Reference getReferences() {
        return references;
    }

    public void setReferences(Reference reference) {
        this.references = reference;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }
}
