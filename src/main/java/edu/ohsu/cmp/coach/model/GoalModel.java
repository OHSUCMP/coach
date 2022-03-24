package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.app.AchievementStatus;
import edu.ohsu.cmp.coach.entity.app.GoalHistory;
import edu.ohsu.cmp.coach.entity.app.MyGoal;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Goal;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

public class GoalModel implements FHIRCompatible, Comparable<GoalModel> {
    public static final String BP_GOAL_ID = "bp-goal";
    public static final Integer BP_GOAL_DEFAULT_SYSTOLIC = 140;
    public static final Integer BP_GOAL_DEFAULT_DIASTOLIC = 90;
    public static final String ACHIEVEMENT_STATUS_CODING_SYSTEM = "http://terminology.hl7.org/CodeSystem/goal-achievement";
    public static final String ACHIEVEMENT_STATUS_CODING_INPROGRESS_CODE = "in-progress";

    private Goal sourceGoal;

    private Long id;
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
    public GoalModel(Goal g, FhirConfigManager fcm) {
        sourceGoal = g;

        this.id = null; // EHR-based
//        this.patId = null; // EHR-based
        this.extGoalId = g.getId();
        this.referenceSystem = fcm.getBpSystem();
        this.referenceCode = fcm.getBpCode();
        this.referenceDisplay = "Blood Pressure";
        this.goalText = g.getDescription().getText();

        for (Goal.GoalTargetComponent gtc : g.getTarget()) {
            if (gtc.getMeasure().hasCoding(fcm.getBpSystem(), fcm.getBpSystolicCode())) {
                this.systolicTarget = gtc.getDetailQuantity().getValue().intValue();

            } else if (gtc.getMeasure().hasCoding(fcm.getBpSystem(), fcm.getBpDiastolicCode())) {
                this.diastolicTarget = gtc.getDetailQuantity().getValue().intValue();
            }
        }

        this.targetDate = null; // EHR-based
        this.createdDate = g.getStartDateType().getValue();
    }

    @Override
    public Bundle toBundle() {
        return FhirUtil.bundleResources(sourceGoal);
    }

    public boolean isEHRGoal() {
        return sourceGoal != null;
    }

    public boolean isBPGoal() {
        return systolicTarget != null && diastolicTarget != null;
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

    @JsonIgnore
    public Goal getSourceGoal() {
        return sourceGoal;
    }

    public Long getId() {
        return id;
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
