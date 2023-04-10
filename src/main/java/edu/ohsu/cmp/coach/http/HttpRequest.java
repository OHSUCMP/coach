package edu.ohsu.cmp.coach.http;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
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
    private URLCodec urlCodec;

    public HttpRequest() {
        this.urlCodec = new URLCodec();
    }

    public HttpResponse get(String url) throws IOException, EncoderException {
        return get(url, null, null);
    }

    public HttpResponse get(String url, Map<String, String> urlParams) throws IOException, EncoderException {
        return get(url, urlParams, null);
    }

    public HttpResponse get(String url, Map<String, String> urlParams, Map<String, String> requestHeaders) throws IOException, EncoderException {
        if (urlParams != null && ! urlParams.isEmpty()) {
            url += "?" + buildURLEncodedParams(urlParams);
        }

        HttpGet httpget = new HttpGet(url);

        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                httpget.setHeader(entry.getKey(), entry.getValue());
            }
        }

        return execute(httpget);
    }

    public HttpResponse post(String url) throws IOException, EncoderException {
        return post(url, null, null, (String) null);
    }

    public HttpResponse post(String url, Map<String, String> urlParams) throws IOException, EncoderException {
        return post(url, urlParams, null, (String) null);
    }

    public HttpResponse post(String url, Map<String, String> urlParams, Map<String, String> requestHeaders) throws IOException, EncoderException {
        return post(url, urlParams, requestHeaders, (String) null);
    }

    public HttpResponse post(String url, Map<String, String> urlParams, Map<String, String> requestHeaders, Map<String, String> bodyParams) throws IOException, EncoderException {
        return post(url, urlParams, requestHeaders, buildURLEncodedParams(bodyParams));
    }

    public HttpResponse post(String url, Map<String, String> urlParams, Map<String, String> requestHeaders, String body) throws IOException, EncoderException {
        if (urlParams != null && ! urlParams.isEmpty()) {
            url += "?" + buildURLEncodedParams(urlParams);
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
                    int c = 0;
                    while ((c = br.read()) != -1) {
                        sb.append((char) c);
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

    private String buildURLEncodedParams(Map<String, String> params) throws EncoderException {
        if (params == null || params.isEmpty()) return null;

        List<String> list = new ArrayList<String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            list.add(String.format("%s=%s", entry.getKey(), urlCodec.encode(entry.getValue())));
        }
        return StringUtils.join(list, '&');
    }
}
