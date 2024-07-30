package edu.ohsu.cmp.coach.workspace;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import edu.ohsu.cmp.coach.entity.RandomizationGroup;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.SessionMissingException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.fhir.FhirQueryManager;
import edu.ohsu.cmp.coach.fhir.transform.VendorTransformer;
import edu.ohsu.cmp.coach.model.Audience;
import edu.ohsu.cmp.coach.model.AuditLevel;
import edu.ohsu.cmp.coach.model.MyOmronTokenData;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.service.AuditService;
import edu.ohsu.cmp.coach.service.EHRService;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserWorkspaceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private FhirQueryManager fqm;

    @Autowired
    private FhirConfigManager fcm;

    @Value("${fhir.vendor-transformer-class}")
    private String vendorTransformerClass;

    private final Map<String, UserWorkspace> map;

    public UserWorkspaceService() {
        map = new ConcurrentHashMap<>();
    }

    @Scheduled(cron = "0 0 * * * *") // top of every hour, every day
    public void shutdownExpiredWorkspaces() {
        if (map.isEmpty()) return;

        if (map.size() == 1)    logger.info("checking for expired workspaces (1 workspace registered) -");
        else                    logger.info("checking for expired workspaces (" + map.size() + " workspaces registered) -");

        EHRService ehrService = ctx.getBean(EHRService.class);
        AuditService auditService = ctx.getBean(AuditService.class);

        Iterator<Map.Entry<String, UserWorkspace>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, UserWorkspace> entry = iter.next();
            String sessionId = entry.getKey();
            try {
                // if we can get the Patient resource, the access token is still valid, and the workspace should persist
                Patient p = ehrService.getPatient(sessionId);
                logger.debug("successfully retrieved Patient resource with id=" + p.getId() + " for session=" + sessionId + " - workspace is valid");

            } catch (AuthenticationException ae) {
                logger.info("credentials expired for session " + sessionId + " - shutting down associated workspace -");
                auditService.doAudit(sessionId, AuditLevel.INFO, "session expired", sessionId);
                UserWorkspace workspace = entry.getValue();
                workspace.shutdown();
                iter.remove();

            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " retrieving Patient resource for session=" + sessionId + " - " + e.getMessage(), e);
            }
        }

        if (map.size() == 1)    logger.info("done. (1 workspace remains)");
        else                    logger.info("done. (" + map.size() + " workspaces remain)");
    }

    public boolean exists(String sessionId) {
        return map.containsKey(sessionId);
    }

    public void init(String sessionId, Audience audience, RandomizationGroup randomizationGroup, boolean requiresEnrollment, FHIRCredentialsWithClient fcc) throws ConfigurationException {
        try {
            UserWorkspace workspace = new UserWorkspace(ctx, sessionId, audience, randomizationGroup, requiresEnrollment, fcc, fqm, fcm);
            workspace.setVendorTransformer(buildVendorTransformer(workspace));
            map.put(sessionId, workspace);

        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public UserWorkspace get(String sessionId) throws SessionMissingException {
        if (map.containsKey(sessionId)) {
            return map.get(sessionId);

        } else {
            throw new SessionMissingException(sessionId);
        }
    }

    public UserWorkspace getByOmronUserId(String omronUserId) throws SessionMissingException {
        for (UserWorkspace workspace : map.values()) {
            MyOmronTokenData tokenData = workspace.getOmronTokenData();
            if (tokenData != null && StringUtils.equals(tokenData.getUserIdToken(), omronUserId)) {
                return workspace;
            }
        }
        throw new SessionMissingException("no session found for Omron User with id=" + omronUserId);
    }

    public boolean shutdown(String sessionId) {
        if (map.containsKey(sessionId)) {
            UserWorkspace workspace = map.remove(sessionId);
            workspace.shutdown();
            return true;
        }
        return false;
    }

    private VendorTransformer buildVendorTransformer(UserWorkspace workspace) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (VendorTransformer) Class.forName(vendorTransformerClass)
                    .getDeclaredConstructor(UserWorkspace.class)
                    .newInstance(workspace);
    }
}
