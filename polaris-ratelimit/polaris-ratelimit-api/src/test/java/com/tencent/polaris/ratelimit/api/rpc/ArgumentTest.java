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
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Argument}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ArgumentTest {

    /**
     * 测试 buildMethod 工厂方法
     * 测试目的：验证 buildMethod 能正确构造 METHOD 类型参数
     * 测试场景：传入方法名构造 Argument
     * 验证内容：类型为 METHOD，key 为空，value 为传入的方法名
     */
    @Test
    public void testBuildMethod() {
        // Arrange
        String method = "GET";

        // Act
        Argument argument = Argument.buildMethod(method);

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.METHOD);
        assertThat(argument.getKey()).isEmpty();
        assertThat(argument.getValue()).isEqualTo(method);
    }

    /**
     * 测试 buildMethod 对 null 入参的兜底
     * 测试目的：验证 buildMethod 对 null 方法名转换为空串
     * 测试场景：传入 null 方法名
     * 验证内容：value 为空串而非 null
     */
    @Test
    public void testBuildMethodWithNull() {
        // Act
        Argument argument = Argument.buildMethod(null);

        // Assert
        assertThat(argument.getValue()).isEmpty();
    }

    /**
     * 测试 buildCallerIP 工厂方法
     * 测试目的：验证 buildCallerIP 能正确构造 CALLER_IP 类型参数
     * 测试场景：传入 IP 地址构造 Argument
     * 验证内容：类型为 CALLER_IP，key 为空，value 为传入的 IP
     */
    @Test
    public void testBuildCallerIP() {
        // Arrange
        String ip = "10.0.0.1";

        // Act
        Argument argument = Argument.buildCallerIP(ip);

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.CALLER_IP);
        assertThat(argument.getKey()).isEmpty();
        assertThat(argument.getValue()).isEqualTo(ip);
    }

    /**
     * 测试 buildHeader 工厂方法
     * 测试目的：验证 buildHeader 能正确构造 HEADER 类型参数
     * 测试场景：传入 header 键值构造 Argument
     * 验证内容：类型为 HEADER，key/value 与入参一致
     */
    @Test
    public void testBuildHeader() {
        // Arrange
        String headerKey = "X-Token";
        String headerValue = "abc";

        // Act
        Argument argument = Argument.buildHeader(headerKey, headerValue);

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.HEADER);
        assertThat(argument.getKey()).isEqualTo(headerKey);
        assertThat(argument.getValue()).isEqualTo(headerValue);
    }

    /**
     * 测试 buildQuery 工厂方法
     * 测试目的：验证 buildQuery 能正确构造 QUERY 类型参数
     * 测试场景：传入 query 键值构造 Argument
     * 验证内容：类型为 QUERY，key/value 与入参一致
     */
    @Test
    public void testBuildQuery() {
        // Arrange
        String queryKey = "uid";
        String queryValue = "123";

        // Act
        Argument argument = Argument.buildQuery(queryKey, queryValue);

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.QUERY);
        assertThat(argument.getKey()).isEqualTo(queryKey);
        assertThat(argument.getValue()).isEqualTo(queryValue);
    }

    /**
     * 测试 buildCallerService 工厂方法
     * 测试目的：验证 buildCallerService 能正确构造 CALLER_SERVICE 类型参数
     * 测试场景：传入命名空间和服务名构造 Argument
     * 验证内容：类型为 CALLER_SERVICE，key 为 namespace，value 为 service
     */
    @Test
    public void testBuildCallerService() {
        // Arrange
        String namespace = "Test";
        String service = "caller-service";

        // Act
        Argument argument = Argument.buildCallerService(namespace, service);

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.CALLER_SERVICE);
        assertThat(argument.getKey()).isEqualTo(namespace);
        assertThat(argument.getValue()).isEqualTo(service);
    }

    /**
     * 测试 buildCustom 工厂方法
     * 测试目的：验证 buildCustom 能正确构造 CUSTOM 类型参数
     * 测试场景：传入自定义键值构造 Argument
     * 验证内容：类型为 CUSTOM，key/value 与入参一致
     */
    @Test
    public void testBuildCustom() {
        // Arrange
        String key = "label";
        String value = "v1";

        // Act
        Argument argument = Argument.buildCustom(key, value);

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.CUSTOM);
        assertThat(argument.getKey()).isEqualTo(key);
        assertThat(argument.getValue()).isEqualTo(value);
    }

    /**
     * 测试 buildCustom 对 null 入参的兜底
     * 测试目的：验证 buildCustom 对 null key/value 转换为空串
     * 测试场景：传入 null key 和 null value
     * 验证内容：key 和 value 均为空串
     */
    @Test
    public void testBuildCustomWithNull() {
        // Act
        Argument argument = Argument.buildCustom(null, null);

        // Assert
        assertThat(argument.getKey()).isEmpty();
        assertThat(argument.getValue()).isEmpty();
    }

    /**
     * 测试 fromLabel 解析 method 标签
     * 测试目的：验证 fromLabel 对 $method 前缀的识别
     * 测试场景：传入 key 为 $method
     * 验证内容：返回 METHOD 类型参数
     */
    @Test
    public void testFromLabelMethod() {
        // Act
        Argument argument = Argument.fromLabel(RateLimitConsts.LABEL_KEY_METHOD, "POST");

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.METHOD);
        assertThat(argument.getValue()).isEqualTo("POST");
    }

    /**
     * 测试 fromLabel 解析 caller_ip 标签
     * 测试目的：验证 fromLabel 对 $caller_ip 前缀的识别
     * 测试场景：传入 key 为 $caller_ip
     * 验证内容：返回 CALLER_IP 类型参数
     */
    @Test
    public void testFromLabelCallerIp() {
        // Act
        Argument argument = Argument.fromLabel(RateLimitConsts.LABEL_KEY_CALLER_IP, "10.0.0.2");

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.CALLER_IP);
        assertThat(argument.getValue()).isEqualTo("10.0.0.2");
    }

    /**
     * 测试 fromLabel 解析 header 标签
     * 测试目的：验证 fromLabel 对 $header. 前缀的识别与剥离
     * 测试场景：传入 key 为 $header.X-Token
     * 验证内容：返回 HEADER 类型，key 为剥离前缀后的 X-Token
     */
    @Test
    public void testFromLabelHeader() {
        // Arrange
        String fullKey = RateLimitConsts.LABEL_KEY_HEADER + "X-Token";

        // Act
        Argument argument = Argument.fromLabel(fullKey, "token-value");

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.HEADER);
        assertThat(argument.getKey()).isEqualTo("X-Token");
        assertThat(argument.getValue()).isEqualTo("token-value");
    }

    /**
     * 测试 fromLabel 解析 query 标签
     * 测试目的：验证 fromLabel 对 $query. 前缀的识别与剥离
     * 测试场景：传入 key 为 $query.uid
     * 验证内容：返回 QUERY 类型，key 为剥离前缀后的 uid
     */
    @Test
    public void testFromLabelQuery() {
        // Arrange
        String fullKey = RateLimitConsts.LABEL_KEY_QUERY + "uid";

        // Act
        Argument argument = Argument.fromLabel(fullKey, "999");

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.QUERY);
        assertThat(argument.getKey()).isEqualTo("uid");
        assertThat(argument.getValue()).isEqualTo("999");
    }

    /**
     * 测试 fromLabel 解析 caller_service 标签
     * 测试目的：验证 fromLabel 对 $caller_service. 前缀的识别与剥离
     * 测试场景：传入 key 为 $caller_service.Test
     * 验证内容：返回 CALLER_SERVICE 类型，key 为剥离前缀后的 Test
     */
    @Test
    public void testFromLabelCallerService() {
        // Arrange
        String fullKey = RateLimitConsts.LABEL_KEY_CALLER_SERVICE + "Test";

        // Act
        Argument argument = Argument.fromLabel(fullKey, "svc");

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.CALLER_SERVICE);
        assertThat(argument.getKey()).isEqualTo("Test");
        assertThat(argument.getValue()).isEqualTo("svc");
    }

    /**
     * 测试 fromLabel 对未知前缀回退为 CUSTOM
     * 测试目的：验证 fromLabel 对非内置前缀的 key 作为自定义标签
     * 测试场景：传入 key 为未知前缀 label-x
     * 验证内容：返回 CUSTOM 类型，key 原样保留
     */
    @Test
    public void testFromLabelCustom() {
        // Act
        Argument argument = Argument.fromLabel("label-x", "v2");

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.CUSTOM);
        assertThat(argument.getKey()).isEqualTo("label-x");
        assertThat(argument.getValue()).isEqualTo("v2");
    }

    /**
     * 测试 fromLabel 对 null key 回退为 CUSTOM
     * 测试目的：验证 fromLabel 对 null key 的兜底处理
     * 测试场景：传入 null key
     * 验证内容：返回 CUSTOM 类型，key 为空串
     */
    @Test
    public void testFromLabelWithNullKey() {
        // Act
        Argument argument = Argument.fromLabel(null, "v3");

        // Assert
        assertThat(argument.getType()).isEqualTo(Argument.ArgumentType.CUSTOM);
        assertThat(argument.getKey()).isEmpty();
    }

    /**
     * 测试 toLabel 对所有类型的标签序列化
     * 测试目的：验证 toLabel 能将各类型 Argument 正确写入 map
     * 测试场景：构造 6 种类型的 Argument，调用 toLabel 写入同一 map
     * 验证内容：map 中各 key 的前缀与类型对应，值正确
     */
    @Test
    public void testToLabelAllTypes() {
        // Arrange
        Map<String, String> labels = new HashMap<>();
        Argument method = Argument.buildMethod("GET");
        Argument callerIp = Argument.buildCallerIP("10.0.0.3");
        Argument header = Argument.buildHeader("X-Id", "h1");
        Argument query = Argument.buildQuery("qid", "q1");
        Argument callerService = Argument.buildCallerService("ns", "svc");
        Argument custom = Argument.buildCustom("ck", "cv");

        // Act
        method.toLabel(labels);
        callerIp.toLabel(labels);
        header.toLabel(labels);
        query.toLabel(labels);
        callerService.toLabel(labels);
        custom.toLabel(labels);

        // Assert
        assertThat(labels).containsEntry(RateLimitConsts.LABEL_KEY_METHOD, "GET");
        assertThat(labels).containsEntry(RateLimitConsts.LABEL_KEY_CALLER_IP, "10.0.0.3");
        assertThat(labels).containsEntry(RateLimitConsts.LABEL_KEY_HEADER + "X-Id", "h1");
        assertThat(labels).containsEntry(RateLimitConsts.LABEL_KEY_QUERY + "qid", "q1");
        assertThat(labels).containsEntry(RateLimitConsts.LABEL_KEY_CALLER_SERVICE + "ns", "svc");
        assertThat(labels).containsEntry("ck", "cv");
    }

    /**
     * 测试 fromLabel 与 toLabel 的往返一致性
     * 测试目的：验证 fromLabel 解析出的 Argument 经 toLabel 能还原原始标签
     * 测试场景：构造一个 header 标签，fromLabel 解析后再 toLabel 写回
     * 验证内容：写回的 key/value 与原始标签一致
     */
    @Test
    public void testFromLabelToLabelRoundTrip() {
        // Arrange
        String originalKey = RateLimitConsts.LABEL_KEY_HEADER + "X-Round";
        String originalValue = "round-val";

        // Act
        Argument argument = Argument.fromLabel(originalKey, originalValue);
        Map<String, String> labels = new HashMap<>();
        argument.toLabel(labels);

        // Assert
        assertThat(labels).containsEntry(originalKey, originalValue);
    }

    /**
     * 测试 equals 与 hashCode
     * 测试目的：验证相同 type/key/value 的 Argument 相等且 hashCode 一致
     * 测试场景：构造两个相同的 header Argument 和一个不同的 query Argument
     * 验证内容：相同对象相等、与不同对象不等、与 null 不等、hashCode 一致
     */
    @Test
    public void testEqualsAndHashCode() {
        // Arrange
        Argument a1 = Argument.buildHeader("X-Token", "v");
        Argument a2 = Argument.buildHeader("X-Token", "v");
        Argument different = Argument.buildQuery("X-Token", "v");

        // Assert
        assertThat(a1.equals(a1)).isTrue();
        assertThat(a1.equals(a2)).isTrue();
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        assertThat(a1.equals(different)).isFalse();
        assertThat(a1.equals(null)).isFalse();
        assertThat(a1.equals("not-an-argument")).isFalse();
    }

    /**
     * 测试 toString 包含关键字段
     * 测试目的：验证 toString 输出包含类型、key、value
     * 测试场景：构造一个 custom Argument 调用 toString
     * 验证内容：toString 结果包含 type、key、value 字样
     */
    @Test
    public void testToString() {
        // Arrange
        Argument argument = Argument.buildCustom("ck", "cv");

        // Act
        String str = argument.toString();

        // Assert
        assertThat(str).contains("CUSTOM");
        assertThat(str).contains("ck");
        assertThat(str).contains("cv");
    }
}
