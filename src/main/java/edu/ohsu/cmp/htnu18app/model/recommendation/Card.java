package edu.ohsu.cmp.htnu18app.model.recommendation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSCard;
import edu.ohsu.cmp.htnu18app.cqfruler.model.Source;

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
    private List<Link> links;

    public Card(CDSCard cdsCard) throws IOException {
        this.summary = cdsCard.getSummary();
        this.indicator = cdsCard.getIndicator();
        this.detail = cdsCard.getDetail();
        this.source = cdsCard.getSource();

        this.rationale = cdsCard.getRationale();
        this.source2 = cdsCard.getSource2();

        this.selectionBehavior = cdsCard.getSelectionBehavior();

        Gson gson = new GsonBuilder().create();
        this.suggestions = gson.fromJson(cdsCard.getSuggestions(), new TypeToken<ArrayList<Suggestion>>(){}.getType());
        this.links = gson.fromJson(cdsCard.getLinks(), new TypeToken<ArrayList<Link>>(){}.getType());
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

    public List<Link> getLinks() {
        return links;
    }
}
