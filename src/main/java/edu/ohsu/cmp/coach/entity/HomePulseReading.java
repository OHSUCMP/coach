package edu.ohsu.cmp.coach.entity;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.ObservationSource;
import edu.ohsu.cmp.coach.model.PulseModel;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "home_pulse_reading")
public class HomePulseReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private Integer pulse;
    private Date readingDate;
    private Boolean followedInstructions;
    private Date createdDate;
    private String source;

    protected HomePulseReading() {
    }

    public HomePulseReading(Integer pulse, Date readingDate, Boolean followedInstructions, ObservationSource source) {
        this.pulse = pulse;
        this.readingDate = readingDate;
        this.followedInstructions = followedInstructions;
        this.source = source.name();
    }

    // used during create, do not set ID
    public HomePulseReading(PulseModel pm) throws DataException {
        if (pm.getSource() != ObservationSource.COACH_UI && pm.getSource() != ObservationSource.OMRON) {
            throw new DataException("cannot convert PulseModel with source=" +
                    pm.getSource() + " to HomePulseReading");
        }
        this.pulse = pm.getPulse().getValue().intValue();
        this.readingDate = pm.getReadingDate();
        this.followedInstructions = pm.getFollowedProtocol();
        this.source = pm.getSource().name();
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
