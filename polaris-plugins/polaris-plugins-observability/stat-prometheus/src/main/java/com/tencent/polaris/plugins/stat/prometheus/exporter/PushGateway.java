/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.plugins.stat.prometheus.exporter;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.DatatypeConverter;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.DefaultHttpConnectionFactory;
import io.prometheus.client.exporter.HttpConnectionFactory;
import io.prometheus.client.exporter.common.TextFormat;

public class PushGateway extends io.prometheus.client.exporter.PushGateway {

    private static final int MILLISECONDS_PER_SECOND = 1000;

    private HttpConnectionFactory connectionFactory = new DefaultHttpConnectionFactory();

    public PushGateway(String address) {
        super(address);
    }


    public void setConnectionFactory(HttpConnectionFactory connectionFactory) {
        super.setConnectionFactory(connectionFactory);
        this.connectionFactory = connectionFactory;
    }

    /**
     * Pushes all metrics in a registry, replacing only previously pushed metrics of the same name, job and grouping key.
     * <p>
     * This uses the POST HTTP method.
     */
    public void pushAddByGzip(CollectorRegistry registry, String job, Map<String, String> groupingKey) throws IOException {
        doRequestByGzip(registry, job, groupingKey, "POST");
    }

    void doRequestByGzip(CollectorRegistry registry, String job, Map<String, String> groupingKey, String method) throws IOException {
        String url = gatewayBaseURL;
        if (job.contains("/")) {
            url += "job@base64/" + base64url(job);
        } else {
            url += "job/" + URLEncoder.encode(job, "UTF-8");
        }

        if (groupingKey != null) {
            for (Map.Entry<String, String> entry : groupingKey.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    url += "/" + entry.getKey() + "@base64/=";
                } else if (entry.getValue().contains("/")) {
                    url += "/" + entry.getKey() + "@base64/" + base64url(entry.getValue());
                } else {
                    url += "/" + entry.getKey() + "/" + URLEncoder.encode(entry.getValue(), "UTF-8");
                }
            }
        }
        HttpURLConnection connection = connectionFactory.create(url);
        connection.setRequestProperty("Content-Type", TextFormat.CONTENT_TYPE_004);
        connection.setRequestProperty("Content-Encoding", "gzip");
        if (!method.equals("DELETE")) {
            connection.setDoOutput(true);
        }
        connection.setRequestMethod(method);

        connection.setConnectTimeout(10 * MILLISECONDS_PER_SECOND);
        connection.setReadTimeout(10 * MILLISECONDS_PER_SECOND);
        connection.connect();

        try {
            if (!method.equals("DELETE")) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                TextFormat.write004(writer, registry.metricFamilySamples());
                writer.flush();
                writer.close();
                GZIPOutputStream zipStream = new GZIPOutputStream(connection.getOutputStream());
                zipStream.write(outputStream.toByteArray());
                zipStream.finish();
                zipStream.flush();
                zipStream.close();
            }

            int response = connection.getResponseCode();
            if (response / 100 != 2) {
                String errorMessage;
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    String errBody = readFromStream(errorStream);
                    errorMessage = "Response code from " + url + " was " + response + ", response body: " + errBody;
                } else {
                    errorMessage = "Response code from " + url + " was " + response;
                }
                throw new IOException(errorMessage);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String base64url(String v) {
        // Per RFC4648 table 2. We support Java 6, and java.util.Base64 was only added in Java 8,
        try {
            return DatatypeConverter.printBase64Binary(v.getBytes("UTF-8")).replace("+", "-").replace("/", "_");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);  // Unreachable.
        }
    }

    private static String readFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
}
