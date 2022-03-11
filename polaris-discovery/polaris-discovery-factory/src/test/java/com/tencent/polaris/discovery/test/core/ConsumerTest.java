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

package com.tencent.polaris.discovery.test.core;

import static com.tencent.polaris.test.common.Consts.ITERATE_COUNT;
import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumerTest.class);
    private static final Map<Operation, ValidParam> validParams = new HashMap<>();
    private static final String SERVICE_TEST_NORMAL = "java_test_normal";
    private static final String SERVICE_TEST_ABNORMAL = "java_test_abnormal";
    private static final String NOT_EXISTS_SERVICE = "java_test_not_exists";

    static {
        validParams.put(Operation.ALL_HEALTHY,
                new ValidParam(SERVICE_TEST_NORMAL, 6, 6, 6));
        validParams.put(Operation.HAS_UNHEALTHY,
                new ValidParam(SERVICE_TEST_ABNORMAL, 10, 4, 8));
    }

    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(10081);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        for (ValidParam validParam : validParams.values()) {
            InstanceParameter instanceParameter = new InstanceParameter();
            instanceParameter.setHealthy(true);
            instanceParameter.setIsolated(false);
            instanceParameter.setWeight(100);
            ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, validParam.getServiceName());
            List<Node> nodes = namingServer.getNamingService().batchAddInstances(serviceKey, 10000,
                    validParam.getCountAll(), instanceParameter);
            if (validParam.getCountAll() > validParam.getCountHealth()) {
                int abnormalCount = validParam.getCountAll() - validParam.getCountHealth();
                int unhealthyCount = abnormalCount / 2;
                int isolatedCount = abnormalCount - unhealthyCount;
                for (int i = 0; i < unhealthyCount; i++) {
                    namingServer.getNamingService().setInstanceHealthyStatus(
                            serviceKey, nodes.get(i), false, null, null);
                }
                for (int i = 0; i < isolatedCount; i++) {
                    namingServer.getNamingService().setInstanceHealthyStatus(
                            serviceKey, nodes.get(nodes.size() - 1 - i), null, true, null);
                }
            }
            if (validParam.getCountAll() > validParam.getCountHasWeight()) {
                int weightZeroCount = validParam.getCountAll() - validParam.getCountHasWeight();
                for (int i = 0; i < weightZeroCount; i++) {
                    namingServer.getNamingService().setInstanceHealthyStatus(
                            serviceKey, nodes.get(i), null, null, 0);
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

    @Test
    public void testSyncGetAllInstancesNormal() {
        commonTestSyncGetAllInstances(Operation.ALL_HEALTHY);
    }

    @Test
    public void testSyncGetInstancesNormal() {
        commonTestSyncGetInstances(Operation.ALL_HEALTHY);
    }

    @Test
    public void testSyncGetOneInstanceNormal() {
        commonTestSyncGetOneInstance(Operation.ALL_HEALTHY);
    }

    @Test
    public void testConcurrentSyncGetOneInstanceNormal() {
        concurrentTestSyncGetOneInstance(Operation.ALL_HEALTHY);
    }

    @Test
    public void testSyncGetAllInstancesAbnormal() {
        commonTestSyncGetAllInstances(Operation.HAS_UNHEALTHY);
    }

    @Test
    public void testSyncGetInstancesAbnormal() {
        commonTestSyncGetInstances(Operation.HAS_UNHEALTHY);
    }

    @Test
    public void testSyncGetOneInstanceAbnormal() {
        commonTestSyncGetOneInstance(Operation.HAS_UNHEALTHY);
    }

    @Test
    public void testUseBuiltinAsDiscover() {
        ValidParam validParam = validParams.get(Operation.ALL_HEALTHY);
        Configuration configuration = TestUtils.configWithEnvAddress();
        ConfigurationImpl configImpl = (ConfigurationImpl) configuration;
        configImpl.setDefault();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            for (int i = 0; i < ITERATE_COUNT; i++) {
                GetOneInstanceRequest request = new GetOneInstanceRequest();
                request.setNamespace(NAMESPACE_TEST);
                request.setService(validParam.getServiceName());

                InstancesResponse instancesResponse = consumerAPI.getOneInstance(request);
                Assert.assertEquals(1, instancesResponse.getInstances().length);

                Instance instance = instancesResponse.getInstances()[0];
                Assert.assertTrue(instance.isHealthy());
                Assert.assertFalse(instance.isIsolated());
                Assert.assertEquals(100, instance.getWeight());
            }
        }
    }

    @Test
    public void testGetNotExistsService() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            for (int i = 0; i < 10; i++) {
                GetOneInstanceRequest request = new GetOneInstanceRequest();
                request.setNamespace(NAMESPACE_TEST);
                request.setService(NOT_EXISTS_SERVICE);
                InstancesResponse oneInstance = consumerAPI.getOneInstance(request);
                Assert.assertFalse(oneInstance.isServiceExist());
            }
            //把实例加上去，可以重新获取
            InstanceParameter parameter = new InstanceParameter();
            parameter.setHealthy(true);
            parameter.setIsolated(false);
            parameter.setWeight(100);
            namingServer.getNamingService().batchAddInstances(
                    new ServiceKey(NAMESPACE_TEST, NOT_EXISTS_SERVICE), 10100, 10, parameter);
            GetOneInstanceRequest request = new GetOneInstanceRequest();
            request.setNamespace(NAMESPACE_TEST);
            request.setService(NOT_EXISTS_SERVICE);
            InstancesResponse oneInstance = consumerAPI.getOneInstance(request);
            Assert.assertEquals(1, oneInstance.getInstances().length);

        }
    }

    public void commonTestSyncGetAllInstances(Operation operation) {
        ValidParam validParam = validParams.get(operation);
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            for (int i = 0; i < ITERATE_COUNT; i++) {
                GetAllInstancesRequest request = new GetAllInstancesRequest();
                request.setNamespace(NAMESPACE_TEST);
                request.setService(validParam.getServiceName());

                InstancesResponse instancesResponse = consumerAPI.getAllInstance(request);
                Assert.assertEquals(validParam.getCountAll(), instancesResponse.getInstances().length);
                Assert.assertEquals(validParam.getCountHasWeight() * 100, instancesResponse.getTotalWeight());
            }
        }
    }

    public void commonTestSyncGetInstances(Operation operation) {
        ValidParam validParam = validParams.get(operation);
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            for (int i = 0; i < ITERATE_COUNT; i++) {
                GetInstancesRequest request = new GetInstancesRequest();
                request.setNamespace(NAMESPACE_TEST);
                request.setService(validParam.getServiceName());

                InstancesResponse instancesResponse = consumerAPI.getInstances(request);
                Assert.assertEquals(validParam.getCountHealth(), instancesResponse.getInstances().length);
                Assert.assertEquals(validParam.getCountHealth() * 100, instancesResponse.getTotalWeight());
            }
        }
    }

    public void concurrentTestSyncGetOneInstance(Operation operation) {
        ValidParam validParam = validParams.get(operation);
        Configuration configuration = TestUtils.configWithEnvAddress();
        ExecutorService executorService = Executors.newCachedThreadPool();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < ITERATE_COUNT; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        GetOneInstanceRequest request = new GetOneInstanceRequest();
                        request.setNamespace(NAMESPACE_TEST);
                        request.setService(validParam.getServiceName());

                        InstancesResponse instancesResponse = consumerAPI.getOneInstance(request);
                        Assert.assertEquals(1, instancesResponse.getInstances().length);

                        Instance instance = instancesResponse.getInstances()[0];
                        Assert.assertTrue(instance.isHealthy());
                        Assert.assertFalse(instance.isIsolated());
                        Assert.assertEquals(100, instance.getWeight());
                    }
                });
            }
        }
    }

    public void commonTestSyncGetOneInstance(Operation operation) {
        ValidParam validParam = validParams.get(operation);
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            for (int i = 0; i < ITERATE_COUNT; i++) {
                GetOneInstanceRequest request = new GetOneInstanceRequest();
                request.setNamespace(NAMESPACE_TEST);
                request.setService(validParam.getServiceName());

                InstancesResponse instancesResponse = consumerAPI.getOneInstance(request);
                Assert.assertEquals(1, instancesResponse.getInstances().length);

                Instance instance = instancesResponse.getInstances()[0];
                Assert.assertTrue(instance.isHealthy());
                Assert.assertFalse(instance.isIsolated());
                Assert.assertEquals(100, instance.getWeight());
            }
        }
    }

    private enum Operation {
        ALL_HEALTHY, HAS_UNHEALTHY
    }

    private static class ValidParam {

        final String serviceName;

        final int countAll;

        final int countHealth;

        final int countHasWeight;

        public ValidParam(String serviceName, int countAll, int countHealth, int countHasWeight) {
            this.serviceName = serviceName;
            this.countAll = countAll;
            this.countHealth = countHealth;
            this.countHasWeight = countHasWeight;
        }

        public String getServiceName() {
            return serviceName;
        }

        public int getCountAll() {
            return countAll;
        }

        public int getCountHealth() {
            return countHealth;
        }

        public int getCountHasWeight() {
            return countHasWeight;
        }
    }

}
