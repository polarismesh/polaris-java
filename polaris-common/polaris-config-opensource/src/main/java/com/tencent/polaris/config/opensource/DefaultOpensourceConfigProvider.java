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

package com.tencent.polaris.config.opensource;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.DefaultConfigProvider;
import com.tencent.polaris.factory.ConfigAPIFactory;
import java.io.InputStream;

public class DefaultOpensourceConfigProvider implements DefaultConfigProvider {

    private static final String CONF_PATH = "conf/opensource-config.yml";

    private Configuration configuration;

    private final Object lock = new Object();

    @Override
    public String getName() {
        return Configuration.DEFAULT_CONFIG_OPENSOURCE;
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
            return configuration;
        }
    }
}