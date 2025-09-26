package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.*;
import edu.ohsu.cmp.coach.model.*;
import edu.ohsu.cmp.coach.model.recommendation.Card;
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
    private AdverseEventService adverseEventService;

    @Autowired
    private MedicationService medicationService;

    @Autowired
    private SummaryRepository repository;

    public Summary buildSummary(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        String bpGoal = null;
        String calculatedAverageBP = null;
        Boolean bpAtOrBelowGoal = null;
        String mostRecentBP = null;
        Date mostRecentBPDate = null;
        Boolean mostRecentBPInCrisis = null;
        Boolean mostRecentBPInLowCrisis = null;
        String secondMostRecentBP = null;
        Date secondMostRecentBPDate = null;
        Boolean twoMostRecentBPsInCrisis = null;
        Boolean twoMostRecentBPsInLowCrisis = null;
        List<String> notesList = new ArrayList<>();

        try {
            BloodPressureSummaryModel currentBP = new BloodPressureSummaryModel(bpService.getBloodPressureReadings(sessionId));
            GoalModel currentBPGoal = goalService.getCurrentBPGoal(sessionId);

            if (currentBPGoal != null && currentBPGoal.isBPGoal()) {
                bpGoal = currentBPGoal.getSystolicTarget() + "/" + currentBPGoal.getDiastolicTarget();
            }

            if (currentBP.hasCalculatedAverage()) {
                calculatedAverageBP = currentBP.getAvgSystolic() + "/" + currentBP.getAvgDiastolic();
            }

            if (currentBP.hasCalculatedAverage() && currentBPGoal != null && currentBPGoal.isBPGoal()) {
                bpAtOrBelowGoal = currentBP.getAvgSystolic() <= currentBPGoal.getSystolicTarget() &&
                        currentBP.getAvgDiastolic() <= currentBPGoal.getDiastolicTarget();

            } else if ( ! currentBP.hasCalculatedAverage() ) {
                notesList.add("insufficient readings to calculate average BP");

            } else if (currentBPGoal == null || ! currentBPGoal.isBPGoal()) {
                notesList.add("no current BP goal");
            }

            if (currentBP.hasMostRecent()) {
                mostRecentBP = currentBP.getMostRecentSystolic() + "/" + currentBP.getMostRecentDiastolic();
                mostRecentBPDate = currentBP.getMostRecentDate();

                if (currentBP.isMostRecentBPCrisis()) {
                    mostRecentBPInCrisis = true;
                    notesList.add("most recent BP represents hypertension crisis");
                } else {
                    mostRecentBPInCrisis = false;
                }

                if (currentBP.isMostRecentBPLowCrisis()) {
                    mostRecentBPInLowCrisis = true;
                    notesList.add("most recent BP represents hypotension crisis");
                } else {
                    mostRecentBPInLowCrisis = false;
                }
            }

            if (currentBP.hasSecondMostRecent()) {
                secondMostRecentBP = currentBP.getSecondMostRecentSystolic() + "/" + currentBP.getSecondMostRecentDiastolic();
                secondMostRecentBPDate = currentBP.getSecondMostRecentDate();

                if (currentBP.isMostRecentBPCrisis() && currentBP.isSecondMostRecentBPCrisis() && currentBP.twoMostRecentWithin14Days()) {
                    twoMostRecentBPsInCrisis = true;
                    notesList.add("two most-recent BPs were taken within the last two weeks and represent hypertension crisis");

                } else {
                    twoMostRecentBPsInCrisis = false;
                }

                if (currentBP.isMostRecentBPLowCrisis() && currentBP.isSecondMostRecentBPLowCrisis() && currentBP.twoMostRecentWithin14Days()) {
                    twoMostRecentBPsInLowCrisis = true;
                    notesList.add("two most-recent BPs were taken within the last two weeks and represent hypotension crisis");

                } else {
                    twoMostRecentBPsInLowCrisis = false;
                }
            }

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " attempting to generate summary - " + e.getMessage(), e);
            auditService.doAudit(sessionId, AuditSeverity.ERROR, "failed to generate summary", e.getMessage());
            return null;
        }

        String notes = notesList.isEmpty() ?
                null :
                StringUtils.join(notesList, "; ");

        Summary summary = new Summary(bpGoal, calculatedAverageBP, bpAtOrBelowGoal,
                mostRecentBP, mostRecentBPDate, mostRecentBPInCrisis, mostRecentBPInLowCrisis,
                secondMostRecentBP, secondMostRecentBPDate, twoMostRecentBPsInCrisis, twoMostRecentBPsInLowCrisis,
                notes);

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

        Set<SummaryOngoingAdverseEvent> ongoingAdverseEvents = new LinkedHashSet<>();
        for (AdverseEventModel ae : adverseEventService.getAdverseEvents(sessionId)) {
            if (ae.hasOutcome(Outcome.ONGOING)) {
                ongoingAdverseEvents.add(new SummaryOngoingAdverseEvent(ae, summary));
            }
        }

        summary.setOngoingAdverseEvents(ongoingAdverseEvents);

        Set<SummaryActiveAntihtnMeds> activeAntihtnMeds = new LinkedHashSet<>();
        for (MedicationModel mm : medicationService.getAntihypertensiveMedications(sessionId)) {
            activeAntihtnMeds.add(new SummaryActiveAntihtnMeds(mm, summary));
        }

        summary.setActiveAntihtnMeds(activeAntihtnMeds);

        return summary;
    }

    public Summary create(String sessionId, Summary summary) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        summary.setPatId(workspace.getInternalPatientId());
        summary.setCreatedDate(new Date());

        return repository.save(summary);
    }
}
