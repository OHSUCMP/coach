package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;

@Entity
@Table(schema = "htnu18app", name = "adverse_event_outcome")
public class MyAdverseEventOutcome {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private String adverseEventId;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    protected MyAdverseEventOutcome() {
    }

    public MyAdverseEventOutcome(Long patId, String adverseEventId, Outcome outcome) {
        this.patId = patId;
        this.adverseEventId = adverseEventId;
        this.outcome = outcome;
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

    public String getAdverseEventId() {
        return adverseEventId;
    }

    public void setAdverseEventId(String adverseEventId) {
        this.adverseEventId = adverseEventId;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }
}
