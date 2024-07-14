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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.circuitbreaker.composite.utils.HealthCheckUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto;
import org.slf4j.Logger;

import static com.tencent.polaris.logging.LoggingConsts.LOGGING_HEALTHCHECK_EVENT;

public class HealthCheckContainer implements HealthCheckInstanceProvider {

	private static final Logger HC_EVENT_LOG = LoggerFactory.getLogger(LOGGING_HEALTHCHECK_EVENT);

	private static final Logger LOG = LoggerFactory.getLogger(HealthCheckContainer.class);

	private final ServiceKey serviceKey;

	private final Object updateLock = new Object();

	// key is ruleId, value is checker
	private final Map<String, ResourceHealthChecker> healthCheckers = new ConcurrentHashMap<>();

	private final Map<Node, ResourceHealthChecker.ProtocolInstance> instances = new ConcurrentHashMap<>();

	private final long expireIntervalMilli;

	public HealthCheckContainer(ServiceKey serviceKey,
			List<FaultDetectorProto.FaultDetectRule> faultDetectRules, PolarisCircuitBreaker polarisCircuitBreaker) {
		long checkPeriod = polarisCircuitBreaker.getCheckPeriod();
		expireIntervalMilli = polarisCircuitBreaker.getHealthCheckInstanceExpireInterval();
		this.serviceKey = serviceKey;
		LOG.info("schedule expire task: service {}, interval {}", serviceKey, checkPeriod);
		polarisCircuitBreaker.getHealthCheckExecutors().scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				cleanInstances();
			}
		}, checkPeriod, checkPeriod, TimeUnit.MILLISECONDS);
		if (CollectionUtils.isNotEmpty(faultDetectRules)) {
			for (FaultDetectorProto.FaultDetectRule faultDetectRule : faultDetectRules) {
				ResourceHealthChecker resourceHealthChecker = new ResourceHealthChecker(faultDetectRule, this, polarisCircuitBreaker);
				resourceHealthChecker.start();
				healthCheckers.put(faultDetectRule.getId(), resourceHealthChecker);
			}
		}
	}

	public void addInstance(InstanceResource instanceResource) {
		ResourceHealthChecker.ProtocolInstance instance = instances.computeIfAbsent(instanceResource.getNode(), new Function<Node, ResourceHealthChecker.ProtocolInstance>() {
			@Override
			public ResourceHealthChecker.ProtocolInstance apply(Node node) {
				HC_EVENT_LOG.info("add fault detect instance {}, service {}", instanceResource.getNode(), serviceKey);
				return new ResourceHealthChecker.ProtocolInstance(HealthCheckUtils.parseProtocol(instanceResource.getProtocol()),
						instanceResource);
			}
		});
		instance.doReport();
	}

	@Override
	public Map<Node, ResourceHealthChecker.ProtocolInstance> getInstances() {
		return Collections.unmodifiableMap(instances);
	}

	public Collection<ResourceHealthChecker> getHealthCheckerValues() {
		return Collections.unmodifiableCollection(healthCheckers.values());
	}

	public void updateFaultDetectRule() {
		synchronized (updateLock) {
			for (ResourceHealthChecker resourceHealthChecker : healthCheckers.values()) {
				resourceHealthChecker.stop();
			}
			healthCheckers.clear();
		}
	}

	public void cleanInstances() {
		long curTimeMilli = System.currentTimeMillis();
		for (Map.Entry<Node, ResourceHealthChecker.ProtocolInstance> entry : instances.entrySet()) {
			ResourceHealthChecker.ProtocolInstance protocolInstance = entry.getValue();
			long lastReportMilli = protocolInstance.getLastReportMilli();
			Node node = entry.getKey();
			if (!protocolInstance.isCheckSuccess() && curTimeMilli - lastReportMilli >= expireIntervalMilli) {
				instances.remove(node);
				HC_EVENT_LOG
						.info("clean instance from health check tasks, service {}, expired node {}, lastReportMilli {}",
								serviceKey, node, lastReportMilli);
			}
		}
	}

	public void addResource(Resource resource) {
		synchronized (updateLock) {
			for (ResourceHealthChecker resourceHealthChecker : getHealthCheckerValues()) {
				resourceHealthChecker.addResource(resource);
			}
		}
	}

	public void removeResource(Resource resource) {
		synchronized (updateLock) {
			for (ResourceHealthChecker resourceHealthChecker : getHealthCheckerValues()) {
				resourceHealthChecker.removeResource(resource);
			}
		}
	}
}
