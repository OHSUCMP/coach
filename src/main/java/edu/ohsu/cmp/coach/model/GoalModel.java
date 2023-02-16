package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.GoalHistory;
import edu.ohsu.cmp.coach.entity.MyGoal;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.*;
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

        Coding bpCoding = fcm.getBpPanelCommonCoding();
        this.referenceSystem = bpCoding.getSystem();
        this.referenceCode = bpCoding.getCode();
        this.referenceDisplay = bpCoding.getDisplay();

        this.goalText = g.getDescription().getText();

        for (Goal.GoalTargetComponent gtc : g.getTarget()) {
            if (FhirUtil.hasCoding(gtc.getMeasure(), fcm.getBpSystolicCommonCoding())) {
                this.systolicTarget = gtc.getDetailQuantity().getValue().intValue();

            } else if (FhirUtil.hasCoding(gtc.getMeasure(), fcm.getBpDiastolicCommonCoding())) {
                this.diastolicTarget = gtc.getDetailQuantity().getValue().intValue();
            }
        }

        this.targetDate = null; // EHR-based
        this.createdDate = g.hasStartDateType() ?
                g.getStartDateType().getValue() :
                null;

        history = new TreeSet<>();
    }

//    @Override
//    public Bundle toBundle(String patientId, FhirConfigManager fcm) {
//        Goal goal = sourceGoal != null ?
//                sourceGoal :
//                buildGoal(patientId, fcm);
//
//        return FhirUtil.bundleResources(goal);
//    }

//    private Goal buildGoal(String patientId, FhirConfigManager fcm) {
//
//        // this is only used when building local goals, for which sourceGoal == null
//
//        Goal g = new Goal();
//
//        g.setId(getExtGoalId());
//        g.setSubject(new Reference().setReference(patientId));
//        g.setLifecycleStatus(getLifecycleStatus().getFhirValue());
//        g.getAchievementStatus().addCoding().setCode(getAchievementStatus().getFhirValue())
//                .setSystem("http://terminology.hl7.org/CodeSystem/goal-achievement");
//        g.getCategoryFirstRep().addCoding().setCode(getReferenceCode()).setSystem(getReferenceSystem());
//        g.getDescription().setText(getGoalText());
//        g.setStatusDate(getStatusDate());
//        g.getTarget().add(new Goal.GoalTargetComponent()
//                .setDue(new DateType().setValue(getTargetDate())));
//
//        if (isBPGoal()) {
//            Goal.GoalTargetComponent systolic = new Goal.GoalTargetComponent();
//            systolic.getMeasure().addCoding(fcm.getBpSystolicCoding());
//            systolic.setDetail(new Quantity());
//            systolic.getDetailQuantity().setCode(fcm.getBpValueCode());
//            systolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
//            systolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
//            systolic.getDetailQuantity().setValue(getSystolicTarget());
//            g.getTarget().add(systolic);
//
//            Goal.GoalTargetComponent diastolic = new Goal.GoalTargetComponent();
//            diastolic.getMeasure().addCoding(fcm.getBpDiastolicCoding());
//            diastolic.setDetail(new Quantity());
//            diastolic.getDetailQuantity().setCode(fcm.getBpValueCode());
//            diastolic.getDetailQuantity().setSystem(fcm.getBpValueSystem());
//            diastolic.getDetailQuantity().setUnit(fcm.getBpValueUnit());
//            diastolic.getDetailQuantity().setValue(getDiastolicTarget());
//            g.getTarget().add(diastolic);
//        }
//
//        return g;
//    }

    public boolean isEHRGoal() {
        return sourceGoal != null;
    }

    public boolean isBPGoal() {
        return systolicTarget != null && diastolicTarget != null;
    }

    @Override
    public int compareTo(@NotNull GoalModel o) {
        if (isInProgress() && ! o.isInProgress()) {
            return -1;

        } else if ( ! isInProgress() && o.isInProgress()) {
            return 1;

        } else {
            Date d1 = getCreatedDate();
            Date d2 = o.getCreatedDate();
            if      (d1 == null && d2 == null)  return 0;
            else if (d1 != null && d2 != null)  return d1.compareTo(d2);
            else if (d1 != null)                return -1;
            else                                return 1;
        }
    }

    public LifecycleStatus getLifecycleStatus() {
        GoalHistoryModel mostRecent = getMostRecentHistory();
        return mostRecent != null ?
                mostRecent.getLifecycleStatus() :
                null;
    }

    public boolean isInProgress() {
        return getAchievementStatus() == AchievementStatus.IN_PROGRESS;
    }

    public boolean isAchieved() {
        return getAchievementStatus() == AchievementStatus.ACHIEVED;
    }

    public boolean isNotAchieved() {
        return getAchievementStatus() == AchievementStatus.NOT_ACHIEVED;
    }

    public AchievementStatus getAchievementStatus() {
        if (history != null && history.size() > 0) {
            GoalHistoryModel mostRecent = history.last();
            return mostRecent != null ?
                    mostRecent.getAchievementStatus() :
                    null;

        } else {
            return null;
        }
    }

    public String getAchievementStatusLabel() {
        AchievementStatus status = getAchievementStatus();
        return status != null ?
                status.getLabel() :
                null;
    }

    public Date getStatusDate() {
        if (history != null && history.size() > 0) {
            GoalHistoryModel mostRecent = history.last();
            return mostRecent != null ?
                    mostRecent.getCreatedDate() :
                    null;

        } else {
            return null;
        }
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

    private GoalHistoryModel getMostRecentHistory() {
        return history != null && history.size() > 0 ?
                history.last() :
                null;
    }
}
