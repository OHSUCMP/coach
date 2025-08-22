package edu.ohsu.cmp.coach.entity;

import edu.ohsu.cmp.coach.model.AdverseEventModel;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "summary_ongoing_adverse_event")
public class SummaryOngoingAdverseEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summaryId")
    private Summary summary;

    private String description;
    private String conceptSystem;
    private String conceptCode;

    protected SummaryOngoingAdverseEvent() {
    }

    public SummaryOngoingAdverseEvent(AdverseEventModel adverseEventModel, Summary summary) {
        if (adverseEventModel == null) {
            throw new IllegalArgumentException("adverseEventModel cannot be null");
        }

        if ( ! StringUtils.equals(adverseEventModel.getOutcome(), Outcome.ONGOING.getFhirValue()) ) {
            throw new IllegalArgumentException("adverseEventModel must represent an ongoing adverse event");
        }

        this.description = adverseEventModel.getDescription();
        this.conceptSystem = adverseEventModel.getSystem();
        this.conceptCode = adverseEventModel.getCode();
        this.summary = summary;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConceptSystem() {
        return conceptSystem;
    }

    public void setConceptSystem(String conceptSystem) {
        this.conceptSystem = conceptSystem;
    }

    public String getConceptCode() {
        return conceptCode;
    }

    public void setConceptCode(String conceptCode) {
        this.conceptCode = conceptCode;
    }
}
