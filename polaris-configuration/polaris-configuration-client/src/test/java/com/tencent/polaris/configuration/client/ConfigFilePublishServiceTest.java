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

import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.client.internal.ConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.doThrow;

/**
 * @author fabian4 2023-04-06
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ConfigFilePublishServiceTest {

    @Mock
    private ConfigFileManager configFileManager;

    @InjectMocks
    private DefaultConfigFilePublishService defaultConfigFilePublishService;

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNamespaceBlank() {
        defaultConfigFilePublishService.createConfigFile("", "somegroup", "application.yaml", "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGroupBlank() {
        defaultConfigFilePublishService.createConfigFile("somenamespace", "", "application.yaml", "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFileNameBlank() {
        defaultConfigFilePublishService.createConfigFile("somenamespace", "somegroup", "", "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNamespaceBlank2() {
        defaultConfigFilePublishService.createConfigFile(new DefaultConfigFileMetadata("", "somegroup", "application.yaml"), "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGroupBlank2() {
        defaultConfigFilePublishService.createConfigFile(new DefaultConfigFileMetadata("somenamespace", "", "application.yaml"), "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFileNameBlank2() {
        defaultConfigFilePublishService.createConfigFile(new DefaultConfigFileMetadata("somenamespace", "somegroup", ""), "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNamespaceBlank() {
        defaultConfigFilePublishService.updateConfigFile("", "somegroup", "application.yaml", "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateGroupBlank() {
        defaultConfigFilePublishService.updateConfigFile("somenamespace", "", "application.yaml", "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateFileNameBlank() {
        defaultConfigFilePublishService.updateConfigFile("somenamespace", "somegroup", "", "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNamespaceBlank2() {
        defaultConfigFilePublishService.updateConfigFile(new DefaultConfigFileMetadata("", "somegroup", "application.yaml"), "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateGroupBlank2() {
        defaultConfigFilePublishService.updateConfigFile(new DefaultConfigFileMetadata("somenamespace", "", "application.yaml"), "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateFileNameBlank2() {
        defaultConfigFilePublishService.updateConfigFile(new DefaultConfigFileMetadata("somenamespace", "somegroup", ""), "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReleaseNamespaceBlank() {
        defaultConfigFilePublishService.releaseConfigFile("", "somegroup", "application.yaml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReleaseGroupBlank() {
        defaultConfigFilePublishService.releaseConfigFile("somenamespace", "", "application.yaml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReleaseFileNameBlank() {
        defaultConfigFilePublishService.releaseConfigFile("somenamespace", "somegroup", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReleaseNamespaceBlank2() {
        defaultConfigFilePublishService.releaseConfigFile(new DefaultConfigFileMetadata("", "somegroup", "application.yaml"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReleaseGroupBlank2() {
        defaultConfigFilePublishService.releaseConfigFile(new DefaultConfigFileMetadata("somenamespace", "", "application.yaml"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReleaseFileNameBlank2() {
        defaultConfigFilePublishService.releaseConfigFile(new DefaultConfigFileMetadata("somenamespace", "somegroup", ""));
    }

    @Test(expected = RuntimeException.class)
    public void testCreateConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        doThrow(new RuntimeException("test")).when(configFileManager).createConfigFile(configFileMetadata, "content");

        defaultConfigFilePublishService.createConfigFile("testNamespace", "testGroup", "testFile", "content");
    }

    @Test(expected = RuntimeException.class)
    public void testCreateConfigFile2() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        doThrow(new RuntimeException("test")).when(configFileManager).createConfigFile(configFileMetadata, "content");

        defaultConfigFilePublishService.createConfigFile(configFileMetadata, "content");
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        doThrow(new RuntimeException("test")).when(configFileManager).updateConfigFile(configFileMetadata, "content");

        defaultConfigFilePublishService.updateConfigFile("testNamespace", "testGroup", "testFile", "content");
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateConfigFile2() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        doThrow(new RuntimeException("test")).when(configFileManager).updateConfigFile(configFileMetadata, "content");

        defaultConfigFilePublishService.updateConfigFile(configFileMetadata, "content");
    }

    @Test(expected = RuntimeException.class)
    public void testReleaseConfigFile() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        doThrow(new RuntimeException("test")).when(configFileManager).releaseConfigFile(configFileMetadata);

        defaultConfigFilePublishService.releaseConfigFile("testNamespace", "testGroup", "testFile");
    }

    @Test(expected = RuntimeException.class)
    public void testReleaseConfigFile2() {
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");

        doThrow(new RuntimeException("test")).when(configFileManager).releaseConfigFile(configFileMetadata);

        defaultConfigFilePublishService.releaseConfigFile(configFileMetadata);
    }
}