package edu.ohsu.cmp.htnu18app.entity;

import javax.persistence.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Table(name = "home_bp_reading")
public class HomeBloodPressureReading {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pat_id")
    private Long patId;

    private Integer systolic1;
    private Integer diastolic1;
    private Integer pulse1;

    private Integer systolic2;
    private Integer diastolic2;
    private Integer pulse2;

    @Column(name = "reading_date")
    private Date readingDate;

    @Column(name = "followed_instructions")
    private Boolean followedInstructions;

    @Column(name = "created_date")
    private Date createdDate;

    protected HomeBloodPressureReading() {
    }

    public HomeBloodPressureReading(Integer systolic1, Integer diastolic1, Integer pulse1,
                                    Integer systolic2, Integer diastolic2, Integer pulse2,
                                    Date readingDate, Boolean followedInstructions) {
        this.systolic1 = systolic1;
        this.diastolic1 = diastolic1;
        this.pulse1 = pulse1;
        this.systolic2 = systolic2;
        this.diastolic2 = diastolic2;
        this.pulse2 = pulse2;
        this.readingDate = readingDate;
        this.followedInstructions = followedInstructions;
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

    public Integer getSystolic1() {
        return systolic1;
    }

    public void setSystolic1(Integer systolic1) {
        this.systolic1 = systolic1;
    }

    public Integer getDiastolic1() {
        return diastolic1;
    }

    public void setDiastolic1(Integer diastolic1) {
        this.diastolic1 = diastolic1;
    }

    public Integer getPulse1() {
        return pulse1;
    }

    public void setPulse1(Integer pulse1) {
        this.pulse1 = pulse1;
    }

    public Integer getSystolic2() {
        return systolic2;
    }

    public void setSystolic2(Integer systolic2) {
        this.systolic2 = systolic2;
    }

    public Integer getDiastolic2() {
        return diastolic2;
    }

    public void setDiastolic2(Integer diastolic2) {
        this.diastolic2 = diastolic2;
    }

    public Integer getPulse2() {
        return pulse2;
    }

    public void setPulse2(Integer pulse2) {
        this.pulse2 = pulse2;
    }

    public Integer getMeanSystolic() {
        return (int) Math.round((double)(systolic1 + systolic2) / 2);
    }

    public Integer getMeanDiastolic() {
        return (int) Math.round((double)(diastolic1 + diastolic2) / 2);
    }

    public Date getReadingDate() {
        return readingDate;
    }

    public String getReadingDateString() {
        return DATE_FORMAT.format(readingDate);
    }

    public Long getReadingDateTimestamp() {
        return readingDate.getTime();
    }

    public void setReadingDate(Date readingTimestamp) {
        this.readingDate = readingTimestamp;
    }

    public Boolean getFollowedInstructions() {
        return followedInstructions;
    }

    public void setFollowedInstructions(Boolean followedInstructions) {
        this.followedInstructions = followedInstructions;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdTimestamp) {
        this.createdDate = createdTimestamp;
    }
}
