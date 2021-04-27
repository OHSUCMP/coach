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

    @Column(name = "goal_text")
    private String goalText;

    @Column(name = "follow_up_days")
    private Integer followUpDays;

    @Column(name = "created_date")
    private Date createdDate;

    private Boolean completed;

    @Column(name = "completed_date")
    private Date completedDate;

    protected Goal() {
    }

    public Goal(String goalId, String goalText, Integer followUpDays) {
        this.goalId = goalId;
        this.goalText = goalText;
        this.followUpDays = followUpDays;
        this.completed = false;
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

    public String getGoalText() {
        return goalText;
    }

    public void setGoalText(String goalText) {
        this.goalText = goalText;
    }

    public Integer getFollowUpDays() {
        return followUpDays;
    }

    public void setFollowUpDays(Integer followUpDays) {
        this.followUpDays = followUpDays;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date archivedDate) {
        this.completedDate = archivedDate;
    }
}
