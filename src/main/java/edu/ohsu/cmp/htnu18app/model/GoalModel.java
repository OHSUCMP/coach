package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.entity.app.AchievementStatus;
import edu.ohsu.cmp.htnu18app.entity.app.Goal;
import edu.ohsu.cmp.htnu18app.entity.app.GoalHistory;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

public class GoalModel implements Comparable<GoalModel> {
    private Long id;
    private Long patId;
    private String extGoalId;
    private String referenceSystem;
    private String referenceCode;
    private String goalText;
    private Integer systolicTarget;
    private Integer diastolicTarget;
    private Date targetDate;
    private Date createdDate;

    private TreeSet<GoalHistoryModel> history;

    public GoalModel(Goal g) {
        this.id = g.getId();
        this.patId = g.getPatId();
        this.extGoalId = g.getExtGoalId();
        this.referenceSystem = g.getReferenceSystem();
        this.referenceCode = g.getReferenceCode();
        this.goalText = g.getGoalText();
        this.systolicTarget = g.getSystolicTarget();
        this.diastolicTarget = g.getDiastolicTarget();
        this.targetDate = g.getTargetDate();
        this.createdDate = g.getCreatedDate();

        history = new TreeSet<>();
        for (GoalHistory gh : g.getHistory()) {
            history.add(new GoalHistoryModel(gh));
        }
    }

    @Override
    public int compareTo(@NotNull GoalModel o) {
        return createdDate.compareTo(o.getCreatedDate());
    }

    public boolean getIsInProgress() {
        return getAchievementStatus() == AchievementStatus.IN_PROGRESS;
    }

    public AchievementStatus getAchievementStatus() {
        GoalHistoryModel mostRecent = history.last();
        return mostRecent != null ?
                mostRecent.getAchievementStatus() :
                null;
    }

    public Date getStatusDate() {
        GoalHistoryModel mostRecent = history.last();
        return mostRecent != null ?
                mostRecent.getCreatedDate() :
                null;
    }

    public Long getId() {
        return id;
    }

    public Long getPatId() {
        return patId;
    }

    public String getExtGoalId() {
        return extGoalId;
    }

    public String getReferenceSystem() {
        return referenceSystem;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public String getGoalText() {
        return goalText;
    }

    public Integer getSystolicTarget() {
        return systolicTarget;
    }

    public Integer getDiastolicTarget() {
        return diastolicTarget;
    }

    public Date getTargetDate() {
        return targetDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Set<GoalHistoryModel> getHistory() {
        return history;
    }
}
