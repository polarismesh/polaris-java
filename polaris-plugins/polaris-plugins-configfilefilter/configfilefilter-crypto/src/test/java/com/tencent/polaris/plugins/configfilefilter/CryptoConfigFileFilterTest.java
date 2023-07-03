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

import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author fabian4
 * @date 2023/6/14
 */
@RunWith(MockitoJUnitRunner.class)
public class CryptoConfigFileFilterTest {


    @Before
    public void setUp() {

    }

    @Test
    public void testDoFilter() {
        String content = "content";
        ConfigFile configFile = new ConfigFile("namespace",  "group", "fileName");
        configFile.setContent(content);

        CryptoConfigFileFilter cryptoConfigFileFilter = new CryptoConfigFileFilter();

//
//        ConfigFileResponse response = cryptoConfigFileFilter.execute(configFile, new Function<ConfigFile, ConfigFileResponse>() {
//            @Override
//            public ConfigFileResponse apply(ConfigFile configFile) {
//                configFile.setContent(configFile.getContent() + " apply");
//                return new ConfigFileResponse(1, "OK", configFile);
//            }
//        });

        String res = content + " beforeCrypto2" + " beforeCrypto1" + " apply" + " afterCrypto1" + " afterCrypto2";
//        assertEquals(res, response.getConfigFile().getContent());
    }

}
