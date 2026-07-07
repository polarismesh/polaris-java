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

package com.tencent.polaris.api.pojo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link StatusDimension}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class StatusDimensionTest {

    /**
     * 测试构造函数保存 method 和 callerService
     * 测试目的：验证 StatusDimension 正确保存 method 和 callerService
     * 测试场景：构造带 method 和 callerService 的 StatusDimension
     * 验证内容：method/callerService 与入参一致
     */
    @Test
    public void testConstructorSavesFields() {
        // Arrange
        ServiceKey caller = new ServiceKey("Prod", "caller");

        // Act
        StatusDimension dim = new StatusDimension("GET", caller);

        // Assert
        assertThat(dim.getMethod()).isEqualTo("GET");
        assertThat(dim.getCallerService()).isEqualTo(caller);
    }

    /**
     * 测试 blank method 转换为空串
     * 测试目的：验证 method 为 null/空白时转换为空串
     * 测试场景：method 传 null
     * 验证内容：getMethod 返回空串
     */
    @Test
    public void testConstructorBlankMethodToEmpty() {
        // Act
        StatusDimension dim = new StatusDimension(null, new ServiceKey("Prod", "caller"));

        // Assert
        assertThat(dim.getMethod()).isEmpty();
    }

    /**
     * 测试 callerService 命名空间和服务名均为空时置 null
     * 测试目的：验证 callerService 的 namespace/service 均空时 callerService 置 null
     * 测试场景：传入 namespace/service 均空的 ServiceKey
     * 验证内容：getCallerService 为 null
     */
    @Test
    public void testConstructorEmptyCallerServiceToNull() {
        // Arrange
        ServiceKey emptyService = new ServiceKey("", "");

        // Act
        StatusDimension dim = new StatusDimension("GET", emptyService);

        // Assert
        assertThat(dim.getCallerService()).isNull();
    }

    /**
     * 测试 callerService 为 null 时直接保存 null
     * 测试目的：验证 callerService 入参为 null 时保存 null
     * 测试场景：callerService 传 null
     * 验证内容：getCallerService 为 null
     */
    @Test
    public void testConstructorNullCallerService() {
        // Act
        StatusDimension dim = new StatusDimension("GET", null);

        // Assert
        assertThat(dim.getCallerService()).isNull();
    }

    /**
     * 测试 EMPTY_DIMENSION 常量
     * 测试目的：验证 EMPTY_DIMENSION 的 method 为空、callerService 为 null
     * 测试场景：读取 EMPTY_DIMENSION
     * 验证内容：method 为空串，callerService 为 null
     */
    @Test
    public void testEmptyDimension() {
        // Assert
        assertThat(StatusDimension.EMPTY_DIMENSION.getMethod()).isEmpty();
        assertThat(StatusDimension.EMPTY_DIMENSION.getCallerService()).isNull();
    }

    /**
     * 测试 equals 与 hashCode
     * 测试目的：验证相同 method/callerService 的 StatusDimension 相等
     * 测试场景：构造两个相同的 StatusDimension 和一个不同的
     * 验证内容：相同对象相等、与不同对象不等、与 null 不等、hashCode 一致
     */
    @Test
    public void testEqualsAndHashCode() {
        // Arrange
        ServiceKey caller = new ServiceKey("Prod", "caller");
        StatusDimension a1 = new StatusDimension("GET", caller);
        StatusDimension a2 = new StatusDimension("GET", caller);
        StatusDimension different = new StatusDimension("POST", caller);

        // Assert
        assertThat(a1.equals(a1)).isTrue();
        assertThat(a1.equals(a2)).isTrue();
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        assertThat(a1.equals(different)).isFalse();
        assertThat(a1.equals(null)).isFalse();
    }

    /**
     * 测试 toString 包含 method
     * 测试目的：验证 toString 输出包含 method
     * 测试场景：构造后调用 toString
     * 验证内容：toString 包含 method 值
     */
    @Test
    public void testToString() {
        // Arrange
        StatusDimension dim = new StatusDimension("GET", new ServiceKey("Prod", "caller"));

        // Act
        String str = dim.toString();

        // Assert
        assertThat(str).contains("GET");
    }
}
