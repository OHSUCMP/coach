package edu.ohsu.cmp.coach.entity.app;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.BloodPressureModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(schema = "coach", name = "home_bp_reading")
public class HomeBloodPressureReading {

//    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private Integer systolic;
    private Integer diastolic;
    private Integer pulse;
    private Date readingDate;
    private Boolean followedInstructions;
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

    // used during create, do not set ID
    public HomeBloodPressureReading(BloodPressureModel bpm) throws DataException {
        if (bpm.getSource() != BloodPressureModel.Source.HOME) {
            throw new DataException("cannot convert BloodPressureModel with source=" +
                    bpm.getSource() + " to HomeBloodPressureReading");
        }
        this.systolic = bpm.getSystolic().getValue().intValue();
        this.diastolic = bpm.getDiastolic().getValue().intValue();
        this.pulse = bpm.getPulse().getValue().intValue();
        this.readingDate = bpm.getReadingDate();
        this.followedInstructions = bpm.getFollowedProtocol();
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

//    public String getReadingDateString() {
//        return DATE_FORMAT.format(readingDate);
//    }

//    public Long getReadingDateTimestamp() {
//        return readingDate.getTime();
//    }

    public void setReadingDate(Date readingDate) {
        this.readingDate = readingDate;
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
