package edu.ohsu.cmp.coach.fhir;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.ohsu.cmp.coach.entity.Concept;
import edu.ohsu.cmp.coach.entity.ValueSet;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.fhir.SearchParameterMap;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.opencds.cqf.tooling.terminology.CodeSystemLookupDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class FHIRQuery {
    private static final int MAX_CODES_PER_QUERY = 32;

    private FHIRCredentialsWithClient fcc;

    public FHIRQuery(FHIRCredentialsWithClient fcc) {
        this.fcc = fcc;
    }

    /**
     * adapted from https://github.com/DBCG/cql_engine/blob/8a11ef270d9f54b74d9996441ce76572fe1374ef/engine.fhir/src/main/java/org/opencds/cqf/cql/engine/fhir/retrieve/SearchParamFhirRetrieveProvider.java#L172
     * @param valueSet
     * @return
     */
    public Bundle queryByValueSet(Class<? extends IBaseResource> resource, ValueSet valueSet) {
        List<TokenOrListParam> codeParamLists = buildCodeParams(valueSet);
        List<SearchParameterMap> queries = buildQueries(codeParamLists);
        return executeQueries(resource, queries);
    }

    private Bundle executeQueries(Class<? extends IBaseResource> resource, List<SearchParameterMap> queries) {
        List<Bundle> bundles = new ArrayList<>();
        for (SearchParameterMap map : queries) {
            bundles.add(executeQuery(resource, map));
        }

        Bundle flattenedBundle = new Bundle();
        for (Bundle b : bundles) {
            for (Bundle.BundleEntryComponent item : b.getEntry()) {
                flattenedBundle.addEntry(item);
            }
        }

        return flattenedBundle;
    }

    private Bundle executeQuery(Class<? extends IBaseResource> resource, SearchParameterMap map) {
        IQuery<IBaseBundle> search = fcc.getClient()
                .search()
                .forResource(resource);

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

        return search.returnBundle(Bundle.class)
                .execute();
    }

    /**
     * https://github.com/DBCG/cql_engine/blob/8a11ef270d9f54b74d9996441ce76572fe1374ef/engine.fhir/src/main/java/org/opencds/cqf/cql/engine/fhir/retrieve/SearchParamFhirRetrieveProvider.java#L224
     * @param codeParamLists
     * @return
     */
    private List<SearchParameterMap> buildQueries(List<TokenOrListParam> codeParamLists) {
        Pair<String, List<TokenOrListParam>> codeParams = Pair.of("code", codeParamLists);

        List<SearchParameterMap> queries = new ArrayList<>();
        for (TokenOrListParam tolp : codeParams.getValue()) {
            SearchParameterMap base = new SearchParameterMap();
            base.add(codeParams.getKey(), tolp);
            queries.add(base);
        }

        return queries;
    }

    private List<TokenOrListParam> buildCodeParams(ValueSet valueSet) {
        List<TokenOrListParam> list = new ArrayList<>();

        TokenOrListParam codeParam = null;
        int codeCount = 0;
        for (Concept c : valueSet.getConcepts()) {
            if (codeCount % MAX_CODES_PER_QUERY == 0) {
                if (codeParam != null) {
                    list.add(codeParam);
                }

                codeParam = new TokenOrListParam();
            }

            codeCount ++;
            String codeSystem = CodeSystemLookupDictionary.getUrlFromOid(c.getCodeSystem());
            codeParam.addOr(new TokenParam(codeSystem, c.getCode()));
        }

        if (codeParam != null) {
            list.add(codeParam);
        }

        return list;
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
