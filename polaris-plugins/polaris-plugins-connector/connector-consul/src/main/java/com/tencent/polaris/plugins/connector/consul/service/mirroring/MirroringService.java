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

package com.tencent.polaris.plugins.connector.consul.service.mirroring;

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
import com.google.protobuf.FloatValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import com.tencent.polaris.plugins.connector.consul.service.ConsulService;
import com.tencent.polaris.plugins.connector.consul.service.mirroring.entity.MirrorRule;
import com.tencent.polaris.plugins.connector.consul.service.router.RouterUtils;
import com.tencent.polaris.plugins.connector.consul.service.router.entity.RouteRule;
import com.tencent.polaris.plugins.connector.consul.service.router.entity.RouteRuleGroup;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.TrafficMirroringProto;
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
public class MirroringService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(MirroringService.class);

    private final Map<MirroringRuleKey, Long> mirroringRuleConsulIndexMap = new ConcurrentHashMap<>();

    public MirroringService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                            String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String service = serviceUpdateTask.getServiceEventKey().getService();
        String mirrorRuleKey = String.format("/v1/kv/mirror/%s/%s/data", namespace, service);
        LOG.trace("tsf mirror rule, consul kv namespace, getKey: {}", mirrorRuleKey);
        UrlParameters nsTypeParam = new SingleUrlParameters("nsType", "DEF_AND_GLOBAL");
        UrlParameters tokenParam = new SingleUrlParameters("token", consulContext.getAclToken());
        UrlParameters recurseParam = new SingleUrlParameters("recurse");
        MirroringRuleKey mirroringRuleKey = new MirroringRuleKey();
        mirroringRuleKey.setNamespace(namespace);
        mirroringRuleKey.setService(service);
        Long currentIndex = getMirroringRuleConsulIndex(mirroringRuleKey);
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        int code = ServerCodes.DATA_NO_CHANGE;
        try {
            LOG.debug("Begin get mirror rules of {} sync", mirroringRuleKey);
            HttpResponse rawResponse = consulRawClient.makeGetRequest(mirrorRuleKey, recurseParam, tokenParam,
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
                    LOG.debug("tsf mirror rule, consul kv namespace, response: {}", responseStr);
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
                List<TrafficMirroringProto.TrafficMirroring> trafficMirroringList = new ArrayList<>();
                if (Objects.nonNull(newIndex)) {
                    if (!Objects.equals(currentIndex, newIndex)) {
                        code = ServerCodes.EXECUTE_SUCCESS;
                        if (rawResponse.getStatusCode() == 200) {
                            if (rawResponse.getContent() != null) {
                                LOG.info("new mirror rule: {}", rawResponse.getContent());
                                trafficMirroringList = parseResponse(rawResponse, namespace, service);
                            }
                        } else if (rawResponse.getStatusCode() == 404) {
                            LOG.info("empty mirror rule: {}", rawResponse.getContent());
                        }
                    } else {
                        LOG.debug("[TSF Mirror Rule] Consul data is not changed");
                    }
                } else {
                    LOG.warn("[TSF Mirror Rule] Consul data is abnormal. {}", rawResponse);
                }
                if (CollectionUtils.isNotEmpty(trafficMirroringList)) {
                    newDiscoverResponseBuilder.addAllTrafficMirroring(trafficMirroringList);
                }
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                if (newIndex != null) {
                    setMirroringRuleConsulIndex(mirroringRuleKey, currentIndex, newIndex);
                }
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        } catch (Throwable e) {
            LOG.error("[TSF Mirror Rule] tsf mirror rule load error. Will sleep for {} ms. Key path:{}",
                    consulContext.getConsulErrorSleep(), mirrorRuleKey, e);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get mirroring sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        }
    }

    private List<TrafficMirroringProto.TrafficMirroring> parseResponse(final HttpResponse response, String namespace, String service) {
        List<GetValue> valueList = GsonFactory.getGson().fromJson(response.getContent(),
                new TypeToken<List<GetValue>>() {
                }.getType());
        // yaml -> json -> list<RouteRuleGroup>
        Yaml yaml = new Yaml();
        List<RouteRuleGroup> routeRuleGroupList = Lists.newArrayList();
        valueList.forEach(value -> {
            try {
                String mirrorJsonString = mapper
                        .writeValueAsString(yaml.load(value.getDecodedValue()));
                List<RouteRuleGroup> tempList = mapper.readValue(mirrorJsonString,
                        new TypeReference<List<RouteRuleGroup>>() {
                        });
                if (!CollectionUtils.isEmpty(tempList)) {
                    routeRuleGroupList.add(tempList.get(0));
                }
            } catch (Exception ex) {
                LOG.error("tsf mirror rule load error.", ex);
                throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf mirror rule load error", ex);
            }
        });

        // list<RouteRuleGroup> -> List<TrafficMirroringProto.TrafficMirroring>
        List<TrafficMirroringProto.TrafficMirroring> trafficMirroringList = Lists.newArrayList();
        for (RouteRuleGroup routeRuleGroup : routeRuleGroupList) {
            for (RouteRule routeRule : routeRuleGroup.getRuleList()) {
                MirrorRule mirrorRule = routeRule.getMirrorRule();
                TrafficMirroringProto.TrafficMirroring.Builder trafficMirroringBuilder = TrafficMirroringProto.TrafficMirroring.newBuilder();

                // parse enabled
                trafficMirroringBuilder.setEnabled(BoolValue.of(mirrorRule.getEnabled()));

                // parse sources
                List<RoutingProto.Source> sources = RouterUtils.parseTagListToSourceList(routeRule.getTagList());

                // parse mirroring percentage
                if (mirrorRule.getMirrorPercentage() != null) {
                    trafficMirroringBuilder.setMirroringPercent(FloatValue.of(mirrorRule.getMirrorPercentage()));
                } else {
                    trafficMirroringBuilder.setMirroringPercent(FloatValue.of(100.0f));
                }

                // parse destinations
                List<RoutingProto.Destination> destinations = Lists.newArrayList();
                RoutingProto.Destination.Builder destBuilder = RoutingProto.Destination.newBuilder();
                destBuilder.setNamespace(StringValue.of(namespace));
                destBuilder.setService(StringValue.of(service));
                destBuilder.setPriority(UInt32Value.of(0));
                destBuilder.setIsolate(BoolValue.of(false));
                destBuilder.setWeight(UInt32Value.of(100));
                // set applicationId
                ModelProto.MatchString.Builder applicationIdMatchStringBuilder = ModelProto.MatchString.newBuilder();
                applicationIdMatchStringBuilder.setType(ModelProto.MatchString.MatchStringType.EXACT);
                applicationIdMatchStringBuilder.setValue(StringValue.of(mirrorRule.getApplicationId()));
                applicationIdMatchStringBuilder.setValueType(ModelProto.MatchString.ValueType.TEXT);
                destBuilder.putMetadata(TsfMetadataConstants.TSF_APPLICATION_ID, applicationIdMatchStringBuilder.build());
                // set destinationDeployGroup
                ModelProto.MatchString.Builder destinationDeployGroupMatchStringBuilder = ModelProto.MatchString.newBuilder();
                destinationDeployGroupMatchStringBuilder.setType(ModelProto.MatchString.MatchStringType.EXACT);
                destinationDeployGroupMatchStringBuilder.setValue(StringValue.of(mirrorRule.getDestinationDeployGroup()));
                destinationDeployGroupMatchStringBuilder.setValueType(ModelProto.MatchString.ValueType.TEXT);
                destBuilder.putMetadata(TsfMetadataConstants.TSF_GROUP_ID, destinationDeployGroupMatchStringBuilder.build());

                destinations.add(destBuilder.build());
                trafficMirroringBuilder.addAllSources(sources);
                trafficMirroringBuilder.addAllDestinations(destinations);
                trafficMirroringList.add(trafficMirroringBuilder.build());
            }
        }
        return trafficMirroringList;
    }

    private Long getMirroringRuleConsulIndex(MirroringRuleKey mirroringRuleKey) {
        Long index = mirroringRuleConsulIndexMap.get(mirroringRuleKey);
        if (index != null) {
            return index;
        }
        setMirroringRuleConsulIndex(mirroringRuleKey, null, -1L);
        return -1L;
    }

    private void setMirroringRuleConsulIndex(MirroringRuleKey mirroringRuleKey, Long lastIndex, Long newIndex) {
        if (isEnable() && isReset) {
            LOG.info("MirroringRuleKey: {} is reset.", mirroringRuleKey);
            mirroringRuleConsulIndexMap.remove(mirroringRuleKey);
            isReset = false;
        } else if (isEnable()) {
            LOG.debug("MirroringRuleKey: {}; lastIndex: {}; newIndex: {}", mirroringRuleKey, lastIndex, newIndex);
            mirroringRuleConsulIndexMap.put(mirroringRuleKey, newIndex);
        } else {
            LOG.info("MirroringRuleKey: {} is disabled.", mirroringRuleKey);
            mirroringRuleConsulIndexMap.remove(mirroringRuleKey);
        }
    }

    static class MirroringRuleKey {
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
            MirroringRuleKey that = (MirroringRuleKey) object;
            return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getService(), that.getService()) && Objects.equals(getFetchGroup(), that.getFetchGroup());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespace(), getService(), getFetchGroup());
        }

        @Override
        public String toString() {
            return "MirroringRuleKey{" +
                    "namespace='" + namespace + '\'' +
                    ", serviceName='" + service + '\'' +
                    ", fetchGroup=" + fetchGroup +
                    '}';
        }
    }
}
