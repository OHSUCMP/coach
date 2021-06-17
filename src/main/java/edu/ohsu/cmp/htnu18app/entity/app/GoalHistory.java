package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(schema = "htnu18app", name = "goal_history")
public class GoalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long goalId;
    private LifecycleStatus lifecycleStatus;
    private AchievementStatus achievementStatus;
    private Date createdDate;

    protected GoalHistory() {
    }

    public GoalHistory(Long goalId, LifecycleStatus lifecycleStatus, AchievementStatus achievementStatus) {
        this.goalId = goalId;
        this.lifecycleStatus = lifecycleStatus;
        this.achievementStatus = achievementStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public LifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(LifecycleStatus lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public AchievementStatus getAchievementStatus() {
        return achievementStatus;
    }

    public void setAchievementStatus(AchievementStatus achievementStatus) {
        this.achievementStatus = achievementStatus;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
