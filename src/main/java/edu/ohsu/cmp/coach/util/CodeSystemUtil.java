package edu.ohsu.cmp.coach.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// adapted from https://github.com/cqframework/cqf-tooling/blob/master/tooling/src/main/java/org/opencds/cqf/tooling/terminology/CodeSystemLookupDictionary.java

public class CodeSystemUtil {
    private static final List<CodeSystemInfo> CODE_SYSTEMS = List.of(
            new CodeSystemInfo("2.16.840.1.113883.5.4", "ActCode", "http://terminology.hl7.org/CodeSystem/v3-ActCode"),
            new CodeSystemInfo("2.16.840.1.113883.5.1001", "ActMood", "http://terminology.hl7.org/CodeSystem/v3-ActMood"),
            new CodeSystemInfo("2.16.840.1.113883.5.7", "ActPriority", "http://terminology.hl7.org/CodeSystem/v3-ActPriority"),
            new CodeSystemInfo("2.16.840.1.113883.5.8", "ActReason", "http://terminology.hl7.org/CodeSystem/v3-ActReason"),
            new CodeSystemInfo("2.16.840.1.113883.5.1002", "ActRelationshipType", "http://terminology.hl7.org/CodeSystem/v3-ActRelationshipType"),
            new CodeSystemInfo("2.16.840.1.113883.5.14", "ActStatus", "http://terminology.hl7.org/CodeSystem/v3-ActStatus"),
            new CodeSystemInfo("2.16.840.1.113883.5.1119", "AddressUse", "http://terminology.hl7.org/CodeSystem/v3-AddressUse"),
            new CodeSystemInfo("2.16.840.1.113883.5.1", "AdministrativeGender", "http://terminology.hl7.org/CodeSystem/v3-AdministrativeGender"),
            new CodeSystemInfo("2.16.840.1.113883.18.2", "AdministrativeSex", "http://terminology.hl7.org/CodeSystem/v2-0001"),
            new CodeSystemInfo("2.16.840.1.113883.6.12", "CPT", "http://www.ama-assn.org/go/cpt"),
            new CodeSystemInfo("2.16.840.1.113883.6.12", "CPT-CAT-II", "http://www.ama-assn.org/go/cpt"),
            new CodeSystemInfo("2.16.840.1.113883.12.292", "CVX", "http://hl7.org/fhir/sid/cvx"),
            new CodeSystemInfo("2.16.840.1.113883.5.25", "Confidentiality", "http://terminology.hl7.org/CodeSystem/v3-Confidentiality"),
            new CodeSystemInfo("2.16.840.1.113883.12.112", "DischargeDisposition", "urn:oid:2.16.840.1.113883.12.112"),
            new CodeSystemInfo("2.16.840.1.113883.4.642.1.1093", "DischargeDisposition", "http://terminology.hl7.org/CodeSystem/discharge-disposition"),
            new CodeSystemInfo("2.16.840.1.113883.5.43", "EntityNamePartQualifier", "http://terminology.hl7.org/CodeSystem/v3-EntityNamePartQualifier"),
            new CodeSystemInfo("2.16.840.1.113883.5.45", "EntityNameUse", "http://terminology.hl7.org/CodeSystem/v3-EntityNameUse"),
            new CodeSystemInfo("2.16.840.1.113883.6.14", "HCPCS", "http://terminology.hl7.org/CodeSystem/HCPCS"),
            new CodeSystemInfo("2.16.840.1.113883.6.14", "HCPCS Level I: CPT", "http://terminology.hl7.org/CodeSystem/HCPCS"),
            new CodeSystemInfo("2.16.840.1.113883.6.259", "HSLOC", "https://www.cdc.gov/nhsn/cdaportal/terminology/codesystem/hsloc.html"),
            new CodeSystemInfo("2.16.840.1.113883.6.285", "HCPCS Level II", "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets"),
            new CodeSystemInfo("2.16.840.1.113883.6.3", "ICD10", "http://terminology.hl7.org/CodeSystem/icd10"),
            new CodeSystemInfo("2.16.840.1.113883.6.4", "ICD10PCS", "http://www.cms.gov/Medicare/Coding/ICD10"),
            new CodeSystemInfo("2.16.840.1.113883.6.90", "ICD10CM", "http://hl7.org/fhir/sid/icd-10-cm"),
            new CodeSystemInfo("2.16.840.1.113883.6.42", "ICD9", "http://terminology.hl7.org/CodeSystem/icd9"),
            new CodeSystemInfo("2.16.840.1.113883.6.2", "ICD9CM", "http://terminology.hl7.org/CodeSystem/icd9cm"),
            new CodeSystemInfo("2.16.840.1.113883.6.104", "ICD9PCS", "urn:oid:2.16.840.1.113883.6.104"),
            new CodeSystemInfo("2.16.840.1.113883.6.1", "LOINC", "http://loinc.org"),
            new CodeSystemInfo("2.16.840.1.113883.5.60", "LanguageAbilityMode", "http://terminology.hl7.org/CodeSystem/v3-LanguageAbilityMode"),
            new CodeSystemInfo("2.16.840.1.113883.5.61", "LanguageAbilityProficiency", "http://terminology.hl7.org/CodeSystem/v3-LanguageAbilityProficiency"),
            new CodeSystemInfo("2.16.840.1.113883.5.63", "LivingArrangement", "http://terminology.hl7.org/CodeSystem/v3-LivingArrangement"),
            new CodeSystemInfo("2.16.840.1.113883.5.2", "MaritalStatus", "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus"),
            new CodeSystemInfo("2.16.840.1.113883.6.69", "NDC", "http://hl7.org/fhir/sid/ndc"),
            new CodeSystemInfo("2.16.840.1.113883.3.26.1.1", "NCI", "http://ncithesaurus-stage.nci.nih.gov"),
            new CodeSystemInfo("2.16.840.1.113883.3.26.1.5", "NDFRT", "http://terminology.hl7.org/CodeSystem/nciVersionOfNDF-RT"),
            new CodeSystemInfo("2.16.840.1.113883.6.101", "NUCCPT", "http://nucc.org/provider-taxonomy"),
            new CodeSystemInfo("2.16.840.1.113883.6.101", "Provider Taxonomy", "http://nucc.org/provider-taxonomy"),
            new CodeSystemInfo("2.16.840.1.113883.6.301.11", "PresentOnAdmission", "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/HospitalAcqCond/Coding"),
            new CodeSystemInfo("2.16.840.1.113883.5.1008", "NullFlavor", "http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
            new CodeSystemInfo("2.16.840.1.113883.5.83", "ObservationInterpretation", "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation"),
            new CodeSystemInfo("2.16.840.1.113883.5.1063", "ObservationValue", "http://terminology.hl7.org/CodeSystem/v3-ObservationValue"),
            new CodeSystemInfo("2.16.840.1.113883.5.88", "ParticipationFunction", "http://terminology.hl7.org/CodeSystem/v3-ParticipationFunction"),
            new CodeSystemInfo("2.16.840.1.113883.5.1064", "ParticipationMode", "http://terminology.hl7.org/CodeSystem/v3-ParticipationMode"),
            new CodeSystemInfo("2.16.840.1.113883.5.90", "ParticipationType", "http://terminology.hl7.org/CodeSystem/v3-ParticipationType"),
            new CodeSystemInfo("2.16.840.1.113883.6.88", "RXNORM", "http://www.nlm.nih.gov/research/umls/rxnorm"),
            new CodeSystemInfo("2.16.840.1.113883.5.1076", "ReligiousAffiliation", "http://terminology.hl7.org/CodeSystem/v3-ReligiousAffiliation"),
            new CodeSystemInfo("2.16.840.1.113883.5.110", "RoleClass", "http://terminology.hl7.org/CodeSystem/v3-RoleClass"),
            new CodeSystemInfo("2.16.840.1.113883.5.111", "RoleCode", "http://terminology.hl7.org/CodeSystem/v3-RoleCode"),
            new CodeSystemInfo("2.16.840.1.113883.5.1068", "RoleStatus", "http://terminology.hl7.org/CodeSystem/v3-RoleStatus"),
            new CodeSystemInfo("2.16.840.1.113883.6.96", "SNOMEDCT", "http://snomed.info/sct"),
            new CodeSystemInfo("2.16.840.1.113883.6.96", "SNOMED CT US Edition", "http://snomed.info/sct"),
            new CodeSystemInfo("2.16.840.1.113883.6.21", "UBREV", "http://terminology.hl7.org/CodeSystem/nubc-UB92"),
            new CodeSystemInfo("2.16.840.1.113883.6.21", "UBTOB", "http://terminology.hl7.org/CodeSystem/nubc-UB92"),
            new CodeSystemInfo("2.16.840.1.113883.6.301.3", "v2-0456", "http://terminology.hl7.org/CodeSystem/v2-0456"),
            new CodeSystemInfo("2.16.840.1.113883.6.50", "POS", "http://terminology.hl7.org/CodeSystem/POS"),
            new CodeSystemInfo("2.16.840.1.113883.6.238", "CDCREC", "urn:oid:2.16.840.1.113883.6.238"),
            new CodeSystemInfo("2.16.840.1.113883.6.12", "Modifier", "http://www.ama-assn.org/go/cpt"),
            new CodeSystemInfo("2.16.840.1.113883.6.13", "CDT", "http://terminology.hl7.org/CodeSystem/CD2"),
            new CodeSystemInfo("2.16.840.1.113883.5.79", "mediaType", "http://terminology.hl7.org/CodeSystem/v3-mediatypes"),
            new CodeSystemInfo("2.16.840.1.113883.3.221.5", "SOP", "urn:oid:2.16.840.1.113883.3.221.5"),
            new CodeSystemInfo("1.3.6.1.4.1.12009.10.3.1", "UCUM", "urn:oid:1.3.6.1.4.1.12009.10.3.1"),
            new CodeSystemInfo("2.16.840.1.113883.6.8", "UCUM", "http://unitsofmeasure.org"),
            new CodeSystemInfo("2.16.840.1.113883.6.86", "UMLS", "http://terminology.hl7.org/CodeSystem/umls")
    );

    private static final class CodeSystemInfo {
        private String oid;
        private String name;
        private String url;

        public CodeSystemInfo(String oid, String name, String url) {
            this.oid = oid;
            this.name = name;
            this.url = url;
        }

        public String getOid() {
            return oid;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }

    private static final Map<String, List<CodeSystemInfo>> OID_MAP = new HashMap<>();
    static {
        for (CodeSystemInfo codeSystemInfo : CODE_SYSTEMS) {
            if ( ! OID_MAP.containsKey(codeSystemInfo.getOid()) ) {
                OID_MAP.put(codeSystemInfo.getOid(), new ArrayList<>());
            }
            OID_MAP.get(codeSystemInfo.getOid()).add(codeSystemInfo);
        }
    }

    private static final Map<String, List<CodeSystemInfo>> NAME_MAP = new HashMap<>();
    static {
        for (CodeSystemInfo codeSystemInfo : CODE_SYSTEMS) {
            if ( ! NAME_MAP.containsKey(codeSystemInfo.getName()) ) {
                NAME_MAP.put(codeSystemInfo.getName(), new ArrayList<>());
            }
            NAME_MAP.get(codeSystemInfo.getName()).add(codeSystemInfo);
        }
    }

    private static final Map<String, List<CodeSystemInfo>> URL_MAP = new HashMap<>();
    static {
        for (CodeSystemInfo codeSystemInfo : CODE_SYSTEMS) {
            if ( ! URL_MAP.containsKey(codeSystemInfo.getUrl()) ) {
                URL_MAP.put(codeSystemInfo.getUrl(), new ArrayList<>());
            }
            URL_MAP.get(codeSystemInfo.getUrl()).add(codeSystemInfo);
        }
    }

    public static boolean matches(String system1, String system2) {
        List<CodeSystemInfo> system1list = getCodeSystems(system1);
        List<CodeSystemInfo> system2list = getCodeSystems(system2);

        if (system1list == null || system2list == null) {
            return false;

        } else if (system1list.size() == 1 && system2list.size() == 1 && system1list.get(0) == system2list.get(0)) {
            return true;
        }

        for (CodeSystemInfo csi : system1list) {
            if (system2list.contains(csi)) {
                return true;
            }
        }

        for (CodeSystemInfo csi : system2list) {
            if (system1list.contains(csi)) {
                return true;
            }
        }

        return false;
    }

    public static String getOid(String system) {
        if (system == null) return null;
        List<CodeSystemInfo> codeSystems = getCodeSystems(system);
        return codeSystems != null && ! codeSystems.isEmpty() ?
                codeSystems.get(0).getOid() :
                null;
    }

    public static String getUrl(String system) {
        if (system == null) return null;
        List<CodeSystemInfo> codeSystems = getCodeSystems(system);
        return codeSystems != null && ! codeSystems.isEmpty() ?
                codeSystems.get(0).getUrl() :
                null;
    }

    private static final Pattern OID_PATTERN = Pattern.compile("^[0-9.]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://.*)|(urn:oid:[0-9.]+)$");

    private static List<CodeSystemInfo> getCodeSystems(String system) {
        if (OID_PATTERN.matcher(system).matches()) {
            return OID_MAP.get(system);
        } else if (URL_PATTERN.matcher(system).matches()) {
            return URL_MAP.get(system);
        } else {
            return NAME_MAP.get(system);
        }
    }
}
