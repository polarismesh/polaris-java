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

package com.tencent.polaris.cb.example.subset;

import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.cb.example.common.EchoServer;
import com.tencent.polaris.cb.example.common.ServerType;
import com.tencent.polaris.cb.example.common.Utils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import java.net.InetSocketAddress;

public class SubsetBreakerExample {

    private static final int PORT_V1_NORMAL = 10981;

    private static final int PORT_V1_ABNORMAL = 10982;

    private static final int PORT_V2_NORMAL = 10983;

    private static final String NAMESPACE = "test";

    private static final String SERVICE = "subsetCbService";

    public static void main(String[] args) throws Exception {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        SDKContext sdkContext = SDKContext.initContextByConfig(configuration);
        String localHost = Utils.getLocalHost(configuration);
        // v1 normal
        InstanceRegisterRequest v1NormalRequest = new InstanceRegisterRequest();
        v1NormalRequest.setHost(localHost);
        v1NormalRequest.setPort(PORT_V1_NORMAL);
        v1NormalRequest.setNamespace(NAMESPACE);
        v1NormalRequest.setService(SERVICE);
        v1NormalRequest.setVersion("1.0.0");
        EchoServer v1NormalEchoServer = new EchoServer(sdkContext, v1NormalRequest, ServerType.NORMAL);
        HttpServer v1NormalServer = HttpServer.create(new InetSocketAddress(PORT_V1_NORMAL), 0);
        System.out.println("Instance cb v1 normal server listen port is " + PORT_V1_NORMAL);
        v1NormalServer.createContext("/echo", v1NormalEchoServer);

        // v1 abnormal
        InstanceRegisterRequest v1AbnormalRequest = new InstanceRegisterRequest();
        v1AbnormalRequest.setHost(localHost);
        v1AbnormalRequest.setPort(PORT_V1_ABNORMAL);
        v1AbnormalRequest.setNamespace(NAMESPACE);
        v1AbnormalRequest.setService(SERVICE);
        v1AbnormalRequest.setVersion("1.0.0");
        EchoServer v1AbnormalEchoServer = new EchoServer(sdkContext, v1AbnormalRequest, ServerType.CONSECUTIVE);
        HttpServer V1AbnormalServer = HttpServer.create(new InetSocketAddress(PORT_V1_ABNORMAL), 0);
        System.out.println("Instance cb v1 abnormal server listen port is " + PORT_V1_ABNORMAL);
        V1AbnormalServer.createContext("/echo", v1AbnormalEchoServer);

        // v2 normal
        InstanceRegisterRequest v2NormalRequest = new InstanceRegisterRequest();
        v2NormalRequest.setHost(localHost);
        v2NormalRequest.setPort(PORT_V2_NORMAL);
        v2NormalRequest.setNamespace(NAMESPACE);
        v2NormalRequest.setService(SERVICE);
        v2NormalRequest.setVersion("2.0.0");
        EchoServer v2NormalEchoServer = new EchoServer(sdkContext, v2NormalRequest, ServerType.NORMAL);
        HttpServer v2NormalServer = HttpServer.create(new InetSocketAddress(PORT_V2_NORMAL), 0);
        System.out.println("Instance cb v2 normal server listen port is " + PORT_V2_NORMAL);
        v2NormalServer.createContext("/echo", v2NormalEchoServer);

        v1NormalEchoServer.register();
        v1AbnormalEchoServer.register();
        v2NormalEchoServer.register();

        Thread v1NormalStartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                v1NormalServer.start();
            }
        });
        v1NormalStartThread.setDaemon(true);

        Thread v1AbnormalStartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                V1AbnormalServer.start();
            }
        });
        v1AbnormalStartThread.setDaemon(true);

        Thread v2NormalStartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                v2NormalServer.start();
            }
        });
        v2NormalStartThread.setDaemon(true);

        Thread.sleep(5000);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            v1NormalEchoServer.deregister();
            v1AbnormalEchoServer.deregister();
            v2NormalEchoServer.deregister();
            sdkContext.close();
            v1NormalServer.stop(1);
            V1AbnormalServer.stop(1);
            v2NormalServer.stop(1);
        }));

        ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
        for (int i = 0; i < 200; i++) {
            String test = Utils.invokeByNameResolution("/echo", NAMESPACE, SERVICE, "test", consumerAPI);
            System.out.println("invoke " + i + " times, result " + test);
            Thread.sleep(10000);
        }

    }
}
