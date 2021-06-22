package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(schema = "htnu18app")
public class Counseling {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private String extCounselingId;
    private String category;
    private String counselingText;
    private Date createdDate;

    protected Counseling() {
    }

    public Counseling(String extCounselingId, String category, String counselingText) {
        this.extCounselingId = extCounselingId;
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
