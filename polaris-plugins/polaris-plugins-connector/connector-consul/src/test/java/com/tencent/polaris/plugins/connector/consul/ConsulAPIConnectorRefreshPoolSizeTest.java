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

package com.tencent.polaris.plugins.connector.consul;

import com.tencent.polaris.plugins.connector.common.constant.ConsulConstant;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test for {@link ConsulAPIConnector#parseRefreshThreadPoolSize(Map)}.
 *
 * @author Ajax-1
 */
public class ConsulAPIConnectorRefreshPoolSizeTest {

    private static final String KEY = ConsulConstant.MetadataMapKey.REFRESH_THREAD_POOL_SIZE_KEY;

    @Test
    public void testParseRefreshThreadPoolSize_keyAbsent_returnDefault() {
        // Arrange：metadata 中没有该配置项
        Map<String, String> metadata = new HashMap<>();

        // Act：解析线程池大小
        int poolSize = ConsulAPIConnector.parseRefreshThreadPoolSize(metadata);

        // Assert：返回默认值
        Assertions.assertThat(poolSize).isEqualTo(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
    }

    @Test
    public void testParseRefreshThreadPoolSize_nullMetadata_returnDefault() {
        // Act：metadata 为 null 时解析
        int poolSize = ConsulAPIConnector.parseRefreshThreadPoolSize(null);

        // Assert：返回默认值
        Assertions.assertThat(poolSize).isEqualTo(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
    }

    @Test
    public void testParseRefreshThreadPoolSize_blankValue_returnDefault() {
        // Arrange：空串和空白串
        Map<String, String> empty = singleton("");
        Map<String, String> blank = singleton("   ");

        // Act & Assert：均返回默认值
        Assertions.assertThat(ConsulAPIConnector.parseRefreshThreadPoolSize(empty))
                .isEqualTo(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
        Assertions.assertThat(ConsulAPIConnector.parseRefreshThreadPoolSize(blank))
                .isEqualTo(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
    }

    @Test
    public void testParseRefreshThreadPoolSize_validValue_returnValue() {
        // Arrange：合法的正整数
        Map<String, String> metadata = singleton("32");

        // Act：解析线程池大小
        int poolSize = ConsulAPIConnector.parseRefreshThreadPoolSize(metadata);

        // Assert：返回配置值
        Assertions.assertThat(poolSize).isEqualTo(32);
    }

    @Test
    public void testParseRefreshThreadPoolSize_notInteger_returnDefault() {
        // Arrange：非整数
        Map<String, String> metadata = singleton("abc");

        // Act & Assert：不抛异常并返回默认值
        Assertions.assertThatCode(() -> ConsulAPIConnector.parseRefreshThreadPoolSize(metadata))
                .doesNotThrowAnyException();
        Assertions.assertThat(ConsulAPIConnector.parseRefreshThreadPoolSize(metadata))
                .isEqualTo(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
    }

    @Test
    public void testParseRefreshThreadPoolSize_notPositive_returnDefault() {
        // Arrange：0 和负数
        Map<String, String> zero = singleton("0");
        Map<String, String> negative = singleton("-1");

        // Act & Assert：均回退到默认值
        Assertions.assertThat(ConsulAPIConnector.parseRefreshThreadPoolSize(zero))
                .isEqualTo(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
        Assertions.assertThat(ConsulAPIConnector.parseRefreshThreadPoolSize(negative))
                .isEqualTo(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
    }

    private static Map<String, String> singleton(String value) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(KEY, value);
        return metadata;
    }
}
