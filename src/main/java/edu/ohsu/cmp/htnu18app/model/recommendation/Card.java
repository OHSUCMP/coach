package edu.ohsu.cmp.htnu18app.model.recommendation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSCard;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSSuggestion;
import edu.ohsu.cmp.htnu18app.cqfruler.model.Source;
import edu.ohsu.cmp.htnu18app.util.MustacheUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Card {
    private String summary;
    private String indicator;
    private String detail;
    private Source source;
    private String rationale;
    private String source2;
    private List<Suggestion> suggestions;
    private String selectionBehavior;
    private String links;

    public Card(Audience audience, CDSCard cdsCard) throws IOException {
        this.summary = cdsCard.getSummary();
        this.indicator = cdsCard.getIndicator();
        this.detail = cdsCard.getDetail();
        this.source = cdsCard.getSource();

        this.rationale = MustacheUtil.compileMustache(audience, cdsCard.getRationale());
        this.source2 = MustacheUtil.compileMustache(audience, cdsCard.getSource2());

        this.suggestions = new ArrayList<Suggestion>();
        Gson gson = new GsonBuilder().create();
        List<CDSSuggestion> list = gson.fromJson(cdsCard.getSuggestions(), new TypeToken<ArrayList<CDSSuggestion>>(){}.getType());
        if (list != null) {
            for (CDSSuggestion cdsSuggestion : list) {
                this.suggestions.add(new Suggestion(audience, cdsSuggestion));
            }
        }

        this.selectionBehavior = MustacheUtil.compileMustache(audience, cdsCard.getSelectionBehavior());
        this.links = MustacheUtil.compileMustache(audience, cdsCard.getLinks());
    }

    public String getSummary() {
        return summary;
    }

    public String getIndicator() {
        return indicator;
    }

    public String getDetail() {
        return detail;
    }

    public Source getSource() {
        return source;
    }

    public String getRationale() {
        return rationale;
    }

    public String getSource2() {
        return source2;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public String getSelectionBehavior() {
        return selectionBehavior;
    }

    public String getLinks() {
        return links;
    }
}
