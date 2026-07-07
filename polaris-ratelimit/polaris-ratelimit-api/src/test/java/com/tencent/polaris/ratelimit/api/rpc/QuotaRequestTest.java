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

package com.tencent.polaris.ratelimit.api.rpc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link QuotaRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class QuotaRequestTest {

    /**
     * 测试基本属性的 setter/getter
     * 测试目的：验证 namespace/service/method/count 的读写
     * 测试场景：设置各属性后读取
     * 验证内容：读取值与设置值一致，count 默认为 1
     */
    @Test
    public void testBasicSettersAndGetters() {
        // Arrange
        QuotaRequest request = new QuotaRequest();

        // Act
        request.setNamespace("Test");
        request.setService("svc");
        request.setMethod("GET");
        request.setCount(5);

        // Assert
        assertThat(request.getNamespace()).isEqualTo("Test");
        assertThat(request.getService()).isEqualTo("svc");
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getCount()).isEqualTo(5);
    }

    /**
     * 测试 count 默认值为 1
     * 测试目的：验证新构造的 QuotaRequest count 默认为 1
     * 测试场景：构造后不设置 count
     * 验证内容：getCount 返回 1
     */
    @Test
    public void testDefaultCount() {
        // Act
        QuotaRequest request = new QuotaRequest();

        // Assert
        assertThat(request.getCount()).isEqualTo(1);
    }

    /**
     * 测试 getArguments 默认非空
     * 测试目的：验证默认 arguments 为空集合而非 null
     * 测试场景：构造后直接 getArguments
     * 验证内容：返回空集合
     */
    @Test
    public void testDefaultArgumentsNotEmpty() {
        // Act
        QuotaRequest request = new QuotaRequest();

        // Assert
        assertThat(request.getArguments()).isEmpty();
    }

    /**
     * 测试 setLabels 与 getLabels 的往返
     * 测试目的：验证 setLabels 通过 fromLabel 转换、getLabels 通过 toLabel 还原
     * 测试场景：setLabels 设置一个 header 标签，getLabels 读回
     * 验证内容：读回的 key/value 与设置一致
     */
    @Test
    public void testSetLabelsGetLabelsRoundTrip() {
        // Arrange
        QuotaRequest request = new QuotaRequest();
        Map<String, String> labels = new HashMap<>();
        labels.put(RateLimitConsts.LABEL_KEY_HEADER + "X-Id", "h1");
        labels.put(RateLimitConsts.LABEL_KEY_METHOD, "POST");

        // Act
        request.setLabels(labels);
        Map<String, String> result = request.getLabels();

        // Assert
        assertThat(result).containsEntry(RateLimitConsts.LABEL_KEY_HEADER + "X-Id", "h1");
        assertThat(result).containsEntry(RateLimitConsts.LABEL_KEY_METHOD, "POST");
    }

    /**
     * 测试 setLabels 传入 null/空 map
     * 测试目的：验证 setLabels 对空入参的兜底，不修改 arguments
     * 测试场景：setLabels(null) 后 getArguments
     * 验证内容：arguments 仍为空集合
     */
    @Test
    public void testSetLabelsWithEmpty() {
        // Arrange
        QuotaRequest request = new QuotaRequest();

        // Act
        request.setLabels(null);

        // Assert
        assertThat(request.getArguments()).isEmpty();
    }

    /**
     * 测试 setArguments 传入非空集合
     * 测试目的：验证 setArguments 正确保存外部集合
     * 测试场景：传入包含一个 Argument 的集合
     * 验证内容：getArguments 包含该 Argument
     */
    @Test
    public void testSetArgumentsNonEmpty() {
        // Arrange
        QuotaRequest request = new QuotaRequest();
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.buildMethod("GET"));

        // Act
        request.setArguments(arguments);

        // Assert
        assertThat(request.getArguments()).contains(Argument.buildMethod("GET"));
    }

    /**
     * 测试 setArguments 传入 null/空集合
     * 测试目的：验证 setArguments 对空入参兜底为空集合
     * 测试场景：先设置非空 arguments，再 setArguments(null)
     * 验证内容：getArguments 变为空集合
     */
    @Test
    public void testSetArgumentsWithEmpty() {
        // Arrange
        QuotaRequest request = new QuotaRequest();
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.buildMethod("GET"));
        request.setArguments(arguments);

        // Act
        request.setArguments(null);

        // Assert
        assertThat(request.getArguments()).isEmpty();
    }

    /**
     * 测试 metadataContext 的 setter/getter
     * 测试目的：验证 metadataContext 的读写
     * 测试场景：setMetadataContext(null) 后读取
     * 验证内容：getMetadataContext 为 null
     */
    @Test
    public void testMetadataContextSetterGetter() {
        // Arrange
        QuotaRequest request = new QuotaRequest();

        // Act
        request.setMetadataContext(null);

        // Assert
        assertThat(request.getMetadataContext()).isNull();
    }

    /**
     * 测试 timeoutMs 继承自 RequestBaseEntity
     * 测试目的：验证 QuotaRequest 继承的超时属性读写
     * 测试场景：setTimeoutMs 后读取
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testTimeoutMs() {
        // Arrange
        QuotaRequest request = new QuotaRequest();

        // Act
        request.setTimeoutMs(2000L);

        // Assert
        assertThat(request.getTimeoutMs()).isEqualTo(2000L);
    }

    /**
     * 测试 toString 包含关键字段
     * 测试目的：验证 toString 输出包含 namespace/service/method
     * 测试场景：设置属性后调用 toString
     * 验证内容：toString 包含各字段值
     */
    @Test
    public void testToString() {
        // Arrange
        QuotaRequest request = new QuotaRequest();
        request.setNamespace("Test");
        request.setService("svc");
        request.setMethod("GET");

        // Act
        String str = request.toString();

        // Assert
        assertThat(str).contains("Test");
        assertThat(str).contains("svc");
        assertThat(str).contains("GET");
    }
}
