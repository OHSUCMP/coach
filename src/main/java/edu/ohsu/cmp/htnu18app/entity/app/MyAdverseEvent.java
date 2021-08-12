package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;

@Entity
@Table(schema = "htnu18app", name = "adverse_event")
public class MyAdverseEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String conceptOfInterest;
    private String code;
    private String system;

    protected MyAdverseEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConceptOfInterest() {
        return conceptOfInterest;
    }

    public void setConceptOfInterest(String conceptOfInterest) {
        this.conceptOfInterest = conceptOfInterest;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }
}
