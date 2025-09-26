package edu.ohsu.cmp.coach.entity;

import edu.ohsu.cmp.coach.model.RecommendationSeverity;
import jakarta.persistence.*;

@Entity
@Table(name = "summary_recommendation")
public class SummaryRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summaryId")
    private Summary summary;

    private String recommendation;

    @Enumerated(EnumType.STRING)
    private RecommendationSeverity severity;

    private String card;

    protected SummaryRecommendation() {
    }

    public SummaryRecommendation(String recommendation, RecommendationSeverity severity, String card, Summary summary) {
        this.summary = summary;
        this.recommendation = recommendation;
        this.severity = severity;
        this.card = card;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public RecommendationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(RecommendationSeverity severity) {
        this.severity = severity;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }
}
