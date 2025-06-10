/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.circuitbreaker.factory.test;

import com.google.protobuf.util.JsonFormat;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.api.pojo.GetReachableInstancesRequest;
import com.tencent.polaris.assembly.factory.AssemblyAPIFactory;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_CIRCUIT_BREAKER;
import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;

/**
 * CircuitBreakerTest.java
 *
 * @author andrewshan
 * @date 2019/8/30
 */
public class CircuitBreakerTest {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerTest.class);

    private static final int MAX_COUNT = 10;

    private NamingServer namingServer;

    @Before
    public void before() throws IOException {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, SERVICE_CIRCUIT_BREAKER);
        InstanceParameter parameter = new InstanceParameter();
        parameter.setHealthy(true);
        parameter.setIsolated(false);
        parameter.setWeight(100);
        namingServer.getNamingService().batchAddInstances(serviceKey, 10010, MAX_COUNT, parameter);
        CircuitBreakerProto.CircuitBreakerRule.Builder circuitBreakerRuleBuilder = CircuitBreakerProto.CircuitBreakerRule
                .newBuilder();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("circuitBreakerRuleNoDetect.json");
        String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining(""));
        JsonFormat.parser().ignoringUnknownFields().merge(json, circuitBreakerRuleBuilder);
        CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule = circuitBreakerRuleBuilder.build();
        CircuitBreakerProto.CircuitBreaker circuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder()
                .addRules(circuitBreakerRule).build();
        namingServer.getNamingService().setCircuitBreaker(serviceKey, circuitBreaker);

    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    private ServiceCallResult instanceToResult(Instance instance) {
        ServiceCallResult result = new ServiceCallResult();
        result.setNamespace(instance.getNamespace());
        result.setService(instance.getService());
        result.setHost(instance.getHost());
        result.setPort(instance.getPort());
        //result.setInstance(instance);
        return result;
    }

    @Test
    public void testUpdateServiceCallResult() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (AssemblyAPI assemblyAPI = AssemblyAPIFactory.createAssemblyAPIByConfig(configuration)) {
            Utils.sleepUninterrupted(10000);
            int index = 1;
            GetReachableInstancesRequest req = new GetReachableInstancesRequest();
            req.setNamespace(NAMESPACE_TEST);
            req.setService(SERVICE_CIRCUIT_BREAKER);
            List<Instance> instances = assemblyAPI.getReachableInstances(req);
            Assert.assertEquals(MAX_COUNT, instances.size());
            Instance instanceToLimit = instances.get(index);
            ServiceCallResult result = instanceToResult(instanceToLimit);
            result.setRetCode(-1);
            result.setDelay(1000L);
            result.setRetStatus(RetStatus.RetFail);
            assemblyAPI.updateServiceCallResult(result);
        }
    }

    @Test
    public void testFunctionalDecorator() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (CircuitBreakAPI circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPIByConfig(configuration)) {
            FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest(
                    new ServiceKey(NAMESPACE_TEST, SERVICE_CIRCUIT_BREAKER), "", "", "");
            FunctionalDecorator decorator = circuitBreakAPI.makeFunctionalDecorator(makeDecoratorRequest);
            Consumer<Integer> integerConsumer = decorator.decorateConsumer(num -> {
                if (num % 2 == 0) {
                    throw new IllegalArgumentException("invoke failed");
                } else {
                    System.out.println("invoke success");
                }
            });
            try {
                integerConsumer.accept(1);
                Utils.sleepUninterrupted(1000);
                integerConsumer.accept(2);
                Utils.sleepUninterrupted(1000);
            } catch (Exception e) {
                if (!(e instanceof IllegalArgumentException)) {
                    throw e;
                }
            }
            Assert.assertThrows(CallAbortedException.class, () -> integerConsumer.accept(3));
        }
    }

}