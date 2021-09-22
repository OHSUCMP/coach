package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(schema = "htnu18app", name = "adverse_event_outcome")
public class MyAdverseEventOutcome {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String adverseEventIdHash;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    private Date createdDate;
    private Date modifiedDate;

    protected MyAdverseEventOutcome() {
    }

    public MyAdverseEventOutcome(String adverseEventIdHash, Outcome outcome) {
        this.adverseEventIdHash = adverseEventIdHash;
        this.outcome = outcome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdverseEventIdHash() {
        return adverseEventIdHash;
    }

    public void setAdverseEventIdHash(String adverseEventIdHash) {
        this.adverseEventIdHash = adverseEventIdHash;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
}
