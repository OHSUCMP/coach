package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.Concept;
import edu.ohsu.cmp.coach.entity.ValueSet;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.MyHttpException;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.xml.SimpleXMLDOM;
import edu.ohsu.cmp.coach.model.xml.SimpleXMLElement;
import edu.ohsu.cmp.coach.util.UUIDUtil;
import org.apache.commons.codec.EncoderException;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to assist with pulling information from NLM VSAC API
 * Implements the API documented at https://www.nlm.nih.gov/vsac/support/usingvsac/vsacsvsapiv2.html
 */
@Service
public class VSACService {
    private static final String API_KEY_URL = "https://utslogin.nlm.nih.gov/cas/v1/api-key";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Pattern SERVICE_TICKET_URI_PATTERN = Pattern.compile(".*<form.+action=\"(.+?)\".*");


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${vsac.api-key}")
    private String apiKey;

    private String serviceTicketURI = null;

    public boolean isVSACEnabled() {
        return UUIDUtil.isUUID(apiKey);
    }

    public ValueSet getValueSet(String oid) throws IOException, ParserConfigurationException, SAXException, ParseException, DataException, EncoderException {
        if ( ! isVSACEnabled() ) {
            logger.warn("VSAC is not enabled - not getting ValueSet with oid=" + oid);
            return null;
        }

        String xml = getRawValueSet(oid);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        SimpleXMLDOM dom = new SimpleXMLDOM(builder.parse(in));

        SimpleXMLElement xmlValueSet = dom.getRootElement().findFirst("ns0:DescribedValueSet");
        List<SimpleXMLElement> xmlConcepts = xmlValueSet.findAll("ns0:ConceptList/ns0:Concept");

        Set<Concept> concepts = new LinkedHashSet<>();
        for (SimpleXMLElement xmlConcept : xmlConcepts) {
            Concept c = new Concept(
                    xmlConcept.getAttribute("code"),
                    xmlConcept.getAttribute("codeSystem"),
                    xmlConcept.getAttribute("codeSystemName"),
                    xmlConcept.getAttribute("codeSystemVersion"),
                    xmlConcept.getAttribute("displayName")
            );
            concepts.add(c);
        }

        String source = xmlValueSet.hasChildren("ns0:Source") ?
                xmlValueSet.getChildren("ns0:Source").get(0).getText() :
                null;

        String purpose = xmlValueSet.hasChildren("ns0:Purpose") ?
                xmlValueSet.getChildren("ns0:Purpose").get(0).getText() :
                null;

        String type = xmlValueSet.hasChildren("ns0:Type") ?
                xmlValueSet.getChildren("ns0:Type").get(0).getText() :
                null;

        String binding = xmlValueSet.hasChildren("ns0:Binding") ?
                xmlValueSet.getChildren("ns0:Binding").get(0).getText() :
                null;

        String status = xmlValueSet.hasChildren("ns0:Status") ?
                xmlValueSet.getChildren("ns0:Status").get(0).getText() :
                null;

        Date revisionDate = xmlValueSet.hasChildren("ns0:RevisionDate") ?
                DATE_FORMAT.parse(xmlValueSet.getChildren("ns0:RevisionDate").get(0).getText()) :
                null;

        ValueSet valueSet = new ValueSet(
                xmlValueSet.getAttribute("ID"),
                xmlValueSet.getAttribute("displayName"),
                xmlValueSet.getAttribute("version"),
                source,
                purpose,
                type,
                binding,
                status,
                revisionDate
        );
        valueSet.setConcepts(concepts);

        return valueSet;
    }


//////////////////////////////////////////////////////////////////////
// private methods
//

    private String getRawValueSet(String oid) throws IOException, DataException, EncoderException {
        String url = "https://vsac.nlm.nih.gov/vsac/svs/RetrieveMultipleValueSets";

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("id", oid);
        urlParams.put("ticket", getServiceTicket());

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "text/xml");

        HttpResponse response = new HttpRequest().get(url, urlParams, requestHeaders);
        if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return response.getResponseBody();

        } else {
            throw new MyHttpException(response.getResponseCode(), "failed to get ValueSet with OID={" + oid + "} - code=" + response.getResponseCode() + "; body=" + response.getResponseBody());
        }
    }

    /**
     * Generates a VSAC Service Ticket that is good for 5 minutes but expires after one use.
     * @return Service Ticket
     * @throws IOException
     * @throws HttpException
     */
    private String getServiceTicket() throws IOException, DataException, EncoderException {
        HttpResponse response = doGetServiceTicketRequest();

        if (response.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            logger.warn("received UNAUTHORIZED response getting Service Ticket - reattempting with fresh CAS ticket -");
            serviceTicketURI = null;
            response = doGetServiceTicketRequest();
        }

        if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return response.getResponseBody();

        } else {
            throw new MyHttpException(response.getResponseCode(), "failed to get Service Ticket");
        }
    }

    private HttpResponse doGetServiceTicketRequest() throws IOException, DataException, EncoderException {
        if (serviceTicketURI == null) {
            serviceTicketURI = getNewServiceTicketURI();
        }

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("service", "http://umlsks.nlm.nih.gov");

        return new HttpRequest().post(serviceTicketURI, null, requestHeaders, bodyParams);
    }

    /**
     * Generates a VSAC Ticket Granting Ticket that is used to generate Service Tickets and lasts 8 hours.
     * see: https://documentation.uts.nlm.nih.gov/rest/authentication.html
     *
     * @return CAS Ticket
     */
    private String getNewServiceTicketURI() throws IOException, DataException, EncoderException {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> bodyParams = new LinkedHashMap<>();
        bodyParams.put("apikey", apiKey);

        HttpResponse response = new HttpRequest().post(API_KEY_URL, null, requestHeaders, bodyParams);

        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) { // some HTTP 2xx code, successful
            Matcher m = SERVICE_TICKET_URI_PATTERN.matcher(response.getResponseBody());
            if (m.matches()) {
                return m.group(1);

            } else {
                throw new DataException("couldn't find Service Ticket URI in response body");
            }

        } else {
            throw new MyHttpException(response.getResponseCode(), "failed to get Service Ticket URI");
        }
    }
}