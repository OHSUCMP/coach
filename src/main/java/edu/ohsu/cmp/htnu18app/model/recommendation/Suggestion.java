package edu.ohsu.cmp.htnu18app.model.recommendation;

import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSSuggestion;
import edu.ohsu.cmp.htnu18app.util.MustacheUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Suggestion {
    private String label;
    private List<String> actions;

    public Suggestion(Audience audience, CDSSuggestion cdsSuggestion) throws IOException {
        this.label = cdsSuggestion.getLabel();

        actions = new ArrayList<String>();
        for (String cdsAction : cdsSuggestion.getActions()) {
            String action = MustacheUtil.compileMustache(audience, cdsAction);
            if (action != null && !action.trim().equals("")) {
                actions.add(action);
            }
        }
    }

    public String getLabel() {
        return label;
    }

    public List<String> getActions() {
        return actions;
    }
}
