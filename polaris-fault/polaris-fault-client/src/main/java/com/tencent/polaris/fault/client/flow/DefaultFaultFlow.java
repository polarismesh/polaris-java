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

package com.tencent.polaris.fault.client.flow;

import com.tencent.polaris.api.config.consumer.FaultConfig;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.route.RoutingUtils;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.TrieUtil;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.fault.api.flow.FaultFlow;
import com.tencent.polaris.fault.api.rpc.AbortResult;
import com.tencent.polaris.fault.api.rpc.DelayResult;
import com.tencent.polaris.fault.api.rpc.FaultRequest;
import com.tencent.polaris.fault.api.rpc.FaultResponse;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.manager.MetadataContainerGroup;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.FaultInjectionProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;

import java.util.*;

import static com.tencent.polaris.api.plugin.cache.CacheConstants.API_ID;

/**
 * 默认的鉴权Flow实现
 *
 * @author Haotian Zhang
 */
public class DefaultFaultFlow implements FaultFlow {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultFaultFlow.class);

    private FaultConfig faultConfig;

    private Extensions extensions;

    private FlowCache flowCache;

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.faultConfig = sdkContext.getConfig().getConsumer().getFault();
        this.extensions = sdkContext.getExtensions();
    }

    @Override
    public FaultResponse fault(FaultRequest faultRequest) throws PolarisException {
        if (faultConfig == null || !faultConfig.isEnable()) {
            return new FaultResponse(false);
        }

        FaultResponse faultResponse = new FaultResponse(false);
        List<FaultInjectionProto.FaultInjection> faultInjectionRules = getFaultInjectionRules(faultRequest.getTargetService());
        if (CollectionUtils.isNotEmpty(faultInjectionRules)) {
            for (FaultInjectionProto.FaultInjection faultInjection : faultInjectionRules) {
                if (faultInjection != null && faultInjection.getEnabled().getValue()) {
                    LOG.debug("FaultInjectionProto.FaultInjection:{}", faultInjection);

                    // 匹配source规则
                    boolean sourceMatched = matchSource(faultInjection.getSourcesList(),
                            faultRequest.getSourceService(), faultRequest.getMetadataContext().getMetadataContainerGroup(false));
                    if (!sourceMatched) {
                        LOG.debug("Source not matched, skipping fault injection. FaultInjectionProto.FaultInjection:{}", faultInjection);
                        continue;
                    }

                    // 匹配中断故障注入
                    if (faultInjection.hasAbortFault()) {
                        FaultInjectionProto.AbortFault abortFault = faultInjection.getAbortFault();
                        if (matchPercentage(abortFault.getAbortPercent())) {
                            faultResponse.setFaultInjected(true);
                            AbortResult abortResult = new AbortResult(abortFault.getAbortCode());
                            faultResponse.setAbortResult(abortResult);
                        }
                    }

                    // 匹配延迟故障注入
                    if (faultInjection.hasDelayFault()) {
                        FaultInjectionProto.DelayFault delayFault = faultInjection.getDelayFault();
                        if (matchPercentage(delayFault.getDelayPercent())) {
                            faultResponse.setFaultInjected(true);
                            DelayResult delayResult = new DelayResult(delayFault.getDelay());
                            faultResponse.setDelayResult(delayResult);
                        }
                    }

                    if (faultResponse.isFaultInjected()) {
                        LOG.debug("Fault injection matched, injecting fault. FaultResponse:{}, FaultInjectionProto.FaultInjection:{}",
                                faultResponse, faultInjection);
                        break;
                    }
                }
            }
        }

        return faultResponse;
    }

    /**
     * 获取故障注入规则
     *
     * @param serviceKey
     * @return
     */
    public List<FaultInjectionProto.FaultInjection> getFaultInjectionRules(ServiceKey serviceKey) {
        if (StringUtils.isBlank(serviceKey.getService()) || StringUtils.isBlank(serviceKey.getNamespace())) {
            return Collections.emptyList();
        }

        DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();
        BaseFlow.buildFlowControlParam(new RequestBaseEntity(), extensions.getConfiguration(), engineFlowControlParam);
        Set<ServiceEventKey> routerKeys = new HashSet<>();
        ServiceEventKey dstSvcEventKey = ServiceEventKey.builder().serviceKey(serviceKey).eventType(ServiceEventKey.EventType.FAULT_INJECTION).build();
        routerKeys.add(dstSvcEventKey);
        DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
        svcKeysProvider.setSvcEventKeys(routerKeys);
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(extensions, false, svcKeysProvider, engineFlowControlParam);
        ServiceRule faultInjectionServiceRule = resourcesResponse.getServiceRule(dstSvcEventKey);
        Object rule = faultInjectionServiceRule.getRule();
        if (Objects.nonNull(rule) && rule instanceof ResponseProto.DiscoverResponse) {
            return ((ResponseProto.DiscoverResponse) rule).getFaultInjectionList();
        }
        return Collections.emptyList();
    }

    /**
     * 匹配source规则
     */
    private boolean matchSource(List<RoutingProto.Source> sources, Service sourceService, MetadataContainerGroup metadataContainerGroup) {
        if (CollectionUtils.isEmpty(sources)) {
            return true;
        }
        // source匹配成功标志
        boolean matched = true;
        for (RoutingProto.Source source : sources) {
            // 匹配source服务
            matched = RoutingUtils.matchSourceService(source, sourceService);
            if (!matched) {
                continue;
            }
            // 匹配source metadata
            matched = RoutingUtils.matchSourceMetadata(source, sourceService, metadataContainerGroup,
                    key -> getFlowCache().loadPluginCacheObject(API_ID, key, path -> TrieUtil.buildSimpleApiTrieNode((String) path)));
            if (matched) {
                break;
            }
        }
        return matched;
    }

    private FlowCache getFlowCache() {
        if (flowCache == null) {
            flowCache = extensions.getFlowCache();
        }
        return flowCache;
    }

    private boolean matchPercentage(int percentage) {
        int targetPercentage = percentage;
        if (targetPercentage < 0 || targetPercentage > 100) {
            targetPercentage = 100;
        }
        int randomValue = new Random().nextInt(100);
        boolean percentMatched = randomValue < targetPercentage;
        LOG.debug("Random value: {}, target percentage: {}, original percentage: {}", randomValue, targetPercentage, percentage);
        return percentMatched;
    }
}
