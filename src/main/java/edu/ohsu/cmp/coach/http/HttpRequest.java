package edu.ohsu.cmp.coach.http;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpRequest {
    public HttpResponse get(String url) throws IOException {
        return get(url, null, null);
    }

    public HttpResponse get(String url, Map<String, String> urlParams) throws IOException {
        return get(url, urlParams, null);
    }

    public HttpResponse get(String url, Map<String, String> urlParams, Map<String, String> requestHeaders) throws IOException {
        if (urlParams != null && ! urlParams.isEmpty()) {
            url += "?" + buildParams(urlParams);
        }

        HttpGet httpget = new HttpGet(url);

        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                httpget.setHeader(entry.getKey(), entry.getValue());
            }
        }

        return execute(httpget);
    }

    public HttpResponse post(String url) throws IOException {
        return post(url, null, null, null);
    }

    public HttpResponse post(String url, Map<String, String> urlParams) throws IOException {
        return post(url, urlParams, null, null);
    }

    public HttpResponse post(String url, Map<String, String> urlParams, Map<String, String> requestHeaders) throws IOException {
        return post(url, urlParams, requestHeaders, null);
    }

    public HttpResponse post(String url, Map<String, String> urlParams, Map<String, String> requestHeaders, String body) throws IOException {
        if (urlParams != null && ! urlParams.isEmpty()) {
            url += "?" + buildParams(urlParams);
        }

        HttpPost httppost = new HttpPost(url);

        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                httppost.setHeader(entry.getKey(), entry.getValue());
            }
        }

        if (body != null) {
            httppost.setEntity(new StringEntity(body));
        }

        return execute(httppost);
    }

    private HttpResponse execute(HttpUriRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();

// storer 2022-09-16 - modifying creation of httpclient to resolve the following experienced errors:
// 2022-09-16 14:30:05.427 [scheduling-1] WARN  o.a.h.c.p.ResponseProcessCookies [ResponseProcessCookies.java:130] Invalid
//      cookie header: "Set-Cookie: <cookie>; Expires=Fri, 23 Sep 2022 21:30:05 GMT; Path=/". Invalid 'expires' attribute:
//      Fri, 23 Sep 2022 21:30:05 GMT
// see: https://www.lenar.io/invalid-cookie-header-invalid-expires-attribute/
//        CloseableHttpClient httpclient = HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        CloseableHttpClient httpclient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();

        CloseableHttpResponse response = httpclient.execute(request);

        int code = response.getStatusLine().getStatusCode();

        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream is = entity.getContent();
                InputStreamReader isr = null;
                BufferedReader br = null;
                try {
                    isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                } finally {
                    try { if (br != null) br.close(); } catch (Exception e) { }
                    try { if (isr != null) isr.close(); } catch (Exception e) { }
                }
            }

        } finally {
            try { response.close(); } catch (Exception e) { }
        }

        return new HttpResponse(code, sb.toString());
    }

    private String buildParams(Map<String, String> urlParams) {
        List<String> list = new ArrayList<String>();
        for (Map.Entry<String, String> entry : urlParams.entrySet()) {
            list.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }
        return StringUtils.join(list, '&');
    }
}
