package edu.ohsu.cmp.coach.entity;

import edu.ohsu.cmp.coach.model.omron.OmronBloodPressureModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "omron_vitals_cache")
public class MyOmronVitals {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private Long omronId;
    private String dateTime;
    private String dateTimeLocal;
    private String dateTimeUtcOffset;
    private Integer systolic;
    private Integer diastolic;
    private String bloodPressureUnits;
    private Integer pulse;
    private String pulseUnits;
    private String deviceType;
    private Date createdDate;

    public MyOmronVitals() {
    }

    public MyOmronVitals(OmronBloodPressureModel model) {
        omronId = model.getId();
        dateTime = model.getDateTime();
        dateTimeLocal = model.getDateTimeLocal();
        dateTimeUtcOffset = model.getDateTimeUtcOffset();
        systolic = model.getSystolic();
        diastolic = model.getDiastolic();
        bloodPressureUnits = model.getBloodPressureUnits();
        pulse = model.getPulse();
        pulseUnits = model.getPulseUnits();
        deviceType = model.getDeviceType();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getPatId() {
        return patId;
    }

    public void setPatId(Long patId) {
        this.patId = patId;
    }

    public Long getOmronId() {
        return omronId;
    }

    public void setOmronId(Long omronId) {
        this.omronId = omronId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getDateTimeLocal() {
        return dateTimeLocal;
    }

    public void setDateTimeLocal(String dateTimeLocal) {
        this.dateTimeLocal = dateTimeLocal;
    }

    public String getDateTimeUtcOffset() {
        return dateTimeUtcOffset;
    }

    public void setDateTimeUtcOffset(String dateTimeUtcOffset) {
        this.dateTimeUtcOffset = dateTimeUtcOffset;
    }

    public Integer getSystolic() {
        return systolic;
    }

    public void setSystolic(Integer systolic) {
        this.systolic = systolic;
    }

    public Integer getDiastolic() {
        return diastolic;
    }

    public void setDiastolic(Integer diastolic) {
        this.diastolic = diastolic;
    }

    public String getBloodPressureUnits() {
        return bloodPressureUnits;
    }

    public void setBloodPressureUnits(String bloodPressureUnits) {
        this.bloodPressureUnits = bloodPressureUnits;
    }

    public Integer getPulse() {
        return pulse;
    }

    public void setPulse(Integer pulse) {
        this.pulse = pulse;
    }

    public String getPulseUnits() {
        return pulseUnits;
    }

    public void setPulseUnits(String pulseUnits) {
        this.pulseUnits = pulseUnits;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
