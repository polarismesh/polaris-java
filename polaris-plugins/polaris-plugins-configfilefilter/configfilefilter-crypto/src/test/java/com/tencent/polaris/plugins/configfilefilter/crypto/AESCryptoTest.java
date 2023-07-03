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

package com.tencent.polaris.plugins.configfilefilter.crypto;

import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.plugins.configfilefilter.CryptoConfigFileFilter;
import com.tencent.polaris.plugins.configfilefilter.util.AESUtil;
import com.tencent.polaris.plugins.configfilefilter.util.RSAUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.PublicKey;
import java.util.Base64;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fabian4
 * @date 2023/7/1
 */
@RunWith(MockitoJUnitRunner.class)
public class AESCryptoTest {

    private ConfigFile configFile;

    private ConfigFileResponse configFileResponse;

    private AESCrypto aesCrypto;

    @Before
    public void Before() {
        configFile = new ConfigFile("namespace",  "group", "fileName");
        configFile.setDataKey("dataKey");
        configFileResponse = mock(ConfigFileResponse.class);
        aesCrypto = new AESCrypto();
//        aesCrypto.init(mock(InitContext.class));
    }

    @Test
    public void testDoFilter() {
        String content = "content";

//        ConfigFileResponse response = aesCrypto.doFilter(configFile, new Function<ConfigFile, ConfigFileResponse>() {
//            @Override
//            public ConfigFileResponse apply(ConfigFile configFile) {
//
//                assertTrue(configFile.isEncrypted());
//                assertNotNull(configFile.getPublicKey());
//
//                byte[] dataKey = AESUtil.generateAesKey();
//                PublicKey publicKey = aesCrypto.getRsaService().getPublicKey();
//                byte[] encryptDateKey = RSAUtil.encrypt(dataKey, publicKey);
//                configFile.setDataKey(Base64.getEncoder().encodeToString(encryptDateKey));
//
//
//                configFile.setContent(AESUtil.encrypt(content, dataKey));
//                configFile.setEncrypted(Boolean.TRUE);
//                when(configFileResponse.getCode()).thenReturn(ServerCodes.EXECUTE_SUCCESS);
//                when(configFileResponse.getConfigFile()).thenReturn(configFile);
//
//                return configFileResponse;
//            }
//        }).apply(configFile);

//        assertEquals(content, response.getConfigFile().getContent());
    }

}
