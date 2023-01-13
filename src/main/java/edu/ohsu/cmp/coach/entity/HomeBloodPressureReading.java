package edu.ohsu.cmp.coach.entity;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.ObservationSource;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "home_bp_reading")
public class HomeBloodPressureReading {

//    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private Integer systolic;
    private Integer diastolic;
    private Date readingDate;
    private Boolean followedInstructions;
//    private String source;
    private Date createdDate;

    protected HomeBloodPressureReading() {
    }

    public HomeBloodPressureReading(Integer systolic, Integer diastolic, Date readingDate, Boolean followedInstructions) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.readingDate = readingDate;
        this.followedInstructions = followedInstructions;
//        this.source = source.name();
    }

    // used during create, do not set ID
    public HomeBloodPressureReading(BloodPressureModel bpm) throws DataException {
        if (bpm.getSource() != ObservationSource.HOME) {
            throw new DataException("cannot convert BloodPressureModel with source=" +
                    bpm.getSource() + " to HomeBloodPressureReading");
        }

        if (bpm.getSystolic() == null && bpm.getDiastolic() == null) {
            throw new DataException("systolic and diastolic cannot both be null");
        }

        if (bpm.getSystolic() != null) {
            this.systolic = bpm.getSystolic().getValue().intValue();
        }

        if (bpm.getDiastolic() != null) {
            this.diastolic = bpm.getDiastolic().getValue().intValue();
        }

        this.readingDate = bpm.getReadingDate();
        this.followedInstructions = bpm.getFollowedProtocol();
//        this.source = bpm.getSource().name();
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

    public Date getReadingDate() {
        return readingDate;
    }

    public void setReadingDate(Date readingDate) {
        this.readingDate = readingDate;
    }

    public Boolean getFollowedInstructions() {
        return followedInstructions;
    }

    public void setFollowedInstructions(Boolean followedInstructions) {
        this.followedInstructions = followedInstructions;
    }

//    public String getSource() {
//        return source;
//    }

//    public void setSource(String source) {
//        this.source = source;
//    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdTimestamp) {
        this.createdDate = createdTimestamp;
    }
}
