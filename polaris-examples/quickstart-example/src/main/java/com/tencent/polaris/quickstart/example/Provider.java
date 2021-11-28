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

import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class Provider {

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

        String host = "127.0.0.1";
        int port = 8888;
        try (ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPI()) {
            InstanceRegisterRequest registerRequest = new InstanceRegisterRequest();
            registerRequest.setNamespace(namespace);
            registerRequest.setService(service);
            registerRequest.setHost(host);
            registerRequest.setPort(port);
            registerRequest.setTtl(6);
            InstanceRegisterResponse registerResp = providerAPI.register(registerRequest);
            System.out.printf("register instance %s:%d to service %s(%s), id is %s%n",
                    host, port, service, namespace, registerResp.getInstanceId());

            String instanceId = registerResp.getInstanceId();
            InstanceHeartbeatRequest heartbeatRequest = new InstanceHeartbeatRequest();
            heartbeatRequest.setInstanceID(instanceId);
            providerAPI.heartbeat(heartbeatRequest);
            System.out.printf("heartbeat instance, id is %s%n", instanceId);

            InstanceDeregisterRequest deregisterRequest = new InstanceDeregisterRequest();
            deregisterRequest.setInstanceID(instanceId);
            providerAPI.deRegister(deregisterRequest);
            System.out.printf("deregister instance, id is %s%n", instanceId);
        }



    }
}
