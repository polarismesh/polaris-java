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

package com.tencent.polaris.cb.example.instance;

import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.cb.example.common.EchoServer;
import com.tencent.polaris.cb.example.common.HealthServer;
import com.tencent.polaris.cb.example.common.ServerType;
import com.tencent.polaris.cb.example.common.Utils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import java.net.InetSocketAddress;

public class InstanceBreakerExample {

    private static final int PORT_NORMAL = 10881;

    private static final int PORT_ABNORMAL = 10882;

    private static final String NAMESPACE = "test";

    private static final String SERVICE = "instanceCbService";

    public static void main(String[] args) throws Exception {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        SDKContext sdkContext = SDKContext.initContextByConfig(configuration);
        String localHost = Utils.getLocalHost(configuration);
        InstanceRegisterRequest normalRequest = new InstanceRegisterRequest();
        normalRequest.setHost(localHost);
        normalRequest.setPort(PORT_NORMAL);
        normalRequest.setNamespace(NAMESPACE);
        normalRequest.setService(SERVICE);
        EchoServer normalEchoServer = new EchoServer(sdkContext, normalRequest, ServerType.NORMAL);
        HttpServer normalServer = HttpServer.create(new InetSocketAddress(PORT_NORMAL), 0);
        System.out.println("Instance cb normal server listen port is " + PORT_NORMAL);
        normalServer.createContext("/echo", normalEchoServer);
        normalServer.createContext("/health", new HealthServer(true));
        InstanceRegisterRequest abnormalRequest = new InstanceRegisterRequest();
        abnormalRequest.setHost(localHost);
        abnormalRequest.setPort(PORT_ABNORMAL);
        abnormalRequest.setNamespace(NAMESPACE);
        abnormalRequest.setService(SERVICE);
        EchoServer abnormalEchoServer = new EchoServer(sdkContext, abnormalRequest, ServerType.CONSECUTIVE);
        HttpServer abnormalServer = HttpServer.create(new InetSocketAddress(PORT_ABNORMAL), 0);
        System.out.println("Instance cb abnormal server listen port is " + PORT_ABNORMAL);
        abnormalServer.createContext("/echo", abnormalEchoServer);
        abnormalServer.createContext("/health", new HealthServer(false));

        normalEchoServer.register();
        abnormalEchoServer.register();
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            normalEchoServer.deregister();
            abnormalEchoServer.deregister();
            sdkContext.close();
            normalServer.stop(1);
            abnormalServer.stop(1);
        }));

        ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
        for (int i = 0; i < 20000; i++) {
            String test = Utils.invokeByNameResolution("/echo", NAMESPACE, SERVICE, "test", consumerAPI);
            System.out.println("invoke " + i + " times, result " + test);
            Thread.sleep(1000);
        }

    }
}
