package edu.ohsu.cmp.coach.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "patient")
public class MyPatient {
    public static final String CONSENT_GRANTED_YES = "Y";
    public static final String CONSENT_GRANTED_NO = "N";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patIdHash;
    private Date omronLastUpdated;
    private String redcapId;
    private Boolean bpGoalUpdated;

    private Boolean confirmedEndOfStudy;

    protected MyPatient() {
    }

    public MyPatient(String patIdHash) {
        this.patIdHash = patIdHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatIdHash() {
        return patIdHash;
    }

    public void setPatIdHash(String patIdHash) {
        this.patIdHash = patIdHash;
    }

    public Date getOmronLastUpdated() {
        return omronLastUpdated;
    }

    public void setOmronLastUpdated(Date omronLastUpdated) {
        this.omronLastUpdated = omronLastUpdated;
    }

    public String getRedcapId() {
        return redcapId;
    }

    public void setRedcapId(String redcapId) {
        this.redcapId = redcapId;
    }

    public Boolean getBpGoalUpdated() {
        return bpGoalUpdated;
    }

    public void setBpGoalUpdated(Boolean bpGoalUpdated) {
        this.bpGoalUpdated = bpGoalUpdated;
    }

    public Boolean getConfirmedEndOfStudy() {
        return confirmedEndOfStudy;
    }

    public void setConfirmedEndOfStudy(Boolean confirmedEndOfStudy) {
        this.confirmedEndOfStudy = confirmedEndOfStudy;
    }
}
