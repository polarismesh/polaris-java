/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.auth.blockallowlist;

import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.auth.AuthInfo;
import com.tencent.polaris.api.plugin.auth.AuthResult;
import com.tencent.polaris.api.plugin.auth.Authenticator;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.TrieUtil;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.specification.api.v1.security.BlockAllowListProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.tencent.polaris.api.plugin.cache.CacheConstants.API_ID;
import static com.tencent.polaris.api.utils.RuleUtils.matchMethod;
import static com.tencent.polaris.metadata.core.constant.MetadataConstants.LOCAL_NAMESPACE;
import static com.tencent.polaris.metadata.core.constant.MetadataConstants.LOCAL_SERVICE;

/**
 * 黑白名单服务鉴权插件.
 *
 * @author Haotian Zhang
 */
public class BlockAllowListAuthenticator implements Authenticator {

    private static final Logger LOG = LoggerFactory.getLogger(BlockAllowListAuthenticator.class);

    protected Extensions extensions;

    private Function<String, Pattern> regexFunction;

    private Function<String, TrieNode<String>> trieNodeFunction;

    @Override
    public AuthResult authenticate(AuthInfo authInfo) {
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = getBlockAllowListRule(authInfo);
        if (CollectionUtils.isNotEmpty(blockAllowListRuleList)) {
            if (!checkAllow(authInfo, blockAllowListRuleList)) {
                return new AuthResult(AuthResult.Code.AuthResultForbidden);
            }
        }
        return new AuthResult(AuthResult.Code.AuthResultOk);
    }

    /**
     * 获取黑白名单鉴权规则
     *
     * @param authInfo 鉴权信息
     * @return 目标服务黑白名单鉴权规则
     */
    private List<BlockAllowListProto.BlockAllowListRule> getBlockAllowListRule(AuthInfo authInfo) {
        // 获取服务鉴权规则
        DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();
        BaseFlow.buildFlowControlParam(new RequestBaseEntity(), extensions.getConfiguration(), engineFlowControlParam);
        Set<ServiceEventKey> serviceEventKeys = new HashSet<>();
        ServiceEventKey dstSvcEventKey = new ServiceEventKey(new ServiceKey(authInfo.getNamespace(), authInfo.getService()),
                ServiceEventKey.EventType.BLOCK_ALLOW_RULE);
        serviceEventKeys.add(dstSvcEventKey);
        DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
        svcKeysProvider.setSvcEventKeys(serviceEventKeys);
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(extensions, false, svcKeysProvider, engineFlowControlParam);
        ServiceRule serviceRule = resourcesResponse.getServiceRule(dstSvcEventKey);

        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = new ArrayList<>();
        if (serviceRule != null && serviceRule.getRule() != null) {
            ResponseProto.DiscoverResponse discoverResponse = (ResponseProto.DiscoverResponse) serviceRule.getRule();
            blockAllowListRuleList = discoverResponse.getBlockAllowListRuleList();
        }

        return blockAllowListRuleList;
    }

    /**
     * 1、如果全是白名单策略，那么只要有一个匹配，才算通过。
     * 2、如果全是黑名单策略，那么只要有一个匹配，才算不通过。
     * 3、如果又有白名单策略，又有黑名单策略，只要有一个白名单策略匹配，才算通过。
     *
     * @param authInfo
     * @param blockAllowListRuleList
     * @return
     */
    protected boolean checkAllow(AuthInfo authInfo, List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList) {
        boolean containsAllowList = false;
        if (CollectionUtils.isNotEmpty(blockAllowListRuleList)) {
            for (BlockAllowListProto.BlockAllowListRule balr : blockAllowListRuleList) {
                if (balr.getEnable()) {
                    for (BlockAllowListProto.BlockAllowConfig config : balr.getBlockAllowConfigList()) {
                        if (config.getBlockAllowPolicy().equals(BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.ALLOW_LIST)) {
                            containsAllowList = true;
                        }
                        boolean methodMatched = matchMethod(authInfo.getPath(), authInfo.getProtocol(),
                                authInfo.getMethod(), config.getApi(), regexFunction, trieNodeFunction);
                        if (methodMatched) {
                            boolean matched = true;
                            MetadataContext metadataContext = authInfo.getMetadataContext();
                            List<BlockAllowListProto.BlockAllowConfig.MatchArgument> argumentsList = config.getArgumentsList();
                            if (CollectionUtils.isNotEmpty(argumentsList)) {
                                for (BlockAllowListProto.BlockAllowConfig.MatchArgument matchArgument : argumentsList) {
                                    String labelValue = null;
                                    if (metadataContext != null) {
                                        labelValue = getLabelValue(matchArgument, metadataContext);
                                    }
                                    matched = RuleUtils.matchStringValue(matchArgument.getValue(), labelValue, regexFunction);
                                    if (!matched) {
                                        LOG.debug("match fail because label value [{}] is null or not match [{}]", labelValue, matchArgument.getValue());
                                        break;
                                    }
                                }
                            }
                            if (matched) {
                                return config.getBlockAllowPolicy().equals(BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.ALLOW_LIST);
                            }
                        }
                    }
                }
            }
        }
        if (containsAllowList) {
            LOG.debug("check allow fail because no matched allow list.");
        }
        return !containsAllowList;
    }

    private static String getLabelValue(BlockAllowListProto.BlockAllowConfig.MatchArgument matchArgument,
                                        MetadataContext metadataContext) {
        MessageMetadataContainer messageMetadataContainer = metadataContext.getMetadataContainer(MetadataType.MESSAGE, true);
        if (messageMetadataContainer == null) {
            return null;
        }
        switch (matchArgument.getType()) {
            case HEADER:
                return messageMetadataContainer.getHeader(matchArgument.getKey());
            case QUERY:
                return messageMetadataContainer.getQuery(matchArgument.getKey());
            case CALLER_SERVICE: {
                String namespace = metadataContext.getMetadataContainer(MetadataType.APPLICATION, true).getRawMetadataStringValue(LOCAL_NAMESPACE);
                if (StringUtils.equals(matchArgument.getKey(), namespace) || StringUtils.equals("*", matchArgument.getKey())) {
                    return metadataContext.getMetadataContainer(MetadataType.APPLICATION, true).getRawMetadataStringValue(LOCAL_SERVICE);
                } else {
                    return null;
                }
            }
            case CALLER_IP:
                return messageMetadataContainer.getCallerIP();
            case CALLER_METADATA:
                return metadataContext.getMetadataContainer(MetadataType.APPLICATION, true).getRawMetadataStringValue(matchArgument.getKey());
            case CUSTOM:
            default:
                return metadataContext.getMetadataContainer(MetadataType.CUSTOM, false).getRawMetadataStringValue(matchArgument.getKey());
        }
    }

    @Override
    public String getName() {
        return DefaultPlugins.BLOCK_ALLOW_LIST_AUTHENTICATOR_TYPE;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.AUTHENTICATOR.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        this.extensions = ctx;
        this.regexFunction = regex -> {
            if (null == extensions) {
                return Pattern.compile(regex);
            }
            FlowCache flowCache = extensions.getFlowCache();
            return flowCache.loadOrStoreCompiledRegex(regex);
        };
        this.trieNodeFunction = key -> {
            if (null == extensions) {
                return null;
            }
            FlowCache flowCache = extensions.getFlowCache();
            return flowCache.loadPluginCacheObject(API_ID, key, path -> TrieUtil.buildSimpleApiTrieNode((String) path));
        };
    }

    @Override
    public void destroy() {

    }
}
