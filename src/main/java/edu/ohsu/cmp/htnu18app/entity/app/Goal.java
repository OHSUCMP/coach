package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(schema = "htnu18app")
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id")
    private String goalId;

    @Column(name = "pat_id")
    private Long patId;

    @Column(name = "follow_up_days")
    private Integer followUpDays;

    private String value;

    @Column(name = "created_date")
    private Date createdDate;

    protected Goal() {
    }

    public Goal(String goalId, Integer followUpDays, String value) {
        this.goalId = goalId;
        this.followUpDays = followUpDays;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public Long getPatId() {
        return patId;
    }

    public void setPatId(Long patId) {
        this.patId = patId;
    }

    public Integer getFollowUpDays() {
        return followUpDays;
    }

    public void setFollowUpDays(Integer followUpDays) {
        this.followUpDays = followUpDays;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
