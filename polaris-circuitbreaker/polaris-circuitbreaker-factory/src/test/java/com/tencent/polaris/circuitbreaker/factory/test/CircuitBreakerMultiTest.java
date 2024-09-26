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

package com.tencent.polaris.circuitbreaker.factory.test;

import com.google.common.cache.Cache;
import com.google.protobuf.StringValue;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.circuitbreaker.composite.HealthCheckContainer;
import com.tencent.polaris.plugins.circuitbreaker.composite.PolarisCircuitBreaker;
import com.tencent.polaris.plugins.circuitbreaker.composite.ResourceCounters;
import com.tencent.polaris.plugins.circuitbreaker.composite.ResourceHealthChecker;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_CIRCUIT_BREAKER;
import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;

public class CircuitBreakerMultiTest {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerMultiTest.class);

    private NamingServer namingServer;

    private final ServiceKey matchMethodService = new ServiceKey("Test", "SvcCbMethod");

    private final ServiceKey matchMethodDetectService = new ServiceKey("Test", "SvcCbMethodDetect");

    @Before
    public void before() throws IOException {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        CircuitBreakerProto.CircuitBreakerRule cbRule1 = CbTestUtils.loadCbRule("circuitBreakerMethodRuleNoDetect.json");
        CircuitBreakerProto.CircuitBreaker circuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder()
                .addRules(cbRule1).setRevision(StringValue.newBuilder().setValue("0000").build()).build();
        namingServer.getNamingService().setCircuitBreaker(matchMethodService, circuitBreaker);


        CircuitBreakerProto.CircuitBreakerRule cbRule3 = CbTestUtils.loadCbRule("circuitBreakerMethodRule.json");
        CircuitBreakerProto.CircuitBreakerRule cbRule4 = CbTestUtils.loadCbRule("circuitBreakerRule.json");
        circuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder()
                .addRules(cbRule3).addRules(cbRule4).setRevision(StringValue.newBuilder().setValue("1111").build()).build();
        namingServer.getNamingService().setCircuitBreaker(matchMethodDetectService, circuitBreaker);
        FaultDetectorProto.FaultDetectRule rule1 = CbTestUtils.loadFdRule("faultDetectRule.json");
        FaultDetectorProto.FaultDetectRule rule2 = CbTestUtils.loadFdRule("faultDetectMethodRule.json");
        FaultDetectorProto.FaultDetector faultDetector = FaultDetectorProto.FaultDetector.newBuilder()
                .addRules(rule1).addRules(rule2).setRevision("2222").build();
        namingServer.getNamingService().setFaultDetector(matchMethodDetectService, faultDetector);
    }

    @Test
    public void testMultipleUrlsNoRule() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
        configurationImpl.getConsumer().getCircuitBreaker().setCountersExpireInterval(5000);
        try (CircuitBreakAPI circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPIByConfig(configurationImpl)) {
            for (int i = 0; i < 50; i++) {
                if (i == 1) {
                    Utils.sleepUninterrupted(5 * 1000);
                }
                FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest(
                        new ServiceKey(NAMESPACE_TEST, SERVICE_CIRCUIT_BREAKER), "", "", "/test/" + i);
                FunctionalDecorator decorator = circuitBreakAPI.makeFunctionalDecorator(makeDecoratorRequest);
                int finalI = i;
                Consumer<Integer> integerConsumer = decorator.decorateConsumer(num -> {
                    if (num % 2 == 0) {
                        throw new IllegalArgumentException("invoke failed" + finalI);
                    } else {
                        System.out.println("invoke success" + finalI);
                    }
                });
                integerConsumer.accept(1);
            }
            Utils.sleepUninterrupted(10 * 1000);
            BaseEngine baseEngine = (BaseEngine) circuitBreakAPI;
            CircuitBreaker resourceBreaker = baseEngine.getSDKContext().getExtensions().getResourceBreaker();
            PolarisCircuitBreaker polarisCircuitBreaker = (PolarisCircuitBreaker) resourceBreaker;
            Cache<Resource, Optional<ResourceCounters>> methodCache = polarisCircuitBreaker.getCountersCache()
                    .get(CircuitBreakerProto.Level.METHOD);
            polarisCircuitBreaker.cleanupExpiredResources();
            Assert.assertEquals(0, methodCache.size());
        }
    }

    @Test
    public void testMultipleUrlsMethodRule() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
        configurationImpl.getConsumer().getCircuitBreaker().setCountersExpireInterval(5000);
        try (CircuitBreakAPI circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPIByConfig(configurationImpl)) {
            for (int i = 0; i < 50; i++) {
                if (i == 1) {
                    Utils.sleepUninterrupted(5 * 1000);
                }
                FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest(
                        matchMethodService, "", "", "/test1/path/" + i);
                FunctionalDecorator decorator = circuitBreakAPI.makeFunctionalDecorator(makeDecoratorRequest);
                int finalI = i;
                Consumer<Integer> integerConsumer = decorator.decorateConsumer(num -> {
                    if (num % 2 == 0) {
                        throw new IllegalArgumentException("invoke failed" + finalI);
                    } else {
                        System.out.println("invoke success" + finalI);
                    }
                });
                if (i == 0) {
                    integerConsumer.accept(1);
                    try {
                        integerConsumer.accept(2);
                    } catch (Exception e) {
                        if (!(e instanceof IllegalArgumentException)) {
                            throw e;
                        }
                    }
                    Assert.assertThrows(CallAbortedException.class, () -> integerConsumer.accept(3));
                } else {
                    Assert.assertThrows(CallAbortedException.class, () -> integerConsumer.accept(1));
                }
            }
            BaseEngine baseEngine = (BaseEngine) circuitBreakAPI;
            CircuitBreaker resourceBreaker = baseEngine.getSDKContext().getExtensions().getResourceBreaker();
            PolarisCircuitBreaker polarisCircuitBreaker = (PolarisCircuitBreaker) resourceBreaker;
            Cache<Resource, Optional<ResourceCounters>> methodCache = polarisCircuitBreaker.getCountersCache()
                    .get(CircuitBreakerProto.Level.METHOD);
            Assert.assertEquals(1, methodCache.size());
        }
    }

    @Test
    public void testCircuitBreakerRuleChanged() throws IOException {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
        try (CircuitBreakAPI circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPIByConfig(configurationImpl)) {
            for (int i = 0; i < 50; i++) {
                if (i == 1) {
                    Utils.sleepUninterrupted(5 * 1000);
                }
                String method = "";
                if (i > 0) {
                    method = "/test1/path/" + i;
                }
                FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest(
                        matchMethodDetectService, "", "", method);
                FunctionalDecorator decorator = circuitBreakAPI.makeFunctionalDecorator(makeDecoratorRequest);
                int finalI = i;
                Consumer<Integer> integerConsumer = decorator.decorateConsumer(num -> {
                    if (num % 2 == 0) {
                        throw new IllegalArgumentException("invoke failed" + finalI);
                    } else {
                        System.out.println("invoke success" + finalI);
                    }
                });
                integerConsumer.accept(1);
            }
            CircuitBreakerProto.CircuitBreakerRule cbRule1 = CbTestUtils.loadCbRule("circuitBreakerMethodRuleChanged.json");
            CircuitBreakerProto.CircuitBreaker circuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder()
                    .addRules(cbRule1).setRevision(StringValue.newBuilder().setValue("444441").build()).build();
            namingServer.getNamingService().setCircuitBreaker(matchMethodDetectService, circuitBreaker);
            Utils.sleepUninterrupted(20 * 1000);
            BaseEngine baseEngine = (BaseEngine) circuitBreakAPI;
            CircuitBreaker resourceBreaker = baseEngine.getSDKContext().getExtensions().getResourceBreaker();
            PolarisCircuitBreaker polarisCircuitBreaker = (PolarisCircuitBreaker) resourceBreaker;
            Cache<Resource, Optional<ResourceCounters>> methodCache = polarisCircuitBreaker.getCountersCache()
                    .get(CircuitBreakerProto.Level.METHOD);
            Assert.assertEquals(0, methodCache.size());

            Map<ServiceKey, HealthCheckContainer> healthCheckCache = polarisCircuitBreaker.getHealthCheckCache();
            HealthCheckContainer healthCheckContainer = healthCheckCache.get(matchMethodDetectService);
            Assert.assertNotNull(healthCheckContainer);
            Collection<ResourceHealthChecker> healthCheckerValues = healthCheckContainer.getHealthCheckerValues();
            Assert.assertEquals(2, healthCheckerValues.size());
            for (int i = 0; i < 10; i++) {
                String method = "/test1/path/" + i;
                FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest(
                        matchMethodDetectService, "", "", method);
                FunctionalDecorator decorator = circuitBreakAPI.makeFunctionalDecorator(makeDecoratorRequest);
                int finalI = i;
                Consumer<Integer> integerConsumer = decorator.decorateConsumer(num -> {
                    if (num < 3) {
                        throw new IllegalArgumentException("invoke failed" + finalI);
                    } else {
                        System.out.println("invoke success" + finalI);
                    }
                });
                if (i == 0) {
                    try {
                        integerConsumer.accept(1);
                    } catch (Exception e) {
                        if (!(e instanceof IllegalArgumentException)) {
                            throw e;
                        }
                    }
                    try {
                        integerConsumer.accept(2);
                    } catch (Exception e) {
                        if (!(e instanceof IllegalArgumentException)) {
                            throw e;
                        }
                    }
                    Assert.assertThrows(CallAbortedException.class, () -> integerConsumer.accept(3));
                } else {
                    Assert.assertThrows(CallAbortedException.class, () -> integerConsumer.accept(1));
                }
            }
        }
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

}
