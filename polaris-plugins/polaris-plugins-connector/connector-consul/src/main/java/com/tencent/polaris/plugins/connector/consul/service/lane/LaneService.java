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

package com.tencent.polaris.plugins.connector.consul.service.lane;

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
import com.google.protobuf.Any;
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
import com.tencent.polaris.plugins.connector.consul.service.lane.entity.*;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;
import static com.tencent.polaris.metadata.core.constant.TsfMetadataConstants.TSF_GROUP_ID;
import static com.tencent.polaris.plugins.connector.consul.service.common.TagConditionUtil.parseMatchStringType;

/**
 * @author Haotian Zhang
 */
public class LaneService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(LaneService.class);

    private final Map<LaneService.LaneRuleKey, Long> laneInfoConsulIndexMap = new ConcurrentHashMap<>();

    private final Map<LaneService.LaneRuleKey, Long> laneRuleConsulIndexMap = new ConcurrentHashMap<>();

    public LaneService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                       String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    protected void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> lock = new AtomicReference<>();
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        AtomicBoolean isLaneInfoReturn = new AtomicBoolean(true);
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String service = serviceUpdateTask.getServiceEventKey().getService();
        LaneRuleKey laneRuleKey = new LaneRuleKey();
        laneRuleKey.setNamespace(namespace);
        laneRuleKey.setService(service);
        Long currentLaneInfoConsulIndex = getLaneInfoConsulIndex(laneRuleKey);
        Long currentLaneRuleConsulIndex = getLaneRuleConsulIndex(laneRuleKey);

        // Task for /lane/info
        AtomicReference<LaneInfoResponse> laneInfoResponseAtomicReference = new AtomicReference<>();
        Runnable laneInfoMainTask = () -> {
            try {
                laneInfoResponseAtomicReference.set(syncLaneInfo(serviceUpdateTask, currentLaneInfoConsulIndex, true));
                if (lock.compareAndSet(null, new Object())) {
                    latch.countDown();
                }
            } catch (Throwable t) {
                if (throwable.compareAndSet(null, t)) {
                    latch.countDown();
                }
            }
        };

        // Task for /lane/rule
        AtomicReference<LaneRuleResponse> laneRuleResponseAtomicReference = new AtomicReference<>();
        Runnable laneRuleMainTask = () -> {
            try {
                laneRuleResponseAtomicReference.set(syncLaneRule(serviceUpdateTask, currentLaneRuleConsulIndex, true));
                if (lock.compareAndSet(null, new Object())) {
                    isLaneInfoReturn.set(false);
                    latch.countDown();
                }
            } catch (Throwable t) {
                if (throwable.compareAndSet(null, t)) {
                    latch.countDown();
                }
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            executorService.execute(laneInfoMainTask);
            executorService.execute(laneRuleMainTask);
            latch.await();
            executorService.shutdownNow();
            if (throwable.get() != null) {
                throw throwable.get();
            }
            LaneInfoResponse laneInfoResponse = null;
            LaneRuleResponse laneRuleResponse = null;
            if (isLaneInfoReturn.get()) {
                laneInfoResponse = laneInfoResponseAtomicReference.get();
                if (laneInfoResponse != null) {
                    laneRuleResponse = syncLaneRule(serviceUpdateTask, currentLaneRuleConsulIndex, false);
                }
            } else {
                laneRuleResponse = laneRuleResponseAtomicReference.get();
                if (laneRuleResponse != null) {
                    laneInfoResponse = syncLaneInfo(serviceUpdateTask, currentLaneInfoConsulIndex, false);
                }
            }

            int code = ServerCodes.DATA_NO_CHANGE;
            // create service.
            ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
            newServiceBuilder.setNamespace(StringValue.of(namespace));
            newServiceBuilder.setName(StringValue.of(service));
            // create discover response.
            ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
            if (laneInfoResponse != null && laneRuleResponse != null) {
                code = ServerCodes.EXECUTE_SUCCESS;
                newServiceBuilder.setRevision(StringValue.of(laneInfoResponse.getIndex() + "-" + laneRuleResponse.getIndex()));
                List<LaneProto.LaneGroup> laneGroupList = parseResponse(laneInfoResponse, laneRuleResponse, namespace, service);
                if (CollectionUtils.isNotEmpty(laneGroupList)) {
                    newDiscoverResponseBuilder.addAllLanes(laneGroupList);
                }
            }
            newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
            newDiscoverResponseBuilder.setService(newServiceBuilder);
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
            boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
            // 重写index
            if (laneInfoResponse != null && laneRuleResponse != null) {
                setLaneInfoConsulIndex(laneRuleKey, currentLaneInfoConsulIndex, laneInfoResponse.getIndex());
                setLaneRuleConsulIndex(laneRuleKey, currentLaneRuleConsulIndex, laneRuleResponse.getIndex());
            }
            if (!svcDeleted) {
                serviceUpdateTask.addUpdateTaskSet();
            }
        } catch (Throwable e) {
            LOG.error("[TSF Lane] tsf lane load error. Will sleep for {} ms.", consulContext.getConsulErrorSleep(), e);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get lane sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        } finally {
            if (!executorService.isShutdown() || !executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        }
    }

    private List<LaneProto.LaneGroup> parseResponse(LaneInfoResponse laneInfoResponse,
                                                    LaneRuleResponse laneRuleResponse, String namespace, String service) {
        List<LaneProto.LaneGroup> laneGroupList = new ArrayList<>();

        LaneProto.LaneGroup.Builder laneGroupBuilder = LaneProto.LaneGroup.newBuilder();
        laneGroupBuilder.setName("tsf");
        // set destination group list
        RoutingProto.DestinationGroup.Builder destinationGroupBuilder = RoutingProto.DestinationGroup.newBuilder();
        destinationGroupBuilder.setNamespace("*");
        destinationGroupBuilder.setService("*");
        laneGroupBuilder.addDestinations(destinationGroupBuilder.build());
        // set entry list
        List<LaneInfo> laneInfoList = laneInfoResponse.getLaneInfoList();
        List<String> entranceList = new ArrayList<>();
        for (LaneInfo laneInfo : laneInfoList) {
            for (LaneGroup laneGroup : laneInfo.getLaneGroupList()) {
                if (laneGroup.isEntrance()) {
                    entranceList.add(laneGroup.getGroupId());
                }
            }
        }
        for (String entrance : entranceList) {
            LaneProto.TrafficEntry.Builder trafficEntryBuilder = LaneProto.TrafficEntry.newBuilder();
            trafficEntryBuilder.setType("polarismesh.cn/service");
            LaneProto.ServiceSelector.Builder serviceSelectorBuilder = LaneProto.ServiceSelector.newBuilder();
            serviceSelectorBuilder.setNamespace("*");
            serviceSelectorBuilder.setService("*");
            ModelProto.MatchString.Builder label = ModelProto.MatchString.newBuilder();
            label.setType(ModelProto.MatchString.MatchStringType.EXACT);
            label.setValue(StringValue.of(entrance));
            label.setValueType(ModelProto.MatchString.ValueType.TEXT);
            serviceSelectorBuilder.putLabels(TSF_GROUP_ID, label.build());
            trafficEntryBuilder.setSelector(Any.pack(serviceSelectorBuilder.build()));
            laneGroupBuilder.addEntries(trafficEntryBuilder.build());
        }
        // set rule list
        List<LaneRule> tsfLaneRuleList = laneRuleResponse.getLaneRuleList();
        List<LaneProto.LaneRule> laneRuleList = new ArrayList<>();
        for (LaneRule laneRule : tsfLaneRuleList) {
            LaneProto.LaneRule.Builder laneRuleBuilder = LaneProto.LaneRule.newBuilder();
            laneRuleBuilder.setId(laneRule.getLaneId());
            laneRuleBuilder.setName(laneRule.getLaneId());
            laneRuleBuilder.setGroupName("tsf");
            laneRuleBuilder.setEnable(true);
            laneRuleBuilder.setMatchMode(LaneProto.LaneRule.LaneMatchMode.PERMISSIVE);
            laneRuleBuilder.setPriority(laneRule.getPriority());
            laneRuleBuilder.setLabelKey(TSF_GROUP_ID);
            // set TrafficMatchRule
            LaneProto.TrafficMatchRule.Builder trafficMatchRuleBuilder = LaneProto.TrafficMatchRule.newBuilder();
            trafficMatchRuleBuilder.setMatchMode(parseTrafficMatchMode(laneRule.getRuleTagRelationship()));
            List<RoutingProto.SourceMatch> sourceMatchList = new ArrayList<>();
            for (LaneRuleTag laneRuleTag : laneRule.getRuleTagList()) {
                RoutingProto.SourceMatch.Builder sourceMatchBuilder = RoutingProto.SourceMatch.newBuilder();
                sourceMatchBuilder.setType(RoutingProto.SourceMatch.Type.CUSTOM);
                sourceMatchBuilder.setKey(laneRuleTag.getTagName());
                ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                matchStringBuilder.setType(parseMatchStringType(laneRuleTag.getTagOperator()));
                matchStringBuilder.setValue(StringValue.of(laneRuleTag.getTagValue()));
                matchStringBuilder.setValueType(ModelProto.MatchString.ValueType.TEXT);
                sourceMatchBuilder.setValue(matchStringBuilder.build());
                sourceMatchList.add(sourceMatchBuilder.build());
            }
            if (CollectionUtils.isNotEmpty(sourceMatchList)) {
                trafficMatchRuleBuilder.addAllArguments(sourceMatchList);
            }
            laneRuleBuilder.setTrafficMatchRule(trafficMatchRuleBuilder.build());
            // set DefaultLabelValue
            List<String> labelValueList = new ArrayList<>();
            for (LaneInfo laneInfo : laneInfoList) {
                if (StringUtils.equals(laneInfo.getLaneId(), laneRule.getLaneId())) {
                    for (LaneGroup laneGroup : laneInfo.getLaneGroupList()) {
                        labelValueList.add(laneGroup.getGroupId());
                    }
                }
            }
            laneRuleBuilder.setDefaultLabelValue(String.join(",", labelValueList));
            laneRuleList.add(laneRuleBuilder.build());
        }
        if (CollectionUtils.isNotEmpty(laneRuleList)) {
            laneGroupBuilder.addAllRules(laneRuleList);
        }

        laneGroupList.add(laneGroupBuilder.build());
        return laneGroupList;
    }

    private LaneProto.TrafficMatchRule.TrafficMatchMode parseTrafficMatchMode(RuleTagRelationship ruleTagRelationship) {
        switch (ruleTagRelationship) {
            case RELEATION_OR:
                return LaneProto.TrafficMatchRule.TrafficMatchMode.OR;
            case RELEATION_AND:
            default:
                return LaneProto.TrafficMatchRule.TrafficMatchMode.AND;
        }
    }

    private LaneInfoResponse syncLaneInfo(ServiceUpdateTask serviceUpdateTask, Long currentIndex, boolean wait) {
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String service = serviceUpdateTask.getServiceEventKey().getService();
        String laneInfoPrefixKey = "/v1/kv/lane/info/";
        // 带等待时间发起对Consul的KV请求
        LOG.trace("tsf lane info, consul kv key: {}", laneInfoPrefixKey);
        UrlParameters tokenParam = new SingleUrlParameters("token", consulContext.getAclToken());
        UrlParameters recurseParam = new SingleUrlParameters("recurse");
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        LOG.debug("Begin get lane info of {}:{} sync", namespace, service);
        HttpResponse rawResponse;
        if (wait) {
            rawResponse = consulRawClient.makeGetRequest(laneInfoPrefixKey, recurseParam, tokenParam, queryParams);
        } else {
            rawResponse = consulRawClient.makeGetRequest(laneInfoPrefixKey, recurseParam, tokenParam);
        }
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
                LOG.debug("tsf lane info, consul kv namespace, response: {}", responseStr);
            }

            Long newIndex = rawResponse.getConsulIndex();
            if (Objects.nonNull(newIndex)) {
                if (!Objects.equals(currentIndex, newIndex) || !wait) {
                    LaneInfoResponse laneInfoResponse = new LaneInfoResponse();
                    laneInfoResponse.setIndex(newIndex);
                    laneInfoResponse.setLaneInfoList(new ArrayList<>());
                    if (rawResponse.getStatusCode() == 200) {
                        if (rawResponse.getContent() != null) {
                            LOG.info("new lane info: {}", rawResponse.getContent());
                            laneInfoResponse.setLaneInfoList(parseLaneInfoResponse(rawResponse, mapper));
                        }
                    } else if (rawResponse.getStatusCode() == 404) {
                        LOG.info("empty lane info: {}", rawResponse.getContent());
                    }
                    return laneInfoResponse;
                } else {
                    LOG.debug("[TSF Lane Info] Consul data is not changed");
                }
            } else {
                LOG.warn("[TSF Lane Info] Consul data is abnormal. {}", rawResponse);
            }
        }
        return null;
    }

    private List<LaneInfo> parseLaneInfoResponse(final HttpResponse response, ObjectMapper mapper) {
        List<GetValue> valueList = GsonFactory.getGson().fromJson(response.getContent(),
                new TypeToken<List<GetValue>>() {
                }.getType());
        Yaml yaml = new Yaml();
        List<LaneInfo> laneInfoList = Lists.newArrayList();
        valueList.forEach(value -> {
            try {
                String routeJsonString = mapper
                        .writeValueAsString(yaml.load(value.getDecodedValue()));
                laneInfoList.add(mapper.readValue(routeJsonString,
                        new TypeReference<LaneInfo>() {
                        }));
            } catch (Exception ex) {
                LOG.error("tsf lane info load error, ex", ex);
                throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf lane info load error.", ex);
            }
        });
        return laneInfoList;
    }

    private LaneRuleResponse syncLaneRule(ServiceUpdateTask serviceUpdateTask, Long currentIndex, boolean wait) {
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String service = serviceUpdateTask.getServiceEventKey().getService();
        String laneRulePrefixKey = "/v1/kv/lane/rule/";
        // 带等待时间发起对Consul的KV请求
        LOG.trace("tsf lane rule, consul kv key: {}", laneRulePrefixKey);
        UrlParameters tokenParam = new SingleUrlParameters("token", consulContext.getAclToken());
        UrlParameters recurseParam = new SingleUrlParameters("recurse");
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        LOG.debug("Begin get lane rule of {}:{} sync", namespace, service);
        HttpResponse rawResponse;
        if (wait) {
            rawResponse = consulRawClient.makeGetRequest(laneRulePrefixKey, recurseParam, tokenParam, queryParams);
        } else {
            rawResponse = consulRawClient.makeGetRequest(laneRulePrefixKey, recurseParam, tokenParam);
        }
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
                LOG.debug("tsf lane rule, consul kv namespace, response: {}", responseStr);
            }

            Long newIndex = rawResponse.getConsulIndex();
            if (Objects.nonNull(newIndex)) {
                if (!Objects.equals(currentIndex, newIndex) || !wait) {
                    LaneRuleResponse laneRuleResponse = new LaneRuleResponse();
                    laneRuleResponse.setIndex(newIndex);
                    laneRuleResponse.setLaneRuleList(new ArrayList<>());
                    if (rawResponse.getStatusCode() == 200) {
                        if (rawResponse.getContent() != null) {
                            LOG.info("new lane rule: {}", rawResponse.getContent());
                            laneRuleResponse.setLaneRuleList(parseLaneRuleResponse(rawResponse, mapper));
                        }
                    } else if (rawResponse.getStatusCode() == 404) {
                        LOG.info("empty lane rule: {}", rawResponse.getContent());
                    }
                    return laneRuleResponse;
                } else {
                    LOG.debug("[TSF Lane rule] Consul data is not changed");
                }
            } else {
                LOG.warn("[TSF Lane rule] Consul data is abnormal. {}", rawResponse);
            }
        }
        return null;
    }

    private List<LaneRule> parseLaneRuleResponse(final HttpResponse response, ObjectMapper mapper) {
        List<GetValue> valueList = GsonFactory.getGson().fromJson(response.getContent(),
                new TypeToken<List<GetValue>>() {
                }.getType());
        Yaml yaml = new Yaml();
        List<LaneRule> laneRuleList = Lists.newArrayList();
        valueList.forEach(value -> {
            try {
                String routeJsonString = mapper
                        .writeValueAsString(yaml.load(value.getDecodedValue()));
                laneRuleList.add(mapper.readValue(routeJsonString,
                        new TypeReference<LaneRule>() {
                        }));
            } catch (Exception ex) {
                LOG.error("tsf lane rule load error, ex", ex);
                throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf lane rule load error.", ex);
            }
        });
        return laneRuleList;
    }

    private Long getLaneInfoConsulIndex(LaneRuleKey laneInfoKey) {
        Long index = laneInfoConsulIndexMap.get(laneInfoKey);
        if (index != null) {
            return index;
        }
        setLaneInfoConsulIndex(laneInfoKey, null, -1L);
        return -1L;
    }

    private void setLaneInfoConsulIndex(LaneRuleKey laneInfoKey, Long lastIndex, Long newIndex) {
        if (isEnable() && isReset) {
            LOG.info("LaneInfoKey: {} is reset.", laneInfoKey);
            laneInfoConsulIndexMap.remove(laneInfoKey);
            isReset = false;
        } else if (isEnable()) {
            LOG.debug("LaneInfoKey: {}; lastIndex: {}; newIndex: {}", laneInfoKey, lastIndex, newIndex);
            laneInfoConsulIndexMap.put(laneInfoKey, newIndex);
        } else {
            LOG.info("LaneInfoKey: {} is disabled.", laneInfoKey);
            laneInfoConsulIndexMap.remove(laneInfoKey);
        }
    }

    private Long getLaneRuleConsulIndex(LaneRuleKey laneRuleKey) {
        Long index = laneRuleConsulIndexMap.get(laneRuleKey);
        if (index != null) {
            return index;
        }
        setLaneRuleConsulIndex(laneRuleKey, null, -1L);
        return -1L;
    }

    private void setLaneRuleConsulIndex(LaneRuleKey laneRuleKey, Long lastIndex, Long newIndex) {
        if (isEnable() && isReset) {
            LOG.info("LaneRuleKey: {} is reset.", laneRuleKey);
            laneRuleConsulIndexMap.remove(laneRuleKey);
            isReset = false;
        } else if (isEnable()) {
            LOG.debug("LaneRuleKey: {}; lastIndex: {}; newIndex: {}", laneRuleKey, lastIndex, newIndex);
            laneRuleConsulIndexMap.put(laneRuleKey, newIndex);
        } else {
            LOG.info("LaneRuleKey: {} is disabled.", laneRuleKey);
            laneRuleConsulIndexMap.remove(laneRuleKey);
        }
    }

    static class LaneRuleKey {
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LaneRuleKey that = (LaneRuleKey) o;
            return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getService(), that.getService());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespace(), getService());
        }

        @Override
        public String toString() {
            return "LaneRuleKey{" +
                    "namespace='" + namespace + '\'' +
                    ", service='" + service + '\'' +
                    '}';
        }
    }

    static class LaneInfoResponse {
        private Long index = -1L;

        private int code = ServerCodes.EXECUTE_SUCCESS;

        private List<LaneInfo> laneInfoList = null;

        public Long getIndex() {
            return index;
        }

        public void setIndex(Long index) {
            this.index = index;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public List<LaneInfo> getLaneInfoList() {
            return laneInfoList;
        }

        public void setLaneInfoList(List<LaneInfo> laneInfoList) {
            this.laneInfoList = laneInfoList;
        }
    }

    static class LaneRuleResponse {
        private Long index = -1L;

        private int code = ServerCodes.EXECUTE_SUCCESS;

        private List<LaneRule> laneRuleList = null;

        public Long getIndex() {
            return index;
        }

        public void setIndex(Long index) {
            this.index = index;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public List<LaneRule> getLaneRuleList() {
            return laneRuleList;
        }

        public void setLaneRuleList(List<LaneRule> laneRuleList) {
            this.laneRuleList = laneRuleList;
        }
    }
}
