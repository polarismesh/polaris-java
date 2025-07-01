/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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
import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * ServiceExpireTest.java
 *
 * @author andrewshan
 * @date 2019/9/7
 */
public class ServiceExpireTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceExpireTest.class);

    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        InstanceParameter parameter = new InstanceParameter();
        parameter.setHealthy(true);
        parameter.setIsolated(false);
        parameter.setWeight(100);
        for (int i = 0; i < 2; i++) {
            namingServer.getNamingService().batchAddInstances(
                    new ServiceKey(NAMESPACE_TEST, SERVICES[i]), 10100, 10, parameter);
        }
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    private static class GetServiceTask implements Runnable {

        private final ConsumerAPI consumerAPI;

        private final CountDownLatch countDownLatch;

        private final int index;

        GetServiceTask(ConsumerAPI consumerAPI, CountDownLatch countDownLatch, int index) {
            this.consumerAPI = consumerAPI;
            this.countDownLatch = countDownLatch;
            this.index = index;
        }

        @Override
        public void run() {
            try {
                Assert.assertNotNull(consumerAPI);
                GetOneInstanceRequest req = new GetOneInstanceRequest();
                req.setNamespace(NAMESPACE_TEST);
                req.setService(SERVICES[index]);
                req.setTimeoutMs(2 * 1000);
                long startTime = System.currentTimeMillis();
                InstancesResponse instances = consumerAPI.getOneInstance(req);
                long endTime = System.currentTimeMillis();
                LOG.info(String.format("testGetInstance time consumer is %dms", endTime - startTime));
                Assert.assertEquals(1, instances.getInstances().length);
            } catch (PolarisException e) {
                Assert.fail(e.getMessage());
            } finally {
                if (null != countDownLatch) {
                    countDownLatch.countDown();
                }
            }
        }
    }

    @Test
    public void testMultiServices() {
        ConsumerAPI consumerAPI = null;
        try {
            Configuration configuration = createConfiguration();
            consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration);
            CountDownLatch countDownLatch = new CountDownLatch(2);
            Thread firstThread = new Thread(new GetServiceTask(consumerAPI, countDownLatch, 0));
            Thread secondThread = new Thread(new GetServiceTask(consumerAPI, countDownLatch, 1));
            firstThread.start();
            secondThread.start();
            countDownLatch.await(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (null != consumerAPI) {
                consumerAPI.destroy();
            }
        }
    }

    @Test
    public void testServiceExpiration() {
        ConsumerAPI consumerAPI = null;
        try {
            Configuration configuration = createConfiguration();
            consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration);
            Runnable firstThread = new GetServiceTask(consumerAPI, null, 0);
            Runnable secondThread = new GetServiceTask(consumerAPI, null, 1);
            firstThread.run();
            Utils.sleepUninterrupted(2000);
            secondThread.run();
            LOG.info("wait for expire");
            Utils.sleepUninterrupted(6000);
            firstThread.run();
            secondThread.run();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (null != consumerAPI) {
                consumerAPI.destroy();
            }
        }

    }

    private static Configuration createConfiguration() {
        ConfigurationImpl configuration = (ConfigurationImpl) TestUtils.configWithEnvAddress();
        configuration.setDefault();
        configuration.getConsumer().getLocalCache().setServiceExpireTime(5000);
        configuration.getGlobal().getServerConnector().setServerSwitchInterval(1500L);
        return configuration;
    }
}