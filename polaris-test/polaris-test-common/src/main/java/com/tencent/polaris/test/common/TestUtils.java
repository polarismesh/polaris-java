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

package com.tencent.polaris.test.common;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import java.util.Arrays;
import java.util.Collections;

public class TestUtils {

    public static final String SERVER_ADDRESS_ENV = "POLARIS_SEVER_ADDRESS";

    private static String[] getServerAddressFromEnv() {
        String addressStr = System.getenv(SERVER_ADDRESS_ENV);
        if (StringUtils.isBlank(addressStr)) {
            return null;
        }
        return addressStr.split(",");
    }

    private static String[] getServerAddressFromProperties() {
        String addressStr = System.getProperty(SERVER_ADDRESS_ENV);
        if (StringUtils.isBlank(addressStr)) {
            return null;
        }
        return addressStr.split(",");
    }

    /**
     * 从环境变量中获取服务端地址并创建配置
     *
     * @return 配置的服务端地址
     */
    public static Configuration configWithEnvAddress() {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        String[] addresses = getServerAddressFromEnv();
        if (addresses == null) {
            addresses = getServerAddressFromProperties();
        }
        if (null != addresses) {
            ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
            configurationImpl.setDefault();
            configurationImpl.getGlobal().getServerConnector().setAddresses(Arrays.asList(addresses.clone()));
            configurationImpl.getGlobal().getAPI().setTimeout(5000);
        }
        return configuration;
    }

    public static Configuration createSimpleConfiguration(int port) {
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setDefault();
        configuration.getGlobal().getServerConnector().setAddresses(
                Collections.singletonList(String.format("127.0.0.1:%d", port)));
        return configuration;
    }
}
