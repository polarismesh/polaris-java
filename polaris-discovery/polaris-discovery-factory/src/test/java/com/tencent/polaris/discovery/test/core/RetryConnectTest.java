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

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICES;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RetryConnectTest.java
 *
 * @author andrewshan
 * @date 2019/9/7
 */
public class RetryConnectTest {

    private static final Logger LOG = LoggerFactory.getLogger(RetryConnectTest.class);

    private NamingServer namingServer;

    @Before
    public void before() {
        Thread startThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Utils.sleepUninterrupted(1000);
                LOG.info("now start the naming server mock");
                try {
                    namingServer = NamingServer.startNamingServer(10081);
                } catch (Exception e) {
                    Assert.fail(e.getMessage());
                }
                LOG.info("finish starting the naming server mock");
                InstanceParameter parameter = new InstanceParameter();
                parameter.setHealthy(true);
                parameter.setIsolated(false);
                parameter.setWeight(100);
                namingServer.getNamingService().batchAddInstances(
                        new ServiceKey(NAMESPACE_TEST, SERVICES[0]), 10100, 10, parameter);
            }
        });
        startThread.start();
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    public static Configuration createMaxRetryConfiguration() {
        ConfigurationImpl configuration = (ConfigurationImpl) TestUtils.configWithEnvAddress();
        configuration.setDefault();
        configuration.getGlobal().getAPI().setMaxRetryTimes(50);
        configuration.getGlobal().getAPI().setRetryInterval(100);
        return configuration;
    }

    @Test
    public void testRetryConsumer() {
        ConsumerAPI consumerAPI = null;
        try {
            Configuration simpleConfiguration = createMaxRetryConfiguration();
            consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(simpleConfiguration);
            Assert.assertNotNull(consumerAPI);
            GetInstancesRequest req = new GetInstancesRequest();
            req.setNamespace(NAMESPACE_TEST);
            req.setService(SERVICES[0]);
            req.setTimeoutMs(5000);
            long startTime = System.currentTimeMillis();
            consumerAPI.getInstances(req);
            long endTime = System.currentTimeMillis();
            LOG.info(String.format("testGetInstances time consumer is %dms", endTime - startTime));
        } catch (PolarisException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (null != consumerAPI) {
                consumerAPI.destroy();
            }
        }
    }
}