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

package com.tencent.polaris.cb.example.common;

import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

public class Utils {

    public static String getLocalHost(Configuration configuration) throws Exception {
        String serverAddress = configuration.getGlobal().getServerConnector().getAddresses().get(0);
        String[] tokens = serverAddress.split(":");
        try (Socket socket = new Socket(tokens[0], Integer.parseInt(tokens[1]))) {
            return socket.getLocalAddress().getHostAddress();
        }
    }

    private static class HttpResult {

        private final int code;

        private final String message;

        public HttpResult(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    public static String invokeByNameResolution(String path, String namespace, String service, String value,
            ServiceInfo sourceService, ConsumerAPI consumerAPI, CircuitBreakAPI circuitBreakAPI) {
        System.out.println("namespace " + namespace + ", service " + service);
        // 1. we need to do naming resolution to get a load balanced host and port
        GetOneInstanceRequest getOneInstanceRequest = new GetOneInstanceRequest();
        getOneInstanceRequest.setNamespace(namespace);
        getOneInstanceRequest.setServiceInfo(sourceService);
        getOneInstanceRequest.setService(service);
        InstancesResponse oneInstance = consumerAPI.getOneInstance(getOneInstanceRequest);
        Instance[] instances = oneInstance.getInstances();
        System.out.println("instances count is " + instances.length);
        Instance targetInstance = instances[0];
        System.out.printf("[InstanceCbTest] now is %d, target instance is %s:%d%n", System.currentTimeMillis(),
                targetInstance.getHost(), targetInstance.getPort());

        // 2. invoke the server by the resolved address
        String urlStr = String
                .format("http://%s:%d%s?value=%s", targetInstance.getHost(), targetInstance.getPort(), path, value);
        long startMillis = System.currentTimeMillis();
        HttpResult httpResult = httpGet(urlStr);
        long delay = System.currentTimeMillis() - startMillis;
        System.out.printf("invoke %s, code is %d, delay is %d%n", urlStr, httpResult.code, delay);

        // 3. report the invoke result to polaris-java, to eliminate the fail address
        InstanceResource instanceResource = new InstanceResource(new ServiceKey(namespace, service),
                targetInstance.getHost(), targetInstance.getPort(), sourceService.getServiceKey());
        circuitBreakAPI.report(new ResourceStat(instanceResource, httpResult.code, delay));
        System.out.println("success to call report stat");
        return httpResult.message;
    }

    private static HttpResult httpGet(String urlStr) {
        HttpURLConnection connection = null;
        String respMessage = "";
        int code = -1;
        BufferedReader bufferedReader = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            code = connection.getResponseCode();
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            respMessage = bufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            code = 500;
            respMessage = e.getMessage();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new HttpResult(code, respMessage);
    }

    public static void createHttpServers(int normalPort, int abnormalPort) throws Exception {
        HttpServer normalServer = HttpServer.create(new InetSocketAddress(normalPort), 0);
        System.out.println("cb normal server listen port is " + normalPort);
        normalServer.createContext("/health", new HealthServer(true));
        HttpServer abnormalServer = HttpServer.create(new InetSocketAddress(abnormalPort), 0);
        System.out.println("cb abnormal server listen port is " + abnormalPort);
        abnormalServer.createContext("/health", new HealthServer(false));
        Thread normalStartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                normalServer.start();
            }
        });
        normalStartThread.setDaemon(true);
        Thread abnormalStartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                abnormalServer.start();
            }
        });
        abnormalStartThread.setDaemon(true);
        normalStartThread.start();
        abnormalStartThread.start();
        Thread.sleep(5000);
    }
}
