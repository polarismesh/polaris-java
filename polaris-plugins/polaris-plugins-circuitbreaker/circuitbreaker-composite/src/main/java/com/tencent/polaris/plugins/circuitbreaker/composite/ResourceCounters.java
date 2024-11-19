/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.circuitbreaker.composite;

import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.event.FlowEvent;
import com.tencent.polaris.api.plugin.event.FlowEventConstants;
import com.tencent.polaris.api.plugin.stat.DefaultCircuitBreakResult;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.FallbackInfo;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.Status;
import com.tencent.polaris.api.utils.TrieUtil;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.circuitbreaker.composite.trigger.ConsecutiveCounter;
import com.tencent.polaris.plugins.circuitbreaker.composite.trigger.CounterOptions;
import com.tencent.polaris.plugins.circuitbreaker.composite.trigger.ErrRateCounter;
import com.tencent.polaris.plugins.circuitbreaker.composite.trigger.TriggerCounter;
import com.tencent.polaris.plugins.circuitbreaker.composite.utils.CircuitBreakerUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.*;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.tencent.polaris.api.plugin.cache.CacheConstants.API_ID;
import static com.tencent.polaris.api.plugin.event.tsf.TsfEventDataConstants.*;
import static com.tencent.polaris.logging.LoggingConsts.LOGGING_CIRCUIT_BREAKER;

/**
 * resource counter corresponding to resource, has expire_duration:
 * - for consecutive counter: no expire
 * - for error rate counter: 2 * (interval + sleepWindow)
 */
public class ResourceCounters implements StatusChangeHandler {

    private static final Logger CB_LOG = LoggerFactory.getLogger(LOGGING_CIRCUIT_BREAKER);

    private static final Logger LOG = LoggerFactory.getLogger(ResourceCounters.class);

    private final CircuitBreakerProto.CircuitBreakerRule currentActiveRule;

    private final List<TriggerCounter> counters = new ArrayList<>();

    private final Resource resource;

    private final ScheduledExecutorService stateChangeExecutors;

    private final AtomicReference<CircuitBreakerStatus> circuitBreakerStatusReference = new AtomicReference<>();

    private final FallbackInfo fallbackInfo;

    private final Function<String, Pattern> regexFunction;

    private final Function<String, TrieNode<String>> trieNodeFunction;

    private Extensions extensions;

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private final CircuitBreakerConfig circuitBreakerConfig;

    private AtomicBoolean reloadFaultDetect = new AtomicBoolean(false);

    public ResourceCounters(Resource resource, CircuitBreakerRule currentActiveRule,
                            ScheduledExecutorService stateChangeExecutors, PolarisCircuitBreaker polarisCircuitBreaker) {
        this.currentActiveRule = currentActiveRule;
        this.resource = resource;
        this.stateChangeExecutors = stateChangeExecutors;
        this.regexFunction = regex -> {
            if (null == polarisCircuitBreaker.getExtensions()) {
                return Pattern.compile(regex);
            }
            FlowCache flowCache = polarisCircuitBreaker.getExtensions().getFlowCache();
            return flowCache.loadOrStoreCompiledRegex(regex);
        };
        this.trieNodeFunction = key -> {
            if (null == polarisCircuitBreaker.getExtensions()) {
                return null;
            }
            FlowCache flowCache = polarisCircuitBreaker.getExtensions().getFlowCache();
            return flowCache.loadPluginCacheObject(API_ID, key, path -> TrieUtil.buildSimpleApiTrieNode((String) path));
        };
        circuitBreakerStatusReference
                .set(new CircuitBreakerStatus(currentActiveRule.getName(), Status.CLOSE, System.currentTimeMillis()));
        CB_LOG.info("circuit breaker is created, current status {}, resource {}, rule {}",
                circuitBreakerStatusReference.get().getStatus(), resource, currentActiveRule.getName());
        fallbackInfo = buildFallbackInfo(currentActiveRule);
        extensions = polarisCircuitBreaker.getExtensions();
        circuitBreakerConfig = polarisCircuitBreaker.getCircuitBreakerConfig();
        init();
    }

