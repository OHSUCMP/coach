package edu.ohsu.cmp.coach.cqfruler;

import edu.ohsu.cmp.coach.cqfruler.model.CDSHook;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class CQFRulerService {
    private static final boolean TESTING = false; // true: use hard-coded 'canned' responses from CQF Ruler (fast, cheap)
                                                  // false: make CQF Ruler calls (slow, expensive)

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String cdsHooksEndpointURL;
    private Boolean showDevErrors;
    private List<String> cdsHookOrder;

    @Autowired private EHRService ehrService;
    @Autowired private BloodPressureService bpService;
    @Autowired private GoalService goalService;
    @Autowired private CounselingService counselingService;
    @Autowired private FhirConfigManager fcm;

    public CQFRulerService(@Value("${cqfruler.cdshooks.endpoint.url}") String cdsHooksEndpointURL,
                           @Value("#{new Boolean('${security.show-dev-errors}')}") Boolean showDevErrors,
                           @Value("${cqfruler.cdshooks.order.csv}") String cdsHookOrder) {
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
        this.showDevErrors = showDevErrors;
        this.cdsHookOrder = Arrays.asList(cdsHookOrder.split("\\s*,\\s*"));
    }

    public void requestHooksExecution(String sessionId) {
        try {
            CDSHookExecutor executor = new CDSHookExecutor(TESTING, showDevErrors, sessionId, cdsHooksEndpointURL,
                    ehrService,
                    bpService,
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
        Map<String, CDSHook> map = new LinkedHashMap<>();
        for (CDSHook cdsHook : CDSHooksUtil.getCDSHooks(TESTING, cdsHooksEndpointURL)) {
            map.put(cdsHook.getId(), cdsHook);
        }

        List<CDSHook> list = new ArrayList<>();
        for (String hookId : cdsHookOrder) {
            CDSHook cdsHook = map.remove(hookId);
            if (cdsHook != null) {
                list.add(cdsHook);
            }
        }

        list.addAll(map.values());

        return list;
    }
}
