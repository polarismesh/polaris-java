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

package com.tencent.polaris.quickstart.example;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class Consumer {

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("namespace", "namespace", true, "namespace for service");
        options.addOption("service", "service", true, "name for service");

        CommandLine commandLine = parser.parse(options, args);
        String namespace = commandLine.getOptionValue("namespace");
        String service = commandLine.getOptionValue("service");
        if (StringUtils.isBlank(namespace) || StringUtils.isBlank(service)) {
            System.out.println("namespace or service is required");
            System.exit(1);
        }
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPI()) {
            System.out.println("namespace " + namespace + ", service " + service);
            GetOneInstanceRequest getOneInstanceRequest = new GetOneInstanceRequest();
            getOneInstanceRequest.setNamespace(namespace);
            getOneInstanceRequest.setService(service);
            InstancesResponse oneInstance = consumerAPI.getOneInstance(getOneInstanceRequest);
            Instance[] instances = oneInstance.getInstances();
            System.out.println("instances count is " + instances.length);
            Instance targetInstance = instances[0];
            System.out.printf("target instance is %s:%d%n", targetInstance.getHost(), targetInstance.getPort());

            String path = "/";
            String urlStr = String.format("http://%s:%d%s", targetInstance.getHost(), targetInstance.getPort(), path);
            long startMillis = System.currentTimeMillis();
            int code = httpGet(urlStr);
            long delay = System.currentTimeMillis() - startMillis;
            System.out.printf("invoke %s, code is %d, delay is %d%n", urlStr, code, delay);

            RetStatus status = RetStatus.RetSuccess;
            if (code != 200) {
                status = RetStatus.RetFail;
            }
            ServiceCallResult result = new ServiceCallResult();
            result.setNamespace(namespace);
            result.setService(service);
            result.setHost(targetInstance.getHost());
            result.setPort(targetInstance.getPort());
            result.setRetCode(code);
            result.setDelay(delay);
            result.setRetStatus(status);
            consumerAPI.updateServiceCallResult(result);
            System.out.println("success to call updateServiceCallResult");
        }
    }


    private static int httpGet(String urlStr) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            return connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
        return -1;
    }

}
