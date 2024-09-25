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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.Any;
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
import com.tencent.polaris.plugins.connector.consul.service.router.entity.RouteAffinity;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;

/**
 * @author Haotian Zhang
 */
public class NearByRouteRuleService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(NearByRouteRuleService.class);

    private final Map<NearByRouteRuleKey, Long> affinityConsulIndexMap = new ConcurrentHashMap<>();

    public NearByRouteRuleService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                                  String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String service = serviceUpdateTask.getServiceEventKey().getService();
        String routeAffinityKey = String.format("/v1/kv/affinity/%s/data", namespace);
        // 带等待时间发起对Consul的KV请求
        LOG.trace("tsf route affinity, consul kv namespace, getKey: {}", routeAffinityKey);
        UrlParameters tokenParam = new SingleUrlParameters("token", consulContext.getAclToken());
        NearByRouteRuleKey nearByRouteRuleKey = new NearByRouteRuleKey();
        nearByRouteRuleKey.setNamespace(namespace);
        nearByRouteRuleKey.setService(service);
        Long currentIndex = getRouterRuleConsulIndex(nearByRouteRuleKey);
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        int code = ServerCodes.DATA_NO_CHANGE;
        try {
            LOG.debug("Begin get affinity rules of {}:{} sync", namespace, service);
            HttpResponse rawResponse = consulRawClient.makeGetRequest(routeAffinityKey, tokenParam, queryParams);
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
                    LOG.debug("tsf route affinity, consul kv namespace, response: {}", responseStr);
                }

                Long newIndex = rawResponse.getConsulIndex();
                // create service.
                ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
                newServiceBuilder.setNamespace(StringValue.of(namespace));
                newServiceBuilder.setName(StringValue.of(service));
                newServiceBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));
                // create route rule list.
                List<RoutingProto.RouteRule> routes = new ArrayList<>();
                // create discover response.
                ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
                newDiscoverResponseBuilder.setService(newServiceBuilder);
                if (Objects.nonNull(newIndex)) {
                    if (!Objects.equals(currentIndex, newIndex)) {
                        code = ServerCodes.EXECUTE_SUCCESS;
                        if (rawResponse.getStatusCode() == 200) {
                            if (rawResponse.getContent() != null) {
                                LOG.info("new affinity route rule: {}", rawResponse.getContent());
                                routes = parseResponse(rawResponse, namespace, service);
                            }
                        } else if (rawResponse.getStatusCode() == 404) {
                            LOG.info("empty route rule: {}", rawResponse.getContent());
                        }
                    } else {
                        LOG.debug("[TSF Route Affinity] Consul data is not changed");
                    }
                } else {
                    LOG.warn("[TSF Route Affinity] Consul data is abnormal. {}", rawResponse);
                }
                if (CollectionUtils.isNotEmpty(routes)) {
                    newDiscoverResponseBuilder.addAllNearbyRouteRules(routes);
                }
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                // 重写index
                if (newIndex != null) {
                    setRouterRuleConsulIndex(nearByRouteRuleKey, currentIndex, newIndex);
                }
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        } catch (Throwable e) {
            LOG.error("[TSF Route Rule] tsf route affinity rule load error. Will sleep for {} ms. Key path:{}",
                    consulContext.getConsulErrorSleep(), routeAffinityKey, e);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get nearby route rule sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        }
    }

    private List<RoutingProto.RouteRule> parseResponse(final HttpResponse response, String namespace, String service) {
        List<GetValue> valueList = GsonFactory.getGson().fromJson(response.getContent(),
                new TypeToken<List<GetValue>>() {
                }.getType());
        // yaml -> json -> list<RouteAffinity>
        Representer representer = new Representer(new DumperOptions());
        representer.addClassTag(RouteAffinity.class, Tag.MAP);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(representer);
        List<RouteAffinity> routeAffinityList = Lists.newArrayList();
        valueList.forEach(value -> {
            try {
                String routeJsonString = mapper
                        .writeValueAsString(yaml.load(value.getDecodedValue()));
                RouteAffinity routeAffinity = yaml.loadAs(routeJsonString, RouteAffinity.class);
                LOG.info("tsf route affinity, namespace:{}, affinity: {}", namespace, routeAffinity.getAffinity());
                routeAffinityList.add(routeAffinity);
            } catch (Exception ex) {
                LOG.error("tsf affinity rule load error.", ex);
                throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf affinity rule load error.", ex);
            }
        });

        // list<RouteAffinity> -> List<RoutingProto.RouteRule>
        List<RoutingProto.RouteRule> routes = Lists.newArrayList();
        for (RouteAffinity routeAffinity : routeAffinityList) {
            RoutingProto.RouteRule.Builder routeRuleBuilder = RoutingProto.RouteRule.newBuilder();
            routeRuleBuilder.setNamespace(routeAffinity.getNamespaceId());
            routeRuleBuilder.setEnable(routeAffinity.getAffinity());
            RoutingProto.NearbyRoutingConfig.Builder nearbyRoutingConfigBuilder = RoutingProto.NearbyRoutingConfig.newBuilder();
            nearbyRoutingConfigBuilder.setNamespace(namespace);
            nearbyRoutingConfigBuilder.setService(service);
            nearbyRoutingConfigBuilder.setMatchLevel(RoutingProto.NearbyRoutingConfig.LocationLevel.ZONE);
            nearbyRoutingConfigBuilder.setMaxMatchLevel(RoutingProto.NearbyRoutingConfig.LocationLevel.ALL);
            routeRuleBuilder.setRoutingConfig(Any.pack(nearbyRoutingConfigBuilder.build()));
            routes.add(routeRuleBuilder.build());
        }
        return routes;
    }

    private Long getRouterRuleConsulIndex(NearByRouteRuleKey nearByRouteRuleKey) {
        Long index = affinityConsulIndexMap.get(nearByRouteRuleKey);
        if (index != null) {
            return index;
        }
        setRouterRuleConsulIndex(nearByRouteRuleKey, null, -1L);
        return -1L;
    }

    private void setRouterRuleConsulIndex(NearByRouteRuleKey nearByRouteRuleKey, Long lastIndex, Long newIndex) {
        LOG.debug("NearByRouteRuleKey: {}; lastIndex: {}; newIndex: {}", nearByRouteRuleKey, lastIndex, newIndex);
        affinityConsulIndexMap.put(nearByRouteRuleKey, newIndex);
    }

    static class NearByRouteRuleKey {
        private String namespace = "";
        private String service = "";

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

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            NearByRouteRuleKey that = (NearByRouteRuleKey) object;
            return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getService(), that.getService());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespace(), getService());
        }

        @Override
        public String toString() {
            return "NearByRouteRuleKey{" +
                    "namespace='" + namespace + '\'' +
                    ", service='" + service + '\'' +
                    '}';
        }
    }
}
