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

package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.ConfigFileTestUtils;
import com.tencent.polaris.configuration.client.factory.ConfigFileFactory;
import com.tencent.polaris.configuration.client.factory.ConfigFileFactoryManager;

import com.tencent.polaris.configuration.client.factory.ConfigFilePublishFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ConfigFileManagerTest {

    @Mock
    private ConfigFileFactory        configFileFactory;
    @Mock
    private ConfigFilePublishFactory configFilePublishFactory;
    @Mock
    private ConfigFileFactoryManager configFileFactoryManager;
    @InjectMocks
    private DefaultConfigFileManager defaultConfigFileManager;

    @Before
    public void before() {
        defaultConfigFileManager.setConfigFileFactoryManager(configFileFactoryManager);
    }
    @Test
    public void testGetConfigFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile mockedConfigFile = mock(ConfigFile.class);

        when(configFileFactoryManager.getConfigFileFactory(any())).thenReturn(configFileFactory);
        when(configFileFactory.createConfigFile(configFileMetadata)).thenReturn(mockedConfigFile);

        //第一次获取
        ConfigFile configFile = defaultConfigFileManager.getConfigFile(configFileMetadata);

        verify(configFileFactoryManager).getConfigFileFactory(configFileMetadata);
        verify(configFileFactory).createConfigFile(configFileMetadata);
        Assert.assertEquals(mockedConfigFile, configFile);

        //第二次获取，经过缓存
        ConfigFile configFile2 = defaultConfigFileManager.getConfigFile(configFileMetadata);
        verify(configFileFactoryManager).getConfigFileFactory(configFileMetadata);
        verify(configFileFactory).createConfigFile(configFileMetadata);
        Assert.assertEquals(mockedConfigFile, configFile2);

    }

    @Test
    public void testGetConfigPropertiesFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigKVFile mockedConfigFile = mock(ConfigKVFile.class);

        when(configFileFactoryManager.getConfigFileFactory(any())).thenReturn(configFileFactory);
        when(configFileFactory.createConfigKVFile(configFileMetadata, ConfigFileFormat.Properties)).thenReturn(mockedConfigFile);

        //第一次获取
        ConfigKVFile configFile = defaultConfigFileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);

        verify(configFileFactoryManager).getConfigFileFactory(configFileMetadata);
        verify(configFileFactory).createConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
        Assert.assertEquals(mockedConfigFile, configFile);

        //第二次获取，经过缓存
        ConfigKVFile configFile2 = defaultConfigFileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
        verify(configFileFactoryManager).getConfigFileFactory(configFileMetadata);
        verify(configFileFactory).createConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
        Assert.assertEquals(mockedConfigFile, configFile2);

    }

    @Test(expected = RuntimeException.class)
    public void testCreateConfigFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        doThrow(new RuntimeException("test")).when(configFilePublishFactory).createConfigFile(configFileMetadata, "content");

        defaultConfigFileManager.createConfigFile(configFileMetadata, "content");
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateConfigFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        doThrow(new RuntimeException("test")).when(configFilePublishFactory).updateConfigFile(configFileMetadata, "content");

        defaultConfigFileManager.updateConfigFile(configFileMetadata, "content");
    }

    @Test(expected = RuntimeException.class)
    public void testUpsertConfigFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        doThrow(new RuntimeException("test")).when(configFilePublishFactory).upsertConfigFile(configFileMetadata, "content");

        defaultConfigFileManager.upsertConfigFile(configFileMetadata, "content");
    }

    @Test(expected = RuntimeException.class)
    public void testReleaseConfigFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        doThrow(new RuntimeException("test")).when(configFilePublishFactory).releaseConfigFile(configFileMetadata);

        defaultConfigFileManager.releaseConfigFile(configFileMetadata);
    }
}
