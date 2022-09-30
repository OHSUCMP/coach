package edu.ohsu.cmp.coach.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "vsac_valueset")
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
    @CreationTimestamp
    private Date created;
    @UpdateTimestamp
    private Date updated;

    // see: https://attacomsian.com/blog/spring-data-jpa-many-to-many-mapping
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "vsac_valueset_concept",
            joinColumns = {
                    @JoinColumn(name = "valueSetId", referencedColumnName = "id",
                            nullable = false, updatable = false)},
            inverseJoinColumns = {
                    @JoinColumn(name = "conceptId", referencedColumnName = "id",
                            nullable = false, updatable = false)})
    private Set<Concept> concepts;

    public ValueSet() {
    }

    public ValueSet(String oid, String displayName, String version, String source, String purpose, String type,
                    String binding, String status, Date revisionDate) {
        this.oid = oid;
        this.displayName = displayName;
        this.version = version;
        this.source = source;
        this.purpose = purpose;
        this.type = type;
        this.binding = binding;
        this.status = status;
        this.revisionDate = revisionDate;
    }

    public void update(ValueSet vs) {
        setDisplayName(vs.getDisplayName());
        setVersion(vs.getVersion());
        setSource(vs.getSource());
        setPurpose(vs.getPurpose());
        setType(vs.getType());
        setBinding(vs.getBinding());
        setStatus(vs.getStatus());
        setRevisionDate(vs.getRevisionDate());
        setConcepts(vs.getConcepts());
    }

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
        if (this.concepts == null || this.concepts.isEmpty()) {
            this.concepts = concepts;

        } else {
            // we need to do a little work to update concepts that persist; to delete those no longer present; and to
            // add those that are new.

            Map<String, Concept> updatedMap = new LinkedHashMap<>();
            for (Concept c : concepts) {
                updatedMap.put(c.getKey(), c);
            }

            Iterator<Concept> iter = this.concepts.iterator();
            while (iter.hasNext()) {
                Concept current = iter.next();
                if (updatedMap.containsKey(current.getKey())) {
                    current.update(updatedMap.remove(current.getKey()));
                } else {
                    iter.remove();
                }
            }
            this.concepts.addAll(updatedMap.values());
        }
    }
}
