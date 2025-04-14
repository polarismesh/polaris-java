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

package com.tencent.polaris.configuration.client;

import com.google.gson.reflect.TypeToken;

import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeListener;
import com.tencent.polaris.configuration.client.internal.ConfigFileRepo;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.when;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFileTest {

    @Mock
    private ConfigFileRepo   configFileRepo;
    @Mock
    private ConfigFileConfig configFileConfig;


    @Test
    public void testGetContent() {
        String content = "hello";
        when(configFileRepo.getContent()).thenReturn(content);

        ConfigFile configFile = new DefaultConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                                      ConfigFileTestUtils.testFileName, configFileRepo,
                                                      configFileConfig);
        Assert.assertEquals(content, configFile.getContent());
        Assert.assertTrue(configFile.hasContent());
        Assert.assertEquals(ConfigFileTestUtils.testNamespace, configFile.getNamespace());
        Assert.assertEquals(ConfigFileTestUtils.testGroup, configFile.getFileGroup());
        Assert.assertEquals(ConfigFileTestUtils.testFileName, configFile.getFileName());
    }

    @Test
    public void testGetJsonObject() {
        String content = "{\n"
                         + "\t\"name\":\"zhangsan\",\n"
                         + "\t\"age\":18,\n"
                         + "\t\"labels\": {\n"
                         + "\t  \"key1\":\"value1\"\n"
                         + "\t}\n"
                         + "}";
        when(configFileRepo.getContent()).thenReturn(content);

        ConfigFile configFile = new DefaultConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                                      ConfigFileTestUtils.testFileName, configFileRepo,
                                                      configFileConfig);
        Assert.assertEquals(content, configFile.getContent());

        ConfigFileTestUtils.User user = configFile.asJson(ConfigFileTestUtils.User.class, null);

        Assert.assertNotNull(user);
        Assert.assertEquals("zhangsan", user.getName());
        Assert.assertEquals(18, user.getAge());
        Assert.assertEquals(user.getLabels().size(), 1);
        Assert.assertEquals(user.getLabels().get("key1"), "value1");
    }

    @Test
    public void testGetJsonArray() {
        String content = "[\n"
                         + "\t{\n"
                         + "\t\t\"name\":\"zhangsan\",\n"
                         + "\t\t\"age\":18,\n"
                         + "\t\t\"labels\": {\n"
                         + "\t\t  \"key1\":\"value1\"\n"
                         + "\t\t}\n"
                         + "\t},\n"
                         + "\t{\n"
                         + "\t\t\"name\":\"lisi\",\n"
                         + "\t\t\"age\":20,\n"
                         + "\t\t\"labels\": {\n"
                         + "\t\t  \"key1\":\"value2\"\n"
                         + "\t\t}\n"
                         + "\t}\n"
                         + "]";
        when(configFileRepo.getContent()).thenReturn(content);

        ConfigFile configFile = new DefaultConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                                      ConfigFileTestUtils.testFileName, configFileRepo,
                                                      configFileConfig);
        Assert.assertEquals(content, configFile.getContent());

        List<ConfigFileTestUtils.User> users = configFile.asJson(new TypeToken<List<ConfigFileTestUtils.User>>() {
        }.getType(), null);

        Assert.assertNotNull(users);
        Assert.assertEquals(2, users.size());
        for (ConfigFileTestUtils.User user : users) {
            Assert.assertNotNull(user.getName());
            Assert.assertTrue(user.getAge() > 0);
            Assert.assertTrue(user.getLabels().size() > 0);
        }
    }

    @Test
    public void testModifiedContent() throws InterruptedException {
        String content = "hello";
        when(configFileRepo.getContent()).thenReturn(content);

        DefaultConfigFile configFile =
            new DefaultConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                  ConfigFileTestUtils.testFileName, configFileRepo,
                                  configFileConfig);
        Assert.assertEquals(content, configFile.getContent());
        Assert.assertTrue(configFile.hasContent());

        String newContent = "hello2";
        AtomicBoolean check = new AtomicBoolean(false);
        configFile.addChangeListener(new ConfigFileChangeListener() {
            @Override
            public void onChange(ConfigFileChangeEvent event) {
                check.set(event.getNewValue().equals(newContent) && event.getOldValue().equals(content)
                          && event.getChangeType() == ChangeType.MODIFIED);
            }
        });

        com.tencent.polaris.api.plugin.configuration.ConfigFile newConfigFile = new com.tencent.polaris.api.plugin.configuration.ConfigFile("ns", "group", "file");
        newConfigFile.setContent("hello2");
        configFile.onChange(ConfigFileTestUtils.assembleDefaultConfigFileMeta(), newConfigFile);

        TimeUnit.MILLISECONDS.sleep(100);

        Assert.assertTrue(check.get());
    }

    @Test
    public void testDeleteContent() throws InterruptedException {
        String content = "hello";
        when(configFileRepo.getContent()).thenReturn(content);

        DefaultConfigFile configFile =
            new DefaultConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                  ConfigFileTestUtils.testFileName, configFileRepo,
                                  configFileConfig);
        Assert.assertEquals(content, configFile.getContent());
        Assert.assertTrue(configFile.hasContent());

        AtomicBoolean check = new AtomicBoolean(false);
        configFile.addChangeListener(new ConfigFileChangeListener() {
            @Override
            public void onChange(ConfigFileChangeEvent event) {
                check.set(event.getNewValue() == null && event.getOldValue().equals(content)
                          && event.getChangeType() == ChangeType.DELETED);
            }
        });

        configFile.onChange(ConfigFileTestUtils.assembleDefaultConfigFileMeta(), null);

        TimeUnit.MILLISECONDS.sleep(100);

        Assert.assertTrue(check.get());
    }

    @Test
    public void testAddContent() throws InterruptedException {
        when(configFileRepo.getContent()).thenReturn(null);

        DefaultConfigFile configFile =
            new DefaultConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                  ConfigFileTestUtils.testFileName, configFileRepo,
                                  configFileConfig);
        Assert.assertNull(configFile.getContent());
        Assert.assertFalse(configFile.hasContent());

        String content = "hello";

        AtomicBoolean check = new AtomicBoolean(false);
        configFile.addChangeListener(new ConfigFileChangeListener() {
            @Override
            public void onChange(ConfigFileChangeEvent event) {
                check.set(event.getNewValue().equals(content) && event.getOldValue() == null
                          && event.getChangeType() == ChangeType.ADDED);
            }
        });

        com.tencent.polaris.api.plugin.configuration.ConfigFile newConfigFile = new com.tencent.polaris.api.plugin.configuration.ConfigFile("ns", "group", "file");
        newConfigFile.setContent(content);

        configFile.onChange(ConfigFileTestUtils.assembleDefaultConfigFileMeta(), newConfigFile);

        TimeUnit.MILLISECONDS.sleep(100);

        Assert.assertTrue(check.get());
    }

    @Test
    public void testContentNotChangedWithNull() throws InterruptedException {
        when(configFileRepo.getContent()).thenReturn(null);

        DefaultConfigFile configFile =
            new DefaultConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                  ConfigFileTestUtils.testFileName, configFileRepo,
                                  configFileConfig);
        Assert.assertNull(configFile.getContent());
        Assert.assertFalse(configFile.hasContent());

        AtomicBoolean check = new AtomicBoolean(false);
        configFile.addChangeListener(new ConfigFileChangeListener() {
            @Override
            public void onChange(ConfigFileChangeEvent event) {
                check.set(event.getNewValue() == null && event.getOldValue() == null
                          && event.getChangeType() == ChangeType.NOT_CHANGED);
            }
        });

        configFile.onChange(ConfigFileTestUtils.assembleDefaultConfigFileMeta(), null);

        TimeUnit.MILLISECONDS.sleep(100);

        Assert.assertTrue(check.get());
    }

    @Test
    public void testContentNotChanged() throws InterruptedException {
        String content = "hello";
        when(configFileRepo.getContent()).thenReturn(content);

        DefaultConfigFile configFile =
            new DefaultConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                  ConfigFileTestUtils.testFileName, configFileRepo,
                                  configFileConfig);

        AtomicBoolean check = new AtomicBoolean(false);
        configFile.addChangeListener(new ConfigFileChangeListener() {
            @Override
            public void onChange(ConfigFileChangeEvent event) {
                check.set(event.getNewValue().equals(content) && event.getOldValue().equals(content)
                          && event.getChangeType() == ChangeType.NOT_CHANGED);
            }
        });

        com.tencent.polaris.api.plugin.configuration.ConfigFile newConfigFile = new com.tencent.polaris.api.plugin.configuration.ConfigFile("ns", "group", "file");
        newConfigFile.setContent(content);

        configFile.onChange(ConfigFileTestUtils.assembleDefaultConfigFileMeta(), newConfigFile);

        TimeUnit.MILLISECONDS.sleep(100);

        Assert.assertTrue(check.get());
    }

    @Test
    public void testRemoveChangeListener() throws InterruptedException {
        String content = "hello";
        when(configFileRepo.getContent()).thenReturn(content);

        DefaultConfigFile configFile =
            new DefaultConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                  ConfigFileTestUtils.testFileName, configFileRepo,
                                  configFileConfig);

        String content2 = "hello2";
        AtomicBoolean invoked = new AtomicBoolean(false);
        AtomicBoolean check = new AtomicBoolean(false);
        ConfigFileChangeListener listener = new ConfigFileChangeListener() {
            @Override
            public void onChange(ConfigFileChangeEvent event) {
                check.set(event.getNewValue().equals(content2) && event.getOldValue().equals(content)
                          && event.getChangeType() == ChangeType.MODIFIED);
                invoked.set(true);
            }
        };
        configFile.addChangeListener(listener);

        com.tencent.polaris.api.plugin.configuration.ConfigFile newConfigFile = new com.tencent.polaris.api.plugin.configuration.ConfigFile("ns", "group", "file");
        newConfigFile.setContent(content2);
        configFile.onChange(ConfigFileTestUtils.assembleDefaultConfigFileMeta(), newConfigFile);

        TimeUnit.MILLISECONDS.sleep(100);

        Assert.assertTrue(invoked.get());
        Assert.assertTrue(check.get());

        //重置状态位
        invoked.set(false);
        configFile.removeChangeListener(listener);

        String content3 = "hello3";
        newConfigFile = new com.tencent.polaris.api.plugin.configuration.ConfigFile("ns", "group", "file");
        newConfigFile.setContent(content3);
        configFile.onChange(ConfigFileTestUtils.assembleDefaultConfigFileMeta(), newConfigFile);

        TimeUnit.MILLISECONDS.sleep(100);
        
        Assert.assertFalse(invoked.get());
    }

}
