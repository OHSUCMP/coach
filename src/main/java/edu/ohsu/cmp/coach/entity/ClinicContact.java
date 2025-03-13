package edu.ohsu.cmp.coach.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "clinic_contact")
public class ClinicContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String primaryPhone;
    private String afterHoursPhone;
    private Boolean active;

    protected ClinicContact() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(String primaryPhone) {
        this.primaryPhone = primaryPhone;
    }

    public String getAfterHoursPhone() {
        return afterHoursPhone;
    }

    public void setAfterHoursPhone(String afterHoursPhone) {
        this.afterHoursPhone = afterHoursPhone;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
