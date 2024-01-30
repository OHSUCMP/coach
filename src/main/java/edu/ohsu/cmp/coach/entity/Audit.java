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

    private String event;

    private String details;

    private Date created;

    protected Audit() {
    }

    public Audit(Long patId, AuditLevel level, String event) {
        this(patId, level, event, null);
    }

    public Audit(Long patId, AuditLevel level, String event, String details) {
        this.patId = patId;
        this.level = level;
        this.event = event;
        this.details = details;
        this.created = new Date();
    }

    @Override
    public String toString() {
        return "Audit{" +
                "id=" + id +
                ", patId=" + patId +
                ", level=" + level +
                ", event='" + event + '\'' +
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

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
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
