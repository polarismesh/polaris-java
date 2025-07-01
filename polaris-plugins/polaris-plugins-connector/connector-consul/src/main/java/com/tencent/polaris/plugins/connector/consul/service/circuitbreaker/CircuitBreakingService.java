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

package com.tencent.polaris.plugins.connector.consul.service.circuitbreaker;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
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
import com.tencent.polaris.plugins.connector.consul.service.circuitbreaker.entity.CircuitBreakerApi;
import com.tencent.polaris.plugins.connector.consul.service.circuitbreaker.entity.CircuitBreakerRule;
import com.tencent.polaris.plugins.connector.consul.service.circuitbreaker.entity.CircuitBreakerStrategy;
import com.tencent.polaris.plugins.connector.consul.service.circuitbreaker.entity.TsfCircuitBreakerIsolationLevelEnum;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;

/**
 * @author Haotian Zhang
 */
public class CircuitBreakingService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakingService.class);

    private final Map<CircuitBreakingKey, CircuitBreakingValue> circuitBreakingConsulIndexMap = new ConcurrentHashMap<>();

    public CircuitBreakingService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                                  String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
        String service = serviceUpdateTask.getServiceEventKey().getService();
        String circuitBreakingRuleKey = String.format("circuitbreaker/%s/%s/%s/%s/data",
                consulContext.getNamespace(), consulContext.getServiceName(), namespace, service);
        CircuitBreakingKey circuitBreakingKey = new CircuitBreakingKey();
        circuitBreakingKey.setNamespace(namespace);
        circuitBreakingKey.setService(service);
        Long currentIndex = getCircuitBreakingConsulIndex(circuitBreakingKey);
        CircuitBreakingValue currentCircuitBreakingValue = getCircuitBreakingValue(circuitBreakingKey);
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        int code = ServerCodes.DATA_NO_CHANGE;
        try {
            LOG.debug("Begin get circuit break rules of {} sync", circuitBreakingKey);
            Response<List<GetValue>> response = consulClient.getKVValues(circuitBreakingRuleKey, consulContext.getAclToken(), queryParams);
            if (response != null) {
                if (LOG.isDebugEnabled()) {
                    String responseStr = "Response{" +
                            "value='" + response.getValue() + '\'' +
                            ", consulIndex=" + response.getConsulIndex() + '\'' +
                            ", consulKnownLeader=" + response.isConsulKnownLeader() + '\'' +
                            ", consulLastContact=" + response.getConsulLastContact() +
                            '}';
                    LOG.debug("tsf circuit breaker rule, consul kv namespace, response: {}", responseStr);
                }

                Long newIndex = response.getConsulIndex();
                boolean is404 = false;
                // create service.
                ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
                newServiceBuilder.setNamespace(StringValue.of(namespace));
                newServiceBuilder.setName(StringValue.of(service));
                newServiceBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));
                // create circuit breaker.
                CircuitBreakerProto.CircuitBreaker.Builder newCircuitBreakerBuilder = CircuitBreakerProto.CircuitBreaker.newBuilder();
                newCircuitBreakerBuilder.setNamespace(StringValue.of(namespace));
                newCircuitBreakerBuilder.setService(StringValue.of(service));
                newCircuitBreakerBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));
                // create discover response.
                ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
                newDiscoverResponseBuilder.setService(newServiceBuilder);
                // 重写index
                List<CircuitBreakerProto.CircuitBreakerRule> circuitBreakerRuleList = new ArrayList<>();
                if (Objects.nonNull(newIndex)) {
                    if (!Objects.equals(currentIndex, newIndex)) {
                        code = ServerCodes.EXECUTE_SUCCESS;
                        List<GetValue> getValues = response.getValue();
                        if (!CollectionUtils.isEmpty(getValues)) {
                            // 目前只有一个
                            GetValue circuitBreakerConsulValue = getValues.get(0);
                            if (circuitBreakerConsulValue != null) {
                                String decodedValue = circuitBreakerConsulValue.getDecodedValue();
                                LOG.info("[TSF CIRCUIT BREAKER LOADER] tsf circuit breaker rule, consul kv service, value: {}", decodedValue);
                                if (!StringUtils.isEmpty(decodedValue)) {
                                    circuitBreakerRuleList = parseResponse(decodedValue, namespace, service);
                                }
                            }
                        } else {
                            if (currentCircuitBreakingValue != null && currentCircuitBreakingValue.isLast404) {
                                code = ServerCodes.DATA_NO_CHANGE;
                            }
                            is404 = true;
                            LOG.info("empty circuit breaker rule: {}", response);
                        }
                    } else {
                        if (currentCircuitBreakingValue != null && currentCircuitBreakingValue.isLast404) {
                            is404 = true;
                        }
                        LOG.debug("[TSF CIRCUIT BREAKER] Consul data is not changed");
                    }
                } else {
                    LOG.warn("[TSF CIRCUIT BREAKER] Consul data is abnormal. {}", response);
                }
                if (CollectionUtils.isNotEmpty(circuitBreakerRuleList)) {
                    newCircuitBreakerBuilder.addAllRules(circuitBreakerRuleList);
                }
                newDiscoverResponseBuilder.setCircuitBreaker(newCircuitBreakerBuilder);
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                if (newIndex != null) {
                    setCircuitBreakingConsulIndex(circuitBreakingKey, currentIndex, newIndex, is404);
                }
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        } catch (Throwable e) {
            LOG.error("[TSF CIRCUIT BREAKER LOADER ERROR] tsf circuit breaker rule load error. Will sleep for {} ms. Key path:{}",
                    consulContext.getConsulErrorSleep(), circuitBreakingRuleKey, e);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get circuit breaking sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        }
    }

    private List<CircuitBreakerProto.CircuitBreakerRule> parseResponse(String decodedValue, String namespace, String service) {
        List<CircuitBreakerProto.CircuitBreakerRule> circuitBreakerRuleList = Lists.newArrayList();

        // yaml -> json -> CircuitBreakerRule
        Yaml yaml = new Yaml();
        CircuitBreakerRule circuitBreakerRule;
        try {
            String circuitBreakerJsonString = mapper.writeValueAsString(yaml.load(decodedValue));
            circuitBreakerRule = mapper.readValue(circuitBreakerJsonString, new TypeReference<CircuitBreakerRule>() {
            });
        } catch (Exception ex) {
            LOG.error("tsf circuit breaker rule load error.", ex);
            throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf circuit breaker rule load error.", ex);
        }

        // CircuitBreakerRule -> List<CircuitBreakerProto.CircuitBreakerRule>
        if (circuitBreakerRule != null) {
            for (CircuitBreakerStrategy circuitBreakerStrategy : circuitBreakerRule.getStrategyList()) {
                CircuitBreakerProto.CircuitBreakerRule.Builder circuitBreakerRuleBuilder = CircuitBreakerProto.CircuitBreakerRule.newBuilder();

                // set name
                if (StringUtils.isNotBlank(circuitBreakerRule.getRuleId())) {
                    circuitBreakerRuleBuilder.setName(circuitBreakerRule.getRuleId());
                }
                // set enable
                circuitBreakerRuleBuilder.setEnable(true);
                // set level
                circuitBreakerRuleBuilder.setLevel(parseLevel(circuitBreakerRule.getIsolationLevel()));
                // set maxEjectionPercent
                circuitBreakerRuleBuilder.setMaxEjectionPercent(circuitBreakerStrategy.getMaxEjectionPercent());

                // build ruleMatcher
                CircuitBreakerProto.RuleMatcher.Builder ruleMatcher = CircuitBreakerProto.RuleMatcher.newBuilder();
                CircuitBreakerProto.RuleMatcher.SourceService.Builder sourceServiceBuilder = CircuitBreakerProto.RuleMatcher.SourceService.newBuilder();
                sourceServiceBuilder.setNamespace(consulContext.getNamespace());
                sourceServiceBuilder.setService(consulContext.getServiceName());
                ruleMatcher.setSource(sourceServiceBuilder);
                CircuitBreakerProto.RuleMatcher.DestinationService.Builder destinationServiceBuilder = CircuitBreakerProto.RuleMatcher.DestinationService.newBuilder();
                destinationServiceBuilder.setNamespace(namespace);
                destinationServiceBuilder.setService(service);
                ruleMatcher.setDestination(destinationServiceBuilder);
                circuitBreakerRuleBuilder.setRuleMatcher(ruleMatcher);

                // build blockConfigs
                List<CircuitBreakerProto.BlockConfig> blockConfigList = Lists.newArrayList();
                if (CollectionUtils.isNotEmpty(circuitBreakerStrategy.getApiList())) {
                    for (CircuitBreakerApi circuitBreakerApi : circuitBreakerStrategy.getApiList()) {
                        // build api
                        ModelProto.API.Builder apiBuilder = ModelProto.API.newBuilder();
                        apiBuilder.setProtocol("*");
                        apiBuilder.setMethod(circuitBreakerApi.getMethod());
                        ModelProto.MatchString.Builder pathMatchStringBuilder = ModelProto.MatchString.newBuilder();
                        pathMatchStringBuilder.setType(ModelProto.MatchString.MatchStringType.EXACT);
                        pathMatchStringBuilder.setValue(StringValue.of(circuitBreakerApi.getPath()));
                        apiBuilder.setPath(pathMatchStringBuilder);

                        parseAndAddBlockConfig(blockConfigList, circuitBreakerStrategy, apiBuilder.build());
                    }
                } else {
                    parseAndAddBlockConfig(blockConfigList, circuitBreakerStrategy, null);
                }
                circuitBreakerRuleBuilder.addAllBlockConfigs(blockConfigList);

                // build recoverCondition
                CircuitBreakerProto.RecoverCondition.Builder recoverConditionBuilder = CircuitBreakerProto.RecoverCondition.newBuilder();
                recoverConditionBuilder.setSleepWindow(circuitBreakerStrategy.getWaitDurationInOpenState());
                // default 3
                recoverConditionBuilder.setConsecutiveSuccess(3);
                circuitBreakerRuleBuilder.setRecoverCondition(recoverConditionBuilder);

                circuitBreakerRuleList.add(circuitBreakerRuleBuilder.build());
            }
        }
        return circuitBreakerRuleList;
    }

    private Long getCircuitBreakingConsulIndex(CircuitBreakingKey circuitBreakingKey) {
        CircuitBreakingValue circuitBreakingValue = circuitBreakingConsulIndexMap.get(circuitBreakingKey);
        if (circuitBreakingValue != null && circuitBreakingValue.getIndex() != null) {
            return circuitBreakingValue.getIndex();
        }
        setCircuitBreakingConsulIndex(circuitBreakingKey, null, -1L, false);
        return -1L;
    }

    private CircuitBreakingValue getCircuitBreakingValue(CircuitBreakingKey circuitBreakingKey) {
        return circuitBreakingConsulIndexMap.get(circuitBreakingKey);
    }

    private void setCircuitBreakingConsulIndex(CircuitBreakingKey circuitBreakingKey, Long lastIndex, Long newIndex, Boolean is404) {
        if (isEnable() && isReset) {
            LOG.info("CircuitBreakingKey: {} is reset.", circuitBreakingKey);
            circuitBreakingConsulIndexMap.remove(circuitBreakingKey);
            isReset = false;
        } else if (isEnable()) {
            LOG.debug("CircuitBreakingKey: {}; lastIndex: {}; newIndex: {}, is404: {}", circuitBreakingKey, lastIndex, newIndex, is404);
            circuitBreakingConsulIndexMap.put(circuitBreakingKey, new CircuitBreakingValue(newIndex, is404));
        } else {
            LOG.info("CircuitBreakingKey: {} is disabled.", circuitBreakingKey);
            circuitBreakingConsulIndexMap.remove(circuitBreakingKey);
        }
    }

    static class CircuitBreakingKey {
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
            CircuitBreakingKey that = (CircuitBreakingKey) object;
            return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getService(), that.getService()) && Objects.equals(getFetchGroup(), that.getFetchGroup());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespace(), getService(), getFetchGroup());
        }

        @Override
        public String toString() {
            return "CircuitBreakingKey{" +
                    "namespace='" + namespace + '\'' +
                    ", serviceName='" + service + '\'' +
                    ", fetchGroup=" + fetchGroup +
                    '}';
        }
    }

    static class CircuitBreakingValue {
        private Long index;
        private Boolean isLast404 = false;

        public CircuitBreakingValue(Long index, Boolean isLast404) {
            this.index = index;
            this.isLast404 = isLast404;
        }

        public Long getIndex() {
            return index;
        }

        public void setIndex(Long index) {
            this.index = index;
        }

        public Boolean getLast404() {
            return isLast404;
        }

        public void setLast404(Boolean last404) {
            isLast404 = last404;
        }

        @Override
        public String toString() {
            return "CircuitBreakingValue{" +
                    "index=" + index +
                    ", isLast404=" + isLast404 +
                    '}';
        }
    }

    private CircuitBreakerProto.Level parseLevel(TsfCircuitBreakerIsolationLevelEnum isolationLevel) {
        switch (isolationLevel) {
            case INSTANCE:
                return CircuitBreakerProto.Level.INSTANCE;
            case API:
                return CircuitBreakerProto.Level.METHOD;
            case SERVICE:
            default:
                return CircuitBreakerProto.Level.SERVICE;
        }
    }

    private void parseAndAddBlockConfig(List<CircuitBreakerProto.BlockConfig> blockConfigList,
                                        CircuitBreakerStrategy circuitBreakerStrategy, ModelProto.API api) {
        // build failure block config
        CircuitBreakerProto.BlockConfig.Builder failureBlockConfigBuilder = CircuitBreakerProto.BlockConfig.newBuilder();
        failureBlockConfigBuilder.setName("failure");
        if (api != null) {
            failureBlockConfigBuilder.setApi(api);
        }
        CircuitBreakerProto.ErrorCondition.Builder errorConditionBuilder = CircuitBreakerProto.ErrorCondition.newBuilder();
        errorConditionBuilder.setInputType(CircuitBreakerProto.ErrorCondition.InputType.RET_CODE);
        ModelProto.MatchString.Builder codeMatchStringBuilder = ModelProto.MatchString.newBuilder();
        codeMatchStringBuilder.setType(ModelProto.MatchString.MatchStringType.IN);
        String statusCodes = IntStream.range(400, 600)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
        codeMatchStringBuilder.setValue(StringValue.of(statusCodes));
        errorConditionBuilder.setCondition(codeMatchStringBuilder);
        failureBlockConfigBuilder.addErrorConditions(errorConditionBuilder);
        CircuitBreakerProto.TriggerCondition.Builder codeTriggerConditionBuilder = CircuitBreakerProto.TriggerCondition.newBuilder();
        codeTriggerConditionBuilder.setTriggerType(CircuitBreakerProto.TriggerCondition.TriggerType.ERROR_RATE);
        codeTriggerConditionBuilder.setErrorPercent(circuitBreakerStrategy.getFailureRateThreshold());
        codeTriggerConditionBuilder.setInterval(circuitBreakerStrategy.getSlidingWindowSize());
        codeTriggerConditionBuilder.setMinimumRequest(circuitBreakerStrategy.getMinimumNumberOfCalls());
        failureBlockConfigBuilder.addTriggerConditions(codeTriggerConditionBuilder);
        blockConfigList.add(failureBlockConfigBuilder.build());

        // build slow error block config
        CircuitBreakerProto.BlockConfig.Builder slowBlockConfigBuilder = CircuitBreakerProto.BlockConfig.newBuilder();
        slowBlockConfigBuilder.setName("slow");
        if (api != null) {
            slowBlockConfigBuilder.setApi(api);
        }
        CircuitBreakerProto.ErrorCondition.Builder delayConditionBuilder = CircuitBreakerProto.ErrorCondition.newBuilder();
        delayConditionBuilder.setInputType(CircuitBreakerProto.ErrorCondition.InputType.DELAY);
        ModelProto.MatchString.Builder delayMatchStringBuilder = ModelProto.MatchString.newBuilder();
        delayMatchStringBuilder.setType(ModelProto.MatchString.MatchStringType.EXACT);
        delayMatchStringBuilder.setValue(StringValue.of(String.valueOf(circuitBreakerStrategy.getSlowCallDurationThreshold())));
        delayConditionBuilder.setCondition(delayMatchStringBuilder);
        slowBlockConfigBuilder.addErrorConditions(delayConditionBuilder);
        CircuitBreakerProto.TriggerCondition.Builder delayTriggerConditionBuilder = CircuitBreakerProto.TriggerCondition.newBuilder();
        delayTriggerConditionBuilder.setTriggerType(CircuitBreakerProto.TriggerCondition.TriggerType.ERROR_RATE);
        delayTriggerConditionBuilder.setErrorPercent(circuitBreakerStrategy.getSlowCallRateThreshold());
        delayTriggerConditionBuilder.setInterval(circuitBreakerStrategy.getSlidingWindowSize());
        delayTriggerConditionBuilder.setMinimumRequest(circuitBreakerStrategy.getMinimumNumberOfCalls());
        slowBlockConfigBuilder.addTriggerConditions(delayTriggerConditionBuilder);
        blockConfigList.add(slowBlockConfigBuilder.build());
    }
}
