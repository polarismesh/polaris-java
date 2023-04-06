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

import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import com.tencent.polaris.factory.ConfigAPIFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.doThrow;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ConfigFilePublishFactoryTest {

    @Mock
    private ConfigFileConnector configFileConnector;

    @Test(expected = RuntimeException.class)
    public void testCreateConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        ConfigFile configFile = new ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent("content");

        doThrow(new RuntimeException("test")).when(configFileConnector).createConfigFile(configFile);

        SDKContext context = SDKContext.initContextByConfig(ConfigAPIFactory.defaultConfig());

        DefaultConfigFilePublishFactory.getInstance(context).createConfigFile(configFileMetadata, "content");
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        ConfigFile configFile = new ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent("content");

        doThrow(new RuntimeException("test")).when(configFileConnector).updateConfigFile(configFile);

        SDKContext context = SDKContext.initContextByConfig(ConfigAPIFactory.defaultConfig());

        DefaultConfigFilePublishFactory.getInstance(context).updateConfigFile(configFileMetadata, "content");
    }

    @Test(expected = RuntimeException.class)
    public void testUpsertConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        ConfigFile configFile = new ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent("content");

        doThrow(new RuntimeException("test")).when(configFileConnector).upsertConfigFile(configFile);

        SDKContext context = SDKContext.initContextByConfig(ConfigAPIFactory.defaultConfig());

        DefaultConfigFilePublishFactory.getInstance(context).upsertConfigFile(configFileMetadata, "content");
    }

    @Test(expected = RuntimeException.class)
    public void testReleaseConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        ConfigFile configFile = new ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent("content");

        doThrow(new RuntimeException("test")).when(configFileConnector).releaseConfigFile(configFile);

        SDKContext context = SDKContext.initContextByConfig(ConfigAPIFactory.defaultConfig());

        DefaultConfigFilePublishFactory.getInstance(context).releaseConfigFile(configFileMetadata);
    }
}
