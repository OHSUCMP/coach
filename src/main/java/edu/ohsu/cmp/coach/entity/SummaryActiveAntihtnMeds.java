package edu.ohsu.cmp.coach.entity;

import edu.ohsu.cmp.coach.model.MedicationModel;
import jakarta.persistence.*;

@Entity
@Table(name = "summary_active_antihtn_meds")
public class SummaryActiveAntihtnMeds {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summaryId")
    private Summary summary;

    private String description;
    private String conceptSystem;
    private String conceptCode;

    protected SummaryActiveAntihtnMeds() {
    }

    public SummaryActiveAntihtnMeds(MedicationModel medicationModel, Summary summary) {
        if (medicationModel == null) {
            throw new IllegalArgumentException("MedicationModel cannot be null");
        }

        this.description = medicationModel.getDescription();
        this.conceptSystem = medicationModel.getSystem();
        this.conceptCode = medicationModel.getCode();
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
