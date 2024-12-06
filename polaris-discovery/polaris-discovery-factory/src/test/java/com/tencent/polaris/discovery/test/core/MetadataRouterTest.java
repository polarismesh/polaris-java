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

package com.tencent.polaris.discovery.test.core;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_PRODUCTION;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.MetadataFailoverType;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author starkwen
 * @date 2021/2/25 下午4:56
 */
public class MetadataRouterTest {

    public static final String METADATA_SERVICE = "192000705:65767";
    public static final String METADATA_SERVICE_1 = "192000705:65584";
    private static final String POLARIS_SERVER_ADDRESS_PROPERTY = "POLARIS_SEVER_ADDRESS";
    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(POLARIS_SERVER_ADDRESS_PROPERTY, String.format("127.0.0.1:%d", namingServer.getPort()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        /**
         * 该服务下有四个实例
         * 1. 127.0.0.1:80 不健康 Env-set:1-0
         * 2. 127.0.0.1:70 健康 Env-set:1-0
         * 3. 127.0.0.1:100 健康 Env-set:1-0
         * 4. 127.0.0.1:90 健康
         */
        ServiceKey serviceKey = new ServiceKey(NAMESPACE_PRODUCTION, METADATA_SERVICE);
        InstanceParameter parameter = new InstanceParameter();
        parameter.setWeight(100);
        parameter.setHealthy(false);
        parameter.setIsolated(false);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Env-set", "1-0");
        parameter.setMetadata(metadata);
        namingServer.getNamingService().addInstance(serviceKey, new Node("127.0.0.1", 80), parameter);
        parameter.setHealthy(true);
        namingServer.getNamingService().addInstance(serviceKey, new Node("127.0.0.1", 70), parameter);
        namingServer.getNamingService().addInstance(serviceKey, new Node("127.0.0.1", 100), parameter);
        parameter.setMetadata(null);
        namingServer.getNamingService().addInstance(serviceKey, new Node("127.0.0.1", 90), parameter);
        /**
         * 该服务下有两个实例
         * 1. 127.0.0.1:80 不健康 Env-set:1-0
         * 2. 127.0.0.1:81 不健康
         */
        ServiceKey serviceKey1 = new ServiceKey(NAMESPACE_PRODUCTION, METADATA_SERVICE_1);
        parameter.setMetadata(metadata);
        parameter.setHealthy(false);
        namingServer.getNamingService().addInstance(serviceKey1, new Node("127.0.0.1", 80), parameter);
        parameter.setMetadata(null);
        namingServer.getNamingService().addInstance(serviceKey1, new Node("127.0.0.1", 81), parameter);
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
            System.clearProperty(POLARIS_SERVER_ADDRESS_PROPERTY);
        }
    }

    @Test
    public void testNormalScene() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumer = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 该服务下有四个实例，其中有两个满足metadata路由(分别为port70，100),80 90端口不满足
            for (int i = 0; i < 10; i++) {
                GetOneInstanceRequest getInstances2 = new GetOneInstanceRequest();
                getInstances2.setNamespace(NAMESPACE_PRODUCTION);
                getInstances2.setService(METADATA_SERVICE);
                Map<String, String> map = new HashMap<>();
                map.put("Env-set", "1-0");
                getInstances2.setMetadata(map);
                InstancesResponse ins = null;
                try {
                    ins = consumer.getOneInstance(getInstances2);
                } catch (PolarisException e) {
                    e.printStackTrace();
                }
                int port = ins.getInstances()[0].getPort();
                Assert.assertNotEquals(90, port);
                Assert.assertNotEquals(80, port);
            }
        }
    }

    @Test
    public void testFailoverNoneScene() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumer = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 该服务下有四个实例，只有一个80端口实例满足metadata路由，但是不健康，降级策略采用默认不降级
            for (int i = 0; i < 10; i++) {
                GetOneInstanceRequest getInstances2 = new GetOneInstanceRequest();
                getInstances2.setNamespace(NAMESPACE_PRODUCTION);
                getInstances2.setService(METADATA_SERVICE);
                Map<String, String> map = new HashMap<>();
                map.put("Env-set", "1-1");
                getInstances2.setMetadata(map);
                InstancesResponse ins = null;
                try {
                    ins = consumer.getOneInstance(getInstances2);
                } catch (PolarisException e) {
                    Assert.assertEquals(ErrorCode.METADATA_MISMATCH, e.getCode());
                }
            }
        }
    }

    @Test
    public void testFailoverAllScene() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumer = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 该服务下有四个实例，只有一个80端口实例满足metadata路由，
            // 但是不健康，降级策略采用默认返回所有健康实例，其中80端口实例为不健康，故80端口的实例不会返回
            for (int i = 0; i < 10; i++) {
                GetOneInstanceRequest getInstances2 = new GetOneInstanceRequest();
                getInstances2.setNamespace("Production");
                getInstances2.setService(METADATA_SERVICE);
                Map<String, String> map = new HashMap<>();
                map.put("Env-set", "1-1");
                getInstances2.setMetadata(map);
                getInstances2.setMetadataFailoverType(MetadataFailoverType.METADATAFAILOVERALL);
                InstancesResponse ins = null;
                try {
                    ins = consumer.getOneInstance(getInstances2);
                } catch (PolarisException e) {
                    e.printStackTrace();
                }
                Assert.assertNotEquals(80, ins.getInstances()[0].getPort());
            }
        }
    }

    @Test
    public void testFailoverNotKeyScene() {
        // 传入Env-set:1-1 ,应该返回第4个实例，因为前三个都包含Env-set这个key
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumer = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < 10; i++) {
                GetOneInstanceRequest getInstances2 = new GetOneInstanceRequest();
                getInstances2.setNamespace("Production");
                getInstances2.setService(METADATA_SERVICE);
                Map<String, String> map = new HashMap<>();
                map.put("Env-set", "1-1");
                getInstances2.setMetadata(map);
                //TODO: 通过配置文件来设置该配置testFailoverAllScene
                getInstances2.setMetadataFailoverType(MetadataFailoverType.METADATAFAILOVERNOTKEY);
                InstancesResponse ins = null;
                try {
                    ins = consumer.getOneInstance(getInstances2);
                } catch (PolarisException e) {
                    Assert.fail(e.getMessage());
                }
                Assert.assertEquals(90, ins.getInstances()[0].getPort());
                Assert.assertEquals("127.0.0.1", ins.getInstances()[0].getHost());
            }
        }
    }

    @Test
    public void testFailoverNotKeyScene2() {
        // 传入Env-set:1-1 ,应该返回第2个实例，因为第一个都包含Env-set这个key
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumer = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            // 该服务下有两个实例，都不健康，且只有一个实例满足metadata路由，
            // 降级策略采用默认返回所有非metadata健康实例，故只返回不满足metadata的那个实例
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < 10; i++) {
                GetOneInstanceRequest getInstances2 = new GetOneInstanceRequest();
                getInstances2.setNamespace(NAMESPACE_PRODUCTION);
                getInstances2.setService(METADATA_SERVICE_1);
                Map<String, String> map = new HashMap<>();
                map.put("Env-set", "1-1");
                getInstances2.setMetadata(map);
                getInstances2.setMetadataFailoverType(MetadataFailoverType.METADATAFAILOVERNOTKEY);
                InstancesResponse ins = null;
                try {
                    ins = consumer.getOneInstance(getInstances2);
                } catch (PolarisException e) {
                    e.printStackTrace();
                }
                Assert.assertEquals(81, ins.getInstances()[0].getPort());
            }
        }
    }
}
