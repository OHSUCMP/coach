package edu.ohsu.cmp.coach.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "vsac_concept")
public class Concept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String codeSystem;
    private String codeSystemName;
    private String codeSystemVersion;
    private String displayName;
    @CreationTimestamp
    private Date created;
    @UpdateTimestamp
    private Date updated;

    // see: https://attacomsian.com/blog/spring-data-jpa-many-to-many-mapping
    @ManyToMany(mappedBy = "concepts", fetch = FetchType.LAZY)
    private Set<ValueSet> valueSets;

    public Concept() {
    }

    public Concept(String code, String codeSystem, String codeSystemName, String codeSystemVersion, String displayName) {
        this.code = code;
        this.codeSystem = codeSystem;
        this.codeSystemName = codeSystemName;
        this.codeSystemVersion = codeSystemVersion;
        this.displayName = displayName;
    }

    public void update(Concept c) {
        setCode(c.getCode());
        setCodeSystem(c.getCodeSystem());
        setCodeSystemName(c.getCodeSystemName());
        setCodeSystemVersion(c.getCodeSystemVersion());
        setDisplayName(c.getDisplayName());
    }

    @Override
    public String toString() {
        return "Concept{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", codeSystem='" + codeSystem + '\'' +
                ", codeSystemName='" + codeSystemName + '\'' +
                ", codeSystemVersion='" + codeSystemVersion + '\'' +
                ", displayName='" + displayName + '\'' +
                ", created=" + created +
                ", updated=" + updated +
//                ", valueSets=" + valueSets +
                '}';
    }

    @JsonIgnore
    public String getKey() {
        return code + "|" + codeSystem + "|" + codeSystemVersion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    public String getCodeSystemName() {
        return codeSystemName;
    }

    public void setCodeSystemName(String codeSystemName) {
        this.codeSystemName = codeSystemName;
    }

    public String getCodeSystemVersion() {
        return codeSystemVersion;
    }

    public void setCodeSystemVersion(String codeSystemVersion) {
        this.codeSystemVersion = codeSystemVersion;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public Set<ValueSet> getValueSets() {
        return valueSets;
    }

    public void setValueSets(Set<ValueSet> valueSets) {
        this.valueSets = valueSets;
    }
}
