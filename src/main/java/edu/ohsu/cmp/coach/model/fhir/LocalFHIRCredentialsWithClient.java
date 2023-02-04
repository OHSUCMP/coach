package edu.ohsu.cmp.coach.model.fhir;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.MethodNotImplementedException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.service.JWTService;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Bundle;

import java.io.IOException;

public class LocalFHIRCredentialsWithClient implements IFHIRCredentialsWithClient {

    private LocalFHIRCredentials credentials;

    private final Bundle patientDataBundle;

    public LocalFHIRCredentialsWithClient(LocalFHIRCredentials credentials, Bundle patientDataBundle) {
        this.credentials = credentials;
        this.patientDataBundle = patientDataBundle;
    }

    @Override
    public IFHIRCredentials getCredentials() {
        return credentials;
    }

    @Override
    public <T extends IBaseResource> T read(Class<T> aClass, String id) {
        return FhirUtil.getResourceFromBundleById(patientDataBundle, aClass, id);
    }

    @Override
    public Bundle search(String query, int count, String accept) {
        return FhirUtil.getResourceListMatchingCriteria(patientDataBundle, query);
    }

    @Override
    public <T extends IDomainResource> T transact(JWTService jwtService, T resource, Integer socketTimeout) throws IOException, ConfigurationException, DataException {
        throw new MethodNotImplementedException("transact unsupported in local context");
    }

    @Override
    public Bundle transact(JWTService jwtService, Bundle bundle, boolean stripIfNotInScope, Integer socketTimeout) throws IOException, DataException, ConfigurationException, ScopeException {
        throw new MethodNotImplementedException("transact unsupported in local context");
    }
}
