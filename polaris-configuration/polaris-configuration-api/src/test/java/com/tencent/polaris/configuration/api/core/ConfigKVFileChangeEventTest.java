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

package com.tencent.polaris.configuration.api.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConfigKVFileChangeEvent}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigKVFileChangeEventTest {

    private ConfigPropertyChangeInfo buildInfo(String name, Object oldValue, Object newValue, ChangeType type) {
        return new ConfigPropertyChangeInfo(name, oldValue, newValue, type);
    }

    /**
     * 测试 changedKeys 返回所有变更 key
     * 测试目的：验证 changedKeys 返回变更信息的全部 key
     * 测试场景：构造包含两个变更 key 的事件
     * 验证内容：changedKeys 包含两个 key
     */
    @Test
    public void testChangedKeys() {
        // Arrange
        Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
        changeInfos.put("key1", buildInfo("key1", "v1", "v2", ChangeType.MODIFIED));
        changeInfos.put("key2", buildInfo("key2", null, "v3", ChangeType.ADDED));
        ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos, null);

        // Act
        Set<String> keys = event.changedKeys();

        // Assert
        assertThat(keys).containsExactlyInAnyOrder("key1", "key2");
    }

    /**
     * 测试 getChangeInfo 返回对应 key 的变更信息
     * 测试目的：验证 getChangeInfo 能按 key 取出变更信息
     * 测试场景：构造包含 key1 的事件，查询 key1
     * 验证内容：返回的变更信息 propertyName 为 key1
     */
    @Test
    public void testGetChangeInfo() {
        // Arrange
        Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
        changeInfos.put("key1", buildInfo("key1", "v1", "v2", ChangeType.MODIFIED));
        ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos, null);

        // Act
        ConfigPropertyChangeInfo info = event.getChangeInfo("key1");

        // Assert
        assertThat(info).isNotNull();
        assertThat(info.getPropertyName()).isEqualTo("key1");
    }

    /**
     * 测试 getChangeInfo 对不存在 key 返回 null
     * 测试目的：验证 getChangeInfo 对未知 key 的兜底
     * 测试场景：查询不存在的 key3
     * 验证内容：返回 null
     */
    @Test
    public void testGetChangeInfoNotFound() {
        // Arrange
        ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(new HashMap<>(), null);

        // Act
        ConfigPropertyChangeInfo info = event.getChangeInfo("key3");

        // Assert
        assertThat(info).isNull();
    }

    /**
     * 测试 isChanged 判断 key 是否变更
     * 测试目的：验证 isChanged 对存在/不存在 key 的返回
     * 测试场景：构造包含 key1 的事件
     * 验证内容：key1 返回 true，key2 返回 false
     */
    @Test
    public void testIsChanged() {
        // Arrange
        Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
        changeInfos.put("key1", buildInfo("key1", "v1", "v2", ChangeType.MODIFIED));
        ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos, null);

        // Assert
        assertThat(event.isChanged("key1")).isTrue();
        assertThat(event.isChanged("key2")).isFalse();
    }

    /**
     * 测试 getPropertyOldValue 和 getPropertyNewValue
     * 测试目的：验证按 key 取 oldValue/newValue
     * 测试场景：构造 key1 的 MODIFIED 变更
     * 验证内容：oldValue/newValue 与入参一致
     */
    @Test
    public void testGetPropertyOldAndNewValue() {
        // Arrange
        Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
        changeInfos.put("key1", buildInfo("key1", "old", "new", ChangeType.MODIFIED));
        ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos, null);

        // Assert
        assertThat(event.getPropertyOldValue("key1")).isEqualTo("old");
        assertThat(event.getPropertyNewValue("key1")).isEqualTo("new");
    }

    /**
     * 测试 getPropertyOldValue 对不存在 key 返回 null
     * 测试目的：验证对未知 key 取 oldValue 的兜底
     * 测试场景：查询不存在的 key
     * 验证内容：oldValue/newValue 均为 null
     */
    @Test
    public void testGetPropertyValueNotFound() {
        // Arrange
        ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(new HashMap<>(), null);

        // Assert
        assertThat(event.getPropertyOldValue("missing")).isNull();
        assertThat(event.getPropertyNewValue("missing")).isNull();
    }

    /**
     * 测试 getPropertiesChangeType 对存在 key
     * 测试目的：验证按 key 取变更类型
     * 测试场景：构造 key1 为 DELETED 的变更
     * 验证内容：getPropertiesChangeType 返回 DELETED
     */
    @Test
    public void testGetPropertiesChangeType() {
        // Arrange
        Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
        changeInfos.put("key1", buildInfo("key1", "v1", null, ChangeType.DELETED));
        ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos, null);

        // Assert
        assertThat(event.getPropertiesChangeType("key1")).isEqualTo(ChangeType.DELETED);
    }

    /**
     * 测试 getPropertiesChangeType 对不存在 key 返回 NOT_CHANGED
     * 测试目的：验证对未知 key 的变更类型兜底为 NOT_CHANGED
     * 测试场景：查询不存在的 key
     * 验证内容：返回 NOT_CHANGED
     */
    @Test
    public void testGetPropertiesChangeTypeNotFound() {
        // Arrange
        ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(new HashMap<>(), null);

        // Assert
        assertThat(event.getPropertiesChangeType("missing")).isEqualTo(ChangeType.NOT_CHANGED);
    }
}
