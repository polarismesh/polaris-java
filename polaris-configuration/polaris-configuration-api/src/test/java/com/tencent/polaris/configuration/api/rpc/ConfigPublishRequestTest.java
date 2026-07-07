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

/**
 * Test for {@link ConfigPublishRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigPublishRequestTest {

    /**
     * 测试默认值
     * 测试目的：验证 casMd5/releaseName 默认为空串，labels 默认为空 map
     * 测试场景：new 构造后直接读取
     * 验证内容：casMd5/releaseName 为空串，labels 非空
     */
    @Test
    public void testDefaultValues() {
        // Act
        ConfigPublishRequest request = new ConfigPublishRequest();

        // Assert
        assertThat(request.getCasMd5()).isEmpty();
        assertThat(request.getReleaseName()).isEmpty();
        assertThat(request.getLabels()).isEmpty();
    }

    /**
     * 测试 setter/getter
     * 测试目的：验证各字段读写
     * 测试场景：设置各字段后读取
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testSettersAndGetters() {
        // Arrange
        ConfigPublishRequest request = new ConfigPublishRequest();
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        // Act
        request.setNamespace("Test");
        request.setGroup("group1");
        request.setFilename("app.yaml");
        request.setContent("key: value");
        request.setCasMd5("abc123");
        request.setReleaseName("release-1");
        request.setLabels(labels);

        // Assert
        assertThat(request.getNamespace()).isEqualTo("Test");
        assertThat(request.getGroup()).isEqualTo("group1");
        assertThat(request.getFilename()).isEqualTo("app.yaml");
        assertThat(request.getContent()).isEqualTo("key: value");
        assertThat(request.getCasMd5()).isEqualTo("abc123");
        assertThat(request.getReleaseName()).isEqualTo("release-1");
        assertThat(request.getLabels()).isEqualTo(labels);
    }

    /**
     * 测试 verify 为空实现不抛异常
     * 测试目的：验证 verify() 当前为空实现，调用不抛异常
     * 测试场景：new 构造后调用 verify
     * 验证内容：不抛出任何异常
     */
    @Test
    public void testVerifyDoesNotThrow() {
        // Arrange
        ConfigPublishRequest request = new ConfigPublishRequest();

        // Act & Assert
        assertThatCode(request::verify).doesNotThrowAnyException();
    }

    /**
     * 测试 Builder 构建完整对象
     * 测试目的：验证 Builder 能正确组装所有字段
     * 测试场景：通过 Builder 设置所有字段后 build
     * 验证内容：各 getter 返回 Builder 设置的值
     */
    @Test
    public void testBuilderBuild() {
        // Arrange
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        // Act
        ConfigPublishRequest request = ConfigPublishRequest.builder()
                .namespace("Test")
                .group("group1")
                .filename("app.yaml")
                .content("key: value")
                .casMd5("abc123")
                .releaseName("release-1")
                .labels(labels)
                .build();

        // Assert
        assertThat(request.getNamespace()).isEqualTo("Test");
        assertThat(request.getGroup()).isEqualTo("group1");
        assertThat(request.getFilename()).isEqualTo("app.yaml");
        assertThat(request.getContent()).isEqualTo("key: value");
        assertThat(request.getCasMd5()).isEqualTo("abc123");
        assertThat(request.getReleaseName()).isEqualTo("release-1");
        assertThat(request.getLabels()).isEqualTo(labels);
    }

    /**
     * 测试 Builder 构建时 labels 为 null 覆盖默认值
     * 测试目的：验证 Builder.labels(null) 会将 labels 置为 null
     * 测试场景：Builder 不调用 labels，build 后 labels 应为默认空 map；显式传 null 则为 null
     * 验证内容：显式 labels(null) 后 getLabels 为 null
     */
    @Test
    public void testBuilderLabelsNullOverridesDefault() {
        // Act
        ConfigPublishRequest request = ConfigPublishRequest.builder()
                .namespace("Test")
                .labels(null)
                .build();

        // Assert
        assertThat(request.getLabels()).isNull();
    }

    /**
     * 测试 toString 包含关键字段
     * 测试目的：验证 toString 输出包含 namespace/filename 等字段
     * 测试场景：设置字段后调用 toString
     * 验证内容：toString 包含各字段值
     */
    @Test
    public void testToString() {
        // Arrange
        ConfigPublishRequest request = new ConfigPublishRequest();
        request.setNamespace("Test");
        request.setFilename("app.yaml");

        // Act
        String str = request.toString();

        // Assert
        assertThat(str).contains("Test");
        assertThat(str).contains("app.yaml");
    }
}
