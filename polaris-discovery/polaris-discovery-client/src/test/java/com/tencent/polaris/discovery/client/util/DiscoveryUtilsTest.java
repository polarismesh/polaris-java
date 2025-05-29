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

package com.tencent.polaris.discovery.client.util;

import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link  DiscoveryUtils}.
 *
 * @author Haotian Zhang
 */
public class DiscoveryUtilsTest {

    @Test
    public void testCheckIpv6Instance_WithValidIpv6Metadata() {
        // 准备：创建包含有效IPv6 metadata的实例
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MetadataConstants.ADDRESS_IPV6, "2001:db8::1");
        DefaultInstance instance = new DefaultInstance();
        instance.setMetadata(metadata);
        instance.setHost("127.0.0.1");

        // 执行 & 验证
        assertThat(DiscoveryUtils.checkIpv6Instance(instance)).isTrue();
    }

    @Test
    public void testCheckIpv6Instance_WithEmptyIpv6Metadata() {
        // 准备：创建包含空IPv6 metadata的实例
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MetadataConstants.ADDRESS_IPV6, "");
        DefaultInstance instance = new DefaultInstance();
        instance.setMetadata(metadata);
        instance.setHost("127.0.0.1");

        // 执行 & 验证
        assertThat(DiscoveryUtils.checkIpv6Instance(instance)).isFalse();
    }

    @Test
    public void testCheckIpv6Instance_WithBlankIpv6Metadata() {
        // 准备：创建包含空白IPv6 metadata的实例
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MetadataConstants.ADDRESS_IPV6, "   ");
        DefaultInstance instance = new DefaultInstance();
        instance.setMetadata(metadata);
        instance.setHost("127.0.0.1");

        // 执行 & 验证
        assertThat(DiscoveryUtils.checkIpv6Instance(instance)).isFalse();
    }

    @Test
    public void testCheckIpv6Instance_WithoutIpv6MetadataButValidIpv6Host() {
        // 准备：创建不包含IPv6 metadata但有IPv6 host的实例
        DefaultInstance instance = new DefaultInstance();
        instance.setMetadata(new HashMap<>());
        instance.setHost("2001:db8::1");

        // 执行 & 验证
        assertThat(DiscoveryUtils.checkIpv6Instance(instance)).isTrue();
    }

    @Test
    public void testCheckIpv6Instance_WithoutIpv6MetadataAndIpv4Host() {
        // 准备：创建不包含IPv6 metadata且host是IPv4的实例
        DefaultInstance instance = new DefaultInstance();
        instance.setMetadata(new HashMap<>());
        instance.setHost("192.168.1.1");

        // 执行 & 验证
        assertThat(DiscoveryUtils.checkIpv6Instance(instance)).isFalse();
    }

    @Test
    public void testCheckIpv6Instance_WithNullMetadataAndValidIpv6Host() {
        // 准备：创建metadata为null但有IPv6 host的实例
        DefaultInstance instance = new DefaultInstance();
        instance.setMetadata(null);
        instance.setHost("2001:db8::1");

        // 执行 & 验证
        assertThat(DiscoveryUtils.checkIpv6Instance(instance)).isTrue();
    }

    @Test
    public void testCheckIpv6Instance_WithNullHost() {
        // 准备：创建host为null的实例
        DefaultInstance instance = new DefaultInstance();
        instance.setHost(null);

        // 执行 & 验证
        assertThat(DiscoveryUtils.checkIpv6Instance(instance)).isFalse();
    }

    @Test
    public void testCheckIpv6Instance_WithBracketedIpv6Host() {
        // 准备：创建带方括号的IPv6 host的实例
        DefaultInstance instance = new DefaultInstance();
        instance.setHost("[2001:db8::1]");

        // 执行 & 验证
        assertThat(DiscoveryUtils.checkIpv6Instance(instance)).isTrue();
    }
}