package edu.ohsu.cmp.htnu18app.cqfruler.model;

public class Card {
    private String indicator;
    private String title;
    private String detail;
    private String sourceLabel;
    private String sourceUrl;

    public Card(String indicator, String title, String detail, String sourceLabel, String sourceUrl) {
        this.indicator = indicator;
        this.title = title;
        this.detail = detail;
        this.sourceLabel = sourceLabel;
        this.sourceUrl = sourceUrl;
    }

    public String getIndicator() {
        return indicator;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }
}
