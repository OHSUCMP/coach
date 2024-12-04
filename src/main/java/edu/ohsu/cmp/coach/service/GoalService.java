package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.GoalHistory;
import edu.ohsu.cmp.coach.entity.MyGoal;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.model.AchievementStatus;
import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.model.GoalModel;
import edu.ohsu.cmp.coach.repository.GoalHistoryRepository;
import edu.ohsu.cmp.coach.repository.GoalRepository;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GoalService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${security.salt}")
    private String salt;

    @Autowired
    private EHRService ehrService;

    @Autowired
    private GoalRepository repository;

    @Autowired
    private GoalHistoryRepository historyRepository;

    public List<GoalModel> getGoals(String sessionId) {
        List<GoalModel> list = new ArrayList<>();
        list.addAll(userWorkspaceService.get(sessionId).getRemoteGoals());
        list.addAll(buildLocalGoals(sessionId));
        return list;
    }

    private List<GoalModel> buildLocalGoals(String sessionId) {
        List<GoalModel> list = new ArrayList<>();

        List<MyGoal> goalList = getLocalGoalList(sessionId);
        for (MyGoal item : goalList) {
            list.add(new GoalModel(item));
        }

        return list;
    }

    /**
     * Builds a list of GoalModel objects from FHIR Resources read directly from the FHIR server
     * @param sessionId
     * @return
     * @throws DataException
     */
    public List<GoalModel> buildRemoteGoals(String sessionId) throws DataException, ConfigurationException, IOException {
        CompositeBundle compositeBundle = new CompositeBundle();
        compositeBundle.consume(ehrService.getGoals(sessionId));
        compositeBundle.consume(buildServiceRequestBasedBPGoals(sessionId));
        return userWorkspaceService.get(sessionId).getVendorTransformer().transformIncomingGoals(compositeBundle.getBundle());
    }

    public Bundle buildServiceRequestBasedBPGoals(String sessionId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Bundle serviceRequestBundle = userWorkspaceService.get(sessionId).getOrderServiceRequests();
        if (serviceRequestBundle != null) {
            for (Bundle.BundleEntryComponent entry : serviceRequestBundle.getEntry()) {
                if (entry.getResource() instanceof ServiceRequest) {
                    ServiceRequest sr = (ServiceRequest) entry.getResource();
                    try {
                        if (sr.hasCode() && FhirUtil.hasCoding(sr.getCode(), fcm.getServiceRequestOrderBPGoalCodings())) {
                            Goal g = buildBPGoal(sessionId, sr);
                            if (logger.isDebugEnabled()) {
                                logger.debug("created Goal: " + FhirUtil.toJson(g));
                            }
                            FhirUtil.appendResourceToBundle(bundle, g);
                        }

                    } catch (Exception e) {
                        logger.error("caught " + e.getClass().getName() + " building BP Goal from ServiceRequest " +
                                sr.getId() + " - " + e.getMessage(), e);
                        logger.debug("ServiceRequest = " + FhirUtil.toJson(sr));
                    }
                }
            }
        }

        return bundle;
    }

    private Goal buildBPGoal(String sessionId, ServiceRequest sr) {
        String gid = "goal-" + DigestUtils.sha256Hex(sr.getId() + salt);

        Goal g = new Goal();
        g.setId(gid);

        g.setLifecycleStatus(Goal.GoalLifecycleStatus.ACTIVE);

        g.setAchievementStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem(GoalModel.ACHIEVEMENT_STATUS_CODING_SYSTEM)
                        .setCode(GoalModel.ACHIEVEMENT_STATUS_CODING_INPROGRESS_CODE)
                )
        );

        Patient p = userWorkspaceService.get(sessionId).getPatient().getSourcePatient();
        g.setSubject(new Reference().setReference(p.getId()));

        g.addAddresses(new Reference().setReference(sr.getId()));

        Pattern systolicPattern = fcm.getServiceRequestOrderBPGoalNoteSystolicRegex();
        int systolic = Integer.parseInt(parseFromNote(sr.getNote(), systolicPattern));
        g.addTarget(new Goal.GoalTargetComponent()
                .setMeasure(new CodeableConcept()
                        .addCoding(fcm.getBpSystolicCommonCoding())
                        .setText("Systolic Goal: " + systolic)
                )
                .setDetail(new Quantity()
                        .setValue(systolic)
                        .setUnit(fcm.getBpValueUnit())
                )
        );

        Pattern diastolicPattern = fcm.getServiceRequestOrderBPGoalNoteDiastolicRegex();
        int diastolic = Integer.parseInt(parseFromNote(sr.getNote(), diastolicPattern));
        g.addTarget(new Goal.GoalTargetComponent()
                .setMeasure(new CodeableConcept()
                        .addCoding(fcm.getBpDiastolicCommonCoding())
                        .setText("Diastolic Goal: " + diastolic)
                )
                .setDetail(new Quantity()
                        .setValue(diastolic)
                        .setUnit(fcm.getBpValueUnit())
                )
        );

        g.setDescription(new CodeableConcept()
                .addCoding(fcm.getBpPanelCommonCoding())
                .setText("BP Goal: " + systolic + "/" + diastolic)
        );

        g.setStart(new DateType()
                .setValue(sr.getAuthoredOn())
        );

        g.setStatusDate(sr.getAuthoredOn());

        return g;
    }

    private String parseFromNote(List<Annotation> note, Pattern p) {
        if (note == null || note.isEmpty() || p == null) return null;

        for (Annotation a : note) {
            if (StringUtils.isNotBlank(a.getText())) {
                Matcher m = p.matcher(a.getText());
                if (m.find()) {
                    return m.group(1);
                }
            }
        }

        return null;
    }

    public List<String> getExtGoalIdList(String sessionId) {
        List<String> list = new ArrayList<>();
        for (MyGoal g : getLocalGoalList(sessionId)) {
            list.add(g.getExtGoalId());
        }
        return list;
    }

    private List<MyGoal> getLocalGoalList(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return repository.findAllByPatId(workspace.getInternalPatientId());
    }

    public MyGoal getLocalGoal(String sessionId, String extGoalId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return repository.findOneByPatIdAndExtGoalId(workspace.getInternalPatientId(), extGoalId);
    }

    /**
     * @param sessionId
     * @return the most recent, current BP goal from either the EHR or from the app, and create an
     * app-based BP goal if no goal exists
     */
    public GoalModel getCurrentBPGoal(String sessionId) {
        // goals can be stored locally and remotely in the EHR.  so get current BP goals from each source, then
        // return whichever is more current, creating a fresh local goal if none could be found.

        GoalModel remoteBPGoal = getCurrentRemoteBPGoal(sessionId);

        MyGoal g = getCurrentLocalBPGoal(sessionId);
        GoalModel localBPGoal = g != null ?
                new GoalModel(g) :
                null;

        if (remoteBPGoal != null && localBPGoal != null) {
            int comparison = remoteBPGoal.getCreatedDate().compareTo(localBPGoal.getCreatedDate());
            if (comparison < 0) return localBPGoal;
            else if (comparison > 0) return remoteBPGoal;
            else {
                // conflicting dates!  default to app goal I guess?
                return localBPGoal;
            }

        } else if (remoteBPGoal != null) {
            return remoteBPGoal;

        } else if (localBPGoal != null) {
            return localBPGoal;

        } else {
            MyGoal defaultBPGoal = create(sessionId, new MyGoal(fcm.getBpPanelCommonCoding(),
                    GoalModel.BP_GOAL_DEFAULT_SYSTOLIC,
                    GoalModel.BP_GOAL_DEFAULT_DIASTOLIC
            ));

            auditService.doAudit(sessionId, AuditSeverity.INFO, "created default BP goal", "id=" + defaultBPGoal.getId() +
                    ", target=" + defaultBPGoal.getSystolicTarget() + "/" + defaultBPGoal.getDiastolicTarget());

            return new GoalModel(defaultBPGoal);
        }
    }

    public MyGoal getCurrentLocalBPGoal(String sessionId) {
        return repository.findCurrentBPGoal(
                userWorkspaceService.get(sessionId).getInternalPatientId()
        );
    }

    // utility function to get the latest BP goal from the EHR
    private GoalModel getCurrentRemoteBPGoal(String sessionId) {
        GoalModel currentRemoteBPGoal = null;

        // Remove goals without a createdDate since they can't accurately be compared to others
        List<GoalModel> goals = userWorkspaceService.get(sessionId).getRemoteGoals().
            stream().filter(g -> g.getCreatedDate() != null).collect(Collectors.toList());
        for (GoalModel goal : goals) {
            if (goal.isRemoteGoal() && goal.isBPGoal()) {
                if (currentRemoteBPGoal == null) {
                    currentRemoteBPGoal = goal;
                } else if (goal.compareTo(currentRemoteBPGoal) < 0) {
                    currentRemoteBPGoal = goal;
                }
            }
        }
        return currentRemoteBPGoal;
    }

    public boolean hasAnyLocalNonBPGoals(String sessionId) {
        for (MyGoal g : getLocalGoalList(sessionId)) {
            if ( ! g.isBloodPressureGoal() ) {
                return true;
            }
        }
        return false;
    }

    public List<MyGoal> getAllLocalNonBPGoals(String sessionId) {
        List<MyGoal> list = getLocalGoalList(sessionId);
        Iterator<MyGoal> iter = list.iterator();
        while (iter.hasNext()) {
            MyGoal g = iter.next();
            if (g.isBloodPressureGoal()) {
                iter.remove();
            }
        }
        return list;
    }

    public MyGoal create(String sessionId, MyGoal goal) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        // todo : implement remote storage

        // AUDIT NOTE : auditing of goal creation is handled by calling functions, no need to recreate that here

        goal.setPatId(workspace.getInternalPatientId());
        goal.setCreatedDate(new Date());

        MyGoal g = repository.save(goal);

        GoalHistory gh = historyRepository.save(new GoalHistory(AchievementStatus.IN_PROGRESS, g));
        Set<GoalHistory> set = new HashSet<>();
        set.add(gh);
        g.setHistory(set);

        return g;
    }

    public MyGoal update(MyGoal goal) {
        return repository.save(goal);
    }

    public void deleteByGoalId(String sessionId, String extGoalId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        repository.deleteByGoalIdForPatient(extGoalId, workspace.getInternalPatientId());
    }

    public void deleteBPGoalIfExists(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        repository.deleteBPGoalForPatient(workspace.getInternalPatientId());
    }

    public GoalHistory createHistory(GoalHistory goalHistory) {
        goalHistory.setCreatedDate(new Date());
        return historyRepository.save(goalHistory);
    }

    public void deleteAll(String sessionId) {
        // todo : uncomment when 'create' remote storage is implemented
//        try {
//            if (storeRemotely) {
//                throw new MethodNotImplementedException("remote delete is not implemented");
//
//            } else {
                UserWorkspace workspace = userWorkspaceService.get(sessionId);
                repository.deleteAllByPatId(workspace.getInternalPatientId());
//            }
//        } catch (Exception e) {
//            logger.error("caught " + e.getClass().getName() + " attempting to delete Goals for session " + sessionId, e);
//        }
    }
}
