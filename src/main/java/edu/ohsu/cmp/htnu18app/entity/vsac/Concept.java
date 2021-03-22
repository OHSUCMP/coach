package edu.ohsu.cmp.htnu18app.entity.vsac;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(schema = "vsac")
public class Concept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String codeSystem;
    private String codeSystemName;
    private String codeSystemVersion;
    private String displayName;
    private Date created;
    private Date updated;

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
}
