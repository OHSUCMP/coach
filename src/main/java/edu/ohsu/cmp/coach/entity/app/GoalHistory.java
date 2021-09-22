package edu.ohsu.cmp.coach.entity.app;

import edu.ohsu.cmp.coach.exception.CaseNotHandledException;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(schema = "coach", name = "goal_history")
public class GoalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long goalId;

    @Enumerated(EnumType.STRING)
    private AchievementStatus achievementStatus;

    private Date createdDate;

//    @ManyToOne(fetch = FetchType.EAGER, optional = false)
//    @JoinColumn(name = "goalId", nullable = false)
//    private Goal goal;

    protected GoalHistory() {
    }

    public GoalHistory(AchievementStatus achievementStatus, MyGoal myGoal) {
        this.goalId = myGoal.getId();
        this.achievementStatus = achievementStatus;
        this.createdDate = new Date();
//        this.goal = goal;
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
        switch (achievementStatus) {
            case IN_PROGRESS:   return LifecycleStatus.ACTIVE;
            case ACHIEVED:      return LifecycleStatus.COMPLETED;
            case NOT_ACHIEVED:  return LifecycleStatus.CANCELLED;
            default: throw new CaseNotHandledException("couldn't determine Lifecycle Status for Achievement Status '" + achievementStatus + "'");
        }
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

//    public Goal getGoal() {
//        return goal;
//    }
//
//    public void setGoal(Goal goal) {
//        this.goal = goal;
//    }

//    @Override
//    public boolean equals(Object o) {
//        if (o == null) return false;
//        if (o == this) return true;
//        if (o.getClass() != getClass()) return false;
//
//        GoalHistory gh = (GoalHistory) o;
//        return new EqualsBuilder()
////                .appendSuper(super.equals(o))
//                .append(lifecycleStatus, gh.lifecycleStatus)
//                .append(achievementStatus, gh.achievementStatus)
//                .append(createdDate, gh.createdDate)
//                .append(goal, gh.goal)
//                .isEquals();
//    }
//
//    @Override
//    public int hashCode() {
//        return new HashCodeBuilder(2467, 647)
//                .append(lifecycleStatus)
//                .append(achievementStatus)
//                .append(createdDate)
//                .append(goal)
//                .toHashCode();
//    }
//
//    @Override
//    public int compareTo(@NotNull GoalHistory o) {
//        return new CompareToBuilder()
//                .append(this.createdDate, o.createdDate)
//                .toComparison();
//    }
}
