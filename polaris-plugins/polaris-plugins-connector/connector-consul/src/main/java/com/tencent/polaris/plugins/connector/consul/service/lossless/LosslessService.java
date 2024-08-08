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

package com.tencent.polaris.plugins.connector.consul.service.lossless;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.json.GsonFactory;
import com.ecwid.consul.transport.HttpResponse;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import com.tencent.polaris.plugins.connector.consul.service.ConsulService;
import com.tencent.polaris.plugins.connector.consul.service.lossless.entity.WarmupSetting;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;

/**
 * Watch warmup rule from tsf consul.
 * @author Shedfree Wu
 */
public class LosslessService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(LosslessService.class);

    private final Map<WarmupRuleKey, Long> warmupRuleConsulIndexMap = new ConcurrentHashMap<>();

    public LosslessService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                          String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String consulApi = String.format("/v1/kv/warmup/%s/", namespace);
        LOG.trace("tsf warmup rule, consul kv namespace, getKey: {}", consulApi);
        UrlParameters tokenParam = new SingleUrlParameters("token", consulContext.getAclToken());
        UrlParameters recurseParam = new SingleUrlParameters("recurse");
        WarmupRuleKey warmupRuleKey = new WarmupRuleKey();
        warmupRuleKey.setNamespace(namespace);
        warmupRuleKey.setService(serviceUpdateTask.getServiceEventKey().getService());
        Long currentIndex = getWarmupRuleConsulIndex(warmupRuleKey);
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        int code = ServerCodes.DATA_NO_CHANGE;
        try {
            LOG.debug("Begin get warmup rules of {} sync", warmupRuleKey);
            HttpResponse rawResponse = consulRawClient.makeGetRequest(consulApi, recurseParam, tokenParam, queryParams);
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
                    LOG.debug("tsf warmup rule, consul kv namespace, response: {}", responseStr);
                }

                Long newIndex = rawResponse.getConsulIndex();

                // 重写index
                Map<String, LosslessProto.Warmup> warmups = new HashMap<>();
                if (Objects.nonNull(newIndex)) {
                    if (!Objects.equals(currentIndex, newIndex)) {
                        code = ServerCodes.EXECUTE_SUCCESS;
                        if (rawResponse.getStatusCode() == 200) {
                            if (rawResponse.getContent() != null) {
                                LOG.info("new warmup rule: {}", rawResponse.getContent());
                                warmups = parseResponse(rawResponse);
                            }
                        } else if (rawResponse.getStatusCode() == 404) {
                            LOG.info("empty warmup rule: {}", rawResponse.getContent());
                        }
                    } else {
                        LOG.debug("[TSF Warmup Rule] Consul data is not changed");
                    }
                } else {
                    LOG.warn("[TSF Warmup Rule] Consul data is abnormal. {}", rawResponse);
                }

                // create service.
                ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
                newServiceBuilder.setNamespace(StringValue.of(namespace));
                newServiceBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));

                // create discover response.
                ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
                newDiscoverResponseBuilder.setService(newServiceBuilder);

                List<LosslessProto.LosslessRule> losslessRuleList = new ArrayList<>(warmups.size());
                for (Map.Entry<String, LosslessProto.Warmup> entry : warmups.entrySet()) {
                    LosslessProto.LosslessOnline.Builder newLosslessOnlineBuilder = LosslessProto.LosslessOnline.newBuilder();
                    newLosslessOnlineBuilder.setWarmup(entry.getValue());
                    // tsf mode, default true
                    newLosslessOnlineBuilder.setReadiness(LosslessProto.Readiness.newBuilder().setEnable(true).build());

                    LosslessProto.LosslessOffline.Builder newLosslessOfflineBuilder = LosslessProto.LosslessOffline.newBuilder();
                    // tsf mode, default true
                    newLosslessOfflineBuilder.setEnable(true);

                    LosslessProto.LosslessRule.Builder newLosslessRuleBuilder = LosslessProto.LosslessRule.newBuilder();
                    newLosslessRuleBuilder.setLosslessOnline(newLosslessOnlineBuilder.build());
                    newLosslessRuleBuilder.setLosslessOffline(newLosslessOfflineBuilder.build());
                    // tsf use TSF_GROUP_ID key to do warmup
                    newLosslessRuleBuilder.putMetadata("TSF_GROUP_ID", entry.getKey());

                    losslessRuleList.add(newLosslessRuleBuilder.build());
                }

                newDiscoverResponseBuilder.addAllLosslessRules(losslessRuleList);
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                if (newIndex != null) {
                    setWarmupRuleConsulIndex(warmupRuleKey, currentIndex, newIndex);
                }
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        } catch (Throwable e) {
            LOG.error("[TSF Warmup Rule] tsf warmup rule load error. Will sleep for {} ms. Key path:{}",
                    consulContext.getConsulErrorSleep(), consulApi, e);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get warmup sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        }
    }

    private Map<String, LosslessProto.Warmup> parseResponse(final HttpResponse response) {
        List<GetValue> valueList = GsonFactory.getGson().fromJson(response.getContent(),
                new TypeToken<List<GetValue>>() {
                }.getType());
        // yaml -> json -> list<WarmupSetting>
        Yaml yaml = new Yaml();
        ObjectMapper mapper = new ObjectMapper();
        // 配置 ObjectMapper在反序列化时，忽略目标对象没有的属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Map<String, WarmupSetting> warmupSettingMap = new HashMap<>();
        valueList.forEach(value -> {
            try {
                // 从路径上获取 ns_id 和 group_id
                String[] split = value.getKey().split("/");
                // 这里比较粗暴，直接判断 数组长度是否为 3，如果不为 3 ，不当作 tsf 的预热规则处理。
                if (split.length > 3) {
                    String groupId = split[2];
                    WarmupSetting ws = yaml.loadAs(value.getDecodedValue(), WarmupSetting.class);
                    if (ws != null) {
                        warmupSettingMap.put(groupId, ws);
                    }
                }
            } catch (Exception ex) {
                LOG.error("tsf warmup rule load error.", ex);
                throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf warmup rule load error", ex);
            }
        });

        Map<String, LosslessProto.Warmup> warmupMap = new HashMap<>();
        for (Map.Entry<String, WarmupSetting> entry : warmupSettingMap.entrySet()) {
            String groupId = entry.getKey();
            WarmupSetting warmupSetting = entry.getValue();

            LosslessProto.Warmup.Builder warmupBuilder = LosslessProto.Warmup.newBuilder();
            warmupBuilder.setEnable(warmupSetting.isEnabled());
            warmupBuilder.setCurvature(warmupSetting.getCurvature());
            warmupBuilder.setIntervalSecond(warmupSetting.getWarmupTime());
            warmupBuilder.setEnableOverloadProtection(warmupSetting.isEnabledProtection());

            warmupMap.put(groupId, warmupBuilder.build());

        }
        return warmupMap;
    }

    private Long getWarmupRuleConsulIndex(WarmupRuleKey warmupRuleKey) {
        Long index = warmupRuleConsulIndexMap.get(warmupRuleKey);
        if (index != null) {
            return index;
        }
        setWarmupRuleConsulIndex(warmupRuleKey, null, -1L);
        return -1L;
    }

    private void setWarmupRuleConsulIndex(WarmupRuleKey warmupRuleKey, Long lastIndex, Long newIndex) {
        LOG.debug("WarmupRuleKey: {}; lastIndex: {}; newIndex: {}", warmupRuleKey, lastIndex, newIndex);
        warmupRuleConsulIndexMap.put(warmupRuleKey, newIndex);
    }

    static class WarmupRuleKey {
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
            WarmupRuleKey that = (WarmupRuleKey) object;
            return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getService(), that.getService());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespace(), getService());
        }

        @Override
        public String toString() {
            return "WarmupRuleKey{" +
                    "namespace='" + namespace + '\'' +
                    ", service='" + service + '\'' +
                    '}';
        }
    }
}
