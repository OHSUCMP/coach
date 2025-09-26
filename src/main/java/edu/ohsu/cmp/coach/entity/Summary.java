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
    private String calculatedAverageBP;
    private Boolean bpAtOrBelowGoal;
    private String mostRecentBP;
    private Date mostRecentBPDate;
    private Boolean mostRecentBPInCrisis;
    private Boolean mostRecentBPInLowCrisis;
    private String secondMostRecentBP;
    private Date secondMostRecentBPDate;
    private Boolean twoMostRecentBPsInCrisis;
    private Boolean twoMostRecentBPsInLowCrisis;
    private String notes;
    private Date createdDate;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "summary", cascade = CascadeType.ALL)
    private Set<SummaryRecommendation> recommendations;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "summary", cascade = CascadeType.ALL)
    private Set<SummaryOngoingAdverseEvent> ongoingAdverseEvents;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "summary", cascade = CascadeType.ALL)
    private Set<SummaryActiveAntihtnMeds> activeAntihtnMeds;

    protected Summary() {
    }

    public Summary(String bpGoal, String calculatedAverageBP, Boolean bpAtOrBelowGoal,
                   String mostRecentBP, Date mostRecentBPDate, Boolean mostRecentBPInCrisis, Boolean mostRecentBPInLowCrisis,
                   String secondMostRecentBP, Date secondMostRecentBPDate, Boolean twoMostRecentBPsInCrisis, Boolean twoMostRecentBPsInLowCrisis,
                   String notes) {
        this.bpGoal = bpGoal;
        this.calculatedAverageBP = calculatedAverageBP;
        this.bpAtOrBelowGoal = bpAtOrBelowGoal;
        this.mostRecentBP = mostRecentBP;
        this.mostRecentBPDate = mostRecentBPDate;
        this.mostRecentBPInCrisis = mostRecentBPInCrisis;
        this.mostRecentBPInLowCrisis = mostRecentBPInLowCrisis;
        this.secondMostRecentBP = secondMostRecentBP;
        this.secondMostRecentBPDate = secondMostRecentBPDate;
        this.twoMostRecentBPsInCrisis = twoMostRecentBPsInCrisis;
        this.twoMostRecentBPsInLowCrisis = twoMostRecentBPsInLowCrisis;
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

    public String getCalculatedAverageBP() {
        return calculatedAverageBP;
    }

    public void setCalculatedAverageBP(String calculatedBP) {
        this.calculatedAverageBP = calculatedBP;
    }

    public Boolean getBpAtOrBelowGoal() {
        return bpAtOrBelowGoal;
    }

    public void setBpAtOrBelowGoal(Boolean bpAtOrBelowGoal) {
        this.bpAtOrBelowGoal = bpAtOrBelowGoal;
    }

    public String getMostRecentBP() {
        return mostRecentBP;
    }

    public void setMostRecentBP(String mostRecentBP) {
        this.mostRecentBP = mostRecentBP;
    }

    public Date getMostRecentBPDate() {
        return mostRecentBPDate;
    }

    public void setMostRecentBPDate(Date mostRecentBPDate) {
        this.mostRecentBPDate = mostRecentBPDate;
    }

    public Boolean getMostRecentBPInCrisis() {
        return mostRecentBPInCrisis;
    }

    public void setMostRecentBPInCrisis(Boolean mostRecentBPInCrisis) {
        this.mostRecentBPInCrisis = mostRecentBPInCrisis;
    }

    public Boolean getMostRecentBPInLowCrisis() {
        return mostRecentBPInLowCrisis;
    }

    public void setMostRecentBPInLowCrisis(Boolean mostRecentBPInLowCrisis) {
        this.mostRecentBPInLowCrisis = mostRecentBPInLowCrisis;
    }

    public String getSecondMostRecentBP() {
        return secondMostRecentBP;
    }

    public void setSecondMostRecentBP(String secondMostRecentBP) {
        this.secondMostRecentBP = secondMostRecentBP;
    }

    public Date getSecondMostRecentBPDate() {
        return secondMostRecentBPDate;
    }

    public void setSecondMostRecentBPDate(Date secondMostRecentBPDate) {
        this.secondMostRecentBPDate = secondMostRecentBPDate;
    }

    public Boolean getTwoMostRecentBPsInCrisis() {
        return twoMostRecentBPsInCrisis;
    }

    public void setTwoMostRecentBPsInCrisis(Boolean twoMostRecentBPsInCrisis) {
        this.twoMostRecentBPsInCrisis = twoMostRecentBPsInCrisis;
    }

    public Boolean getTwoMostRecentBPsInLowCrisis() {
        return twoMostRecentBPsInLowCrisis;
    }

    public void setTwoMostRecentBPsInLowCrisis(Boolean twoMostRecentBPsInLowCrisis) {
        this.twoMostRecentBPsInLowCrisis = twoMostRecentBPsInLowCrisis;
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

    public Set<SummaryActiveAntihtnMeds> getActiveAntihtnMeds() {
        return activeAntihtnMeds;
    }

    public void setActiveAntihtnMeds(Set<SummaryActiveAntihtnMeds> activeAntihtnMeds) {
        this.activeAntihtnMeds = activeAntihtnMeds;
    }
}
