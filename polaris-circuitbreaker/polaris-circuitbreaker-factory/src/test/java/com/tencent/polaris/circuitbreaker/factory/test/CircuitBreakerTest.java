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

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_CIRCUIT_BREAKER;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

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
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(10081);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, SERVICE_CIRCUIT_BREAKER);
        InstanceParameter parameter = new InstanceParameter();
        parameter.setHealthy(true);
        parameter.setIsolated(false);
        parameter.setWeight(100);
        namingServer.getNamingService().batchAddInstances(serviceKey, 10010, MAX_COUNT, parameter);
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
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            int index = 1;
            GetInstancesRequest req = new GetInstancesRequest();
            req.setNamespace(NAMESPACE_TEST);
            req.setService(SERVICE_CIRCUIT_BREAKER);
            InstancesResponse instances = consumerAPI.getInstances(req);
            Assert.assertEquals(MAX_COUNT, instances.getInstances().length);
            Instance instanceToLimit = instances.getInstances()[index];
            ServiceCallResult result = instanceToResult(instanceToLimit);
            result.setRetCode(-1);
            result.setDelay(1000L);
            result.setRetStatus(RetStatus.RetFail);
            consumerAPI.updateServiceCallResult(result);
        }
    }

    @Test
    public void testCircuitBreakByErrorCount() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            Utils.sleepUninterrupted(10000);
            Assert.assertNotNull(consumerAPI);
            GetInstancesRequest getInstancesRequest = new GetInstancesRequest();
            getInstancesRequest.setNamespace(NAMESPACE_TEST);
            getInstancesRequest.setService(SERVICE_CIRCUIT_BREAKER);
            InstancesResponse instances = consumerAPI.getInstances(getInstancesRequest);
            Assert.assertEquals(MAX_COUNT, instances.getInstances().length);
            Instance instanceToLimit = instances.getInstances()[1];
            //report 60 fail in 500ms
            for (int i = 0; i < 60; ++i) {
                ServiceCallResult result = instanceToResult(instanceToLimit);
                result.setRetCode(-1);
                result.setDelay(1000L);
                result.setRetStatus(RetStatus.RetFail);
                consumerAPI.updateServiceCallResult(result);
                if (i % 10 == 0) {
                    Utils.sleepUninterrupted(1);
                }
            }
            Utils.sleepUninterrupted(1000);
            instances = consumerAPI.getInstances(getInstancesRequest);
            Assert.assertEquals(MAX_COUNT - 1, instances.getInstances().length);
            Instance[] instanceArray = instances.getInstances();
            boolean exists = false;
            for (int i = 0; i < instanceArray.length; ++i) {
                if (instanceArray[i].getId().equals(instanceToLimit.getId())) {
                    exists = true;
                }
            }
            Assert.assertFalse(exists);
            LOG.info("start to test half open by error rate");
            Utils.sleepUninterrupted(10000);
            instances = consumerAPI.getInstances(getInstancesRequest);
            Assert.assertEquals(MAX_COUNT, instances.getInstances().length);
            for (Instance instance : instances.getInstances()) {
                CircuitBreakerStatus circuitBreakerStatus = instance.getCircuitBreakerStatus();
                if (null != circuitBreakerStatus
                        && circuitBreakerStatus.getStatus() == CircuitBreakerStatus.Status.HALF_OPEN) {
                    LOG.info("half open instance is {}", instance);
                }
            }
            //default halfopen pass 3 success
            int requestCountAfterHalfOpen = configuration.getConsumer().getCircuitBreaker()
                    .getRequestCountAfterHalfOpen();
            for (int i = 0; i < requestCountAfterHalfOpen; i++) {
                ServiceCallResult result = instanceToResult(instanceToLimit);
                result.setRetCode(-1);
                result.setRetStatus(RetStatus.RetSuccess);
                consumerAPI.updateServiceCallResult(result);
                Utils.sleepUninterrupted(200);
                consumerAPI.updateServiceCallResult(result);
            }
            LOG.info("start to test half open to close");
            Utils.sleepUninterrupted(1000);
            instances = consumerAPI.getInstances(getInstancesRequest);
            Assert.assertEquals(MAX_COUNT, instances.getInstances().length);
        }
    }

    @Test
    public void testCircuitBreakByErrorRate() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            GetInstancesRequest getInstancesRequest = new GetInstancesRequest();
            getInstancesRequest.setNamespace(NAMESPACE_TEST);
            getInstancesRequest.setService(SERVICE_CIRCUIT_BREAKER);
            InstancesResponse instances = consumerAPI.getInstances(getInstancesRequest);
            Assert.assertEquals(MAX_COUNT, instances.getInstances().length);
            Instance instanceToLimit = instances.getInstances()[1];
            //report 60 fail in 500ms
            for (int i = 0; i < 60; ++i) {
                ServiceCallResult result = instanceToResult(instanceToLimit);
                result.setDelay(1000L);
                if (i % 2 == 0) {
                    result.setRetCode(0);
                    result.setRetStatus(RetStatus.RetSuccess);
                    Utils.sleepUninterrupted(1);
                } else {
                    result.setRetCode(-1);
                    result.setRetStatus(RetStatus.RetFail);
                }
                consumerAPI.updateServiceCallResult(result);
                Utils.sleepUninterrupted(1);
            }
            Utils.sleepUninterrupted(1000);
            instances = consumerAPI.getInstances(getInstancesRequest);
            Assert.assertEquals(MAX_COUNT - 1, instances.getInstances().length);
            Instance[] instanceArray = instances.getInstances();
            boolean exists = false;
            for (int i = 0; i < instanceArray.length; ++i) {
                if (instanceArray[i].getId().equals(instanceToLimit.getId())) {
                    exists = true;
                }
            }
            Assert.assertFalse(exists);
            Utils.sleepUninterrupted(10000);
            //default halfopen pass 3 success
            int requestCountAfterHalfOpen = configuration.getConsumer().getCircuitBreaker()
                    .getRequestCountAfterHalfOpen();
            for (int i = 0; i < requestCountAfterHalfOpen; i++) {
                ServiceCallResult result = instanceToResult(instanceToLimit);
                result.setRetCode(-1);
                result.setRetStatus(RetStatus.RetSuccess);
                consumerAPI.updateServiceCallResult(result);
                Utils.sleepUninterrupted(200);
                consumerAPI.updateServiceCallResult(result);
            }
            LOG.info("start to test half open to close");
            Utils.sleepUninterrupted(1000);
            instances = consumerAPI.getInstances(getInstancesRequest);
            Assert.assertEquals(MAX_COUNT, instances.getInstances().length);
        }
    }

}