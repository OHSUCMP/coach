package edu.ohsu.cmp.coach.cqfruler;

import edu.ohsu.cmp.coach.cqfruler.model.CDSHook;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.service.CounselingService;
import edu.ohsu.cmp.coach.service.EHRService;
import edu.ohsu.cmp.coach.service.GoalService;
import edu.ohsu.cmp.coach.service.HomeBloodPressureReadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class CQFRulerService {
    private static final boolean TESTING = false; // true: use hard-coded 'canned' responses from CQF Ruler (fast, cheap)
                                                  // false: make CQF Ruler calls (slow, expensive)

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String cdsHooksEndpointURL;

    @Autowired private EHRService ehrService;
    @Autowired private HomeBloodPressureReadingService hbprService;
    @Autowired private GoalService goalService;
    @Autowired private CounselingService counselingService;
    @Autowired private FhirConfigManager fcm;

    public CQFRulerService(@Value("${cqfruler.cdshooks.endpoint.url}") String cdsHooksEndpointURL) {
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
    }

    public void requestHooksExecution(String sessionId) {
        try {
            CDSHookExecutor executor = new CDSHookExecutor(TESTING, sessionId, cdsHooksEndpointURL,
                    ehrService,
                    hbprService,
                    goalService,
                    counselingService,
                    fcm);

            logger.info("created " + executor);

            CDSHookExecutorService.getInstance().queue(executor);

        } catch (InterruptedException ie) {
            logger.error("caught " + ie.getClass().getName() + " attempting to execute hooks for session " + sessionId, ie);
        }
    }

    public int getQueuePosition(String sessionId) {
        return CDSHookExecutorService.getInstance().getPosition(sessionId);
    }

    public List<CDSHook> getCDSHooks() throws IOException {
        return CDSHooksUtil.getCDSHooks(TESTING, cdsHooksEndpointURL);
    }
}
