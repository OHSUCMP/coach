package edu.ohsu.cmp.coach.model.recommendation;

import edu.ohsu.cmp.coach.entity.MyGoal;

import java.util.List;

public class Suggestion {
    public static final String TYPE_GOAL = "goal";
    public static final String TYPE_UPDATE_GOAL = "update-goal";
    public static final String TYPE_SUGGESTION_LINK = "suggestion-link";
    public static final String TYPE_COUNSELING_LINK = "counseling-link";
    public static final String TYPE_BP_GOAL = "bp-goal";
    public static final String TYPE_UPDATE_BP_GOAL = "update-bp-goal";

    private String id;
    private String type;
    private Reference references;
    private String label;
    private List<Action> actions;
    private MyGoal goal;  // the patient's recorded goal when type is an 'update' variant

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

    public MyGoal getGoal() {
        return goal;
    }

    public void setGoal(MyGoal goal) {
        this.goal = goal;
    }
}
