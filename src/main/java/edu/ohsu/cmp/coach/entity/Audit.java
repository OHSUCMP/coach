package edu.ohsu.cmp.coach.entity;

import edu.ohsu.cmp.coach.model.AuditSeverity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "audit_data")
public class Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;

    @Enumerated(EnumType.STRING)
    private AuditSeverity severity;

    private String event;

    private String details;

    private Date created;

    protected Audit() {
    }

    public Audit(Long patId, AuditSeverity severity, String event) {
        this(patId, severity, event, null);
    }

    public Audit(Long patId, AuditSeverity severity, String event, String details) {
        this.patId = patId;
        this.severity = severity;
        this.event = event;
        this.details = details;
        this.created = new Date();
    }

    @Override
    public String toString() {
        return "Audit{" +
                "id=" + id +
                ", patId=" + patId +
                ", severity=" + severity +
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

    public AuditSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AuditSeverity severity) {
        this.severity = severity;
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
