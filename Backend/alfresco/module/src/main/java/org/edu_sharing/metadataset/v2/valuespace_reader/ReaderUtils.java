package org.edu_sharing.metadataset.v2.valuespace_reader;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ReaderUtils {

    public static String query(String url) throws IOException {

        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        String proxyScheme = System.getProperty("http.proxyScheme", "http");

        HttpHost proxy = null;
        if (proxyHost != null && proxyPort != null) {
            proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), proxyScheme);
        }

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().
                setConnectTimeout(30000);

        if (proxy != null) {
            requestConfigBuilder.setProxy(proxy);
        }

        RequestConfig requestConfig = requestConfigBuilder.build();

        CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        RequestBuilder request = RequestBuilder.get(url);
        CloseableHttpResponse result = httpclient.execute(request.build());
        String data= StreamUtils.copyToString(result.getEntity().getContent(), StandardCharsets.UTF_8);
        result.close();
        return data;
    }
}
