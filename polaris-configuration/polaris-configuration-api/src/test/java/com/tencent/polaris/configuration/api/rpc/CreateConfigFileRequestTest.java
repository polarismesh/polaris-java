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

package com.tencent.polaris.configuration.api.rpc;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link CreateConfigFileRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateConfigFileRequestTest {

    /**
     * 测试 setter/getter
     * 测试目的：验证各字段读写
     * 测试场景：设置各字段后读取
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testSettersAndGetters() {
        // Arrange
        CreateConfigFileRequest request = new CreateConfigFileRequest();
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        // Act
        request.setNamespace("Test");
        request.setGroup("group1");
        request.setFilename("app.yaml");
        request.setContent("key: value");
        request.setLabels(labels);

        // Assert
        assertThat(request.getNamespace()).isEqualTo("Test");
        assertThat(request.getGroup()).isEqualTo("group1");
        assertThat(request.getFilename()).isEqualTo("app.yaml");
        assertThat(request.getContent()).isEqualTo("key: value");
        assertThat(request.getLabels()).isEqualTo(labels);
    }

    /**
     * 测试 verify 通过
     * 测试目的：验证必填字段齐全时 verify 不抛异常
     * 测试场景：namespace/group/filename 均非空
     * 验证内容：verify 正常返回
     */
    @Test
    public void testVerifyPass() {
        // Arrange
        CreateConfigFileRequest request = new CreateConfigFileRequest();
        request.setNamespace("Test");
        request.setGroup("group1");
        request.setFilename("app.yaml");

        // Act & Assert
        assertThatCode(request::verify).doesNotThrowAnyException();
    }

    /**
     * 测试 verify 在 namespace 为空时抛异常
     * 测试目的：验证 namespace 为空时抛 IllegalArgumentException
     * 测试场景：仅设置 group 和 filename
     * 验证内容：抛出异常且消息包含 namespace
     */
    @Test
    public void testVerifyNamespaceBlank() {
        // Arrange
        CreateConfigFileRequest request = new CreateConfigFileRequest();
        request.setGroup("group1");
        request.setFilename("app.yaml");

        // Act & Assert
        assertThatThrownBy(request::verify)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("namespace");
    }

    /**
     * 测试 verify 在 group 为空时抛异常
     * 测试目的：验证 group 为空时抛 IllegalArgumentException
     * 测试场景：设置 namespace 和 filename，不设 group
     * 验证内容：抛出异常且消息包含 group
     */
    @Test
    public void testVerifyGroupBlank() {
        // Arrange
        CreateConfigFileRequest request = new CreateConfigFileRequest();
        request.setNamespace("Test");
        request.setFilename("app.yaml");

        // Act & Assert
        assertThatThrownBy(request::verify)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("group");
    }

    /**
     * 测试 verify 在 filename 为空时抛异常
     * 测试目的：验证 filename 为空时抛 IllegalArgumentException
     * 测试场景：设置 namespace 和 group，不设 filename
     * 验证内容：抛出异常且消息包含 name
     */
    @Test
    public void testVerifyFilenameBlank() {
        // Arrange
        CreateConfigFileRequest request = new CreateConfigFileRequest();
        request.setNamespace("Test");
        request.setGroup("group1");

        // Act & Assert
        assertThatThrownBy(request::verify)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    /**
     * 测试 Builder 构建完整对象
     * 测试目的：验证 Builder 能正确组装各字段
     * 测试场景：通过 Builder 设置所有字段后 build
     * 验证内容：各 getter 返回 Builder 设置的值
     */
    @Test
    public void testBuilderBuild() {
        // Arrange
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        // Act
        CreateConfigFileRequest request = CreateConfigFileRequest.Builder.aCreateConfigFileRequest()
                .namespace("Test")
                .group("group1")
                .filename("app.yaml")
                .content("key: value")
                .labels(labels)
                .build();

        // Assert
        assertThat(request.getNamespace()).isEqualTo("Test");
        assertThat(request.getGroup()).isEqualTo("group1");
        assertThat(request.getFilename()).isEqualTo("app.yaml");
        assertThat(request.getContent()).isEqualTo("key: value");
        assertThat(request.getLabels()).isEqualTo(labels);
    }
}
