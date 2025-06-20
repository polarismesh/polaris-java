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

package com.tencent.polaris.plugins.connector.grpc;

import com.tencent.polaris.client.pojo.Node;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * Test for {@link LoadBalanceUtils}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class LoadBalanceUtilsTest {

    @Test
    public void testNormalCaseSelectLowestDelayNode() {
        // 准备测试数据：3个节点，其中node2延迟最低
        Node node1 = new Node("host1", 8080);
        Node node2 = new Node("host2", 8080);
        Node node3 = new Node("host3", 8080);
        List<Node> nodes = Arrays.asList(node1, node2, node3);

        // 模拟时间差，使node2的延迟最低
        try (MockedStatic<LoadBalanceUtils> mockedLoadBalanceUtils = mockStatic(LoadBalanceUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLoadBalanceUtils.when(() -> LoadBalanceUtils.getDelay(any(Node.class)))
                    .thenAnswer(invocationOnMock -> {
                        Node node = invocationOnMock.getArgument(0);
                        if (node.equals(node1)) {
                            return 25L; // node1: 25ms
                        } else if (node.equals(node2)) {
                            return 5L; // node2: 5ms
                        } else if (node.equals(node3)) {
                            return 100L; // node3: 100ms
                        }
                        return 500L;
                    });

            // 执行并验证
            Node result = LoadBalanceUtils.nearbyBackupLoadBalance(nodes, node1);
            assertThat(result)
                    .isEqualTo(node2)
                    .isIn(nodes)
                    .extracting(Node::getHost)
                    .isEqualTo("host2");
        }
    }

    @Test
    public void testCurrentNodeUnavailableSelectRandomAvailable() {
        // 准备测试数据：当前节点(node1)不可用
        Node node1 = new Node("host1", 8080);
        Node node2 = new Node("host2", 8080);
        Node node3 = new Node("host3", 8080);
        List<Node> nodes = Arrays.asList(node1, node2, node3);

        // 模拟随机数选择可用节点
        try (MockedStatic<LoadBalanceUtils> mockedLoadBalanceUtils = mockStatic(LoadBalanceUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLoadBalanceUtils.when(() -> LoadBalanceUtils.getDelay(any(Node.class)))
                    .thenAnswer(invocationOnMock -> {
                        Node node = invocationOnMock.getArgument(0);
                        if (node.equals(node1)) {
                            return Long.MAX_VALUE; // node1: 不可用
                        } else if (node.equals(node2)) {
                            return 5L; // node2: 5ms
                        } else if (node.equals(node3)) {
                            return 5L; // node3: 5ms
                        }
                        return 500L;
                    });

            // 执行并验证
            Node result = LoadBalanceUtils.nearbyBackupLoadBalance(nodes, node1);
            assertThat(result)
                    .isNotEqualTo(node1)
                    .matches(node -> node.equals(node2) || node.equals(node3));
        }
    }

    @Test
    public void testAllNodesUnavailableReturnFirstNode() {
        // 准备测试数据：所有节点都不可用
        Node node1 = new Node("host1", 8080);
        Node node2 = new Node("host2", 8080);
        List<Node> nodes = Arrays.asList(node1, node2);

        // 模拟随机数选择可用节点
        try (MockedStatic<LoadBalanceUtils> mockedLoadBalanceUtils = mockStatic(LoadBalanceUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLoadBalanceUtils.when(() -> LoadBalanceUtils.getDelay(any(Node.class)))
                    .thenAnswer(invocationOnMock -> {
                        Node node = invocationOnMock.getArgument(0);
                        if (node.equals(node1)) {
                            return Long.MAX_VALUE; // node1: 不可用
                        } else if (node.equals(node2)) {
                            return Long.MAX_VALUE; // node2: 不可用
                        }
                        return 500L;
                    });

            // 执行并验证
            Node result = LoadBalanceUtils.nearbyBackupLoadBalance(nodes, null);
            assertThat(result)
                    .isEqualTo(node1)
                    .isIn(nodes)
                    .extracting(Node::getPort)
                    .isEqualTo(8080);
        }
    }

    @Test
    public void testAllNodesUnavailableReturnLastNodeWithCurNode() {
        // 准备测试数据：所有节点都不可用
        Node node1 = new Node("host1", 8080);
        Node node2 = new Node("host2", 8080);
        List<Node> nodes = Arrays.asList(node1, node2);

        // 模拟随机数选择可用节点
        try (MockedStatic<LoadBalanceUtils> mockedLoadBalanceUtils = mockStatic(LoadBalanceUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLoadBalanceUtils.when(() -> LoadBalanceUtils.getDelay(any(Node.class)))
                    .thenAnswer(invocationOnMock -> {
                        Node node = invocationOnMock.getArgument(0);
                        if (node.equals(node1)) {
                            return Long.MAX_VALUE; // node1: 不可用
                        } else if (node.equals(node2)) {
                            return Long.MAX_VALUE; // node2: 不可用
                        }
                        return 500L;
                    });

            // 执行并验证
            Node result = LoadBalanceUtils.nearbyBackupLoadBalance(nodes, node2);
            assertThat(result)
                    .isEqualTo(node2)
                    .isIn(nodes)
                    .extracting(Node::getPort)
                    .isEqualTo(8080);
        }
    }

    @Test
    public void testEmptyNodeListThrowsException() {
        // 准备测试数据：空节点列表
        List<Node> emptyList = Collections.emptyList();

        // 执行并验证
        assertThatThrownBy(() -> LoadBalanceUtils.nearbyBackupLoadBalance(emptyList, null))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testSingleAvailableNode() {
        // 准备测试数据：单个可用节点
        Node node = new Node("host1", 8080);
        List<Node> singleNodeList = Collections.singletonList(node);

        // 模拟时间差
        try (MockedStatic<LoadBalanceUtils> mockedLoadBalanceUtils = mockStatic(LoadBalanceUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLoadBalanceUtils.when(() -> LoadBalanceUtils.getDelay(any(Node.class))).thenReturn(5L);
            // 执行并验证
            Node result = LoadBalanceUtils.nearbyBackupLoadBalance(singleNodeList, null);
            assertThat(result)
                    .isSameAs(node)
                    .extracting("host", "port")
                    .containsExactly("host1", 8080);
        }
    }

    @Test
    public void testSingleUnavailableNode() {
        // 准备测试数据：单个可用节点
        Node node = new Node("host1", 8080);
        List<Node> singleNodeList = Collections.singletonList(node);

        // 模拟时间差
        try (MockedStatic<LoadBalanceUtils> mockedLoadBalanceUtils = mockStatic(LoadBalanceUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLoadBalanceUtils.when(() -> LoadBalanceUtils.getDelay(any(Node.class))).thenReturn(Long.MAX_VALUE);
            // 执行并验证
            Node result = LoadBalanceUtils.nearbyBackupLoadBalance(singleNodeList, null);
            assertThat(result)
                    .isSameAs(node)
                    .extracting("host", "port")
                    .containsExactly("host1", 8080);
        }
    }

    @Test
    public void testSingleUnavailableNodeWithCurNode() {
        // 准备测试数据：单个可用节点
        Node node = new Node("host1", 8080);
        List<Node> singleNodeList = Collections.singletonList(node);

        // 模拟时间差
        try (MockedStatic<LoadBalanceUtils> mockedLoadBalanceUtils = mockStatic(LoadBalanceUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLoadBalanceUtils.when(() -> LoadBalanceUtils.getDelay(any(Node.class))).thenReturn(Long.MAX_VALUE);
            // 执行并验证
            Node result = LoadBalanceUtils.nearbyBackupLoadBalance(singleNodeList, node);
            assertThat(result)
                    .isSameAs(node)
                    .extracting("host", "port")
                    .containsExactly("host1", 8080);
        }
    }

    @Test
    public void testMixedAvailableAndUnavailableNodes() {
        // 准备测试数据：混合可用和不可用节点
        Node node1 = new Node("host1", 8080); // 不可用
        Node node2 = new Node("host2", 8080); // 可用(5ms)
        Node node3 = new Node("host3", 8080); // 可用(6ms)
        List<Node> nodes = Arrays.asList(node1, node2, node3);

        try (MockedStatic<LoadBalanceUtils> mockedLoadBalanceUtils = mockStatic(LoadBalanceUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLoadBalanceUtils.when(() -> LoadBalanceUtils.getDelay(any(Node.class)))
                    .thenAnswer(invocationOnMock -> {
                        Node node = invocationOnMock.getArgument(0);
                        if (node.equals(node1)) {
                            return Long.MAX_VALUE; // node1: 不可用
                        } else if (node.equals(node2)) {
                            return 5L; // node2: 5ms
                        } else if (node.equals(node3)) {
                            return 10L; // node3: 10ms
                        }
                        return 500L;
                    });

            // 执行并验证
            Node result = LoadBalanceUtils.nearbyBackupLoadBalance(nodes, null);
            assertThat(result)
                    .isEqualTo(node2)
                    .isNotIn(Collections.singletonList(node1))
                    .extracting(Node::getHost)
                    .asString()
                    .startsWith("host");
        }
    }
}
