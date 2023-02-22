/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.cb.example.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;

import java.io.*;
import java.net.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class Consumer {

    private static final String namespace = "default";

    private static final String service = "polaris-circuitbreaker-example-b";

    private static final String PATH = "/example/service/b/info";

    public static String invokeByNameResolution(ConsumerAPI consumerAPI) {
        GetOneInstanceRequest getOneInstanceRequest = new GetOneInstanceRequest();
        getOneInstanceRequest.setNamespace(namespace);
        getOneInstanceRequest.setService(service);
        InstancesResponse oneInstance = consumerAPI.getOneInstance(getOneInstanceRequest);
        Instance[] instances = oneInstance.getInstances();
        Instance targetInstance = instances[0];
        String urlStr = String
                .format("http://%s:%d%s", targetInstance.getHost(), targetInstance.getPort(), PATH);
        HttpResult httpResult = httpGet(urlStr);
        return httpResult.message;
    }


    private static class HttpResult {

        private final int code;

        private final String message;

        public HttpResult(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    private static HttpResult httpGet(String urlStr) {
        HttpURLConnection connection = null;
        String respMessage;
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

}
