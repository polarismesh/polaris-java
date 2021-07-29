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

package com.tencent.polaris.factory.api.test;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.APIFactory;
import com.tencent.polaris.test.common.TestUtils;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Test;

/**
 * 测试API工厂相关接口
 *
 * @author andrewshan
 * @date 2019/8/28
 */
public class APIFactoryTest {

    @Test
    public void testInitContextByFile() {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("conf/default.yaml");
        SDKContext sdkContext = null;
        try {
            sdkContext = APIFactory.initContextByFile(resourceAsStream);
            Supplier plugins = sdkContext.getPlugins();
            Plugin plugin = plugins
                    .getPlugin(PluginTypes.CIRCUIT_BREAKER.getBaseType(), DefaultPlugins.CIRCUIT_BREAKER_ERROR_COUNT);
            Assert.assertNotNull(plugin);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (null != sdkContext) {
                sdkContext.destroy();
            }
        }
    }

    @Test
    public void testInitContextByConfig() {
        SDKContext sdkContext = null;
        try {
            sdkContext = APIFactory.initContextByConfig(TestUtils.createSimpleConfiguration(8888));
            Supplier plugins = sdkContext.getPlugins();
            Plugin plugin = plugins.getPlugin(
                    PluginTypes.LOAD_BALANCER.getBaseType(), LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_RANDOM);
            Assert.assertNotNull(plugin);
        } catch (PolarisException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (null != sdkContext) {
                sdkContext.destroy();
            }
        }
    }

    @Test
    public void testCreateConsumerAPIByFile() {
        // FIXME @thrteenwang update route config in default.yaml
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("conf/default.yaml");
        ConsumerAPI consumerAPI = null;
        try {
            consumerAPI = APIFactory.createConsumerAPIByFile(resourceAsStream);
            Assert.assertNotNull(consumerAPI);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (null != consumerAPI) {
                consumerAPI.destroy();
            }
        }
    }

    @Test
    public void testCreateProviderAPIByFile() {
        // FIXME @thrteenwang update route config in default.yaml
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("conf/default.yaml");
        ProviderAPI providerAPI = null;
        try {
            providerAPI = APIFactory.createProviderAPIByFile(resourceAsStream);
            Assert.assertNotNull(providerAPI);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (null != providerAPI) {
                providerAPI.destroy();
            }
        }
    }

}
