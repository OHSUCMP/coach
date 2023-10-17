package edu.ohsu.cmp.coach.entity;

import edu.ohsu.cmp.coach.model.BloodPressureModel;

import javax.persistence.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Entity
@Table(name = "hypotension_adverse_event")
public class HypotensionAdverseEvent {
    public static final String SYSTEM = "http://snomed.info/sct";
    public static final String CODE = "271870002";
    public static final String DISPLAY = "Low blood pressure reading (disorder)";

    private static final String KEY_DELIM = "|";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private Integer bp1Systolic;
    private Integer bp1Diastolic;
    private Date bp1ReadingDate;
    private Integer bp2Systolic;
    private Integer bp2Diastolic;
    private Date bp2ReadingDate;
    private Date createdDate;

    protected HypotensionAdverseEvent() {
    }

    public HypotensionAdverseEvent(BloodPressureModel bpm1, BloodPressureModel bpm2) {
        bp1Systolic = bpm1.getSystolic().getValue();
        bp1Diastolic = bpm1.getDiastolic().getValue();
        bp1ReadingDate = bpm1.getReadingDate();
        bp2Systolic = bpm2.getSystolic().getValue();
        bp2Diastolic = bpm2.getDiastolic().getValue();
        bp2ReadingDate = bpm2.getReadingDate();
    }

    public String getDescription() {
        return "Hypotension adverse event - BP1 = " + bp1Systolic + "/" + bp1Diastolic + " at " + toDateString(bp1ReadingDate) +
                ", BP2 = " + bp2Systolic + "/" + bp2Diastolic + " at " + toDateString(bp2ReadingDate);
    }

    public String getLogicalEqualityKey() {
            return "HAE" + KEY_DELIM +
                bp1Systolic + KEY_DELIM +
                bp1Diastolic + KEY_DELIM +
                toDateString(bp1ReadingDate) + KEY_DELIM +
                bp2Systolic + KEY_DELIM +
                bp2Diastolic + KEY_DELIM +
                toDateString(bp2ReadingDate);
    }

    private String toDateString(Date d) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                ZonedDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC)
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatId() {
        return patId;
    }

    public void setPatId(Long patId) {
        this.patId = patId;
    }

    public Integer getBp1Systolic() {
        return bp1Systolic;
    }

    public void setBp1Systolic(Integer bp1Systolic) {
        this.bp1Systolic = bp1Systolic;
    }

    public Integer getBp1Diastolic() {
        return bp1Diastolic;
    }

    public void setBp1Diastolic(Integer bp1Diastolic) {
        this.bp1Diastolic = bp1Diastolic;
    }

    public Date getBp1ReadingDate() {
        return bp1ReadingDate;
    }

    public void setBp1ReadingDate(Date bp1ReadingDate) {
        this.bp1ReadingDate = bp1ReadingDate;
    }

    public Integer getBp2Systolic() {
        return bp2Systolic;
    }

    public void setBp2Systolic(Integer bp2Systolic) {
        this.bp2Systolic = bp2Systolic;
    }

    public Integer getBp2Diastolic() {
        return bp2Diastolic;
    }

    public void setBp2Diastolic(Integer bp2Diastolic) {
        this.bp2Diastolic = bp2Diastolic;
    }

    public Date getBp2ReadingDate() {
        return bp2ReadingDate;
    }

    public void setBp2ReadingDate(Date bp2ReadingDate) {
        this.bp2ReadingDate = bp2ReadingDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
