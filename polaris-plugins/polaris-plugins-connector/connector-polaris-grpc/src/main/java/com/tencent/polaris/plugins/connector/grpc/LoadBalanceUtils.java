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

import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utils for node list load balance.
 *
 * @author Haotian Zhang
 */
public class LoadBalanceUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

    public static Node nearbyBackupLoadBalance(List<Node> nodes, Node curNode) {
        Node bestNode = null;
        long minDelay = Long.MAX_VALUE;
        List<Node> availableNodeList = new ArrayList<>();
        List<Node> unAvailableNodeList = new ArrayList<>();
        for (Node node : nodes) {
            long currentDelay = getDelay(node);
            if (currentDelay == Long.MAX_VALUE) {
                unAvailableNodeList.add(node);
            } else {
                availableNodeList.add(node);
                if (currentDelay < minDelay) {
                    minDelay = currentDelay;
                    bestNode = node;
                }
            }
        }
        Node finalNode = null;
        // 如果有不可用的节点，且和上一次使用的节点一样，且存在可用的节点，则从可用的节点中随机选择一个
        if (CollectionUtils.isNotEmpty(unAvailableNodeList) && unAvailableNodeList.contains(curNode)) {
            if (CollectionUtils.isNotEmpty(availableNodeList)) {
                Random random = new Random();
                int index = random.nextInt(availableNodeList.size());
                finalNode = availableNodeList.get(index);
            }
        } else {
            finalNode = bestNode;
            if (finalNode != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.info("Node {} has the least delay {}ms in node list {}.", bestNode, minDelay, nodes);
                } else {
                    LOG.info("Node {} has the least delay {}ms.", bestNode, minDelay);
                }
            }
        }
        // 如果没有找到有效节点，默认返回第一个节点
        if (finalNode == null) {
            LOG.info("Node {} has been chosen the first in node list {}.", bestNode, nodes);
            finalNode = nodes.get(0);
        }
        return finalNode;
    }

    static long getDelay(Node node) {
        long startTime = System.currentTimeMillis();
        boolean flag = IPAddressUtils.detect(node.getHost(), node.getPort(), 1000);
        long currentDelay = (System.currentTimeMillis() - startTime);
        if (!flag) {
            currentDelay = Long.MAX_VALUE;
            LOG.warn("detect {}:{} failed", node.getHost(), node.getPort());
        } else {
            LOG.debug("detected {}:{} with delay {}ms", node.getHost(), node.getPort(), currentDelay);
        }
        return currentDelay;
    }
}
