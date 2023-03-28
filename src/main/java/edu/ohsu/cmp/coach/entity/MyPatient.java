package edu.ohsu.cmp.coach.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "patient")
public class MyPatient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patIdHash;
    private Date omronLastUpdated;
    private String studyClass;
    private String redcapId;
    private Boolean consentGranted;

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

    public String getStudyClass() {
        return studyClass;
    }

    public void setStudyClass(String studyClass) {
        this.studyClass = studyClass;
    }

    public String getRedcapId() {
        return redcapId;
    }

    public void setRedcapId(String redcapId) {
        this.redcapId = redcapId;
    }

    public Boolean getConsentGranted() {
        return consentGranted;
    }

    public void setConsentGranted(Boolean consentGranted) {
        this.consentGranted = consentGranted;
    }
}
