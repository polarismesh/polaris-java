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

package com.tencent.polaris.configuration.client.factory;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.ConfigFileTestUtils;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFileFactoryManagerTest {


    private ConfigFileFactory               defaultConfigFileFactory;
    @InjectMocks
    private DefaultConfigFileFactoryManager configFileFactoryManager;


    @Before
    public void before() {
        defaultConfigFileFactory = mock(MockedConfigFileFactory.class);
        configFileFactoryManager.setDefaultConfigFileFactory(defaultConfigFileFactory);
    }

    @Test
    public void testGetDefaultFactory() {
        ConfigFileMetadata configFileMetadata = assembleConfigFileMeta();

        ConfigFileFactory configFileFactory = configFileFactoryManager.getConfigFileFactory(configFileMetadata);

        Assert.assertEquals(defaultConfigFileFactory, configFileFactory);
    }

    private DefaultConfigFileMetadata assembleConfigFileMeta() {
        return new DefaultConfigFileMetadata(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                             ConfigFileTestUtils.testFileName);
    }

    static class MockedConfigFileFactory implements ConfigFileFactory {

        @Override
        public ConfigFile createConfigFile(ConfigFileMetadata configFileMetadata) {
            return null;
        }

        @Override
        public ConfigKVFile createConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat fileFormat) {
            return null;
        }
    }
}
