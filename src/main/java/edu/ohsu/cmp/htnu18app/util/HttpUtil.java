package edu.ohsu.cmp.htnu18app.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HttpUtil {
    public static String get(String url) throws IOException {
        StringBuilder sb = new StringBuilder();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        CloseableHttpResponse response = httpclient.execute(httpget);
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
