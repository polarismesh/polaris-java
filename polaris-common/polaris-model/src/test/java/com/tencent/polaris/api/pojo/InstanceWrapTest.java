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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class InstanceWrapTest {

    @Test
    public void testConstructorWithIpv6Preference() {
        // Mock data
        DefaultInstance originalInstance = new DefaultInstance();
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MetadataConstants.ADDRESS_IPV6, "::1");
        originalInstance.setMetadata(metadata);
        originalInstance.setHost("127.0.0.1");

        // Test with IPv6 preference
        InstanceWrap instanceWrap = new InstanceWrap(originalInstance, true);

        // Verify host is set to IPv6 address
        assertThat(instanceWrap.getHost()).isEqualTo("::1");
    }

    @Test
    public void testConstructorWithIpv4Preference() {
        // Mock data
        DefaultInstance originalInstance = new DefaultInstance();
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MetadataConstants.ADDRESS_IPV4, "192.168.1.1");
        originalInstance.setMetadata(metadata);
        originalInstance.setHost("::1");

        // Test with IPv4 preference
        InstanceWrap instanceWrap = new InstanceWrap(originalInstance, false);

        // Verify host is set to IPv4 address
        assertThat(instanceWrap.getHost()).isEqualTo("192.168.1.1");
    }

    @Test
    public void testConstructorWithNoMetadata() {
        // Mock data
        DefaultInstance originalInstance = new DefaultInstance();
        originalInstance.setHost("127.0.0.1");
        originalInstance.setMetadata(Collections.emptyMap());

        // Test with no metadata
        InstanceWrap instanceWrap = new InstanceWrap(originalInstance, true);

        // Verify host is set to original host
        assertThat(instanceWrap.getHost()).isEqualTo("127.0.0.1");
    }

    @Test
    public void testConstructorWithBlankMetadata() {
        // Mock data
        DefaultInstance originalInstance = new DefaultInstance();
        originalInstance.setHost("127.0.0.1");
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MetadataConstants.ADDRESS_IPV6, "");
        originalInstance.setMetadata(metadata);

        // Test with blank metadata
        InstanceWrap instanceWrap = new InstanceWrap(originalInstance, true);

        // Verify host is set to original host
        assertThat(instanceWrap.getHost()).isEqualTo("127.0.0.1");
    }
}