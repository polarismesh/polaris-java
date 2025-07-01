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

package com.tencent.polaris.client.pojo;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Node}.
 *
 * @author Haotian Zhang
 */
public class NodeTest {

    @Test
    public void testConstructorAndGetters() {
        // 测试构造器和getter方法
        Node node = new Node("localhost", 8080);

        assertThat(node)
                .extracting(Node::getHost, Node::getPort)
                .containsExactly("localhost", 8080);
    }

    @Test
    public void testCopyConstructor() {
        // 测试拷贝构造器
        Node original = new Node("localhost", 8081);
        Node copy = new Node(original);

        assertThat(copy)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @Test
    public void testGetHostPort() {
        // 测试getHostPort方法
        Node node = new Node("example.com", 443);

        assertThat(node.getHostPort())
                .isEqualTo("example.com:443")
                .contains("example.com")
                .endsWith("443");
    }

    @Test
    public void testEqualsAndHashCode() {
        // 测试equals和hashCode方法
        Node node1 = new Node("host1", 80);
        Node node2 = new Node("host1", 80);
        Node node3 = new Node("host2", 80);

        assertThat(node1)
                .isEqualTo(node2)
                .hasSameHashCodeAs(node2)
                .isNotEqualTo(node3)
                .isNotEqualTo(null)
                .isNotEqualTo("string");
    }

    @Test
    public void testToString() {
        // 测试toString方法
        Node node = new Node("testhost", 9999);

        assertThat(node.toString())
                .isEqualTo("Node{host='testhost', port=9999}")
                .contains("testhost")
                .containsPattern("port=\\d+");
    }

    @Test
    public void testIsAnyAddress() {
        // 测试isAnyAddress方法
        Node localNode = new Node("0.0.0.0", 8080);
        Node nonLocalNode = new Node("8.8.8.8", 53);
        Node invalidNode = new Node("invalid.host.name", 1234);

        assertThat(localNode.isAnyAddress()).isTrue();
        assertThat(nonLocalNode.isAnyAddress()).isFalse();
        assertThat(invalidNode.isAnyAddress()).isFalse();
    }

    @Test
    public void testIsAnyAddressWithNullHost() {
        // 测试null主机名的情况
        Node nullHostNode = new Node(null, 8080);

        assertThat(nullHostNode.isAnyAddress()).isFalse();
    }

    @Test
    public void testEdgeCases() {
        // 测试边界情况
        Node minPortNode = new Node("host", 0);
        Node maxPortNode = new Node("host", 65535);
        Node emptyHostNode = new Node("", 8080);

        assertThat(minPortNode.getPort()).isZero();

        assertThat(maxPortNode.getPort()).isEqualTo(65535);

        assertThat(emptyHostNode.getHost()).isEmpty();
    }
}
