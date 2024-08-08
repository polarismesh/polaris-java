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

package com.tencent.polaris.plugins.connector.consul.service;

import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.transport.HttpResponse;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.health.model.HealthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.tencent.polaris.api.utils.TimeUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ecwid.consul.json.GsonFactory.getGson;
import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;
import static com.tencent.polaris.plugins.connector.consul.ConsulServerUtils.findHost;
import static com.tencent.polaris.plugins.connector.consul.ConsulServerUtils.getMetadata;

/**
 * @author Haotian Zhang
 */
public class InstanceService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceService.class);
    // consul origin key, 10 位时间戳
    private final static String TSF_CREATION_TIME_KEY = "TSF_CREATION_TIME";
    // 保持和 f/g/h 版本一致, 13 位时间戳
    private final static String TSF_START_TIME_KEY = "TSF_START_TIME";

    private final Map<String, Long> serviceConsulIndexMap = new ConcurrentHashMap<>();

    public InstanceService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                           String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String serviceId = serviceUpdateTask.getServiceEventKey().getService();
        String tag = consulContext.getQueryTag();
        String token = consulContext.getAclToken();
        boolean onlyPassing = consulContext.getQueryPassing();
        UrlParameters tokenParam = StringUtils.isNotBlank(token) ? new SingleUrlParameters("token", token) : null;
        UrlParameters tagParams = StringUtils.isNotBlank(tag) ? new SingleUrlParameters("tag", tag) : null;
        UrlParameters passingParams = onlyPassing ? new SingleUrlParameters("passing") : null;
        UrlParameters nsTypeParam = new SingleUrlParameters("nsType", "DEF_AND_GLOBAL");
        Long currentIndex = getServersConsulIndex(serviceId);
        int code = ServerCodes.DATA_NO_CHANGE;
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        try {
            LOG.debug("Begin get service instances of {} sync", serviceId);
            HttpResponse rawResponse = consulRawClient.makeGetRequest("/v1/health/service/" + serviceId, tagParams,
                    passingParams, tokenParam, nsTypeParam, queryParams);
            if (rawResponse != null) {
                if (!currentIndex.equals(rawResponse.getConsulIndex())) {
                    code = ServerCodes.EXECUTE_SUCCESS;
                }
                LOG.debug("raw response: " + rawResponse.getContent() + " ; onlyPassing: " + onlyPassing);
                List<HealthService> value;
                if (rawResponse.getStatusCode() == 200) {
                    value = getGson().fromJson(rawResponse.getContent(),
                            new TypeToken<List<HealthService>>() {
                            }.getType());
                } else {
                    String rawResponseStr = "";
                    try {
                        rawResponseStr = mapper.writeValueAsString(rawResponse);
                    } catch (JsonProcessingException ignore) {
                    }
                    LOG.error("get service server list occur error. serviceId: {}. RawResponse: {}", serviceId,
                            rawResponseStr);
                    throw new OperationException(rawResponse);
                }
                ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
                newServiceBuilder.setNamespace(StringValue.of(namespace));
                newServiceBuilder.setName(StringValue.of(serviceUpdateTask.getServiceEventKey().getService()));
                newServiceBuilder.setRevision(StringValue.of(String.valueOf(rawResponse.getConsulIndex())));
                ServiceProto.Service service = newServiceBuilder.build();
                List<ServiceProto.Instance> instanceList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(value)) {
                    for (HealthService healthService : value) {
                        ServiceProto.Instance.Builder instanceBuilder = ServiceProto.Instance.newBuilder()
                                .setNamespace(StringValue.of(namespace))
                                .setService(StringValue.of(serviceId))
                                .setHost(StringValue.of(findHost(healthService)))
                                .setPort(UInt32Value.of(healthService.getService().getPort()))
                                .setHealthy(BoolValue.of(true))
                                .setIsolate(BoolValue.of(false));
                        // set Id
                        if (StringUtils.isNotBlank(healthService.getService().getId())) {
                            instanceBuilder.setId(StringValue.of(healthService.getService().getId()));
                        } else {
                            String id =
                                    serviceId + "-" + findHost(healthService).replace(".", "-") + "-" + healthService.getService().getPort();
                            instanceBuilder.setId(StringValue.of(id));
                            LOG.info("Instance with name {} host {} port {} doesn't have id.", serviceId
                                    , findHost(healthService), healthService.getService().getPort());
                        }
                        // set metadata
                        Map<String, String> metadata = getMetadata(healthService);
                        if (CollectionUtils.isNotEmpty(metadata)) {
                            instanceBuilder.putAllMetadata(metadata);
                        }
                        // set createTime
                        Long createTime = null;
                        if (StringUtils.isNotEmpty(metadata.get(TSF_CREATION_TIME_KEY))) {
                            createTime = Long.parseLong(metadata.get(TSF_CREATION_TIME_KEY)) * 1000;
                        }
                        if (createTime == null && StringUtils.isNotEmpty(metadata.get(TSF_START_TIME_KEY))) {
                            createTime = Long.parseLong(metadata.get(TSF_START_TIME_KEY));
                        }
                        if (createTime != null) {
                            instanceBuilder.setCtime(StringValue.of(TimeUtils.getCreateTimeStr(createTime)));
                        }
                        // set location
                        ModelProto.Location.Builder locationBuilder = ModelProto.Location.newBuilder();
                        if (metadata.containsKey(TsfMetadataConstants.TSF_ZONE)) {
                            locationBuilder.setZone(StringValue.of(metadata.get(TsfMetadataConstants.TSF_ZONE)));
                        }
                        if (metadata.containsKey(TsfMetadataConstants.TSF_REGION)) {
                            locationBuilder.setRegion(StringValue.of(metadata.get(TsfMetadataConstants.TSF_REGION)));
                        }
                        instanceBuilder.setLocation(locationBuilder.build());
                        instanceList.add(instanceBuilder.build());
                    }
                }

                ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
                newDiscoverResponseBuilder.setService(service);
                newDiscoverResponseBuilder.addAllInstances(instanceList);
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));

                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                // 即使无服务，也要更新 index
                if (rawResponse.getConsulIndex() != null) {
                    setServersConsulIndex(serviceId, currentIndex, rawResponse.getConsulIndex());
                }
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        } catch (Throwable throwable) {
            LOG.error("Get service instances of {} sync failed. Will sleep for {} ms.", serviceId, consulContext.getConsulErrorSleep(), throwable);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: " + e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    String.format("Get service instances of %s sync failed.",
                            serviceUpdateTask.getServiceEventKey().getServiceKey()));
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
            serviceUpdateTask.retry();
        }
    }

    private Long getServersConsulIndex(String serviceId) {
        Long index = serviceConsulIndexMap.get(serviceId);
        if (index != null) {
            return index;
        }
        setServersConsulIndex(serviceId, null, -1L);
        return -1L;
    }

    private void setServersConsulIndex(String serviceId, Long lastIndex, Long newIndex) {
        LOG.debug("serviceId: {}; lastIndex: {}; newIndex: {}", serviceId, lastIndex, newIndex);
        serviceConsulIndexMap.put(serviceId, newIndex);
    }
}
