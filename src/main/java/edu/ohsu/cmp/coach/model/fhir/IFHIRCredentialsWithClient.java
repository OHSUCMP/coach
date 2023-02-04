package edu.ohsu.cmp.coach.model.fhir;

import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.service.JWTService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Bundle;

import java.io.IOException;

public interface IFHIRCredentialsWithClient {
    public IFHIRCredentials getCredentials();
    public <T extends IBaseResource> T read(Class<T> aClass, String id);
    public Bundle search(String query, int count, String accept);
    public <T extends IDomainResource> T transact(JWTService jwtService, T resource, Integer socketTimeout) throws IOException, ConfigurationException, DataException;
    public Bundle transact(JWTService jwtService, Bundle bundle, boolean stripIfNotInScope, Integer socketTimeout) throws IOException, DataException, ConfigurationException, ScopeException;
}
