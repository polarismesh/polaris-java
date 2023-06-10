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

package com.tencent.polaris.configuration.client;

import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.client.flow.DefaultConfigFileFlow;
import com.tencent.polaris.configuration.client.internal.ConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFileServiceTest {

    @Mock
    private ConfigFileManager configFileManager;
    @Mock
    private ConfigFile        configFile;

    private DefaultConfigFileService defaultConfigFileService;

    @Before
    public void before() {
        SDKContext context = mock(SDKContext.class);
        defaultConfigFileService = new DefaultConfigFileService(context);
        defaultConfigFileService.setConfigFileFlow(new DefaultConfigFileFlow(configFileManager));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNamespaceBlank() {
        defaultConfigFileService.getConfigFile("", "somegroup", "application.yaml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGroupBlank() {
        defaultConfigFileService.getConfigFile("somenamespace", "", "application.yaml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileNameBlank() {
        defaultConfigFileService.getConfigFile("somenamespace", "somegroup", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNamespaceBlank2() {
        defaultConfigFileService.getConfigFile(new DefaultConfigFileMetadata("", "somegroup", "application.yaml"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGroupBlank2() {
        defaultConfigFileService.getConfigFile(new DefaultConfigFileMetadata("somenamespace", "", "application.yaml"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileNameBlank2() {
        defaultConfigFileService.getConfigFile(new DefaultConfigFileMetadata("somenamespace", "somegroup", ""));
    }

    @Test
    public void testGetNormalConfigFile() {
        when(configFileManager.getConfigFile(any())).thenReturn(configFile);

        ConfigFile configFile2 = defaultConfigFileService.getConfigFile("somenamespace", "somegroup", "application.yaml");

        verify(configFileManager).getConfigFile(any());
        Assert.assertEquals(configFile2, configFile2);
    }

}