    private void init() {
        List<CircuitBreakerProto.BlockConfig> blockConfigList = currentActiveRule.getBlockConfigsList();
        for (CircuitBreakerProto.BlockConfig blockConfig : blockConfigList) {
            List<TriggerCondition> triggerConditionList = blockConfig.getTriggerConditionsList();
            List<ErrorCondition> errorConditionList = blockConfig.getErrorConditionsList();
            for (TriggerCondition triggerCondition : triggerConditionList) {
                CounterOptions counterOptions = new CounterOptions();
                counterOptions.setResource(resource);
                if (resource instanceof MethodResource) {
                    counterOptions.setApi(blockConfig.getApi());
                }
                counterOptions.setErrorConditionList(errorConditionList);
                counterOptions.setTriggerCondition(triggerCondition);
                counterOptions.setStatusChangeHandler(this);
                counterOptions.setExecutorService(stateChangeExecutors);
                counterOptions.setRegexFunction(regexFunction);
                counterOptions.setTrieNodeFunction(trieNodeFunction);
                String ruleName = currentActiveRule.getName();
                if (StringUtils.isNotBlank(blockConfig.getName())) {
                    ruleName = ruleName + "#" + blockConfig.getName();
                }
                switch (triggerCondition.getTriggerType()) {
                    case ERROR_RATE:
                        counters.add(new ErrRateCounter(ruleName, counterOptions));
                        break;
                    case CONSECUTIVE_ERROR:
                        counters.add(new ConsecutiveCounter(ruleName, counterOptions));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static FallbackInfo buildFallbackInfo(CircuitBreakerRule currentActiveRule) {
        if (null == currentActiveRule) {
            return null;
        }
        if (currentActiveRule.getLevel() != Level.METHOD && currentActiveRule.getLevel() != Level.SERVICE) {
            return null;
        }
        FallbackConfig fallbackConfig = currentActiveRule.getFallbackConfig();
        if (!fallbackConfig.getEnable()) {
            return null;
        }
        FallbackResponse response = fallbackConfig.getResponse();
        Map<String, String> headers = new HashMap<>();
        for (FallbackResponse.MessageHeader messageHeader : response.getHeadersList()) {
            headers.put(messageHeader.getKey(), messageHeader.getValue());
        }
        return new FallbackInfo(response.getCode(), headers, response.getBody());
    }

    public CircuitBreakerRule getCurrentActiveRule() {
        return currentActiveRule;
    }

    @Override
    public void closeToOpen(String circuitBreaker, String reason) {
        synchronized (this) {
            if (destroyed.get()) {
                LOG.info("counters {} for resource {} is destroyed, closeToOpen skipped", currentActiveRule.getName(), resource);
                return;
            }
            CircuitBreakerStatus circuitBreakerStatus = circuitBreakerStatusReference.get();
            if (circuitBreakerStatus.getStatus() == Status.CLOSE) {
                toOpen(circuitBreakerStatus, circuitBreaker, reason);
            }
        }
    }

    private void toOpen(CircuitBreakerStatus preStatus, String circuitBreaker, String reason) {
        CircuitBreakerStatus newStatus = new CircuitBreakerStatus(circuitBreaker, Status.OPEN,
                System.currentTimeMillis(), fallbackInfo);
        circuitBreakerStatusReference.set(newStatus);
        CB_LOG.info("previous status {}, current status {}, resource {}, rule {}", preStatus.getStatus(),
                newStatus.getStatus(), resource, circuitBreaker);
        reportCircuitStatus();
        reportEvent(preStatus.getStatus(), newStatus.getStatus(), circuitBreaker, reason);
        long sleepWindow = CircuitBreakerUtils.getSleepWindowMilli(currentActiveRule, circuitBreakerConfig);
        // add callback after timeout
        stateChangeExecutors.schedule(new Runnable() {
            @Override
            public void run() {
                openToHalfOpen();
            }
        }, sleepWindow, TimeUnit.MILLISECONDS);
    }

    @Override
    public void openToHalfOpen() {
        synchronized (this) {
            if (destroyed.get()) {
                LOG.info("counters {} for resource {} is destroyed, openToHalfOpen skipped", currentActiveRule.getName(), resource);
                return;
            }
            CircuitBreakerStatus circuitBreakerStatus = circuitBreakerStatusReference.get();
            if (circuitBreakerStatus.getStatus() != Status.OPEN) {
                return;
            }
            int consecutiveSuccess = currentActiveRule.getRecoverCondition().getConsecutiveSuccess();
            HalfOpenStatus halfOpenStatus = new HalfOpenStatus(
                    circuitBreakerStatus.getCircuitBreaker(), System.currentTimeMillis(), consecutiveSuccess);
            CB_LOG.info("previous status {}, current status {}, resource {}, rule {}",
                    circuitBreakerStatus.getStatus(),
                    halfOpenStatus.getStatus(), resource, circuitBreakerStatus.getCircuitBreaker());
            circuitBreakerStatusReference.set(halfOpenStatus);
            reportEvent(circuitBreakerStatus.getStatus(), halfOpenStatus.getStatus(), circuitBreakerStatus.getCircuitBreaker());
            reportCircuitStatus();
        }
    }

    @Override
    public void halfOpenToClose() {
        synchronized (this) {
            if (destroyed.get()) {
                LOG.info("counters {} for resource {} is destroyed, halfOpenToClose skipped", currentActiveRule.getName(), resource);
                return;
            }
            CircuitBreakerStatus circuitBreakerStatus = circuitBreakerStatusReference.get();
            if (circuitBreakerStatus.getStatus() == Status.HALF_OPEN) {
                CircuitBreakerStatus newStatus = new CircuitBreakerStatus(circuitBreakerStatus.getCircuitBreaker(),
                        Status.CLOSE, System.currentTimeMillis());
                circuitBreakerStatusReference.set(newStatus);
                CB_LOG.info("previous status {}, current status {}, resource {}, rule {}",
                        circuitBreakerStatus.getStatus(),
                        newStatus.getStatus(), resource, circuitBreakerStatus.getCircuitBreaker());
                for (TriggerCounter triggerCounter : counters) {
                    triggerCounter.resume();
                }
                reportEvent(circuitBreakerStatus.getStatus(), newStatus.getStatus(), circuitBreakerStatus.getCircuitBreaker());
                reportCircuitStatus();
            }
        }
    }

    @Override
    public void halfOpenToOpen() {
        synchronized (this) {
            if (destroyed.get()) {
                LOG.info("counters {} for resource {} is destroyed, halfOpenToOpen skipped", currentActiveRule.getName(), resource);
                return;
            }
            CircuitBreakerStatus circuitBreakerStatus = circuitBreakerStatusReference.get();
            if (circuitBreakerStatus.getStatus() == Status.HALF_OPEN) {
                toOpen(circuitBreakerStatus, circuitBreakerStatus.getCircuitBreaker(), "");
            }
        }
    }

    private List<ErrorCondition> getErrorConditions() {
        List<ErrorCondition> errorConditionList = new ArrayList<>();
        for (BlockConfig blockConfig : currentActiveRule.getBlockConfigsList()) {
            errorConditionList.addAll(blockConfig.getErrorConditionsList());
        }
        return errorConditionList;
    }

    public void report(ResourceStat resourceStat) {
        CircuitBreakerStatus circuitBreakerStatus = circuitBreakerStatusReference.get();
        LOG.debug("[CircuitBreaker] report resource stat {}", resourceStat);
        if (null != circuitBreakerStatus && circuitBreakerStatus.getStatus() == Status.HALF_OPEN) {
            List<ErrorCondition> errorConditions = getErrorConditions();
            RetStatus retStatus = CircuitBreakerUtils.parseRetStatus(resourceStat, errorConditions, regexFunction);
            boolean success = retStatus != RetStatus.RetFail && retStatus != RetStatus.RetTimeout;
            HalfOpenStatus halfOpenStatus = (HalfOpenStatus) circuitBreakerStatus;
            boolean checked = halfOpenStatus.report(success);
            LOG.debug("[CircuitBreaker] report resource halfOpen stat {}, checked {}", resourceStat.getResource(),
                    checked);
            if (checked) {
                Status nextStatus = halfOpenStatus.calNextStatus();
                switch (nextStatus) {
                    case CLOSE:
                        stateChangeExecutors.execute(new Runnable() {
                            @Override
                            public void run() {
                                halfOpenToClose();
                            }
                        });
                        break;
                    case OPEN:
                        stateChangeExecutors.execute(new Runnable() {
                            @Override
                            public void run() {
                                halfOpenToOpen();
                            }
                        });
                        break;
                    default:
                        break;
                }
            }
        } else {
            LOG.debug("[CircuitBreaker] report resource stat to counter {}", resourceStat.getResource());
            for (TriggerCounter counter : counters) {
                counter.report(resourceStat, regexFunction);
            }
        }
    }

    public CircuitBreakerStatus getCircuitBreakerStatus() {
        return circuitBreakerStatusReference.get();
    }

    private void reportEvent(CircuitBreakerStatus.Status previousStatus, CircuitBreakerStatus.Status currentStatus, String ruleName) {
        reportEvent(previousStatus, currentStatus, ruleName, null);
    }

    private void reportEvent(CircuitBreakerStatus.Status previousStatus, CircuitBreakerStatus.Status currentStatus,
                             String ruleName, String reason) {
        if (extensions == null) {
            return;
        }
        FlowEvent.Builder flowEventBuilder = new FlowEvent.Builder()
                .withEventType(ServiceEventKey.EventType.CIRCUIT_BREAKING)
                .withTimestamp(Instant.now())
                .withClientId(extensions.getValueContext().getClientId())
                .withClientIp(extensions.getValueContext().getHost())
                .withNamespace(resource.getService().getNamespace())
                .withService(resource.getService().getService())
                .withSourceNamespace(resource.getCallerService().getNamespace())
                .withSourceService(resource.getService().getService())
                .withCurrentStatus(CircuitBreakerUtils.parseFlowEventStatus(currentStatus))
                .withPreviousStatus(CircuitBreakerUtils.parseFlowEventStatus(previousStatus))
                .withRuleName(ruleName);
        if (StringUtils.isNotBlank(reason)) {
            flowEventBuilder.withReason(reason);
        }
        String isolationObject = "";
        switch (resource.getLevel()) {
            case SERVICE:
                flowEventBuilder = flowEventBuilder.withResourceType(FlowEventConstants.ResourceType.SERVICE);
                isolationObject = CircuitBreakerUtils.getServiceCircuitBreakerName(
                        resource.getService().getNamespace(), resource.getService().getService());
                break;
            case METHOD:
                MethodResource methodResource = (MethodResource) resource;
                flowEventBuilder = flowEventBuilder.withResourceType(FlowEventConstants.ResourceType.METHOD)
                        .withApiProtocol(methodResource.getProtocol())
                        .withApiPath(methodResource.getPath())
                        .withApiMethod(methodResource.getMethod());
                isolationObject = CircuitBreakerUtils.getApiCircuitBreakerName(
                        methodResource.getService().getNamespace(), methodResource.getService().getService(),
                        methodResource.getPath(), methodResource.getMethod());
                break;
            case INSTANCE:
                InstanceResource instanceResource = (InstanceResource) resource;
                flowEventBuilder = flowEventBuilder.withResourceType(FlowEventConstants.ResourceType.INSTANCE)
                        .withHost(instanceResource.getHost())
                        .withPort(instanceResource.getPort());
                isolationObject = CircuitBreakerUtils.getInstanceCircuitBreakerName(
                        instanceResource.getHost(), instanceResource.getPort());
                break;
        }

        FlowEvent flowEvent = flowEventBuilder.build();

        String failureRate = "";
        String slowCallRate = "";
        if (CollectionUtils.isNotEmpty(currentActiveRule.getBlockConfigsList())) {
            for (BlockConfig blockConfig : currentActiveRule.getBlockConfigsList()) {
                if (CollectionUtils.isNotEmpty(blockConfig.getTriggerConditionsList())) {
                    if (StringUtils.equals(blockConfig.getName(), "failure")) {
                        failureRate = String.valueOf(blockConfig.getTriggerConditions(0).getErrorPercent());
                    } else if (StringUtils.equals(blockConfig.getName(), "slow")) {
                        slowCallRate = String.valueOf(blockConfig.getTriggerConditions(0).getErrorPercent());
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(isolationObject)) {
            flowEvent.getAdditionalParams().put(ISOLATION_OBJECT_KEY, isolationObject);
        }
        if (StringUtils.isNotBlank(failureRate)) {
            flowEvent.getAdditionalParams().put(FAILURE_RATE_KEY, failureRate);
        }
        if (StringUtils.isNotBlank(slowCallRate)) {
            flowEvent.getAdditionalParams().put(SLOW_CALL_DURATION_KEY, slowCallRate);
        }
        BaseFlow.reportFlowEvent(extensions, flowEvent);
    }

    public void reportCircuitStatus() {
        if (Objects.isNull(extensions)) {
            return;
        }
        Collection<Plugin> statPlugins = extensions.getPlugins().getPlugins(PluginTypes.STAT_REPORTER.getBaseType());
        if (null != statPlugins) {
            try {
                for (Plugin statPlugin : statPlugins) {
                    if (statPlugin instanceof StatReporter) {
                        DefaultCircuitBreakResult result = new DefaultCircuitBreakResult();
                        result.setCallerService(resource.getCallerService());
                        result.setCircuitBreakStatus(getCircuitBreakerStatus());
                        result.setService(resource.getService().getService());
                        result.setNamespace(resource.getService().getNamespace());
                        result.setLevel(resource.getLevel().name());
                        result.setRuleName(currentActiveRule.getName());
                        switch (resource.getLevel()) {
                            case SERVICE:
                                break;
                            case METHOD:
                                MethodResource methodResource = (MethodResource) resource;
                                result.setMethod(methodResource.getMethod());
                                break;
                            case INSTANCE:
                                InstanceResource instanceResource = (InstanceResource) resource;
                                result.setHost(instanceResource.getHost());
                                result.setPort(instanceResource.getPort());
                                break;
                        }

                        StatInfo info = new StatInfo();
                        info.setCircuitBreakGauge(result);
                        ((StatReporter) statPlugin).reportStat(info);
                    }
                }
            } catch (Exception ex) {
                LOG.info("circuit breaker report encountered exception, e: {}", ex.getMessage());
            }
        }
    }

    public void setReloadFaultDetect(boolean param) {
        reloadFaultDetect.set(param);
    }

    public boolean checkReloadFaultDetect() {
        return reloadFaultDetect.compareAndSet(true, false);
    }

    public void setDestroyed(boolean value) {
        destroyed.set(value);
        toDestroy();
    }

    private void toDestroy() {
        synchronized (this) {
            CircuitBreakerStatus circuitBreakerStatus = circuitBreakerStatusReference.get();
            circuitBreakerStatus.setDestroy(true);
            circuitBreakerStatusReference.set(circuitBreakerStatus);
            CB_LOG.info("previous status {}, current status {}, resource {}, rule {}",
                    circuitBreakerStatus.getStatus(),
                    Status.DESTROY, resource, circuitBreakerStatus.getCircuitBreaker());
            for (TriggerCounter triggerCounter : counters) {
                triggerCounter.resume();
            }
            reportEvent(circuitBreakerStatus.getStatus(), Status.DESTROY, circuitBreakerStatus.getCircuitBreaker());
            reportCircuitStatus();
        }
    }
}
