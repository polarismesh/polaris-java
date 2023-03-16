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

import static com.tencent.polaris.plugins.circuitbreaker.composite.CircuitBreakerRuleContainer.compareService;
import static com.tencent.polaris.plugins.circuitbreaker.composite.CircuitBreakerRuleContainer.compareSingleValue;

import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.SubsetResource;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceResourceProvider;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.DestinationService;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.Protocol;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetector;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public class ResourceHealthChecker {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceHealthChecker.class);

    private static final int DEFAULT_CHECK_INTERVAL = 10;

    private final Resource resource;

    private final FaultDetector faultDetector;

    private final List<FaultDetectRule> rules;

    private final ScheduledExecutorService checkScheduler;

    private final ServiceResourceProvider serviceResourceProvider;

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final Map<Protocol, FaultDetectRule> protocol2Rules = new HashMap<>();

    private final Map<String, HealthChecker> healthCheckers;

    private final PolarisCircuitBreaker polarisCircuitBreaker;

    private int interval;

    private Runnable checkTask;

    public ResourceHealthChecker(Resource resource, FaultDetector faultDetector,
            PolarisCircuitBreaker polarisCircuitBreaker) {
        this.resource = resource;
        this.faultDetector = faultDetector;
        this.rules = sortFaultDetectRules(faultDetector.getRulesList());
        this.checkScheduler = polarisCircuitBreaker.getHealthCheckExecutors();
        this.serviceResourceProvider = polarisCircuitBreaker.getServiceRuleProvider();
        this.healthCheckers = polarisCircuitBreaker.getHealthCheckers();
        this.polarisCircuitBreaker = polarisCircuitBreaker;
        start();
    }

    // 优先匹配非通配规则，再匹配通配规则
    private static List<FaultDetectRule> sortFaultDetectRules(List<FaultDetectRule> rules) {
        List<FaultDetectRule> outRules = new ArrayList<>(rules);
        outRules.sort(new Comparator<FaultDetectRule>() {
            @Override
            public int compare(FaultDetectRule rule1, FaultDetectRule rule2) {
                // 1. compare destination service
                DestinationService targetService1 = rule1.getTargetService();
                String destNamespace1 = targetService1.getNamespace();
                String destService1 = targetService1.getService();
                String destMethod1 = targetService1.getMethod().getValue().getValue();

                DestinationService targetService2 = rule2.getTargetService();
                String destNamespace2 = targetService2.getNamespace();
                String destService2 = targetService2.getService();
                String destMethod2 = targetService2.getMethod().getValue().getValue();

                int svcResult = compareService(destNamespace1, destService1, destNamespace2, destService2);
                if (svcResult != 0) {
                    return svcResult;
                }
                return compareSingleValue(destMethod1, destMethod2);
            }
        });
        return outRules;
    }

    private void start() {
        String methodName = "";
        if (resource instanceof MethodResource) {
            methodName = ((MethodResource) resource).getMethod();
        }
        for (FaultDetectRule rule : rules) {
            if (!matchMethodRule(methodName, rule)) {
                continue;
            }
            protocol2Rules.put(rule.getProtocol(), rule);
            interval = rule.getInterval();
        }
        if (interval <= 0) {
            interval = DEFAULT_CHECK_INTERVAL;
        }
        checkTask = new Runnable() {
            @Override
            public void run() {
                if (stopped.get()) {
                    return;
                }
                ServiceKey service = resource.getService();
                ServiceInstances serviceInstances;
                try {
                    serviceInstances = serviceResourceProvider.getServiceInstances(service);
                } catch (Throwable t) {
                    LOG.warn("fail to get service for {}", service, t);
                    checkScheduler.schedule(checkTask, 5, TimeUnit.SECONDS);
                    return;
                }
                if (resource instanceof InstanceResource) {
                    InstanceResource instanceResource = (InstanceResource) resource;
                    Instance instance = serviceInstances
                            .getInstance(new Node(instanceResource.getHost(), instanceResource.getPort()));
                    if (null == instance) {
                        checkScheduler.schedule(checkTask, 5, TimeUnit.SECONDS);
                        return;
                    }
                    checkInstance(instance);
                    checkScheduler.schedule(checkTask, interval, TimeUnit.SECONDS);
                } else if (resource instanceof SubsetResource) {
                    SubsetResource subsetResource = (SubsetResource) resource;
                    if (CollectionUtils.isEmpty(subsetResource.getMetadata())) {
                        return;
                    }
                    for (Instance instance : serviceInstances.getInstances()) {
                        if (!instance.isHealthy() || instance.isIsolated()) {
                            continue;
                        }
                        if (!RuleUtils.matchMetadata(subsetResource.getMetadata(), instance.getMetadata())) {
                            continue;
                        }
                        checkInstance(instance);
                    }
                    checkScheduler.schedule(checkTask, interval, TimeUnit.SECONDS);
                } else {
                    // check all active instance
                    for (Instance instance : serviceInstances.getInstances()) {
                        if (!instance.isHealthy() || instance.isIsolated()) {
                            continue;
                        }
                        checkInstance(instance);
                    }
                    checkScheduler.schedule(checkTask, interval, TimeUnit.SECONDS);
                }

            }
        };
        checkScheduler.schedule(checkTask, 1, TimeUnit.SECONDS);
    }


    private boolean matchMethodRule(String methodName, FaultDetectRule rule) {
        if (StringUtils.isBlank(methodName)) {
            return true;
        }
        MatchString methodMatcher = rule.getTargetService().getMethod();
        if (null == methodMatcher) {
            return false;
        }
        return RuleUtils.matchStringValue(methodMatcher, methodName, new Function<String, Pattern>() {
            @Override
            public Pattern apply(String s) {
                return Pattern.compile(s);
            }
        });
    }

    private void checkInstance(Instance instance) {
        String protocol = StringUtils.defaultString(instance.getProtocol()).toLowerCase();
        DetectResult detectResult;
        if (protocol.equals("http") || protocol.equals("tcp/http")) {
            // handle with http
            detectResult = doCheck(instance, Protocol.HTTP);
        } else if (protocol.startsWith("udp")) {
            // handle with udp
            detectResult = doCheck(instance, Protocol.UDP);
        } else {
            // handle with tcp
            detectResult = doCheck(instance, Protocol.TCP);
        }
        if (null == detectResult) {
            LOG.info("[FAULT DETECT] instance id={} host={} port={} detect result is null",
                    instance.getId(), instance.getHost(), instance.getPort());
            return;
        }
        ResourceStat resourceStat = new ResourceStat(resource, 0, 0, detectResult.getRetStatus());
        polarisCircuitBreaker.report(resourceStat);
    }

    private DetectResult doCheck(Instance instance, Protocol protocol) {
        FaultDetectRule faultDetectRule = protocol2Rules.get(protocol);
        if (null == faultDetectRule) {
            return null;
        }
        HealthChecker healthChecker = healthCheckers.get(protocol.name().toLowerCase());
        if (null == healthChecker) {
            return null;
        }
        return healthChecker.detectInstance(instance, faultDetectRule);
    }

    public void setStopped(boolean value) {
        stopped.set(value);
    }

    public FaultDetector getFaultDetector() {
        return faultDetector;
    }
}
