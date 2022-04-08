package edu.ohsu.cmp.coach.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.FHIRCompatible;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class FhirUtil {
    private static final Logger logger = LoggerFactory.getLogger(FhirUtil.class);

    public static IGenericClient buildClient(String serverUrl, String bearerToken, int timeout) {
        FhirContext ctx = FhirContext.forR4();
        ctx.getRestfulClientFactory().setSocketTimeout(timeout * 1000);
        IGenericClient client = ctx.newRestfulGenericClient(serverUrl);

        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(bearerToken);
        client.registerInterceptor(authInterceptor);

        return client;
    }

    public static List<String> buildKeys(Reference reference) {
        return reference != null ?
                buildKeys(reference.getReference(), reference.getIdentifier()) :
                new ArrayList<>();
    }

    public static List<String> buildKeys(String id, Identifier identifier) {
        List<Identifier> identifiers = identifier != null ?
                Arrays.asList(identifier) :
                null;

        return buildKeys(id, identifiers);
    }

    public static List<String> buildKeys(String id, List<Identifier> identifiers) {
        List<String> list = new ArrayList<>();

        if (id != null) {
            list.add(id);
            String relativeId = FhirUtil.toRelativeReference(id);
            if ( ! id.equals(relativeId) ) {
                list.add(relativeId);
            }
        }

        if (identifiers != null) {
            for (Identifier identifier : identifiers) {
                // if use is specified, only permit USUAL and OFFICIAL.
                if (identifier != null) {
                    if (identifier.hasUse() &&
                            identifier.getUse() != Identifier.IdentifierUse.USUAL &&
                            identifier.getUse() != Identifier.IdentifierUse.OFFICIAL) {
                        continue;
                    }
                    list.add(FhirUtil.toIdentifierString(identifier));
                }
            }
        }

        return list;
    }


    public static Bundle toBundle(String patientId, FhirConfigManager fcm,
                                  Collection<? extends FHIRCompatible> collection) {
        return toBundle(patientId, fcm, null, collection);
    }

    public static Bundle toBundle(String patientId, FhirConfigManager fcm, Bundle.BundleType bundleType,
                                  Collection<? extends FHIRCompatible> collection) {
        if (collection == null) return null;

        Bundle bundle = new Bundle();
        bundle.setType(bundleType);

        for (FHIRCompatible item : collection) {
            bundle.getEntry().addAll(
                    item.toBundle(patientId, fcm).getEntry()
            );
        }

        return bundle;
    }

    public static Bundle bundleResources(Resource ... resources) {
        return bundleResources(Bundle.BundleType.COLLECTION, Arrays.asList(resources));
    }

    public static Bundle bundleResources(Bundle.BundleType bundleType, Resource ... resources) {
        return bundleResources(bundleType, Arrays.asList(resources));
    }

    public static Bundle bundleResources(Collection<? extends Resource> collection) {
        return bundleResources(Bundle.BundleType.COLLECTION, collection);
    }

    public static Bundle bundleResources(Bundle.BundleType bundleType, Collection<? extends Resource> collection) {
        if (collection == null) return null;

        Bundle bundle = new Bundle();
        bundle.setType(bundleType);
        for (Resource r : collection) {
            appendResourceToBundle(bundle, r);
        }
        return bundle;
    }

    public static void appendResourceToBundle(Bundle bundle, Resource resource) {
        String fullUrl = resource.getId().startsWith("http://") || resource.getId().startsWith("https://") ?
                resource.getId() :
                "http://hl7.org/fhir/" + resource.getClass().getSimpleName() + "/" + resource.getId();

        bundle.getEntry().add(new Bundle.BundleEntryComponent().setFullUrl(fullUrl).setResource(resource));
    }

    public static String toIdentifierString(Identifier identifier) {
        List<String> parts = new ArrayList<>();
        if (identifier.hasSystem()) parts.add(identifier.getSystem());
        if (identifier.hasValue()) parts.add(identifier.getValue());
        return StringUtils.join(parts, "|");
    }

    public static String toCodingString(Coding coding) {
        List<String> parts = new ArrayList<>();
        if (coding.hasSystem()) parts.add(coding.getSystem());
        if (coding.hasCode()) parts.add(coding.getCode());
        return StringUtils.join(parts, "|");
    }

    public static String toRelativeReference(String reference) {
        String s = reference;
        // convert https://api.logicahealth.org/htnu18r42/data/Patient/MedicationTest/_history/4 into
        //         Patient/MedicationTest
        // convert Encounter/1146/_history/1 into Encounter/1146

        int suffixPos = s.indexOf("/_history/");
        if (suffixPos > 0) {
            s = s.substring(0, suffixPos);
        }

        if (reference.startsWith("http://") || reference.startsWith("https://")) {
            String[] parts = s.split("\\/");
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];

        } else {
            return s;
        }
    }

    // converts e.g. "Patient/12345" to just "12345"
    public static String extractIdFromReference(String reference) {
        if (reference == null) return null;

        int index = reference.indexOf('/');
        return index >= 0 ?
                reference.substring(index + 1) :
                reference;
    }

    public static boolean bundleContainsReference(Bundle b, Reference reference) {
        if (b == null || reference == null) return false;

        if (reference.hasReference()) {
            return bundleContainsReference(b, reference.getReference());

        } else if (reference.hasIdentifier()) {
            return bundleContainsResourceWithIdentifier(b, reference.getIdentifier());

        } else {
            logger.warn("Reference does not contain reference or identifier!  returning false");
            return false;
        }
    }

    public static boolean bundleContainsReference(Bundle b, String reference) {
        if (reference == null) return false;

        String referenceId = extractIdFromReference(reference);

        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            Resource r = entry.getResource();
            if (r.hasId()) {
                try {
                    if (Pattern.matches("(.*\\/)?" + referenceId + "(\\/.*)?", r.getId())) {
                        logger.debug("matched: '" + r.getId() + "' contains '" + reference + "'");
                        return true;
                    } else {
                        logger.debug("did not match: '" + r.getId() + "' does not contain '" + referenceId + "'");
                    }
                } catch (NullPointerException npe) {
                    logger.error("caught " + npe.getClass().getName() + " matching reference '" + referenceId +
                            "' against id '" + r.getId() + "'", npe);
                    throw npe;
                }
            }
        }
        return false;
    }

    public static boolean bundleContainsResourceWithIdentifier(Bundle b, Identifier identifier) {
        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            if (entry.hasResource()) {
                Resource r = entry.getResource();
                try {
                    Method m = r.getClass().getMethod("getIdentifier");
                    List<Identifier> idList = (List<Identifier>) m.invoke(r);
                    if (idList != null) {
                        for (Identifier id : idList) {
                            if (identifiersMatch(id, identifier)) {
                                return true;
                            }
                        }
                    }

                } catch (Exception e) {
                    logger.error("caught " + e.getClass().getName() + " attempting to get resource from bundle by identifier - " + e.getMessage(), e);
                }
            }
        }
        return false;
    }

    public static <T extends IBaseResource> T getResourceFromBundleByReference(Bundle b, Class<T> aClass, String reference) {
        String referenceId = extractIdFromReference(reference);

        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            Resource r = entry.getResource();
            if (r.getClass().isAssignableFrom(aClass)) {
                if (r.hasId()) {
                    try {
                        if (Pattern.matches("(.*\\/)?" + referenceId + "(\\/.*)?", r.getId())) {
                            return aClass.cast(entry.getResource());
                        }
                    } catch (NullPointerException npe) {
                        logger.error("caught " + npe.getClass().getName() + " matching reference '" + reference +
                                "' against id '" + r.getId() + "'", npe);
                        throw npe;
                    }
                }
            }
        }
        return null;
    }

    public static <T extends IBaseResource> T getResourceFromBundleByIdentifier(Bundle b, Class<T> aClass, Identifier identifier) {
        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            if (entry.hasResource()) {
                Resource r = entry.getResource();
                if (r.getClass().isAssignableFrom(aClass)) {
                    try {
                        Method m = r.getClass().getMethod("getIdentifier");
                        List<Identifier> idList = (List<Identifier>) m.invoke(r);
                        if (idList != null) {
                            for (Identifier id : idList) {
                                if (identifiersMatch(id, identifier)) {
                                    return aClass.cast(r);
                                }
                            }
                        }

                    } catch (Exception e) {
                        logger.error("caught " + e.getClass().getName() + " attempting to get resource from bundle by identifier - " + e.getMessage(), e);
                    }
                }
            }
        }
        return null;
    }

    private static boolean identifiersMatch(Identifier a, Identifier b) {
        boolean useMatch = (!a.hasUse() && !b.hasUse()) ||
                (a.hasUse() && b.hasUse() && a.getUse() == b.getUse());
        boolean systemMatch = (!a.hasSystem() && !b.hasSystem()) ||
                (a.hasSystem() && b.hasSystem() && a.getSystem().equals(b.getSystem()));
        boolean valueMatch = (!a.hasValue() && !b.hasValue()) ||
                (a.hasValue() && b.hasValue() && a.getValue().equals(b.getValue()));

        return useMatch && systemMatch && valueMatch;
    }

    public static Bundle truncate(Bundle bundle, Integer limit) {

        // note: this function doesn't differentiate between resource types in a Bundle, so it
        //       could behave weirdly if the Bundle includes other associated resources (e.g. via _include)
        //       works fine for filtering BP observations, though, which is the initial use case.
        //       we'll cross this bridge if and when we ever come to it

        if (limit == null || bundle.getEntry().size() <= limit) {
            return bundle;
        }

        Bundle truncatedBundle = new Bundle();
        truncatedBundle.setType(Bundle.BundleType.COLLECTION);

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (truncatedBundle.getEntry().size() >= limit) {
                break;
            }
            truncatedBundle.getEntry().add(entry);
        }

        return truncatedBundle;
    }

    public static void writeBundleTOC(Logger logger, Bundle bundle) {
        Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
        int i = 0;
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            logger.info(i + ": " + entry.getResource().getClass().getName() + " (" + entry.getResource().getId() + ")");
            i ++;
        }
    }

    public static String toJson(IBaseResource r) {
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        parser.setPrettyPrint(true);
        return parser.encodeResourceToString(r);
    }
}
