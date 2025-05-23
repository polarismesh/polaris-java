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

package com.tencent.polaris.plugins.connector.consul.service.ratelimiting;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Duration;
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
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import com.tencent.polaris.plugins.connector.consul.service.ConsulService;
import com.tencent.polaris.plugins.connector.consul.service.common.TagCondition;
import com.tencent.polaris.plugins.connector.consul.service.common.TagConstant;
import com.tencent.polaris.plugins.connector.consul.service.common.TagRule;
import com.tencent.polaris.plugins.connector.consul.service.ratelimiting.entity.Rule;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;
import static com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter.*;
import static com.tencent.polaris.plugins.connector.consul.service.common.TagConditionUtil.parseMatchStringType;

/**
 * @author Haotian Zhang
 */
public class RateLimitingService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitingService.class);

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final Map<RateLimitingKey, Long> rateLimitingConsulIndexMap = new ConcurrentHashMap<>();

    public RateLimitingService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                               String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        String rateLimitRuleKey = String.format("ratelimit/%s/%s/data", consulContext.getNamespace(), consulContext.getServiceName());
        RateLimitingKey rateLimitingKey = new RateLimitingKey();
        rateLimitingKey.setNamespace(consulContext.getNamespace());
        rateLimitingKey.setService(consulContext.getServiceName());
        Long currentIndex = getRateLimitingConsulIndex(rateLimitingKey);
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        int code = ServerCodes.DATA_NO_CHANGE;
        try {
            LOG.debug("Begin get rate limit rules of {} sync", rateLimitingKey);
            Response<GetValue> response = consulClient.getKVValue(rateLimitRuleKey, consulContext.getAclToken(), queryParams);
            if (response != null) {
                if (LOG.isDebugEnabled()) {
                    String responseStr = "Response{" +
                            "value='" + response.getValue() + '\'' +
                            ", consulIndex=" + response.getConsulIndex() + '\'' +
                            ", consulKnownLeader=" + response.isConsulKnownLeader() + '\'' +
                            ", consulLastContact=" + response.getConsulLastContact() +
                            '}';
                    LOG.debug("tsf ratelimit rule, consul kv namespace, response: {}", responseStr);
                }

                Long newIndex = response.getConsulIndex();
                // create service.
                ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
                newServiceBuilder.setNamespace(StringValue.of(consulContext.getNamespace()));
                newServiceBuilder.setName(StringValue.of(consulContext.getServiceName()));
                newServiceBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));
                // create rateLimit.
                RateLimitProto.RateLimit.Builder newRateLimitBuilder = RateLimitProto.RateLimit.newBuilder();
                newRateLimitBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));
                // create discover response.
                ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
                newDiscoverResponseBuilder.setService(newServiceBuilder);
                // 重写index
                List<RateLimitProto.Rule> ruleList = new ArrayList<>();
                if (Objects.nonNull(newIndex)) {
                    if (!Objects.equals(currentIndex, newIndex)) {
                        code = ServerCodes.EXECUTE_SUCCESS;
                        GetValue getValue = response.getValue();
                        if (Objects.nonNull(getValue)) {
                            String decodedValue = getValue.getDecodedValue();
                            LOG.info("[TSF Ratelimit] New consul config: {}", decodedValue);
                            if (!StringUtils.isEmpty(decodedValue)) {
                                ruleList = parseResponse(decodedValue, consulContext.getNamespace(), consulContext.getServiceName(), newIndex);
                            }
                        } else {
                            LOG.info("empty rate limit rule: {}", response);
                        }
                    } else {
                        LOG.debug("[TSF Ratelimit] Consul data is not changed");
                    }
                } else {
                    LOG.warn("[TSF Ratelimit] Consul data is abnormal. {}", response);
                }
                if (CollectionUtils.isNotEmpty(ruleList)) {
                    newRateLimitBuilder.addAllRules(ruleList);
                }
                newDiscoverResponseBuilder.setRateLimit(newRateLimitBuilder);
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                if (newIndex != null) {
                    setRateLimitingConsulIndex(rateLimitingKey, currentIndex, newIndex);
                }
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        } catch (Throwable e) {
            LOG.error("[TSF Ratelimit] tsf rate limit rule load error. Will sleep for {} ms. Key path:{}",
                    consulContext.getConsulErrorSleep(), rateLimitRuleKey, e);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get rate limiting sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        }
    }

    private List<RateLimitProto.Rule> parseResponse(String decodedValue, String namespace, String service, Long index) {
        List<RateLimitProto.Rule> ruleList = Lists.newArrayList();

        // yaml -> Map
        Yaml yaml = new Yaml();
        Map config = null;
        try {
            config = yaml.load(decodedValue);
        } catch (Exception ex) {
            LOG.error("tsf rate limit rule load error.", ex);
            throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf rate limit rule load error.", ex);
        }

        // Map -> List<RateLimitProto.Rule>
        if (config != null) {
            List<Map<String, Object>> rulesConfig = (List<Map<String, Object>>) config.get("rules");
            for (Map<String, Object> ruleConfig : rulesConfig) {
                RateLimitProto.Rule.Builder ruleBuilder = RateLimitProto.Rule.newBuilder();
                String id = (String) ruleConfig.get("id");
                ruleBuilder.setId(StringValue.of(id));
                ruleBuilder.setNamespace(StringValue.of(namespace));
                ruleBuilder.setService(StringValue.of(service));
                ruleBuilder.setName(StringValue.of((String) ruleConfig.get("name")));
                ruleBuilder.setRevision(StringValue.of(String.valueOf(index)));

                // 限流模式，默认是 CLUSTER
                String modeStr = (String) ruleConfig.get("limitMode");
                Rule.Mode mode = Rule.Mode.getModeByStr(modeStr);
                ruleBuilder.setType(parseType(modeStr));

                // 限流阈值，默认是 QPS
                String conditionModel = (String) ruleConfig.get("conditionModel");
                Rule.Condition condition = Rule.Condition.getConditionByStr(conditionModel);
                ruleBuilder.setAction(StringValue.of(parseAction(conditionModel, modeStr)));

                // 如果限流阈值是 QPS，获取周期和阈值
                Integer duration = (Integer) ruleConfig.get("duration");
                Integer totalQuota = (Integer) ruleConfig.get("quota");
                if (duration != null && totalQuota != null) {
                    RateLimitProto.Amount.Builder amountBuilder = RateLimitProto.Amount.newBuilder();
                    amountBuilder.setMaxAmount(UInt32Value.of(totalQuota));
                    amountBuilder.setValidDuration(Duration.newBuilder().setSeconds(duration));
                    ruleBuilder.addAmounts(amountBuilder);
                }

                // 如果限流阈值是 THREAD，获取信号量的容量
                Integer concurrentThreads = (Integer) ruleConfig.get("concurrentThreads");
                if (concurrentThreads != null) {
                    RateLimitProto.ConcurrencyAmount.Builder concurrencyAmountBuilder = RateLimitProto.ConcurrencyAmount.newBuilder();
                    concurrencyAmountBuilder.setMaxAmount(concurrentThreads);
                    ruleBuilder.setConcurrencyAmount(concurrencyAmountBuilder);
                }

                // 限流返回自定义信息
                String limitedResponse = null;
                String encodeStr = (String) ruleConfig.get("limitedResponse");
                if (StringUtils.isNotEmpty(encodeStr)) {
                    byte[] decodeBytes = Base64.getDecoder().decode(encodeStr);
                    limitedResponse = new String(decodeBytes, StandardCharsets.UTF_8);
                    if (StringUtils.isNotEmpty(limitedResponse)) {
                        RateLimitProto.CustomResponse.Builder customResponseBuilder = RateLimitProto.CustomResponse.newBuilder();
                        customResponseBuilder.setBody(limitedResponse);
                        ruleBuilder.setCustomResponse(customResponseBuilder);
                    }
                }

                // tag，新版本的基于标签的限流，如果没有 tag，则是服务整体限流
                List<Map<String, String>> tag = (List<Map<String, String>>) ruleConfig.get("conditions");
                Rule.Type type = Rule.Type.GLOBAL;
                TagRule tagRule = null;
                if (tag != null) {
                    List<RateLimitProto.MatchArgument> list = new ArrayList<>();
                    type = Rule.Type.TAG_CONDITION;
                    tagRule = new TagRule();
                    List<TagCondition> tagConditionList = new ArrayList<>();
                    for (Map<String, String> cond : tag) {
                        // build TagCondition
                        TagCondition tagCondition = new TagCondition();
                        tagCondition.setTagField(cond.get("tagField"));
                        tagCondition.setTagType(cond.get("tagType"));
                        tagCondition.setTagOperator(cond.get("tagOperator"));
                        tagCondition.setTagValue(cond.get("tagValue"));
                        tagConditionList.add(tagCondition);

                        // build MatchArgument
                        RateLimitProto.MatchArgument.Builder matchArgumentBuilder = RateLimitProto.MatchArgument.newBuilder();
                        if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.SOURCE_SERVICE_NAME)) {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.CALLER_SERVICE);
                            matchArgumentBuilder.setKey("*");
                        } else if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.SOURCE_NAMESPACE_SERVICE_NAME)) {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.CALLER_SERVICE);
                            matchArgumentBuilder.setKey("*");
                            String[] tagValues = cond.get("tagValue").split(",");
                            StringBuilder serviceNameStringBuilder = new StringBuilder();
                            for (String tagValue : tagValues) {
                                if (StringUtils.isNotEmpty(tagValue)) {
                                    String[] split = tagValue.split("/");
                                    if (split.length == 2) {
                                        serviceNameStringBuilder.append(split[1]).append(",");
                                    } else {
                                        serviceNameStringBuilder.append(tagValue).append(",");
                                    }
                                }
                            }
                            String serviceNameString = serviceNameStringBuilder.toString();
                            if (serviceNameString.endsWith(",")) {
                                serviceNameString = serviceNameString.substring(0, serviceNameString.length() - 1);
                            }
                            cond.put("tagValue", serviceNameString);
                        } else if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.SOURCE_APPLICATION_ID)) {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.CALLER_METADATA);
                            matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_APPLICATION_ID);
                        } else if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.SOURCE_APPLICATION_VERSION)) {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.CALLER_METADATA);
                            matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_PROG_VERSION);
                        } else if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.SOURCE_GROUP_ID)) {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.CALLER_METADATA);
                            matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_GROUP_ID);
                        } else if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.SOURCE_CONNECTION_IP)) {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.CALLER_IP);
                            matchArgumentBuilder.setKey(MessageMetadataContainer.LABEL_KEY_CALLER_IP);
                        } else if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.DESTINATION_APPLICATION_VERSION)) {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.CUSTOM);
                            matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_APPLICATION_ID);
                        } else if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.DESTINATION_GROUP_ID)) {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.CUSTOM);
                            matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_GROUP_ID);
                        } else if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.DESTINATION_INTERFACE)) {
                            ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                            matchStringBuilder.setType(parseMatchStringType(cond.get("tagOperator")));
                            matchStringBuilder.setValue(StringValue.of(cond.get("tagValue")));
                            ruleBuilder.setMethod(matchStringBuilder);
                            continue;
                        } else if (StringUtils.equals(cond.get("tagField"), TagConstant.SYSTEM_FIELD.REQUEST_HTTP_METHOD)) {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.METHOD);
                            matchArgumentBuilder.setKey(MessageMetadataContainer.LABEL_KEY_METHOD);
                        } else {
                            matchArgumentBuilder.setType(RateLimitProto.MatchArgument.Type.CUSTOM);
                            matchArgumentBuilder.setKey(cond.get("tagField"));
                        }
                        ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                        matchStringBuilder.setType(parseMatchStringType(cond.get("tagOperator")));
                        matchStringBuilder.setValue(StringValue.of(cond.get("tagValue")));
                        matchArgumentBuilder.setValue(matchStringBuilder);
                        list.add(matchArgumentBuilder.build());
                    }
                    tagRule.setConditions(tagConditionList);
                    tagRule.setConditionExpression((String) ruleConfig.get("conditionExpression"));
                    ruleBuilder.addAllArguments(list);
                }

                ruleBuilder.setDisable(BoolValue.of(false));
                ruleBuilder.setRegexCombine(BoolValue.of(true));

                ruleBuilder.putMetadata("limiter", "tsf");

                Rule newRule = new Rule(id, duration, totalQuota, totalQuota, mode, limitedResponse, condition,
                        concurrentThreads, type, tagRule);
                String originalRule = gson.toJson(newRule);
                ruleBuilder.putMetadata("original", originalRule);

                ruleList.add(ruleBuilder.build());
            }
        }
        return ruleList;
    }

    private RateLimitProto.Rule.Type parseType(String modeStr) {
        Rule.Mode mode = Rule.Mode.getModeByStr(modeStr);
        switch (mode) {
            case STANDALONE:
                return RateLimitProto.Rule.Type.LOCAL;
            case CLUSTER:
            default:
                return RateLimitProto.Rule.Type.GLOBAL;
        }
    }

    private String parseAction(String conditionModel, String modeStr) {
        Rule.Condition condition = Rule.Condition.getConditionByStr(conditionModel);
        Rule.Mode mode = Rule.Mode.getModeByStr(modeStr);
        switch (condition) {
            case THREAD:
                return LIMITER_CONCURRENCY;
            case QPS:
                if (mode.equals(Rule.Mode.CLUSTER)) {
                    return LIMITER_TSF;
                } else {
                    return LIMITER_REJECT;
                }
            default:
                return LIMITER_REJECT;
        }
    }

    private Long getRateLimitingConsulIndex(RateLimitingKey rateLimitingKey) {
        Long index = rateLimitingConsulIndexMap.get(rateLimitingKey);
        if (index != null) {
            return index;
        }
        setRateLimitingConsulIndex(rateLimitingKey, null, -1L);
        return -1L;
    }

    private void setRateLimitingConsulIndex(RateLimitingKey rateLimitingKey, Long lastIndex, Long newIndex) {
        if (isEnable() && isReset) {
            LOG.info("RateLimitingKey: {} is reset.", rateLimitingKey);
            rateLimitingConsulIndexMap.remove(rateLimitingKey);
            isReset = false;
        } else if (isEnable()) {
            LOG.debug("RateLimitingKey: {}; lastIndex: {}; newIndex: {}", rateLimitingKey, lastIndex, newIndex);
            rateLimitingConsulIndexMap.put(rateLimitingKey, newIndex);
        } else {
            LOG.info("RateLimitingKey: {} is disabled.", rateLimitingKey);
            rateLimitingConsulIndexMap.remove(rateLimitingKey);
        }
    }

    static class RateLimitingKey {
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
            RateLimitingKey that = (RateLimitingKey) object;
            return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getService(), that.getService()) && Objects.equals(getFetchGroup(), that.getFetchGroup());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespace(), getService(), getFetchGroup());
        }

        @Override
        public String toString() {
            return "RateLimitingKey{" +
                    "namespace='" + namespace + '\'' +
                    ", serviceName='" + service + '\'' +
                    ", fetchGroup=" + fetchGroup +
                    '}';
        }
    }
}
