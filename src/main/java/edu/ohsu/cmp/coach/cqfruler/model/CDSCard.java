package edu.ohsu.cmp.coach.cqfruler.model;

public class CDSCard {
    private static final String DELIM_REGEX = "\\s*\\|\\s*";

    private static final int RATIONALE = 0;
    private static final int SUGGESTIONS = 1;
    private static final int SELECTION_BEHAVIOR = 2;
    private static final int LINKS = 3;

    private String summary;
    private String indicator;
    private String detail;
    private Source source;

    private String[] detailArr;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getIndicator() {
        return indicator;
    }

    public void setIndicator(String indicator) {
        this.indicator = indicator;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getRationale() {
        return getDetailItem(RATIONALE);
    }

    public String getSuggestions() {
        return getDetailItem(SUGGESTIONS);
    }

    public String getSelectionBehavior() {
        return getDetailItem(SELECTION_BEHAVIOR);
    }

    public String getLinks() {
        return getDetailItem(LINKS);
    }

    private String getDetailItem(int index) {
        if (detailArr == null && detail != null) {
            detailArr = detail.split(DELIM_REGEX);
        }

        return detailArr != null && index < detailArr.length ?
                detailArr[index] :
                null;
    }
}
