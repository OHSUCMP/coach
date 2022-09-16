package edu.ohsu.cmp.coach.entity.app;

import edu.ohsu.cmp.coach.entity.app.Concept;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(schema = "vsac", name = "vsac_valueSet")
public class ValueSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String oid;
    private String displayName;
    private String version;
    private String source;
    private String purpose;
    private String type;
    private String binding;
    private String status;
    private Date revisionDate;
    private Date created;
    private Date updated;

    // see: https://attacomsian.com/blog/spring-data-jpa-many-to-many-mapping
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(name = "valuesetconcept",
            joinColumns = {
                    @JoinColumn(name = "valueSetId", referencedColumnName = "id",
                            nullable = false, updatable = false)},
            inverseJoinColumns = {
                    @JoinColumn(name = "conceptId", referencedColumnName = "id",
                            nullable = false, updatable = false)})
    private Set<Concept> concepts;

    @Override
    public String toString() {
        return "ValueSet{" +
                "id=" + id +
                ", oid='" + oid + '\'' +
                ", displayName='" + displayName + '\'' +
                ", version='" + version + '\'' +
                ", source='" + source + '\'' +
                ", purpose='" + purpose + '\'' +
                ", type='" + type + '\'' +
                ", binding='" + binding + '\'' +
                ", status='" + status + '\'' +
                ", revisionDate=" + revisionDate +
                ", created=" + created +
                ", updated=" + updated +
                ", concepts=" + concepts +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBinding() {
        return binding;
    }

    public void setBinding(String binding) {
        this.binding = binding;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getRevisionDate() {
        return revisionDate;
    }

    public void setRevisionDate(Date revisionDate) {
        this.revisionDate = revisionDate;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Set<Concept> getConcepts() {
        return concepts;
    }

    public void setConcepts(Set<Concept> concepts) {
        this.concepts = concepts;
    }
}
