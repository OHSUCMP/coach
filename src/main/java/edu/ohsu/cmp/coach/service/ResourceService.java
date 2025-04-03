package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.model.SiteSpecificResource;
import edu.ohsu.cmp.coach.model.redcap.RandomizationGroup;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ResourceService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${site-specific-resources.pdf-folder.path}")
    private String pathStr;

    private Map<RandomizationGroup, Map<String, SiteSpecificResource>> siteSpecificResources = null;

    private static final FilenameFilter PDF_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".pdf");
        }
    };

    public void clear() {
        if (siteSpecificResources != null) {
            siteSpecificResources.clear();
            siteSpecificResources = null;
        }
    }

    public Collection<SiteSpecificResource> getSiteSpecificResources(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return getSiteSpecificResourcesMap(workspace.getActiveRandomizationGroup()).values();
    }

    public boolean siteSpecificResourceExists(String sessionId, String key) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return getSiteSpecificResourcesMap(workspace.getActiveRandomizationGroup()).containsKey(key);
    }

    public SiteSpecificResource getSiteSpecificResource(String sessionId, String key) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        return getSiteSpecificResourcesMap(workspace.getActiveRandomizationGroup()).get(key);
    }


////////////////////////////////////////////////////////////////////
// private methods
//

    private Map<String, SiteSpecificResource> getSiteSpecificResourcesMap(RandomizationGroup randomizationGroup) {
        if (siteSpecificResources == null) {
            siteSpecificResources = new LinkedHashMap<>();
        }

        if ( ! siteSpecificResources.containsKey(randomizationGroup) ) {
            siteSpecificResources.put(randomizationGroup, buildSiteSpecificResources(randomizationGroup));
        }

        return siteSpecificResources.get(randomizationGroup);
    }

    private Map<String, SiteSpecificResource> buildSiteSpecificResources(RandomizationGroup randomizationGroup) {
        Map<String, SiteSpecificResource> map = new LinkedHashMap<>();

        if (StringUtils.isNotBlank(pathStr)) {
            String randomizationGroupPath = switch (randomizationGroup) {
                case BASIC -> "control";
                case ENHANCED -> "intervention";
            };

            File path = pathStr.endsWith(File.separator) ?
                    new File(pathStr + randomizationGroupPath) :
                    new File(pathStr + File.separator + randomizationGroupPath);

            if (path.isDirectory()) {
                File[] files = path.listFiles(PDF_FILTER);
                if (files != null) {
                    logger.info("build site-specific " + randomizationGroup + " resources from " + path.getAbsolutePath() + " -");
                    for (File file : files) {
                        if (file.isFile() && file.canRead()) {
                            SiteSpecificResource resource = new SiteSpecificResource(file);
                            logger.info("adding site-specific " + randomizationGroup + " resource with key '" +
                                    resource.getKey() + "': " + resource.getFilename());
                            map.put(resource.getKey(), resource);
                        }
                    }
                }

            } else {
                logger.warn("directory not found: " + path.getAbsolutePath());
            }
        }

        return map;
    }
}
