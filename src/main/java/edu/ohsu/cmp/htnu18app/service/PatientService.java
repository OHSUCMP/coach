package edu.ohsu.cmp.htnu18app.service;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.vsac.Concept;
import edu.ohsu.cmp.htnu18app.entity.vsac.ValueSet;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.MedicationModel;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.model.fhir.SearchParameterMap;
import edu.ohsu.cmp.htnu18app.repository.app.PatientRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.tooling.terminology.CodeSystemLookupDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Service
public class PatientService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int MAX_CODES_PER_QUERY = 32; // todo: auto-identify this, or at least put it in the config

    @Autowired
    private PatientRepository repository;

    @Autowired
    private ValueSetService valueSetService;

//    @Autowired
//    private TerminologySystemService terminologySystemService;

    public Patient getPatient(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Patient p = cache.getPatient();
        if (p == null) {
            logger.info("requesting Patient data for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            p = fcc.getClient()
                    .read()
                    .resource(Patient.class)
                    .withId(fcc.getCredentials().getPatientId())
                    .execute();
            cache.setPatient(p);
        }
        return p;
    }

    public Long getInternalPatientId(String fhirPatientId) {
        String patIdHash = buildPatIdHash(fhirPatientId);

        edu.ohsu.cmp.htnu18app.entity.app.Patient p;
        if (repository.existsPatientByPatIdHash(patIdHash)) {
            p = repository.findOneByPatIdHash(patIdHash);

        } else {
            p = new edu.ohsu.cmp.htnu18app.entity.app.Patient(patIdHash);
            p = repository.save(p);
        }

        return p.getId();
    }

    private String buildPatIdHash(String patientId) {
        return DigestUtils.sha256Hex(patientId);
    }

    public Bundle getBloodPressureObservations(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Bundle b = cache.getBloodPressureObservations();
        if (b == null) {
            logger.info("requesting Blood Pressure Observations for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = fcc.getClient()
                    .search()
                    .forResource(Observation.class)
                    .and(Observation.PATIENT.hasId(fcc.getCredentials().getPatientId()))
                    .and(Observation.CODE.exactly().systemAndCode(BloodPressureModel.SYSTEM, BloodPressureModel.CODE))
                    .returnBundle(Bundle.class)
                    .execute();
            cache.setBloodPressureObservations(b);
        }
        return b;
    }

    @Transactional
    public Bundle getMedicationStatements(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);

        Bundle bundle = cache.getMedicationStatements();
        if (bundle == null) {
            logger.info("requesting MedicationStatements for session " + sessionId);

            ValueSet valueSet = valueSetService.getValueSet(MedicationModel.VALUE_SET_OID);

/////////////////////////////
// adapted from https://github.com/DBCG/cql_engine/blob/8a11ef270d9f54b74d9996441ce76572fe1374ef/engine.fhir/src/main/java/org/opencds/cqf/cql/engine/fhir/retrieve/SearchParamFhirRetrieveProvider.java#L172
//
            List<TokenOrListParam> codeParamLists = new ArrayList<>();

            TokenOrListParam codeParam = null;
            int codeCount = 0;
            for (Concept c : valueSet.getConcepts()) {
                if (codeCount % MAX_CODES_PER_QUERY == 0) {
                    if (codeParam != null) {
                        codeParamLists.add(codeParam);
                    }

                    codeParam = new TokenOrListParam();
                }

                if (c.getCode().equalsIgnoreCase("1798281")) {
                    logger.info("here");
                }

                codeCount ++;
                String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
                codeParam.addOr(new TokenParam(codeSystem, c.getCode()));
            }

            if (codeParam != null) {
                codeParamLists.add(codeParam);
            }

///////////////////////////

            Pair<String, List<TokenOrListParam>> codeParams = Pair.of("code", codeParamLists);

///////////////////////////
// https://github.com/DBCG/cql_engine/blob/8a11ef270d9f54b74d9996441ce76572fe1374ef/engine.fhir/src/main/java/org/opencds/cqf/cql/engine/fhir/retrieve/SearchParamFhirRetrieveProvider.java#L224
//

            List<SearchParameterMap> queries = new ArrayList<>();
            for (TokenOrListParam tolp : codeParams.getValue()) {
                SearchParameterMap base = new SearchParameterMap();
                base.add(codeParams.getKey(), tolp);
                queries.add(base);
            }

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();

            List<Bundle> bundles = new ArrayList<>();
            for (SearchParameterMap map : queries) {

///////////////////////

                IQuery<IBaseBundle> search = fcc.getClient()
                        .search()
                        .forResource(MedicationStatement.class);

                for (Map.Entry<String, List<List<IQueryParameterType>>> entry : map.entrySet()) {
                    String name = entry.getKey();

                    List<List<IQueryParameterType>> value = entry.getValue();
                    if (value == null || value.size() == 0) {
                        continue;
                    }

                    for (List<IQueryParameterType> subList : value) {
                        TokenClientParam tcp = new TokenClientParam(name);
                        IBaseCoding[] codings = toCodings(fcc.getClient(), subList);
                        ICriterion<?> criterion = tcp.exactly().codings(codings);
                        search = search.where(criterion);
                    }
                }

                Bundle b = search.returnBundle(Bundle.class)
                        .execute();

///////////////////////

                bundles.add(b);
            }

            bundle = new Bundle();
            for (Bundle b : bundles) {
                for (Bundle.BundleEntryComponent item : b.getEntry()) {
                    bundle.addEntry(item);
                }
            }

            cache.setMedicationStatements(bundle);
        }

        return bundle;
    }

    private IBaseCoding[] toCodings(IGenericClient fhirClient, List<IQueryParameterType> codingList) {
        List<IBaseCoding> codings = new ArrayList<>();

        BiFunction<String, String, IBaseCoding> codingConverter;

        switch (fhirClient.getFhirContext().getVersion().getVersion()) {
            case DSTU3:
                codingConverter = (system, code) -> new org.hl7.fhir.dstu3.model.Coding(system, code, null);
                break;
            case R4:
                codingConverter = (system, code) -> new org.hl7.fhir.r4.model.Coding(system, code, null);
                break;
            default:
                throw new IllegalArgumentException("Unhandled FHIR version");
        }

        for (IQueryParameterType param : codingList) {
            TokenParam token = (TokenParam) param;

            IBaseCoding coding = codingConverter.apply(token.getSystem(), token.getValue());

            codings.add(coding);
        }

        return codings.toArray(new IBaseCoding[codings.size()]);
    }
}
