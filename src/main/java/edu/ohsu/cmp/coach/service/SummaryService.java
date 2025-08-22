package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.Summary;
import edu.ohsu.cmp.coach.entity.SummaryRecommendation;
import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.model.BloodPressureSummaryModel;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.model.RecommendationSeverity;
import edu.ohsu.cmp.coach.model.recommendation.Card;
import edu.ohsu.cmp.coach.repository.SummaryRecommendationRepository;
import edu.ohsu.cmp.coach.repository.SummaryRepository;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SummaryService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BloodPressureService bpService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private SummaryRepository repository;

    @Autowired
    private SummaryRecommendationRepository recommendationRepository;

    public Summary buildSummary(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        String bpGoal = null;
        String calculatedBP = null;
        Boolean bpAtOrBelowGoal = null;
        List<String> notesList = new ArrayList<>();

        try {
            BloodPressureSummaryModel currentBP = new BloodPressureSummaryModel(bpService.getBloodPressureReadings(sessionId));
            GoalModel currentBPGoal = goalService.getCurrentBPGoal(sessionId);

            if (currentBPGoal != null && currentBPGoal.isBPGoal()) {
                bpGoal = currentBPGoal.getSystolicTarget() + "/" + currentBPGoal.getDiastolicTarget();
            }

            if (currentBP.hasCalculatedAverage()) {
                calculatedBP = currentBP.getAvgSystolic() + "/" + currentBP.getAvgDiastolic();
            }

            if (currentBP.hasCalculatedAverage() && currentBPGoal != null && currentBPGoal.isBPGoal()) {
                bpAtOrBelowGoal = currentBP.getAvgSystolic() <= currentBPGoal.getSystolicTarget() &&
                        currentBP.getAvgDiastolic() <= currentBPGoal.getDiastolicTarget();

            } else if ( ! currentBP.hasCalculatedAverage() ) {
                notesList.add("insufficient readings to calculate average BP");

            } else if (currentBPGoal == null || ! currentBPGoal.isBPGoal()) {
                notesList.add("no current BP goal");
            }

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " attempting to generate summary - " + e.getMessage(), e);
            auditService.doAudit(sessionId, AuditSeverity.ERROR, "failed to generate summary", e.getMessage());
            return null;
        }

        String notes = notesList.isEmpty() ?
                null :
                StringUtils.join(notesList, "; ");

        Summary summary = new Summary(bpGoal, calculatedBP, bpAtOrBelowGoal, notes);

        Set<SummaryRecommendation> recommendations = new LinkedHashSet<>();
        for (Map.Entry<String, List<Card>> entry : workspace.getAllCards().entrySet()) {
            String recommendation = entry.getKey();
            for (Card card : entry.getValue()) {
                try {
                    SummaryRecommendation sr = new SummaryRecommendation(recommendation,
                            RecommendationSeverity.fromIndicator(card.getIndicator()),
                            card.getSummary(),
                            summary);
                    recommendations.add(sr);

                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() +
                            " attempting to generate summary recommendation for " + recommendation + " - " + e.getMessage(), e);
                    auditService.doAudit(sessionId, AuditSeverity.ERROR,
                            "failed to generate summary recommendation for " + recommendation, e.getMessage());
                }
            }
        }

        summary.setRecommendations(recommendations);

        return summary;
    }

    public Summary create(String sessionId, Summary summary) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        summary.setPatId(workspace.getInternalPatientId());
        summary.setCreatedDate(new Date());

        return repository.save(summary);
    }
}
