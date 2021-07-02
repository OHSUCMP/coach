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

    @Enumerated(EnumType.STRING)
    private LifecycleStatus lifecycleStatus;

    @Enumerated(EnumType.STRING)
    private AchievementStatus achievementStatus;

    private Date createdDate;

//    @ManyToOne(fetch = FetchType.EAGER, optional = false)
//    @JoinColumn(name = "goalId", nullable = false)
//    private Goal goal;

    protected GoalHistory() {
    }

    public GoalHistory(LifecycleStatus lifecycleStatus, AchievementStatus achievementStatus, Goal goal) {
        this.goalId = goal.getId();
        this.lifecycleStatus = lifecycleStatus;
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
