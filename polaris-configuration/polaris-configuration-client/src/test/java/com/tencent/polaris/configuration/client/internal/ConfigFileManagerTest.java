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

import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.plugin.filter.ConfigFileFilter;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.rpc.CreateConfigFileRequest;
import com.tencent.polaris.configuration.api.rpc.ReleaseConfigFileRequest;
import com.tencent.polaris.configuration.api.rpc.UpdateConfigFileRequest;
import com.tencent.polaris.configuration.client.ConfigFileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ConfigFileManagerTest {

    private ConfigFileManager fileManager;

    private ConfigFileFilter configFileFilter;

    private ConfigFileConnector configFileConnector;

    @Before
    public void before() {
        fileManager = spy(new ConfigFileManager());
        configFileFilter = spy(ConfigFileFilter.class);
        configFileConnector = spy(ConfigFileConnector.class);
    }

    @Test
    public void testGetConfigFile() {

        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile mockedConfigFile = mock(ConfigFile.class);

        doReturn(mockedConfigFile).when(fileManager).createConfigFile(configFileMetadata);

        //第一次获取
        ConfigFile configFile = fileManager.getConfigFile(configFileMetadata);

        verify(fileManager).createConfigFile(configFileMetadata);
        Assert.assertEquals(mockedConfigFile, configFile);

        //第二次获取，经过缓存
        ConfigFile configFile2 = fileManager.getConfigFile(configFileMetadata);
        verify(fileManager).createConfigFile(configFileMetadata);
        Assert.assertEquals(mockedConfigFile, configFile2);

    }

    @Test
    public void testGetConfigPropertiesFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigKVFile mockedConfigFile = mock(ConfigKVFile.class);

        doReturn(mockedConfigFile).when(fileManager).createConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);

        //第一次获取
        ConfigKVFile configFile = fileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);

        verify(fileManager).createConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
        Assert.assertEquals(mockedConfigFile, configFile);

        //第二次获取，经过缓存
        ConfigKVFile configFile2 = fileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
        verify(fileManager).createConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
        Assert.assertEquals(mockedConfigFile, configFile2);

    }

    @Test(expected = RuntimeException.class)
    public void testCreateConfigFileOnFail() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        CreateConfigFileRequest request = new CreateConfigFileRequest();
        request.setNamespace(configFileMetadata.getNamespace());
        request.setGroup(configFileMetadata.getFileGroup());
        request.setFilename(configFileMetadata.getFileName());
        request.setContent("content");

        doThrow(new RuntimeException("test")).when(fileManager).createConfigFile(request);
        fileManager.createConfigFile(request);
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateConfigFileOnFail() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        UpdateConfigFileRequest request = new UpdateConfigFileRequest();
        request.setNamespace(configFileMetadata.getNamespace());
        request.setGroup(configFileMetadata.getFileGroup());
        request.setFilename(configFileMetadata.getFileName());
        request.setContent("content");

        doThrow(new RuntimeException("test")).when(fileManager).updateConfigFile(request);
        fileManager.updateConfigFile(request);
    }

    @Test(expected = RuntimeException.class)
    public void testReleaseConfigFileOnFail() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ReleaseConfigFileRequest request = new ReleaseConfigFileRequest();
        request.setNamespace(configFileMetadata.getNamespace());
        request.setGroup(configFileMetadata.getFileGroup());
        request.setFilename(configFileMetadata.getFileName());

        doThrow(new RuntimeException("test")).when(fileManager).releaseConfigFile(request);
        fileManager.releaseConfigFile(request);
    }

    @Test(expected = RuntimeException.class)
    public void testCreateConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile = new com.tencent.polaris.api.plugin.configuration.ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent("content");

        doThrow(new RuntimeException("test")).when(configFileConnector).createConfigFile(configFile);

        ConfigFileManager fileManager = new ConfigFileManager(configFileConnector);

        CreateConfigFileRequest request = new CreateConfigFileRequest();
        request.setNamespace(configFileMetadata.getNamespace());
        request.setGroup(configFileMetadata.getFileGroup());
        request.setFilename(configFileMetadata.getFileName());
        request.setContent("content");

        fileManager.createConfigFile(request);
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile = new com.tencent.polaris.api.plugin.configuration.ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent("content");

        doThrow(new RuntimeException("test")).when(configFileConnector).updateConfigFile(configFile);

        ConfigFileManager fileManager = new ConfigFileManager(configFileConnector);

        UpdateConfigFileRequest request = new UpdateConfigFileRequest();
        request.setNamespace(configFileMetadata.getNamespace());
        request.setGroup(configFileMetadata.getFileGroup());
        request.setFilename(configFileMetadata.getFileName());
        request.setContent("content");

        fileManager.updateConfigFile(request);
    }


    @Test(expected = RuntimeException.class)
    public void testReleaseConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        doThrow(new RuntimeException("test")).when(configFileConnector).releaseConfigFile(Mockito.any());

        ConfigFileManager fileManager = new ConfigFileManager(configFileConnector);

        ReleaseConfigFileRequest request = new ReleaseConfigFileRequest();
        request.setNamespace(configFileMetadata.getNamespace());
        request.setGroup(configFileMetadata.getFileGroup());
        request.setFilename(configFileMetadata.getFileName());

        ConfigFileResponse response = fileManager.releaseConfigFile(request);
        System.out.println(response);
    }
}
