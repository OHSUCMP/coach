package edu.ohsu.cmp.coach.entity.app;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.ObservationSource;
import edu.ohsu.cmp.coach.model.PulseModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(schema = "coach", name = "home_pulse_reading")
public class HomePulseReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private Integer pulse;
    private Date readingDate;
    private Boolean followedInstructions;
    private Date createdDate;

    protected HomePulseReading() {
    }

    public HomePulseReading(Integer pulse, Date readingDate, Boolean followedInstructions) {
        this.pulse = pulse;
        this.readingDate = readingDate;
        this.followedInstructions = followedInstructions;
    }

    // used during create, do not set ID
    public HomePulseReading(PulseModel pm) throws DataException {
        if (pm.getSource() != ObservationSource.HOME) {
            throw new DataException("cannot convert PulseModel with source=" +
                    pm.getSource() + " to HomePulseReading");
        }
        this.pulse = pm.getPulse().getValue().intValue();
        this.readingDate = pm.getReadingDate();
        this.followedInstructions = pm.getFollowedProtocol();
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

    public Integer getPulse() {
        return pulse;
    }

    public void setPulse(Integer pulse1) {
        this.pulse = pulse1;
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

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdTimestamp) {
        this.createdDate = createdTimestamp;
    }
}
