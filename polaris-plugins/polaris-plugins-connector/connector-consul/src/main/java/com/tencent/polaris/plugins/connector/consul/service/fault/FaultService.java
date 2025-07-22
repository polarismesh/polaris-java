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

package com.tencent.polaris.plugins.connector.consul.service.fault;

import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.json.GsonFactory;
import com.ecwid.consul.transport.HttpResponse;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import com.tencent.polaris.plugins.connector.consul.service.ConsulService;
import com.tencent.polaris.plugins.connector.consul.service.fault.entity.FaultRule;
import com.tencent.polaris.plugins.connector.consul.service.router.RouterUtils;
import com.tencent.polaris.plugins.connector.consul.service.router.entity.RouteRule;
import com.tencent.polaris.plugins.connector.consul.service.router.entity.RouteRuleGroup;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.FaultInjectionProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;

/**
 * @author Haotian Zhang
 */
public class FaultService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(FaultService.class);

    private final Map<FaultRuleKey, Long> faultRuleConsulIndexMap = new ConcurrentHashMap<>();

    public FaultService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                        String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String service = serviceUpdateTask.getServiceEventKey().getService();
        String faultRulePath = String.format("/v1/kv/fault/%s/%s/data", namespace, service);
        LOG.trace("tsf fault rule, consul kv namespace, getKey: {}", faultRulePath);
        UrlParameters nsTypeParam = new SingleUrlParameters("nsType", "DEF_AND_GLOBAL");
        UrlParameters tokenParam = new SingleUrlParameters("token", consulContext.getAclToken());
        UrlParameters recurseParam = new SingleUrlParameters("recurse");
        FaultRuleKey faultRuleKey = new FaultRuleKey();
        faultRuleKey.setNamespace(namespace);
        faultRuleKey.setService(service);
        Long currentIndex = getFaultRuleConsulIndex(faultRuleKey);
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        int code = ServerCodes.DATA_NO_CHANGE;
        try {
            LOG.debug("Begin get fault rules of {} sync", faultRuleKey);
            HttpResponse rawResponse = consulRawClient.makeGetRequest(faultRulePath, recurseParam, tokenParam,
                    nsTypeParam, queryParams);
            if (rawResponse != null) {
                if (LOG.isDebugEnabled()) {
                    String responseStr = "RawResponse{" +
                            "statusCode=" + rawResponse.getStatusCode() +
                            ", statusMessage='" + rawResponse.getStatusMessage() + '\'' +
                            ", content='" + rawResponse.getContent() + '\'' +
                            ", consulIndex=" + rawResponse.getConsulIndex() + '\'' +
                            ", consulKnownLeader=" + rawResponse.isConsulKnownLeader() + '\'' +
                            ", consulLastContact=" + rawResponse.getConsulLastContact() +
                            '}';
                    LOG.debug("tsf fault rule, consul kv namespace, response: {}", responseStr);
                }

                Long newIndex = rawResponse.getConsulIndex();
                // create service.
                ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
                newServiceBuilder.setNamespace(StringValue.of(namespace));
                newServiceBuilder.setName(StringValue.of(service));
                newServiceBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));
                // create discover response.
                ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
                newDiscoverResponseBuilder.setService(newServiceBuilder);
                // 重写index
                List<FaultInjectionProto.FaultInjection> faultInjectionList = new ArrayList<>();
                if (Objects.nonNull(newIndex)) {
                    if (!Objects.equals(currentIndex, newIndex)) {
                        code = ServerCodes.EXECUTE_SUCCESS;
                        if (rawResponse.getStatusCode() == 200) {
                            if (rawResponse.getContent() != null) {
                                LOG.info("new fault rule: {}", rawResponse.getContent());
                                faultInjectionList = parseResponse(rawResponse, namespace, service);
                            }
                        } else if (rawResponse.getStatusCode() == 404) {
                            LOG.info("empty fault rule: {}", rawResponse.getContent());
                        }
                    } else {
                        LOG.debug("[TSF Fault Rule] Consul data is not changed");
                    }
                } else {
                    LOG.warn("[TSF Fault Rule] Consul data is abnormal. {}", rawResponse);
                }
                if (CollectionUtils.isNotEmpty(faultInjectionList)) {
                    newDiscoverResponseBuilder.addAllFaultInjection(faultInjectionList);
                }
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                if (newIndex != null) {
                    setFaultRuleConsulIndex(faultRuleKey, currentIndex, newIndex);
                }
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        } catch (Throwable e) {
            LOG.error("[TSF Fault Rule] tsf fault rule load error. Will sleep for {} ms. Key path:{}",
                    consulContext.getConsulErrorSleep(), faultRulePath, e);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get fault sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        }
    }

    private List<FaultInjectionProto.FaultInjection> parseResponse(final HttpResponse response, String namespace, String service) {
        List<GetValue> valueList = GsonFactory.getGson().fromJson(response.getContent(),
                new TypeToken<List<GetValue>>() {
                }.getType());
        // yaml -> json -> list<RouteRuleGroup>
        Yaml yaml = new Yaml();
        List<RouteRuleGroup> routeRuleGroupList = Lists.newArrayList();
        valueList.forEach(value -> {
            try {
                String faultJsonString = mapper
                        .writeValueAsString(yaml.load(value.getDecodedValue()));
                List<RouteRuleGroup> tempList = mapper.readValue(faultJsonString,
                        new TypeReference<List<RouteRuleGroup>>() {
                        });
                if (!CollectionUtils.isEmpty(tempList)) {
                    routeRuleGroupList.add(tempList.get(0));
                }
            } catch (Exception ex) {
                LOG.error("tsf fault rule load error.", ex);
                throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf fault rule load error", ex);
            }
        });

        // list<RouteRuleGroup> -> List<FaultInjectionProto.FaultInjection>
        List<FaultInjectionProto.FaultInjection> faultInjectionFaultList = Lists.newArrayList();
        for (RouteRuleGroup routeRuleGroup : routeRuleGroupList) {
            for (RouteRule routeRule : routeRuleGroup.getRuleList()) {
                FaultRule faultRule = routeRule.getFaultRule();
                FaultInjectionProto.FaultInjection.Builder faultInjectionBuilder = FaultInjectionProto.FaultInjection.newBuilder();
                // parse sources
                List<RoutingProto.Source> sources = RouterUtils.parseTagListToSourceList(routeRule.getTagList());
                faultInjectionBuilder.addAllSources(sources);

                // parse enabled
                faultInjectionBuilder.setEnabled(BoolValue.of(faultRule.getEnabled()));

                // parse DelayFault
                if (faultRule.getFixedDelay() != null && faultRule.getDelayPercentage() != null) {
                    FaultInjectionProto.DelayFault.Builder delayFaultBuilder = FaultInjectionProto.DelayFault.newBuilder();
                    delayFaultBuilder.setDelay(faultRule.getFixedDelay());
                    delayFaultBuilder.setDelayPercent(faultRule.getDelayPercentage());
                    faultInjectionBuilder.setDelayFault(delayFaultBuilder);
                }

                // parse AbortFault
                if (faultRule.getAbortHttpStatusCode() != null && faultRule.getAbortPercentage() != null) {
                    FaultInjectionProto.AbortFault.Builder abortFaultBuilder = FaultInjectionProto.AbortFault.newBuilder();
                    abortFaultBuilder.setAbortCode(faultRule.getAbortHttpStatusCode());
                    abortFaultBuilder.setAbortPercent(faultRule.getAbortPercentage());
                    faultInjectionBuilder.setAbortFault(abortFaultBuilder);
                }

                faultInjectionFaultList.add(faultInjectionBuilder.build());
            }
        }
        return faultInjectionFaultList;
    }

    private Long getFaultRuleConsulIndex(FaultRuleKey faultRuleKey) {
        Long index = faultRuleConsulIndexMap.get(faultRuleKey);
        if (index != null) {
            return index;
        }
        setFaultRuleConsulIndex(faultRuleKey, null, -1L);
        return -1L;
    }

    private void setFaultRuleConsulIndex(FaultRuleKey faultRuleKey, Long lastIndex, Long newIndex) {
        if (isEnable() && isReset) {
            LOG.info("FaultRuleKey: {} is reset.", faultRuleKey);
            faultRuleConsulIndexMap.remove(faultRuleKey);
            isReset = false;
        } else if (isEnable()) {
            LOG.debug("FaultRuleKey: {}; lastIndex: {}; newIndex: {}", faultRuleKey, lastIndex, newIndex);
            faultRuleConsulIndexMap.put(faultRuleKey, newIndex);
        } else {
            LOG.info("FaultRuleKey: {} is disabled.", faultRuleKey);
            faultRuleConsulIndexMap.remove(faultRuleKey);
        }
    }

    static class FaultRuleKey {
        private String namespace = "";
        private String service = "";
        private Boolean fetchGroup = true;

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public Boolean getFetchGroup() {
            return fetchGroup;
        }

        public void setFetchGroup(Boolean fetchGroup) {
            this.fetchGroup = fetchGroup;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            FaultRuleKey that = (FaultRuleKey) object;
            return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getService(), that.getService()) && Objects.equals(getFetchGroup(), that.getFetchGroup());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespace(), getService(), getFetchGroup());
        }

        @Override
        public String toString() {
            return "FaultRuleKey{" +
                    "namespace='" + namespace + '\'' +
                    ", serviceName='" + service + '\'' +
                    ", fetchGroup=" + fetchGroup +
                    '}';
        }
    }
}
