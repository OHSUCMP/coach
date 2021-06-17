package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(schema = "htnu18app")
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private String extGoalId;
    private String goalText;
    private Integer followUpDays;
    private Date createdDate;
    private Boolean completed;
    private Date completedDate;

    protected Goal() {
    }

    public Goal(String extGoalId, String goalText, Integer followUpDays) {
        this.extGoalId = extGoalId;
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

    public Long getPatId() {
        return patId;
    }

    public void setPatId(Long patId) {
        this.patId = patId;
    }

    public String getExtGoalId() {
        return extGoalId;
    }

    public void setExtGoalId(String goalId) {
        this.extGoalId = goalId;
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
