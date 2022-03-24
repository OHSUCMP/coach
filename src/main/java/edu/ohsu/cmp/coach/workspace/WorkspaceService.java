package edu.ohsu.cmp.coach.workspace;

import edu.ohsu.cmp.coach.exception.SessionMissingException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.service.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WorkspaceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int POOL_SIZE = 5;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private FhirConfigManager fcm;

    @Autowired
    private PatientService patientService;

    private final Map<String, UserWorkspace> map;
    private ExecutorService executorService;

    public WorkspaceService() {
        map = new ConcurrentHashMap<>();
        executorService = Executors.newFixedThreadPool(POOL_SIZE);
    }

    public boolean exists(String sessionId) {
        return map.containsKey(sessionId);
    }

    public void init(String sessionId, Audience audience, FHIRCredentialsWithClient fcc) {
        Long internalPatientId = patientService.getInternalPatientId(fcc.getCredentials().getPatientId());
        UserWorkspace userWorkspace = new UserWorkspace(ctx, sessionId, audience, fcc, fcm, internalPatientId);
        map.put(sessionId, userWorkspace);
    }

    public void populate(String sessionId, boolean runInNewThread) throws SessionMissingException {
        if (runInNewThread) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    doPopulate(sessionId);
                }
            });

        } else {
            doPopulate(sessionId);
        }
    }

    private void doPopulate(String sessionId) throws SessionMissingException {
        UserWorkspace workspace = get(sessionId);

        logger.info("BEGIN populating workspace for session=" + sessionId);
        workspace.populate();
        logger.info("DONE populating workspace for session=" + sessionId);
    }

    public UserWorkspace get(String sessionId) throws SessionMissingException {
        if (map.containsKey(sessionId)) {
            return map.get(sessionId);

        } else {
            throw new SessionMissingException(sessionId);
        }
    }

    public boolean shutdown(String sessionId) {
        if (map.containsKey(sessionId)) {
            UserWorkspace workspace = map.remove(sessionId);
            workspace.shutdown();
            return true;
        }
        return false;
    }
}
