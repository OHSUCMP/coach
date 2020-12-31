package edu.ohsu.cmp.htnu18app.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "home_bp_reading")
public class HomeBloodPressureReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pat_id")
    private Long patId;

    private int systolic;
    private int diastolic;

    @Column(name = "reading_date")
    private Date readingDate;

    @Column(name = "created_date")
    private Date createdDate;

    protected HomeBloodPressureReading() {
    }

    public HomeBloodPressureReading(Long patId, int systolic, int diastolic, Date readingDate) {
        this.patId = patId;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.readingDate = readingDate;
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

    public int getSystolic() {
        return systolic;
    }

    public void setSystolic(int systolic) {
        this.systolic = systolic;
    }

    public int getDiastolic() {
        return diastolic;
    }

    public void setDiastolic(int diastolic) {
        this.diastolic = diastolic;
    }

    public Date getReadingDate() {
        return readingDate;
    }

    public void setReadingDate(Date readingTimestamp) {
        this.readingDate = readingTimestamp;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdTimestamp) {
        this.createdDate = createdTimestamp;
    }
}
