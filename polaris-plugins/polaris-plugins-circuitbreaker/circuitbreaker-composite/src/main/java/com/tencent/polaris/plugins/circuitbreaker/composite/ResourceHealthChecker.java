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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.circuitbreaker.composite.utils.MatchUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.Protocol;
import org.slf4j.Logger;

import static com.tencent.polaris.logging.LoggingConsts.LOGGING_HEALTHCHECK_EVENT;

public class ResourceHealthChecker {

	private static final Logger HC_EVENT_LOG = LoggerFactory.getLogger(LOGGING_HEALTHCHECK_EVENT);

	private static final Logger LOG = LoggerFactory.getLogger(ResourceHealthChecker.class);

	private static final Object PLACE_HOLDER_RESOURCE = new Object();

	private static final int DEFAULT_CHECK_INTERVAL = 30;

	private final ScheduledExecutorService checkScheduler;

	private final AtomicBoolean started = new AtomicBoolean(false);

	private final AtomicBoolean stopped = new AtomicBoolean(false);

	private final Map<String, HealthChecker> healthCheckers;

	private final PolarisCircuitBreaker polarisCircuitBreaker;

	private ScheduledFuture<?> future;

	private final FaultDetectRule faultDetectRule;

	private final HealthCheckInstanceProvider healthCheckInstanceProvider;

	private final AtomicLong lastCheckTimeMilli;

	private final Function<String, Pattern> regexToPattern;

	private final Map<Resource, Object> resources = new ConcurrentHashMap<>();

	public ResourceHealthChecker(FaultDetectRule faultDetectRule,
			HealthCheckInstanceProvider healthCheckInstanceProvider, PolarisCircuitBreaker polarisCircuitBreaker) {
		this.checkScheduler = polarisCircuitBreaker.getHealthCheckExecutors();
		this.healthCheckers = polarisCircuitBreaker.getHealthCheckers();
		this.polarisCircuitBreaker = polarisCircuitBreaker;
		this.faultDetectRule = faultDetectRule;
		this.healthCheckInstanceProvider = healthCheckInstanceProvider;
		lastCheckTimeMilli = new AtomicLong(System.currentTimeMillis());
		if (null != polarisCircuitBreaker.getExtensions()) {
			this.regexToPattern = polarisCircuitBreaker.getExtensions().getFlowCache()::loadOrStoreCompiledRegex;
		} else {
			this.regexToPattern = Pattern::compile;
		}
	}

	private Instance createDefaultInstance(String host, int port) {
		DefaultInstance instance = new DefaultInstance();
		instance.setHost(host);
		instance.setPort(port);
		return instance;
	}

	private Runnable createCheckTask() {
		return () -> {
			if (stopped.get()) {
				return;
			}
			FaultDetectRule faultDetectRule = getFaultDetectRule();
			int interval = DEFAULT_CHECK_INTERVAL;
			if (faultDetectRule.getInterval() > 0) {
				interval = faultDetectRule.getInterval();
			}
			if (System.currentTimeMillis() - lastCheckTimeMilli.get() >= interval) {
				try {
					checkResource(faultDetectRule);
				}
				finally {
					lastCheckTimeMilli.set(System.currentTimeMillis());
				}
			}

		};
	}

	private void checkResource(FaultDetectRule faultDetectRule) {
		Map<Node, ResourceHealthChecker.ProtocolInstance> instances = healthCheckInstanceProvider.getInstances();
		if (CollectionUtils.isEmpty(instances) || CollectionUtils.isEmpty(resources)) {
			return;
		}
		int port = faultDetectRule.getPort();
		Protocol protocol = faultDetectRule.getProtocol();
		if (port > 0) {
			Set<String> hosts = new HashSet<>();
			for (Map.Entry<Node, ProtocolInstance> entry : instances.entrySet()) {
				Node instance = entry.getKey();
				if (!hosts.contains(instance.getHost())) {
					hosts.add(instance.getHost());
					boolean success = doCheck(createDefaultInstance(instance.getHost(), port), protocol, faultDetectRule);
					entry.getValue().checkSuccess.set(success);
				}
			}
		}
		else {
			for (Map.Entry<Node, ProtocolInstance> entry : instances.entrySet()) {
				Protocol currentProtocol = entry.getValue().getProtocol();
				if (currentProtocol == Protocol.UNKNOWN || protocol == currentProtocol) {
					InstanceResource instance = entry.getValue().getInstanceResource();
					boolean success = doCheck(
							createDefaultInstance(instance.getHost(), instance.getPort()), protocol, faultDetectRule);
					entry.getValue().checkSuccess.set(success);
				}
			}
		}
	}

