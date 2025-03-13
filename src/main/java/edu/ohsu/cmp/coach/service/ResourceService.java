package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.model.SiteSpecificResource;
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
public class ResourceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${site-specific-resources.pdf-folder.path}")
    private String pathStr;

    private Map<String, SiteSpecificResource> siteSpecificResources = null;

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

    public Collection<SiteSpecificResource> getSiteSpecificResources() {
        return getSiteSpecificResourcesMap().values();
    }

    public boolean siteSpecificResourceExists(String key) {
        return getSiteSpecificResourcesMap().containsKey(key);
    }

    public SiteSpecificResource getSiteSpecificResource(String key) {
        return getSiteSpecificResourcesMap().get(key);
    }


////////////////////////////////////////////////////////////////////
// private methods
//

    private Map<String, SiteSpecificResource> getSiteSpecificResourcesMap() {
        if (siteSpecificResources == null) {
            siteSpecificResources = buildSiteSpecificResources();
        }
        return siteSpecificResources;
    }

    private Map<String, SiteSpecificResource> buildSiteSpecificResources() {
        Map<String, SiteSpecificResource> map = new LinkedHashMap<>();

        if (StringUtils.isNotBlank(pathStr)) {
            File path = new File(pathStr);
            if (path.isDirectory()) {
                File[] files = path.listFiles(PDF_FILTER);
                if (files != null) {
                    logger.info("build site-specific resources -");
                    for (File file : files) {
                        if (file.isFile() && file.canRead()) {
                            SiteSpecificResource resource = new SiteSpecificResource(file);
                            logger.info("adding site-specific resource with key '" + resource.getKey() + "': " +
                                    resource.getFilename());
                            map.put(resource.getKey(), resource);
                        }
                    }
                }
            }
        }

        return map;
    }
}
