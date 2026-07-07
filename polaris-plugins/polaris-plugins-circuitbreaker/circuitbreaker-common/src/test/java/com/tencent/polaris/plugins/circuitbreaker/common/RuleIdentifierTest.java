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

package com.tencent.polaris.plugins.circuitbreaker.common;

import com.tencent.polaris.api.pojo.ServiceKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link RuleIdentifier}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class RuleIdentifierTest {

    /**
     * 测试构造函数保存所有字段
     * 测试目的：验证 RuleIdentifier 正确保存各字段
     * 测试场景：构造 RuleIdentifier 并读取
     * 验证内容：各 getter 返回值与入参一致
     */
    @Test
    public void testConstructorSavesFields() {
        // Arrange
        ServiceKey caller = new ServiceKey("Prod", "caller");

        // Act
        RuleIdentifier id = new RuleIdentifier("Test", "svc", caller, "GET");

        // Assert
        assertThat(id.getNamespace()).isEqualTo("Test");
        assertThat(id.getService()).isEqualTo("svc");
        assertThat(id.getCallerService()).isEqualTo(caller);
        assertThat(id.getMethod()).isEqualTo("GET");
    }

    /**
     * 测试 equals 与 hashCode
     * 测试目的：验证相同字段的 RuleIdentifier 相等且 hashCode 一致
     * 测试场景：构造两个相同的 RuleIdentifier 和一个不同的
     * 验证内容：相同对象相等、与不同对象不等、与 null 不等、hashCode 一致
     */
    @Test
    public void testEqualsAndHashCode() {
        // Arrange
        ServiceKey caller = new ServiceKey("Prod", "caller");
        RuleIdentifier a1 = new RuleIdentifier("Test", "svc", caller, "GET");
        RuleIdentifier a2 = new RuleIdentifier("Test", "svc", caller, "GET");
        RuleIdentifier different = new RuleIdentifier("Test", "svc", caller, "POST");

        // Assert
        assertThat(a1.equals(a1)).isTrue();
        assertThat(a1.equals(a2)).isTrue();
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        assertThat(a1.equals(different)).isFalse();
        assertThat(a1.equals(null)).isFalse();
    }

    /**
     * 测试 toString 包含关键字段
     * 测试目的：验证 toString 输出包含 namespace/service/method
     * 测试场景：构造后调用 toString
     * 验证内容：toString 包含各字段值
     */
    @Test
    public void testToString() {
        // Arrange
        RuleIdentifier id = new RuleIdentifier("Test", "svc", new ServiceKey("Prod", "caller"), "GET");

        // Act
        String str = id.toString();

        // Assert
        assertThat(str).contains("Test");
        assertThat(str).contains("svc");
        assertThat(str).contains("GET");
    }
}