	public void start() {
		if (started.compareAndSet(false, true)) {
			Runnable checkTask = createCheckTask();
			FaultDetectRule faultDetectRule = getFaultDetectRule();
			LOG.info("schedule task: protocol {}, interval {}, rule {}", faultDetectRule.getProtocol(),
					faultDetectRule.getInterval(), faultDetectRule.getName());
			this.future = checkScheduler
					.scheduleWithFixedDelay(checkTask, DEFAULT_CHECK_INTERVAL, DEFAULT_CHECK_INTERVAL, TimeUnit.SECONDS);
		}
	}

	private boolean doCheck(Instance instance, Protocol protocol, FaultDetectRule faultDetectRule) {
		HealthChecker healthChecker = healthCheckers.get(protocol.name().toLowerCase());
		if (null == healthChecker) {
			HC_EVENT_LOG.info("plugin not found, skip health check for instance {}:{}, protocol {}",
					instance.getHost(), instance.getPort(), protocol);
			return false;
		}
		DetectResult detectResult = healthChecker.detectInstance(instance, faultDetectRule);
		Set<Resource> copiedResources = new HashSet<>(resources.keySet());
		for (Resource resource : copiedResources) {
			if (!matchRuleToResource(resource)) {
				continue;
			}
			ResourceStat resourceStat = new ResourceStat(resource, detectResult.getStatusCode(), detectResult.getDelay(),
					detectResult.getRetStatus());
			HC_EVENT_LOG
					.info("health check for instance {}:{}, resource {}, protocol {}, result: code {}, delay {}ms, status {}",
							instance.getHost(), instance.getPort(), resource, protocol, detectResult.getStatusCode(),
							detectResult.getDelay(), detectResult.getRetStatus());
			polarisCircuitBreaker.doReport(resourceStat, false);
		}
		return detectResult.getRetStatus() == RetStatus.RetSuccess;
	}

	private boolean matchRuleToResource(Resource resource) {
		if (resource.getLevel() != CircuitBreakerProto.Level.METHOD) {
			return true;
		}
		FaultDetectRule faultDetectRule = getFaultDetectRule();
		return MatchUtils.matchMethod(resource, faultDetectRule.getTargetService().getMethod(), regexToPattern);
	}

	public void stop() {
		LOG.info("health checker for rule {} has stopped", faultDetectRule.getName());
		stopped.set(true);
		if (null != future) {
			future.cancel(true);
		}
	}

	public FaultDetectRule getFaultDetectRule() {
		return faultDetectRule;
	}

	public static class ProtocolInstance {

		final Protocol protocol;

		final InstanceResource instanceResource;

		final AtomicLong lastReportMilli = new AtomicLong(0);

		final AtomicBoolean checkSuccess = new AtomicBoolean(true);

		ProtocolInstance(
				Protocol protocol, InstanceResource instanceResource) {
			this.protocol = protocol;
			this.instanceResource = instanceResource;
			lastReportMilli.set(System.currentTimeMillis());
		}

		Protocol getProtocol() {
			return protocol;
		}

		InstanceResource getInstanceResource() {
			return instanceResource;
		}

		public long getLastReportMilli() {
			return lastReportMilli.get();
		}

		void doReport() {
			lastReportMilli.set(System.currentTimeMillis());
		}

		boolean isCheckSuccess() {
			return checkSuccess.get();
		}
	}

	private boolean matchResource(Resource resource, Function<String, Pattern> regexToPattern) {
		FaultDetectRule faultDetectRule = getFaultDetectRule();
		FaultDetectorProto.FaultDetectRule.DestinationService targetService = faultDetectRule.getTargetService();
		if (!MatchUtils.matchService(resource.getService(), targetService.getNamespace(), targetService.getService())) {
			return false;
		}
		if (resource.getLevel() == CircuitBreakerProto.Level.METHOD) {
			return MatchUtils.matchMethod(resource, targetService.getMethod(), regexToPattern);
		}
		else {
			// only match empty method rules
			return RuleUtils.isMatchAllValue(targetService.getMethod());
		}
	}

	public void addResource(Resource resource) {
		if (matchResource(resource, regexToPattern)) {
			resources.put(resource, PLACE_HOLDER_RESOURCE);
		}
	}

	public void removeResource(Resource resource) {
		resources.remove(resource);
	}

	public Collection<Resource> getResources() {
		return Collections.unmodifiableCollection(resources.keySet());
	}
}
