package edu.ohsu.cmp.coach.entity;

import javax.persistence.*;
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
}
