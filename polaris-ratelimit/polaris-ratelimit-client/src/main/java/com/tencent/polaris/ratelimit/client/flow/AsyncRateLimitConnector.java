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

import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.remote.ServiceAddressRepository;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 连接器，单线程调用，不考虑并发
 */
public class AsyncRateLimitConnector {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncRateLimitConnector.class);

    private final Object counterSetLock = new Object();

    /**
     * 节点到客户端连接
     */
    private final Map<Node, StreamCounterSet> nodeToStream = new HashMap<>();
    /**
     * uniqueKey到客户端连接
     */
    private final Map<String, StreamCounterSet> uniqueKeyToStream = new HashMap<>();

    private final List<String> coreRouters = new ArrayList<>();

    public AsyncRateLimitConnector() {
        coreRouters.add(ServiceRouterConfig.DEFAULT_ROUTER_METADATA);
    }

    /**
     * 获取连接流对象
     *
     * @param extensions 插件容器
     * @param remoteCluster 远程限流集群名
     * @param uniqueKey 唯一主键
     * @param serviceIdentifier 服务标识
     * @return 连接流对象
     */
    public StreamCounterSet getStreamCounterSet(Extensions extensions, ServiceKey remoteCluster,
            ServiceAddressRepository serviceAddressRepository, String uniqueKey, ServiceIdentifier serviceIdentifier) {
        // serviceAddressRepository设置了一致性hash lb，保证被调正常的情况下，拿到的都是同一个节点
        Node node = serviceAddressRepository.getServiceAddressNode();
        if (node == null) {
            LOG.error("[getStreamCounterSet] ratelimit cluster service not found.");
            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[getStreamCounterSet] serviceLabel: {} , get node: {}", serviceIdentifier.getLabels(), node);
        }
        StreamCounterSet streamCounterSet = uniqueKeyToStream.get(uniqueKey);
        if (null != streamCounterSet && streamCounterSet.getNode().equals(node)) {
            return streamCounterSet;
        }
        synchronized (counterSetLock) {
            if (null != streamCounterSet && streamCounterSet.getNode().equals(node)) {
                return streamCounterSet;
            }
            if (null != streamCounterSet) {
                //切换了节点，去掉初始化记录
                InitializeRecord removedRecord = streamCounterSet.deleteInitRecord(serviceIdentifier);
                if (removedRecord != null) {
                    RateLimitWindow removedWindow = removedRecord.getRateLimitWindow();
                    String removedUniqueKey = removedWindow != null ? removedWindow.getUniqueKey() : null;
                    LOG.info("[getStreamCounterSet] host switched, and initRecord removed serviceIdentifier: {}, "
                            + "removedWindow {} {}", serviceIdentifier, removedWindow, removedUniqueKey);
                }
                //切换了节点，老的不再使用
                if (streamCounterSet.decreaseReference()) {
                    // 形参 node 是切换后的新 node，必须用旧 stream 自身的 node 才能命中
                    nodeToStream.remove(streamCounterSet.getNode());
                }
            }
            streamCounterSet = nodeToStream.get(node);
            if (null == streamCounterSet) {
                streamCounterSet = new StreamCounterSet(node);
            }
            streamCounterSet.addReference();
            nodeToStream.put(node, streamCounterSet);
            uniqueKeyToStream.put(uniqueKey, streamCounterSet);
            return streamCounterSet;
        }
    }

    /**
     * 仅查询 uniqueKey 对应的 StreamCounterSet，不创建。
     * 用于窗口已 DELETED 后清理 initRecord 的场景，避免 getStreamCounterSet 的"获取或创建"副作用。
     *
     * @param uniqueKey 窗口唯一标识
     * @return StreamCounterSet，不存在时返回 null
     */
    public StreamCounterSet peekStreamCounterSet(String uniqueKey) {
        return uniqueKeyToStream.get(uniqueKey);
    }

    @JustForTest
    Map<Node, StreamCounterSet> getNodeToStream() {
        return nodeToStream;
    }

    @JustForTest
    Map<String, StreamCounterSet> getUniqueKeyToStream() {
        return uniqueKeyToStream;
    }
}
