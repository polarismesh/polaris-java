/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueProvider;
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueRegistration;
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

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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

    /**
     * 测试目的：provider 为 null 时立即抛 NullPointerException（fail-fast），避免静默清空已注册的 provider。
     * 测试场景：传 null 调用 registerEffectiveValueProvider。
     * 验证内容：抛出 NullPointerException，且异常信息包含 provider。
     */
    @Test
    public void testRegisterEffectiveValueProviderWithNull() {
        assertThatThrownBy(() -> fileManager.registerEffectiveValueProvider(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("provider");
    }

    /**
     * 测试目的：longPullService 未初始化（仅测试场景）时返回空句柄，而不是返回 null 或抛异常。
     * 测试场景：无参构造的 ConfigFileManager 注册 provider。
     * 验证内容：返回非 null 句柄，close() 正常执行不抛异常。
     */
    @Test
    public void testRegisterEffectiveValueProviderWithoutLongPullService() {
        ConfigEffectiveValueProvider provider = mock(ConfigEffectiveValueProvider.class);

        ConfigEffectiveValueRegistration registration = fileManager.registerEffectiveValueProvider(provider);

        Assert.assertNotNull(registration);
        assertThatCode(() -> registration.close()).doesNotThrowAnyException();
    }

    /**
     * 测试目的：longPullService 正常时，注册请求原样委托给它。
     * 测试场景：通过反射注入 mock 的 ConfigFileLongPullService。
     * 验证内容：返回的句柄就是 longPullService 返回的句柄。
     */
    @Test
    public void testRegisterEffectiveValueProviderDelegatesToLongPullService() throws Exception {
        ConfigFileManager manager = new ConfigFileManager();
        ConfigEffectiveValueProvider provider = mock(ConfigEffectiveValueProvider.class);
        ConfigEffectiveValueRegistration expected = mock(ConfigEffectiveValueRegistration.class);
        ConfigFileLongPullService longPullService = mock(ConfigFileLongPullService.class);
        when(longPullService.registerEffectiveValueProvider(provider)).thenReturn(expected);
        setPrivateField(manager, "longPullService", longPullService);

        ConfigEffectiveValueRegistration registration = manager.registerEffectiveValueProvider(provider);

        Assert.assertSame(expected, registration);
    }

    private static void setPrivateField(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
