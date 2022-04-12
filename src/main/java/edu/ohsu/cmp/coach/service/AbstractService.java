package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.Constants;
import edu.ohsu.cmp.coach.workspace.WorkspaceService;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

public abstract class AbstractService implements Constants {
    @Autowired
    protected WorkspaceService workspaceService;

    @Autowired
    protected FhirConfigManager fcm;

    @Autowired
    protected EHRService ehrService;

    @Autowired
    protected FHIRService fhirService;

    @Value("${fhir.observation.bp-write}")
    protected Boolean storeRemotely;

    protected List<String> buildKeys(Reference reference) {
        return FhirUtil.buildKeys(reference);
    }

    protected List<String> buildKeys(String id, Identifier identifier) {
        return FhirUtil.buildKeys(id, identifier);
    }

    protected List<String> buildKeys(String id, List<Identifier> identifiers) {
        return FhirUtil.buildKeys(id, identifiers);
    }
}
