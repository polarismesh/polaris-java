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

package com.tencent.polaris.api.utils;

import com.tencent.polaris.api.pojo.HttpElement;
import com.tencent.polaris.api.pojo.TrieNode;

public class TrieUtil {

    /**
     * @param apiPath
     * @return TrieNode
     */
    public static TrieNode<String> buildSimpleApiTrieNode(String apiPath) {
        if (StringUtils.isEmpty(apiPath)) {
            return null;
        }
        return buildSimpleApiTrieNode(new String[]{apiPath});
    }

    public static TrieNode<String> buildSimpleApiTrieNode(String[] apiPathInfoList) {
        if (apiPathInfoList.length == 0) {
            return null;
        }

        TrieNode<String> root = new TrieNode<>(TrieNode.ROOT_PATH);
        for (String apiPathInfo : apiPathInfoList) {
            int flag = apiPathInfo.lastIndexOf("-");
            String method = null;
            String path = apiPathInfo;
            if (flag != -1) {
                method = apiPathInfo.substring(flag + 1);
                path = apiPathInfo;
                if (HttpElement.HTTP_METHOD_SET.contains(method)) {
                    path = apiPathInfo.substring(0, flag);
                } else {
                    method = null;
                }
            }

            // 因为前端的改动（最初的 tagValue 只有 path，某次前端组件改动后变成了 path-method，非客户提的），有兼容性问题，
            // 临时简化处理，不处理 method，前面逻辑保留是为了取出正确的 path
            method = null;
            TrieNode<String> node = root;
            // 一些场景下apiPath 不以"/"开头和分割（
            // 此时方法标识符的格式规定为 '{类路径}#{方法名}' 例如com.tencent.polaris.ServiceName#sayHello
            if (path.contains("#")) {
                node = node.getOrCreateSubNode(path);
                node.setNodeInfo(TrieNode.SIMPLE_VALID_INFO + "method:" + method);
                continue;
            }
            String[] apiPaths = path.split("/");
            // 跳过第一个为空的str
            for (int i = 1; i < apiPaths.length; i++) {
                node = node.getOrCreateSubNode(apiPaths[i]);
                // 叶子节点，需要 info
                if (i == apiPaths.length - 1) {
                    node.setNodeInfo(TrieNode.SIMPLE_VALID_INFO + "method:" + method);
                }
            }
        }
        return root;
    }

    public static boolean checkSimpleApi(TrieNode<String> root, String apiPathInfo) {
        if (root == null) {
            return false;
        }
        int flag = apiPathInfo.lastIndexOf("-");
        String method = apiPathInfo.substring(flag + 1);
        String path = apiPathInfo;
        if (HttpElement.HTTP_METHOD_SET.contains(method)) {
            path = apiPathInfo.substring(0, flag);
        } else {
            method = null;
        }


        TrieNode<String> node = root;
        // 一些场景下apiPath 不以"/"开头和分割（
        // 此时方法标识符的格式规定为 '{类路径}#{方法名}' 例如com.tencent.polaris.ServiceName#sayHello
        if(apiPathInfo.contains("#")){
            node = node.getOrCreateSubNode(apiPathInfo);
            if (node == null) {
                return false;
            } else {
                return checkApiNodeInfo(node, method);
            }
        }

        String[] apiPaths = path.split("/");
        for (int i = 1; i < apiPaths.length; i++) {
            if (node == null) {
                return false;
            }
            node = node.getSubNode(apiPaths[i]);
            // 叶子节点
            if (i == apiPaths.length - 1) {
                if (node == null) {
                    return false;
                } else {
                    return checkApiNodeInfo(node, method);
                }
            }
        }

        return false;
    }

    public static boolean checkApiNodeInfo(TrieNode<String> node, String method) {
        // trie 的 node info 里 method 为 null，说明是旧规则，兼容作用，直接匹配
        if (StringUtils.equals(TrieNode.SIMPLE_VALID_INFO + "method:" + "null", node.getNodeInfo())) {
            return true;
        } else {
            return StringUtils.equals(TrieNode.SIMPLE_VALID_INFO + "method:" + method, node.getNodeInfo());
        }
    }

    public static TrieNode<String> buildConfigTrieNode(String prefix) {
        TrieNode<String> root = new TrieNode<>(TrieNode.ROOT_PATH);
        return buildConfigTrieNode(prefix, root);
    }

    public static TrieNode<String> buildConfigTrieNode(String prefix, TrieNode<String> root) {
        if (StringUtils.isEmpty(prefix)) {
            return null;
        }
        // split by .
        String[] prefixes = prefix.split("\\.");
        TrieNode<String> node = root;
		for (String s : prefixes) {
			node = node.getOrCreateSubNode(s);
		}
        return root;
    }

    public static boolean checkConfig(TrieNode<String> root, String config) {
        if (root == null || root.isEmptyChildren() || StringUtils.isEmpty(config)) {
            return false;
        }

        String[] entities = config.split("\\.");

        TrieNode<String> node = root;
		for (String entity : entities) {
            // empty children means leaf in config matching
			if (node.isEmptyChildren()) {
				return true;
			}
            // for list
            if (entity.indexOf("[") < entity.indexOf("]")) {
                entity = entity.substring(0, entity.indexOf("["));
            }
			node = node.getSubNode(entity);
			if (node == null) {
				return false;
			}
		}

        if (node != null && node.isEmptyChildren()) {
            // exact match
            return true;
        } else {
            // not match or config is shorter than prefix
            return false;
        }
    }
}
