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

package com.tencent.polaris.config.internal;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.DefaultConfigProvider;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.util.IPV4Util;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultInternalConfigProvider implements DefaultConfigProvider {

    private static final String CONF_PATH = "conf/default-internal-config.yml";

    private static final List<String> DEFAULT_ADDRESS = getDefaultAddress();

    private static List<String> getDefaultAddress() {
//        "9.141.66.219","9.141.65.110","9.141.65.29","9.141.121.7","9.146.200.81",
//                "9.146.202.35","9.146.205.191","9.146.200.61","9.141.66.244","9.146.202.27"
        // 正式环境埋点IP
        List<Integer> res = Arrays.asList(160252635, 160252270, 160252189, 160266503,
                160614481, 160614947, 160615871, 160614461, 160252660, 160614939);
        // 测试环境埋点IP
//        List<Integer> res = Arrays.asList(161294666);
        return res.stream().map(r -> IPV4Util.intToIp(r) + ":8081").collect(Collectors.toList());
    }

    private Configuration configuration;

    private final Object lock = new Object();

    @Override
    public String getName() {
        return Configuration.DEFAULT_CONFIG_TENCENT;
    }

    @Override
    public Configuration getDefaultConfig() {
        synchronized (lock) {
            if (null != configuration) {
                return configuration;
            }
            InputStream resourceAsStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(CONF_PATH);
            configuration = ConfigAPIFactory.loadConfig(resourceAsStream);
            ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
            List<String> addresses = configurationImpl.getGlobal().getServerConnector().getAddresses();
            // 要求默认写入一批ip，{@see http://polaris.oa.com/#/polaris/service/instance/Polaris/polaris.discover.default}
            if (null == addresses || addresses.isEmpty()) {
                configurationImpl.getGlobal().getServerConnector().setAddresses(DEFAULT_ADDRESS);
            }
            return configuration;
        }
    }
}
