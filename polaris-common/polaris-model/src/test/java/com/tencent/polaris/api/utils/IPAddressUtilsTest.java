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

package com.tencent.polaris.api.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link  IPAddressUtils}.
 *
 * @author Haotian Zhang
 */
public class IPAddressUtilsTest {

    @Test
    public void testCheckIpv6Host_WithValidIpv6() {
        // 测试标准IPv6地址
        String ipv6Address = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        assertThat(IPAddressUtils.checkIpv6Host(ipv6Address)).isTrue();
    }

    @Test
    public void testCheckIpv6Host_WithBracketedIpv6() {
        // 测试带方括号的IPv6地址格式
        assertThat(IPAddressUtils.checkIpv6Host("[2001:0db8:85a3:0000:0000:8a2e:0370:7334]")).isTrue();
    }

    @Test
    public void testCheckIpv6Host_WithShortenedIpv6() {
        // 测试缩短格式的IPv6地址
        String ipv6Address = "2001:db8::8a2e:370:7334";
        assertThat(IPAddressUtils.checkIpv6Host(ipv6Address)).isTrue();
    }

    @Test
    public void testCheckIpv6Host_WithBracketedShortIpv6() {
        // 测试带方括号的简写IPv6地址
        assertThat(IPAddressUtils.checkIpv6Host("[2001:db8::8a2e:370:7334]")).isTrue();
    }

    @Test
    public void testCheckIpv6Host_WithIpv4() {
        // 测试IPv4地址
        String ipv4Address = "192.168.1.1";
        assertThat(IPAddressUtils.checkIpv6Host(ipv4Address)).isFalse();
    }

    @Test
    public void testCheckIpv6Host_WithInvalidHost() {
        // 测试无效主机名
        String invalidHost = "invalid.host.name";
        assertThat(IPAddressUtils.checkIpv6Host(invalidHost)).isFalse();
    }

    @Test
    public void testCheckIpv6Host_WithNull() {
        // 测试null值
        assertThat(IPAddressUtils.checkIpv6Host(null)).isFalse();
    }

    @Test
    public void testCheckIpv6Host_WithEmptyString() {
        // 测试空字符串
        assertThat(IPAddressUtils.checkIpv6Host("")).isFalse();
    }

    @Test
    public void testCheckIpv6Host_WithBlankString() {
        // 测试空白字符串
        assertThat(IPAddressUtils.checkIpv6Host("   ")).isFalse();
    }

    @Test
    public void testCheckIpv6Host_WithIpv6Loopback() {
        // 测试IPv6回环地址
        assertThat(IPAddressUtils.checkIpv6Host("::1")).isTrue();
    }


    @Test
    public void testCheckIpv6Host_WithBracketedLoopback() {
        // 测试带方括号的IPv6回环地址
        assertThat(IPAddressUtils.checkIpv6Host("[::1]")).isTrue();
    }

    @Test
    public void testCheckIpv6Host_WithIpv6MappedIpv4() {
        // 测试IPv6映射的IPv4地址
        assertThat(IPAddressUtils.checkIpv6Host("::ffff:192.168.1.1")).isFalse();
    }
}
