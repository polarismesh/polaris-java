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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConfigFileChangeEvent}.
 *
 * @author evelynwei
 */
public class ConfigFileChangeEventTest {

    private static final String OLD_CONTENT = "jdbc.password=old-secret-value";

    private static final String NEW_CONTENT = "jdbc.password=new-secret-value";

    /**
     * 测试目的：验证变更事件的 toString 不再泄露变更前后的配置正文。
     * 测试场景：事件持有加密配置解密后的旧值与新值。
     * 验证内容：输出不含两者的值。
     */
    @Test
    public void testToStringWithoutValues() {
        // Arrange
        ConfigFileChangeEvent event = assembleChangeEvent();

        // Act
        String str = event.toString();

        // Assert
        assertThat(str).doesNotContain(OLD_CONTENT);
        assertThat(str).doesNotContain(NEW_CONTENT);
    }

    /**
     * 测试目的：验证问题定位能力不退化。
     * 测试场景：事件持有完整的文件坐标与变更类型。
     * 验证内容：输出包含文件坐标与变更类型。
     */
    @Test
    public void testToStringKeepsMetadata() {
        // Arrange
        ConfigFileChangeEvent event = assembleChangeEvent();

        // Act
        String str = event.toString();

        // Assert
        assertThat(str).contains("biz-config");
        assertThat(str).contains("MODIFIED");
    }

    /**
     * 测试目的：验证取值 API 不受 toString 收敛影响。
     * 测试场景：业务通过 getter 读取变更前后的值。
     * 验证内容：getter 仍返回原值。
     */
    @Test
    public void testGettersKeepValues() {
        // Arrange
        ConfigFileChangeEvent event = assembleChangeEvent();

        // Act & Assert
        assertThat(event.getOldValue()).isEqualTo(OLD_CONTENT);
        assertThat(event.getNewValue()).isEqualTo(NEW_CONTENT);
        assertThat(event.getChangeType()).isEqualTo(ChangeType.MODIFIED);
    }

    private ConfigFileChangeEvent assembleChangeEvent() {
        return new ConfigFileChangeEvent(new TestConfigFileMetadata(), OLD_CONTENT, NEW_CONTENT,
                ChangeType.MODIFIED);
    }

    /**
     * 测试用配置文件坐标，toString 输出三元组，用于验证事件仍保留可定位信息。
     */
    private static class TestConfigFileMetadata implements ConfigFileMetadata {

        @Override
        public String getNamespace() {
            return "default";
        }

        @Override
        public String getFileGroup() {
            return "biz-group";
        }

        @Override
        public String getFileName() {
            return "biz-config";
        }

        @Override
        public String getFileVersion() {
            return null;
        }

        @Override
        public String toString() {
            return "ConfigFileMetadata{namespace='default', fileGroup='biz-group', fileName='biz-config'}";
        }
    }
}
