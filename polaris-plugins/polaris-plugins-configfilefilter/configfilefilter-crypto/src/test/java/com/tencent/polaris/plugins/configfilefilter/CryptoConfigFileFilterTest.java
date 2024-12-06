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

package com.tencent.polaris.plugins.configfilefilter;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.plugin.filter.Crypto;
import com.tencent.polaris.factory.config.configuration.CryptoConfigImpl;
import com.tencent.polaris.plugins.configfilefilter.service.RSAService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author fabian4
 * @date 2023/6/14
 */
@RunWith(MockitoJUnitRunner.class)
public class CryptoConfigFileFilterTest {

    @Mock
    private Crypto crypto;

    @Mock
    private RSAService rsaService;

    @Before
    public void setUp() {
        crypto = new Crypto() {
            @Override
            public void doEncrypt(ConfigFile configFile) {

            }

            @Override
            public void doDecrypt(ConfigFile configFile, byte[] password) {
                configFile.setContent(configFile.getContent() + "-doCrypto");
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
            public void init(InitContext ctx) throws PolarisException {

            }

            @Override
            public void postContextInit(Extensions ctx) throws PolarisException {

            }

            @Override
            public void destroy() {

            }
        };

        when(rsaService.getPKCS1PublicKey()).thenReturn("RSAPublicKey");
        when(rsaService.decrypt(any())).thenReturn(null);
    }

    @Test
    public void testDoFilter() {
        String content = "content";
        ConfigFile configFile = new ConfigFile("namespace",  "group", "fileName");
        configFile.setContent(content);

        CryptoConfigFileFilter cryptoConfigFileFilter = new CryptoConfigFileFilter(crypto, rsaService, new CryptoConfigImpl(), new HashMap<>());

        ConfigFileResponse response = cryptoConfigFileFilter.doFilter(configFile, new Function<ConfigFile, ConfigFileResponse>() {
            @Override
            public ConfigFileResponse apply(ConfigFile configFile) {
                configFile.setContent(configFile.getContent() + "-apply");
                configFile.setDataKey(configFile.getPublicKey());
                return new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "OK", configFile);
            }
        }).apply(configFile);

        String res = content + "-apply" + "-doCrypto";
        assertEquals(res, response.getConfigFile().getContent());
        assertEquals("RSAPublicKey", configFile.getDataKey());
    }

}
