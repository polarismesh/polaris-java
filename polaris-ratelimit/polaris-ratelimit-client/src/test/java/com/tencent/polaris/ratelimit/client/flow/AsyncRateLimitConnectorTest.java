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

package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.remote.ServiceAddressRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AsyncRateLimitConnector}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncRateLimitConnectorTest {

    private static final String OLD_UNIQUE_KEY = "oldRevision#svc#ns#labels";
    private static final String NEW_UNIQUE_KEY = "newRevision#svc#ns#labels";
    private static final String SERVICE = "svc";
    private static final String NAMESPACE = "ns";
    private static final String LABELS = "labels";

    @Mock
    private Extensions extensions;

    @Mock
    private ServiceAddressRepository serviceAddressRepository;

    @Mock
    private RateLimitWindow oldWindow;

    private AsyncRateLimitConnector connector;
    private ServiceKey remoteCluster;
    private ServiceIdentifier serviceIdentifier;
    private Node oldNode;
    private Node newNode;

    @Before
    public void setUp() {
        connector = new AsyncRateLimitConnector();
        remoteCluster = new ServiceKey(NAMESPACE, "limiter");
        serviceIdentifier = new ServiceIdentifier(SERVICE, NAMESPACE, LABELS);
        oldNode = new Node("old-host", 8080);
        newNode = new Node("new-host", 8080);
        when(oldWindow.getUniqueKey()).thenReturn(OLD_UNIQUE_KEY);
    }

    /**
     * 测试目的：rule revision 变更（service/labels 不变）后，触发节点切换。
     * 测试场景：以 NEW_UNIQUE_KEY 调用 getStreamCounterSet。
     * 验证内容：uniqueKeyToStream[NEW_UNIQUE_KEY] 必须存在并指向新建的 stream。
     */
    @Test
    public void getStreamCounterSet_NodeSwitchWithRuleRevisionChange_ShouldPutNewUniqueKey() {
        StreamCounterSet oldStream = newOldStreamWithInitRecord(oldNode, serviceIdentifier);
        seedNodeSwitchScenario(oldStream);

        StreamCounterSet result = connector.getStreamCounterSet(
                extensions, remoteCluster, serviceAddressRepository, NEW_UNIQUE_KEY, serviceIdentifier);

        assertThat(result).isNotNull();
        assertThat(result.getNode()).isEqualTo(newNode);
        assertThat(connector.getUniqueKeyToStream()).containsKey(NEW_UNIQUE_KEY);
        assertThat(connector.getUniqueKeyToStream().get(NEW_UNIQUE_KEY)).isSameAs(result);
    }

    /**
     * 测试目的：节点切换且旧 stream 引用归零后，nodeToStream 应移除旧 node 的 entry。
     * 测试场景：预置 nodeToStream[oldNode]，引用计数 = 1，触发切换。
     * 验证内容：nodeToStream 只含 newNode，不含 oldNode。
     */
    @Test
    public void getStreamCounterSet_NodeSwitchAndOldStreamDereferenced_ShouldRemoveOldNodeFromMap() {
        StreamCounterSet oldStream = new StreamCounterSet(oldNode);
        oldStream.addReference();
        oldStream.setCurrentStreamResource(newStreamResourceForTest(oldNode));
        seedNodeSwitchScenario(oldStream);

        connector.getStreamCounterSet(
                extensions, remoteCluster, serviceAddressRepository, NEW_UNIQUE_KEY, serviceIdentifier);

        assertThat(connector.getNodeToStream())
                .doesNotContainKey(oldNode)
                .containsKey(newNode);
    }

    /**
     * 测试目的：同一 uniqueKey 重复调用 getStreamCounterSet 不应导致引用计数泄漏。
     * 测试场景：以相同 NEW_UNIQUE_KEY 连续调用两次。
     * 验证内容：第二次命中早返回，引用计数与第一次相同。
     */
    @Test
    public void getStreamCounterSet_RepeatedCallSameUniqueKey_ShouldNotLeakReference() {
        StreamCounterSet oldStream = newOldStreamWithInitRecord(oldNode, serviceIdentifier);
        seedNodeSwitchScenario(oldStream);

        StreamCounterSet firstResult = connector.getStreamCounterSet(
                extensions, remoteCluster, serviceAddressRepository, NEW_UNIQUE_KEY, serviceIdentifier);
        int referenceAfterFirst = firstResult.getReferenceCount();

        StreamCounterSet secondResult = connector.getStreamCounterSet(
                extensions, remoteCluster, serviceAddressRepository, NEW_UNIQUE_KEY, serviceIdentifier);
        int referenceAfterSecond = secondResult.getReferenceCount();

        assertThat(secondResult).isSameAs(firstResult);
        assertThat(referenceAfterSecond).isEqualTo(referenceAfterFirst);
    }

    /**
     * 预置「旧 stream 占住 NEW_UNIQUE_KEY、地址仓库返回新 node」的节点切换前置条件。
     */
    private void seedNodeSwitchScenario(StreamCounterSet oldStream) {
        connector.getUniqueKeyToStream().put(NEW_UNIQUE_KEY, oldStream);
        connector.getNodeToStream().put(oldNode, oldStream);
        when(serviceAddressRepository.getServiceAddressNode()).thenReturn(newNode);
    }

    private StreamResource newStreamResourceForTest(Node node) {
        return new StreamResource(node, null, null, null);
    }

    private StreamCounterSet newOldStreamWithInitRecord(Node node, ServiceIdentifier id) {
        StreamCounterSet stream = new StreamCounterSet(node);
        stream.addReference();
        StreamResource resource = newStreamResourceForTest(node);
        resource.getInitRecord().put(id, new InitializeRecord(oldWindow));
        stream.setCurrentStreamResource(resource);
        return stream;
    }
}
