package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(schema = "htnu18app")
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patId;
    private String extGoalId;
    private String referenceSystem;
    private String referenceCode;
    private String goalText;
    private Date targetDate;
    private Date createdDate;
    private Boolean completed;
    private Date completedDate;

    @OneToMany(mappedBy = "goalId", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<GoalHistory> history;

    protected Goal() {
    }

    public Goal(String extGoalId, String referenceSystem, String referenceCode, String goalText, Date targetDate) {
        this.extGoalId = extGoalId;
        this.referenceSystem = referenceSystem;
        this.referenceCode = referenceCode;
        this.goalText = goalText;
        this.targetDate = targetDate;
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

    public String getReferenceSystem() {
        return referenceSystem;
    }

    public void setReferenceSystem(String referenceSystem) {
        this.referenceSystem = referenceSystem;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getGoalText() {
        return goalText;
    }

    public void setGoalText(String goalText) {
        this.goalText = goalText;
    }

    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
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

    public Set<GoalHistory> getHistory() {
        return history;
    }

    public void setHistory(Set<GoalHistory> history) {
        this.history = history;
    }

    public LifecycleStatus getLifecycleStatus() {
        GoalHistory mostRecent = getMostRecentHistory();
        return mostRecent != null ?
                mostRecent.getLifecycleStatus() :
                null;
    }

    public AchievementStatus getAchievementStatus() {
        GoalHistory mostRecent = getMostRecentHistory();
        return mostRecent != null ?
                mostRecent.getAchievementStatus() :
                null;
    }

    public Date getStatusDate() {
        GoalHistory mostRecent = getMostRecentHistory();
        return mostRecent != null ?
                mostRecent.getCreatedDate() :
                null;
    }

    private GoalHistory getMostRecentHistory() {
        GoalHistory mostRecent = null;
        for (GoalHistory gh : history) {
            if (mostRecent == null) mostRecent = gh;
            else {
                if (gh.getCreatedDate().compareTo(mostRecent.getCreatedDate()) > 0) {
                    mostRecent = gh;
                }
            }
        }
        return mostRecent;
    }
}
