package edu.ohsu.cmp.coach.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.cqfruler.CDSHook;
import edu.ohsu.cmp.coach.model.cqfruler.CDSServices;

import java.io.IOException;
import java.util.List;

public class CDSHooksUtil {
    public static List<CDSHook> getCDSHooks(boolean testing, String cdsHooksEndpointURL) throws IOException {
        String json;
        if (testing) {
            json = "{  \"services\": [    {      \"hook\": \"patient-view\",      \"name\": \"TEST\",      \"title\": \"OHSU Test Recommendation\",      \"description\": \"These cards below are hardcoded into the app for testing purposes.\",      \"id\": \"plandefinition-TEST\",      \"prefetch\": { } } ] }\n";

        } else {
            HttpResponse response = new HttpRequest().get(cdsHooksEndpointURL);
            json = response.getResponseBody();
        }

        Gson gson = new GsonBuilder().create();
        CDSServices services = gson.fromJson(json, new TypeToken<CDSServices>(){}.getType());

        return services.getHooks();
    }
}
