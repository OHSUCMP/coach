package edu.ohsu.cmp.htnu18app.cqfruler;

import edu.ohsu.cmp.htnu18app.cqfruler.model.CDSHook;
import edu.ohsu.cmp.htnu18app.service.CounselingService;
import edu.ohsu.cmp.htnu18app.service.GoalService;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import edu.ohsu.cmp.htnu18app.service.PatientService;
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

    @Autowired
    private PatientService patientService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private CounselingService counselingService;

    public CQFRulerService(@Value("${cqfruler.cdshooks.endpoint.url}") String cdsHooksEndpointURL) {
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
    }

    public void requestHooksExecution(String sessionId) {
        try {
            CDSHookExecutor executor = new CDSHookExecutor(TESTING, sessionId, cdsHooksEndpointURL,
                    patientService,
                    hbprService,
                    goalService,
                    counselingService);

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
