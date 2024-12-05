/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.plugins.connector.consul.service.router;

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
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import com.tencent.polaris.plugins.connector.consul.service.ConsulService;
import com.tencent.polaris.plugins.connector.consul.service.common.TagConstant;
import com.tencent.polaris.plugins.connector.consul.service.router.entity.*;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;
import static com.tencent.polaris.api.plugin.route.RouterConstants.ROUTER_FAULT_TOLERANCE_ENABLE;
import static com.tencent.polaris.plugins.connector.consul.service.common.TagConditionUtil.parseMatchStringType;
import static com.tencent.polaris.plugins.connector.consul.service.common.TagConditionUtil.parseMetadataKey;

/**
 * @author Haotian Zhang
 */
public class RoutingService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingService.class);

    private final Map<RouterRuleKey, Long> routerRuleConsulIndexMap = new ConcurrentHashMap<>();

    public RoutingService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                          String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String service = serviceUpdateTask.getServiceEventKey().getService();
        String routeRuleKey = String.format("/v1/kv/route/%s/%s/data", namespace, service);
        LOG.trace("tsf route rule, consul kv namespace, getKey: {}", routeRuleKey);
        UrlParameters nsTypeParam = new SingleUrlParameters("nsType", "DEF_AND_GLOBAL");
        UrlParameters tokenParam = new SingleUrlParameters("token", consulContext.getAclToken());
        UrlParameters recurseParam = new SingleUrlParameters("recurse");
        RouterRuleKey routerRuleKey = new RouterRuleKey();
        routerRuleKey.setNamespace(namespace);
        routerRuleKey.setService(service);
        Long currentIndex = getRouterRuleConsulIndex(routerRuleKey);
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        int code = ServerCodes.DATA_NO_CHANGE;
        try {
            LOG.debug("Begin get router rules of {} sync", routerRuleKey);
            HttpResponse rawResponse = consulRawClient.makeGetRequest(routeRuleKey, recurseParam, tokenParam,
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
                    LOG.debug("tsf route rule, consul kv namespace, response: {}", responseStr);
                }

                Long newIndex = rawResponse.getConsulIndex();
                // create service.
                ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
                newServiceBuilder.setNamespace(StringValue.of(namespace));
                newServiceBuilder.setName(StringValue.of(service));
                newServiceBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));
                // create routing.
                RoutingProto.Routing.Builder newRoutingBuilder = RoutingProto.Routing.newBuilder();
                newRoutingBuilder.setNamespace(StringValue.of(namespace));
                newRoutingBuilder.setService(StringValue.of(service));
                newRoutingBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));
                // create discover response.
                ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
                newDiscoverResponseBuilder.setService(newServiceBuilder);
                // 重写index
                List<RoutingProto.Route> routes = new ArrayList<>();
                if (Objects.nonNull(newIndex)) {
                    if (!Objects.equals(currentIndex, newIndex)) {
                        code = ServerCodes.EXECUTE_SUCCESS;
                        if (rawResponse.getStatusCode() == 200) {
                            if (rawResponse.getContent() != null) {
                                LOG.info("new route rule: {}", rawResponse.getContent());
                                routes = parseResponse(rawResponse, namespace, service);
                            }
                        } else if (rawResponse.getStatusCode() == 404) {
                            LOG.info("empty route rule: {}", rawResponse.getContent());
                        }
                    } else {
                        LOG.debug("[TSF Route Rule] Consul data is not changed");
                    }
                } else {
                    LOG.warn("[TSF Route Rule] Consul data is abnormal. {}", rawResponse);
                }
                if (CollectionUtils.isNotEmpty(routes)) {
                    newRoutingBuilder.addAllInbounds(routes);
                }
                newDiscoverResponseBuilder.setRouting(newRoutingBuilder);
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                if (newIndex != null) {
                    setRouterRuleConsulIndex(routerRuleKey, currentIndex, newIndex);
                }
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        } catch (Throwable e) {
            LOG.error("[TSF Route Rule] tsf route rule load error. Will sleep for {} ms. Key path:{}",
                    consulContext.getConsulErrorSleep(), routeRuleKey, e);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get routing sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        }
    }

    private List<RoutingProto.Route> parseResponse(final HttpResponse response, String namespace, String service) {
        List<GetValue> valueList = GsonFactory.getGson().fromJson(response.getContent(),
                new TypeToken<List<GetValue>>() {
                }.getType());
        // yaml -> json -> list<RouteRuleGroup>
        Yaml yaml = new Yaml();
        List<RouteRuleGroup> routeRuleGroupList = Lists.newArrayList();
        valueList.forEach(value -> {
            try {
                String routeJsonString = mapper
                        .writeValueAsString(yaml.load(value.getDecodedValue()));
                List<RouteRuleGroup> tempList = mapper.readValue(routeJsonString,
                        new TypeReference<List<RouteRuleGroup>>() {
                        });
                if (!CollectionUtils.isEmpty(tempList)) {
                    routeRuleGroupList.add(tempList.get(0));
                }
            } catch (Exception ex) {
                LOG.error("tsf route rule load error.", ex);
                throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf route rule load error", ex);
            }
        });

        // list<RouteRuleGroup> -> List<RoutingProto.Route>
        List<RoutingProto.Route> routes = Lists.newArrayList();
        for (RouteRuleGroup routeRuleGroup : routeRuleGroupList) {
            for (RouteRule routeRule : routeRuleGroup.getRuleList()) {
                RoutingProto.Route.Builder routeBuilder = RoutingProto.Route.newBuilder();
                routeBuilder.putExtendInfo(ROUTER_FAULT_TOLERANCE_ENABLE, String.valueOf(routeRuleGroup.getFallbackStatus()));
                // parse sources
                List<RoutingProto.Source> sources = new ArrayList<>();
                List<RoutingProto.Source.Builder> sourceBuilders = new ArrayList<>();
                List<RoutingProto.Source.Builder> metadataSourceBuilders = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(routeRule.getTagList())) {
                    for (RouteTag routeTag : routeRule.getTagList()) {
                        if (StringUtils.equals(routeTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_SERVICE_NAME)) {
                            String[] tagValues = routeTag.getTagValue().split(",");
                            for (String tagValue : tagValues) {
                                if (StringUtils.isNotEmpty(tagValue)) {
                                    RoutingProto.Source.Builder sourceBuilder = RoutingProto.Source.newBuilder();
                                    sourceBuilder.setNamespace(StringValue.of("*"));
                                    String serviceName = tagValue;
                                    if (routeTag.getTagOperator().equals(TagConstant.OPERATOR.NOT_EQUAL) || routeTag.getTagOperator().equals(TagConstant.OPERATOR.NOT_IN)) {
                                        serviceName = "!" + serviceName;
                                    }
                                    sourceBuilder.setService(StringValue.of(serviceName));
                                    sourceBuilders.add(sourceBuilder);
                                }
                            }
                        } else if (StringUtils.equals(routeTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_NAMESPACE_SERVICE_NAME)) {
                            String[] tagValues = routeTag.getTagValue().split(",");
                            for (String tagValue : tagValues) {
                                if (StringUtils.isNotEmpty(tagValue)) {
                                    String[] split = tagValue.split("/");
                                    RoutingProto.Source.Builder sourceBuilder = RoutingProto.Source.newBuilder();
                                    sourceBuilder.setNamespace(StringValue.of("*"));
                                    String serviceName = tagValue;
                                    if (split.length == 2) {
                                        serviceName = split[1];
                                    }
                                    if (routeTag.getTagOperator().equals(TagConstant.OPERATOR.NOT_EQUAL) || routeTag.getTagOperator().equals(TagConstant.OPERATOR.NOT_IN)) {
                                        serviceName = "!" + serviceName;
                                    } else if (routeTag.getTagOperator().equals(TagConstant.OPERATOR.REGEX)) {
                                        serviceName = "*" + serviceName;
                                    }
                                    sourceBuilder.setService(StringValue.of(serviceName));
                                    sourceBuilders.add(sourceBuilder);
                                }
                            }
                        } else {
                            RoutingProto.Source.Builder metadataSourceBuilder = RoutingProto.Source.newBuilder();
                            metadataSourceBuilder.setNamespace(StringValue.of("*"));
                            metadataSourceBuilder.setService(StringValue.of("*"));
                            ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                            matchStringBuilder.setType(parseMatchStringType(routeTag));
                            matchStringBuilder.setValue(StringValue.of(routeTag.getTagValue()));
                            matchStringBuilder.setValueType(ModelProto.MatchString.ValueType.TEXT);
                            String metadataKey = routeTag.getTagField();
                            metadataSourceBuilder.putMetadata(parseMetadataKey(metadataKey), matchStringBuilder.build());
                            metadataSourceBuilders.add(metadataSourceBuilder);
                        }
                    }
                    for (RoutingProto.Source.Builder sourceBuilder : sourceBuilders) {
                        for (RoutingProto.Source.Builder metadataSourceBuilder : metadataSourceBuilders) {
                            sourceBuilder.putAllMetadata(metadataSourceBuilder.getMetadataMap());
                        }
                        sources.add(sourceBuilder.build());
                    }
                }

                // parse destinations
                List<RoutingProto.Destination> destinations = Lists.newArrayList();
                for (RouteDest routeDest : routeRule.getDestList()) {
                    RoutingProto.Destination.Builder destBuilder = RoutingProto.Destination.newBuilder();
                    destBuilder.setNamespace(StringValue.of(namespace));
                    destBuilder.setService(StringValue.of(service));
                    destBuilder.setPriority(UInt32Value.of(0));
                    destBuilder.setIsolate(BoolValue.of(false));
                    destBuilder.setWeight(UInt32Value.of(routeDest.getDestWeight()));
                    destBuilder.setName(StringValue.of(routeDest.getDestId()));
                    for (RouteDestItem routeDestItem : routeDest.getDestItemList()) {
                        ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                        matchStringBuilder.setType(ModelProto.MatchString.MatchStringType.EXACT);
                        matchStringBuilder.setValue(StringValue.of(routeDestItem.getDestItemValue()));
                        matchStringBuilder.setValueType(ModelProto.MatchString.ValueType.TEXT);
                        destBuilder.putMetadata(routeDestItem.getDestItemField(), matchStringBuilder.build());
                    }
                    destinations.add(destBuilder.build());
                }
                routeBuilder.addAllSources(sources);
                routeBuilder.addAllDestinations(destinations);
                routes.add(routeBuilder.build());
            }
        }
        return routes;
    }

    private Long getRouterRuleConsulIndex(RouterRuleKey routerRuleKey) {
        Long index = routerRuleConsulIndexMap.get(routerRuleKey);
        if (index != null) {
            return index;
        }
        setRouterRuleConsulIndex(routerRuleKey, null, -1L);
        return -1L;
    }

    private void setRouterRuleConsulIndex(RouterRuleKey routerRuleKey, Long lastIndex, Long newIndex) {
        LOG.debug("RouterRuleKey: {}; lastIndex: {}; newIndex: {}", routerRuleKey, lastIndex, newIndex);
        routerRuleConsulIndexMap.put(routerRuleKey, newIndex);
    }

    static class RouterRuleKey {
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
            RouterRuleKey that = (RouterRuleKey) object;
            return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getService(), that.getService()) && Objects.equals(getFetchGroup(), that.getFetchGroup());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespace(), getService(), getFetchGroup());
        }

        @Override
        public String toString() {
            return "RouterRuleKey{" +
                    "namespace='" + namespace + '\'' +
                    ", serviceName='" + service + '\'' +
                    ", fetchGroup=" + fetchGroup +
                    '}';
        }
    }
}
