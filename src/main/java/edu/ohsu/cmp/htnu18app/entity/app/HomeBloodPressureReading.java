package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Table(schema = "htnu18app", name = "home_bp_reading")
public class HomeBloodPressureReading {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pat_id")
    private Long patId;

    private Integer systolic;
    private Integer diastolic;
    private Integer pulse;

    @Column(name = "reading_date")
    private Date readingDate;

    @Column(name = "followed_instructions")
    private Boolean followedInstructions;

    @Column(name = "created_date")
    private Date createdDate;

    protected HomeBloodPressureReading() {
    }

    public HomeBloodPressureReading(Integer systolic, Integer diastolic, Integer pulse,
                                    Date readingDate, Boolean followedInstructions) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.pulse = pulse;
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

    public Integer getSystolic() {
        return systolic;
    }

    public void setSystolic(Integer systolic1) {
        this.systolic = systolic1;
    }

    public Integer getDiastolic() {
        return diastolic;
    }

    public void setDiastolic(Integer diastolic1) {
        this.diastolic = diastolic1;
    }

    public Integer getPulse() {
        return pulse;
    }

    public void setPulse(Integer pulse1) {
        this.pulse = pulse1;
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
