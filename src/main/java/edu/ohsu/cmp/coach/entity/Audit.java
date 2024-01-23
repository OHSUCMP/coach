package edu.ohsu.cmp.coach.entity;

import edu.ohsu.cmp.coach.model.AuditLevel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "audit")
public class Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;

    @Enumerated(EnumType.STRING)
    private AuditLevel level;

    private String action;

    private String details;

    private Date created;

    protected Audit() {
    }

    public Audit(Long patId, AuditLevel level, String action) {
        this(patId, level, action, null);
    }

    public Audit(Long patId, AuditLevel level, String action, String details) {
        this.patId = patId;
        this.level = level;
        this.action = action;
        this.details = details;
        this.created = new Date();
    }

    @Override
    public String toString() {
        return "Audit{" +
                "id=" + id +
                ", patId=" + patId +
                ", level=" + level +
                ", action='" + action + '\'' +
                ", details='" + details + '\'' +
                ", created=" + created +
                '}';
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

    public AuditLevel getLevel() {
        return level;
    }

    public void setLevel(AuditLevel level) {
        this.level = level;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
