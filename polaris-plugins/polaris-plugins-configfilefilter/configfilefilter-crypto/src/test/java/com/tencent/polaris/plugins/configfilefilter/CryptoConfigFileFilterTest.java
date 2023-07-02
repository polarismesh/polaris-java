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

package com.tencent.polaris.plugins.configfilefilter;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.plugin.filter.CryptoChain;
import com.tencent.polaris.plugins.configfilefilter.util.AESUtil;
import com.tencent.polaris.plugins.configfilefilter.util.RSAUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fabian4
 * @date 2023/6/14
 */
@RunWith(MockitoJUnitRunner.class)
public class CryptoConfigFileFilterTest {

    private CryptoChain crypto1;

    private CryptoChain crypto2;

    @Before
    public void setUp() {
        crypto1 = new CryptoChain() {
            @Override
            public Function<ConfigFile, ConfigFileResponse> doFilter(ConfigFile configFile, Function<ConfigFile, ConfigFileResponse> next) {
                return new Function<ConfigFile, ConfigFileResponse>() {
                    @Override
                    public ConfigFileResponse apply(ConfigFile configFile) {
                        configFile.setContent(configFile.getContent() + " beforeCrypto1");
                        ConfigFileResponse response = next.apply(configFile);
                        response.getConfigFile().setContent(response.getConfigFile().getContent() + " afterCrypto1");
                        return response;
                    }
                };
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public PluginType getType() {
                return null;
            }

            @Override
            public void init(InitContext ctx) throws PolarisException {}

            @Override
            public void postContextInit(Extensions ctx) throws PolarisException {}

            @Override
            public void destroy() {}
        };

        crypto2 = new CryptoChain() {

            @Override
            public Function<ConfigFile, ConfigFileResponse> doFilter(ConfigFile configFile, Function<ConfigFile, ConfigFileResponse> next) {
                return new Function<ConfigFile, ConfigFileResponse>() {
                    @Override
                    public ConfigFileResponse apply(ConfigFile configFile) {
                        configFile.setContent(configFile.getContent() + " beforeCrypto2");
                        ConfigFileResponse response = next.apply(configFile);
                        response.getConfigFile().setContent(response.getConfigFile().getContent() + " afterCrypto2");
                        return response;
                    }
                };
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public PluginType getType() {
                return null;
            }

            @Override
            public void init(InitContext ctx) throws PolarisException {}

            @Override
            public void postContextInit(Extensions ctx) throws PolarisException {}

            @Override
            public void destroy() {}
        };

    }

    @Test
    public void testDoFilter() {
        String content = "content";
        ConfigFile configFile = new ConfigFile("namespace",  "group", "fileName");
        configFile.setContent(content);

        CryptoConfigFileFilter cryptoConfigFileFilter = new CryptoConfigFileFilter();
        cryptoConfigFileFilter.getChain().add(crypto1);
        cryptoConfigFileFilter.getChain().add(crypto2);

        ConfigFileResponse response = cryptoConfigFileFilter.execute(configFile, new Function<ConfigFile, ConfigFileResponse>() {
            @Override
            public ConfigFileResponse apply(ConfigFile configFile) {
                configFile.setContent(configFile.getContent() + " apply");
                return new ConfigFileResponse(1, "OK", configFile);
            }
        });

        String res = content + " beforeCrypto2" + " beforeCrypto1" + " apply" + " afterCrypto1" + " afterCrypto2";
        assertEquals(res, response.getConfigFile().getContent());
    }

}
