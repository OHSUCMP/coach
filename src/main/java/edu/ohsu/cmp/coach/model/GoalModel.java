package edu.ohsu.cmp.coach.model;

import edu.ohsu.cmp.coach.entity.app.AchievementStatus;
import edu.ohsu.cmp.coach.entity.app.GoalHistory;
import edu.ohsu.cmp.coach.entity.app.MyGoal;
import org.hl7.fhir.r4.model.Goal;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

public class GoalModel implements Comparable<GoalModel> {
    public static final String BP_GOAL_ID = "bp-goal";
    public static final Integer BP_GOAL_DEFAULT_SYSTOLIC = 140;
    public static final Integer BP_GOAL_DEFAULT_DIASTOLIC = 90;
    public static final String ACHIEVEMENT_STATUS_CODING_SYSTEM = "http://terminology.hl7.org/CodeSystem/goal-achievement";
    public static final String ACHIEVEMENT_STATUS_CODING_INPROGRESS_CODE = "in-progress";

    private Long id;
    private Long patId;
    private String extGoalId;
    private String referenceSystem;
    private String referenceCode;
    private String referenceDisplay;
    private String goalText;
    private Integer systolicTarget = null;
    private Integer diastolicTarget = null;
    private Date targetDate;
    private Date createdDate;

    private TreeSet<GoalHistoryModel> history;

    public GoalModel(MyGoal g) {
        this.id = g.getId();
        this.patId = g.getPatId();
        this.extGoalId = g.getExtGoalId();
        this.referenceSystem = g.getReferenceSystem();
        this.referenceCode = g.getReferenceCode();
        this.referenceDisplay = g.getReferenceDisplay();
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

    // this constructor ONLY used to convert EHR-based BP goals into local goal model
    public GoalModel(Goal g, Long internalPatientId, String bpSystem, String bpCode, String bpDisplay,
                     String systolicCode, String diastolicCode) {
        this.id = null; // EHR-based
        this.patId = internalPatientId;
        this.extGoalId = g.getId();
        this.referenceSystem = bpSystem;
        this.referenceCode = bpCode;
        this.referenceDisplay = bpDisplay;
        this.goalText = g.getDescription().getText();

        for (Goal.GoalTargetComponent gtc : g.getTarget()) {
            if (gtc.getMeasure().hasCoding(bpSystem, systolicCode)) {
                this.systolicTarget = gtc.getDetailQuantity().getValue().intValue();

            } else if (gtc.getMeasure().hasCoding(bpSystem, diastolicCode)) {
                this.diastolicTarget = gtc.getDetailQuantity().getValue().intValue();
            }
        }

        this.targetDate = null; // EHR-based
        this.createdDate = g.getStartDateType().getValue();
    }

    public boolean isEHRGoal() {
        return id == null;
    }

    @Override
    public int compareTo(@NotNull GoalModel o) {
        if (getIsInProgress() && ! o.getIsInProgress()) {
            return -1;

        } else if ( ! getIsInProgress() && o.getIsInProgress()) {
            return 1;

        } else {
            return createdDate.compareTo(o.getCreatedDate());
        }
    }

    public boolean getIsInProgress() {
        return getAchievementStatus() == AchievementStatus.IN_PROGRESS;
    }

    public boolean getIsAchieved() {
        return getAchievementStatus() == AchievementStatus.ACHIEVED;
    }

    public boolean getIsNotAchieved() {
        return getAchievementStatus() == AchievementStatus.NOT_ACHIEVED;
    }

    public AchievementStatus getAchievementStatus() {
        GoalHistoryModel mostRecent = history.last();
        return mostRecent != null ?
                mostRecent.getAchievementStatus() :
                null;
    }

    public String getAchievementStatusLabel() {
        return getAchievementStatus().getLabel();
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

    public String getReferenceDisplay() {
        return referenceDisplay;
    }

    public String getGoalText() {
        return goalText;
    }

    public Boolean getIsBloodPressureGoal() {
        return systolicTarget != null || diastolicTarget != null;
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
