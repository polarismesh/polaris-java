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

import java.util.HashMap;
import java.util.Map;

public class TrieNode<T> {
    private final Map<String, TrieNode<T>> children;
    private final String path;

    // 只有叶子节点才有
    private T nodeInfo;

    // 增加非法字符，确保不会和前端传入的参数相同
    public static final String POLARIS_WILDCARD = "#polaris_wildcard#";

    public static final String SIMPLE_VALID_INFO = "#TSF#";

    public static final String ROOT_PATH = "";

    // root 构造器
    public TrieNode(String path) {
        this.path = path;
        this.children = new HashMap<>();
    }

    public TrieNode<T> getSubNode(String nodeKey) {
        if (children.containsKey(nodeKey)) {
            return children.get(nodeKey);
        } else if (children.containsKey(POLARIS_WILDCARD)) {
            return children.get(POLARIS_WILDCARD);
        }

        return null;
    }

    public boolean isEmptyChildren() {
        return children.isEmpty();
    }

    // only for build trie
    public TrieNode<T> getOrCreateSubNode(String path) {
        if (path.startsWith("{") && path.endsWith("}")) {
            path = POLARIS_WILDCARD;
        }

        if (!children.containsKey(path)) {
            children.putIfAbsent(path, new TrieNode(path));
        }

        return children.get(path);
    }

    public T getNodeInfo() {
        return nodeInfo;
    }

    public void setNodeInfo(T nodeInfo) {
        this.nodeInfo = nodeInfo;
    }
}