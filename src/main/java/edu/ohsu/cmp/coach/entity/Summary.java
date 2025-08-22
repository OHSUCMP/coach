package edu.ohsu.cmp.coach.entity;

import jakarta.persistence.*;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "summary")
public class Summary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private String bpGoal;
    private String calculatedBP;
    private Boolean bpAtOrBelowGoal;
    private String notes;
    private Date createdDate;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "summary", cascade = CascadeType.ALL)
    private Set<SummaryRecommendation> recommendations;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "summary", cascade = CascadeType.ALL)
    private Set<SummaryOngoingAdverseEvent> ongoingAdverseEvents;

    protected Summary() {
    }

    public Summary(String bpGoal, String calculatedBP, Boolean bpAtOrBelowGoal, String notes) {
        this.bpGoal = bpGoal;
        this.calculatedBP = calculatedBP;
        this.bpAtOrBelowGoal = bpAtOrBelowGoal;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatId() {
        return patId;
    }

    public void setPatId(Long patId) {
        this.patId = patId;
    }

    public String getBpGoal() {
        return bpGoal;
    }

    public void setBpGoal(String bpGoal) {
        this.bpGoal = bpGoal;
    }

    public String getCalculatedBP() {
        return calculatedBP;
    }

    public void setCalculatedBP(String calculatedBP) {
        this.calculatedBP = calculatedBP;
    }

    public Boolean getBpAtOrBelowGoal() {
        return bpAtOrBelowGoal;
    }

    public void setBpAtOrBelowGoal(Boolean bpAtOrBelowGoal) {
        this.bpAtOrBelowGoal = bpAtOrBelowGoal;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Set<SummaryRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(Set<SummaryRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public Set<SummaryOngoingAdverseEvent> getOngoingAdverseEvents() {
        return ongoingAdverseEvents;
    }

    public void setOngoingAdverseEvents(Set<SummaryOngoingAdverseEvent> ongoingAdverseEvents) {
        this.ongoingAdverseEvents = ongoingAdverseEvents;
    }
}
