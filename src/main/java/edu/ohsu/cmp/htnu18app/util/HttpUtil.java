package edu.ohsu.cmp.htnu18app.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class HttpUtil {
    public static String get(String url) throws IOException {
        HttpGet httpget = new HttpGet(url);
        return execute(httpget);
    }

    public static String post(String url, Map<String, String> headers, String body) throws IOException {
        HttpPost httppost = new HttpPost(url);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httppost.setHeader(entry.getKey(), entry.getValue());
            }
        }

        if (body != null) {
            httppost.setEntity(new StringEntity(body));
        }

        return execute(httppost);
    }

    private static String execute(HttpUriRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(request);
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

        return sb.toString();
    }
}
