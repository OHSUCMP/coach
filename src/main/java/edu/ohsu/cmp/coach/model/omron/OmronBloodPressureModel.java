package edu.ohsu.cmp.coach.model.omron;

public class OmronBloodPressureModel {
    private Long id;
    private String dateTime;
    private String dateTimeLocal;
    private String dateTimeUtcOffset;
    private Integer systolic;
    private Integer diastolic;
    private String bloodPressureUnits;
    private Integer pulse;
    private String pulseUnits;
    private String deviceType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
