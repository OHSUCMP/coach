package edu.ohsu.cmp.coach.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "counseling")
public class Counseling {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private String extCounselingId;
    private String referenceSystem;
    private String referenceCode;
    private String counselingText;
    private Date createdDate;

    protected Counseling() {
    }

    public Counseling(String extCounselingId, String referenceSystem, String referenceCode, String counselingText) {
        this.extCounselingId = extCounselingId;
        this.referenceSystem = referenceSystem;
        this.referenceCode = referenceCode;
        this.counselingText = counselingText;
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

    public String getExtCounselingId() {
        return extCounselingId;
    }

    public void setExtCounselingId(String extCounselingId) {
        this.extCounselingId = extCounselingId;
    }

    public String getReferenceSystem() {
        return referenceSystem;
    }

    public void setReferenceSystem(String referenceSystem) {
        this.referenceSystem = referenceSystem;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getCounselingText() {
        return counselingText;
    }

    public void setCounselingText(String counselingText) {
        this.counselingText = counselingText;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
