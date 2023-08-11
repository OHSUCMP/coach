package edu.ohsu.cmp.coach.model.recommendation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.model.cqfruler.CDSCard;
import edu.ohsu.cmp.coach.model.cqfruler.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Card {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String summary;
    private String indicator;
    private String detail;
    private Source source;
    private String rationale;
    private List<Suggestion> suggestions;
    private String selectionBehavior;
    private List<Link> links;

    private String errorMessage;

    private boolean prefetchModified;

    public Card(CDSCard cdsCard, boolean prefetchModified) {
        this.summary = cdsCard.getSummary();
        this.indicator = cdsCard.getIndicator();

        // Reset the indicator for the "success" summary. CQF Ruler will not allow any indicators except what is in the CDS Cards specification
        if ("SUCCESS".equals(this.summary)) {
            this.summary = "";
            this.indicator = "success";
        }

        this.detail = cdsCard.getDetail();
        this.source = cdsCard.getSource();

        this.rationale = cdsCard.getRationale();

        this.selectionBehavior = cdsCard.getSelectionBehavior();

        this.prefetchModified = prefetchModified;

        Gson gson = new GsonBuilder().create();
        try {
            this.suggestions = gson.fromJson(cdsCard.getSuggestions(), new TypeToken<ArrayList<Suggestion>>(){}.getType());

        } catch (JsonSyntaxException e) {
            logger.error("caught " + e.getClass().getName() + " processing suggestions - " + e.getMessage(), e);
            logger.error("JSON=" + cdsCard.getSuggestions());
            throw e;
        }

        try {
            this.links = gson.fromJson(cdsCard.getLinks(), new TypeToken<ArrayList<Link>>(){}.getType());

        } catch (JsonSyntaxException e) {
            logger.error("caught " + e.getClass().getName() + " processing links - " + e.getMessage(), e);
            logger.error("JSON=" + cdsCard.getLinks());
            throw e;
        }
    }

    public Card(String errorMessage, boolean prefetchModified) {
        this.indicator = "critical";
        this.errorMessage = errorMessage;
        this.prefetchModified = prefetchModified;
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

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public String getSelectionBehavior() {
        return selectionBehavior;
    }

    public List<Link> getLinks() {
        return links;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isPrefetchModified() {
        return prefetchModified;
    }
}
