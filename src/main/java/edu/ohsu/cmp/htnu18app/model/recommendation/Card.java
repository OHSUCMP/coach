package edu.ohsu.cmp.htnu18app.model.recommendation;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSCard;
import edu.ohsu.cmp.htnu18app.cqfruler.model.Source;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class Card {
    private Audience audience;

    private String summary;
    private String indicator;
    private String detail;
    private Source source;
    private String rationale;
    private String source2;
    private String suggestions;
    private String selectionBehavior;
    private String links;

    public Card(Audience audience, CDSCard cdsCard) throws IOException {
        this.audience = audience;

        this.summary = cdsCard.getSummary();
        this.indicator = cdsCard.getIndicator();
        this.detail = cdsCard.getDetail();
        this.source = cdsCard.getSource();

        this.rationale = compileMustache(audience, cdsCard.getRationale());
        this.source2 = compileMustache(audience, cdsCard.getSource2());
        this.suggestions = compileMustache(audience, cdsCard.getSuggestions());
        this.selectionBehavior = compileMustache(audience, cdsCard.getSelectionBehavior());
        this.links = compileMustache(audience, cdsCard.getLinks());
    }

    public Audience getAudience() {
        return audience;
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

    public String getSuggestions() {
        return suggestions;
    }

    public String getSelectionBehavior() {
        return selectionBehavior;
    }

    public String getLinks() {
        return links;
    }

/////////////////////////////////////////////////////
// private methods
//

    private String compileMustache(Audience audience, String s) throws IOException {
        if (s == null) return null;
        if (s.trim().isEmpty()) return "";

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(audience.getTag(), true);

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache m = mf.compile(new StringReader(s), "template" );
        StringWriter writer = new StringWriter();

        m.execute(writer, map).flush();

        return writer.toString();
    }
}
