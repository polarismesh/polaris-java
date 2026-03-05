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

package com.tencent.polaris.api.plugin.route;

import com.tencent.polaris.api.pojo.Service;
import com.tencent.polaris.api.pojo.TrieNode;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.metadata.core.manager.MetadataContainerGroup;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author Haotian Zhang
 */
public class RoutingUtils {

    /**
     * 匹配source的命名空间和服务名
     *
     * @param ruleSource
     * @param targetSourceService
     * @return
     */
    public static boolean matchSourceService(RoutingProto.Source ruleSource, Service targetSourceService) {
        if (targetSourceService == null) {
            // 如果没有source服务信息, 判断rule是否支持全匹配
            if (!RuleUtils.MATCH_ALL.equals(ruleSource.getNamespace().getValue()) || !RuleUtils.MATCH_ALL
                    .equals(ruleSource.getService().getValue())) {
                return false;
            }
            return true;
        } else {
            if (ruleSource.containsMetadata(TsfMetadataConstants.TSF_SERVICE_TAG_OPERATOR)) {
                ModelProto.MatchString.MatchStringType matchType = ruleSource.getMetadataMap().get(TsfMetadataConstants.TSF_SERVICE_TAG_OPERATOR).getType();
                return matchTsfSourceService(matchType, ruleSource, targetSourceService);
            } else {
                return matchPolarisSourceService(ruleSource, targetSourceService);
            }
        }
    }

    private static boolean matchTsfSourceService(ModelProto.MatchString.MatchStringType matchType, RoutingProto.Source ruleSource, Service targetSourceService) {
        // 如果有source服务信息, 需要匹配服务信息
        // 如果命名空间|服务不为"*"且不等于原服务, 则匹配失败
        String namespace = ruleSource.getNamespace().getValue();
        if (!RuleUtils.MATCH_ALL.equals(namespace)
                && !StringUtils.equals(namespace, targetSourceService.getNamespace())) {
            switch (matchType) {
            case NOT_EQUALS:
            case NOT_IN:
                return true;
            default:
                return false;
            }
        }
        return RuleUtils.matchStringValue(matchType, targetSourceService.getService(),
                ruleSource.getService().getValue());
    }

    private static boolean matchPolarisSourceService(RoutingProto.Source ruleSource, Service targetSourceService) {
        // 如果有source服务信息, 需要匹配服务信息
        // 如果命名空间|服务不为"*"且不等于原服务, 则匹配失败
        String namespace = ruleSource.getNamespace().getValue();
        if (!RuleUtils.MATCH_ALL.equals(namespace)
                && !StringUtils.equals(namespace, targetSourceService.getNamespace())) {
            return false;
        }
        String service = ruleSource.getService().getValue();
        if (!RuleUtils.MATCH_ALL.equals(service) && !StringUtils.startsWith(service, "!")
                && !StringUtils.startsWith(service, "*")
                && !StringUtils.equals(service, targetSourceService.getService())) {
            return false;
        }
        // 如果服务名不等于“*”，且服务名规则以“!”开头，则使用取反匹配
        if (!RuleUtils.MATCH_ALL.equals(service) && StringUtils.startsWith(service, "!")) {
            String realService = StringUtils.substring(service, 1);
            if (StringUtils.equals(realService, targetSourceService.getService())) {
                return false;
            }
        }
        // 如果服务名不等于“*”，且服务名规则以“*”开头，则使用正则匹配
        if (!RuleUtils.MATCH_ALL.equals(service) && StringUtils.startsWith(service, "*")) {
            String regex = StringUtils.substring(service, 1);
            Pattern pattern = Pattern.compile(regex);
            if (!pattern.matcher(targetSourceService.getService()).find()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 匹配source的metadata
     *
     * @param ruleSource
     * @param targetSourceService
     * @return
     */
    public static boolean matchSourceMetadata(RoutingProto.Source ruleSource, Service targetSourceService,
                                              MetadataContainerGroup metadataContainerGroup,
                                              Function<String, TrieNode<String>> trieNodeFunction) {
        // 如果rule中metadata为空, 匹配成功, 结束
        if (MapUtils.isEmpty(ruleSource.getMetadataMap())) {
            return true;
        }

        // 如果没有源服务信息, 本次匹配失败
        if (targetSourceService == null) {
            return false;
        }

        return RuleUtils.matchMetadata(ruleSource.getMetadataMap(), new HashMap<>(), metadataContainerGroup, true,
                new HashMap<>(), new HashMap<>(), trieNodeFunction);
    }

    /**
     * 匹配source的metadata
     *
     * @param ruleSource
     * @param targetSourceService
     * @return
     */
    public static boolean matchSourceMetadata(RoutingProto.Source ruleSource, Service targetSourceService,
                                              Map<String, String> trafficLabels,
                                              MetadataContainerGroup metadataContainerGroup,
                                              Map<String, String> multiEnvRouterParamMap,
                                              Map<String, String> globalVariablesConfig,
                                              Function<String, TrieNode<String>> trieNodeFunction) {
        // 如果rule中metadata为空, 匹配成功, 结束
        if (MapUtils.isEmpty(ruleSource.getMetadataMap())) {
            return true;
        }

        // 如果没有源服务信息, 本次匹配失败
        if (targetSourceService == null) {
            return false;
        }

        return RuleUtils.matchMetadata(ruleSource.getMetadataMap(), trafficLabels, metadataContainerGroup, true,
                multiEnvRouterParamMap, globalVariablesConfig, trieNodeFunction);
    }
}
