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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceResourceProvider;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.client.flow.DefaultServiceResourceProvider;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.circuitbreaker.composite.utils.CircuitBreakerUtils;
import com.tencent.polaris.plugins.circuitbreaker.composite.utils.HealthCheckUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import org.slf4j.Logger;

public class PolarisCircuitBreaker extends Destroyable implements CircuitBreaker {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisCircuitBreaker.class);

	private final Map<Level, Cache<Resource, Optional<ResourceCounters>>> countersCache = new HashMap<>();

	private final Map<ServiceKey, HealthCheckContainer> healthCheckCache = new ConcurrentHashMap<>();

	private final ScheduledExecutorService stateChangeExecutors = new ScheduledThreadPoolExecutor(1,
			new NamedThreadFactory("circuitbreaker-state-worker"));

	private final ScheduledExecutorService healthCheckExecutors = new ScheduledThreadPoolExecutor(4,
			new NamedThreadFactory("circuitbreaker-health-check-worker"));

	private final ScheduledExecutorService expiredCleanupExecutors = new ScheduledThreadPoolExecutor(1,
			new NamedThreadFactory("circuitbreaker-expired-cleanup-worker"));

	// map the wildcard resource to rule specific resource,
	// eg. /path/wildcard/123 => /path/wildcard/.+
	private final Map<Resource, ResourceWrap> resourceMapping = new ConcurrentHashMap<>();

	private Extensions extensions;

	private ServiceResourceProvider serviceResourceProvider;

	private Map<String, HealthChecker> healthCheckers = Collections.emptyMap();

	private long healthCheckInstanceExpireInterval;

	private long checkPeriod;

	private long resourceExpireInterval;

	private CircuitBreakerRuleDictionary circuitBreakerRuleDictionary;

	private FaultDetectRuleDictionary faultDetectRuleDictionary;

	private CircuitBreakerConfig circuitBreakerConfig;

	@Override
	public CircuitBreakerStatus checkResource(Resource resource) {
		Resource ruleResource = getActualResource(resource, false);
		Optional<ResourceCounters> resourceCounters = getResourceCounters(ruleResource);
		if (null == resourceCounters) {
			if (resource.getLevel() == Level.METHOD && Objects.equals(ruleResource, resource)) {
				// 可能是被淘汰了，需要重新计算RuleResource
				CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
				ruleResource = computeResourceByRule(resource, circuitBreakerRule);
				if (!Objects.equals(ruleResource, resource)) {
					// 这里不能放缓存，需要在report的时候统一放，否则会有探测规则无法关联resource的问题
					resourceCounters = getResourceCounters(ruleResource);
				}
			}
		}
		if (null == resourceCounters || !resourceCounters.isPresent()) {
			return null;
		}
		return resourceCounters.get().getCircuitBreakerStatus();
	}

	private Optional<ResourceCounters> getResourceCounters(Resource resource) {
		Cache<Resource, Optional<ResourceCounters>> resourceOptionalCache = countersCache.get(resource.getLevel());
		return resourceOptionalCache.getIfPresent(resource);
	}

	@Override
	public void report(ResourceStat resourceStat) {
		doReport(resourceStat, true);
	}

	public Resource getActualResource(Resource resource, boolean internal) {
		ResourceWrap resourceWrap = resourceMapping.get(resource);
		if (null == resourceWrap) {
			return resource;
		}
		if (!internal) {
			resourceWrap.lastAccessTimeMilli = System.currentTimeMillis();
		}
		return resourceWrap.resource;
	}

	private ResourceCounters getOrInitResourceCounters(Resource resource) throws ExecutionException {
		Resource ruleResource = getActualResource(resource, false);
		Optional<ResourceCounters> resourceCounters = getResourceCounters(ruleResource);
		boolean reloadFaultDetect = false;
		if (null == resourceCounters) {
			synchronized (countersCache) {
				resourceCounters = getResourceCounters(ruleResource);
				if (null == resourceCounters) {
					resourceCounters = initResourceCounter(resource);
					reloadFaultDetect = true;
				}
			}
		}
		if (!reloadFaultDetect) {
			if (null != resourceCounters && resourceCounters.isPresent() && resourceCounters.get().checkReloadFaultDetect()) {
				reloadFaultDetect = true;
			}
		}
		if (reloadFaultDetect) {
			reloadFaultDetector(resource, resourceCounters.orElse(null));
		}
		return resourceCounters.orElse(null);
	}

	void doReport(ResourceStat resourceStat, boolean isNormalRequest) {
		Resource resource = resourceStat.getResource();
		if (!CircuitBreakerUtils.checkLevel(resource.getLevel())) {
			return;
		}
		RetStatus retStatus = resourceStat.getRetStatus();
		if (retStatus == RetStatus.RetReject || retStatus == RetStatus.RetFlowControl) {
			return;
		}
		try {
			ResourceCounters resourceCounters = getOrInitResourceCounters(resource);
			if (null != resourceCounters) {
				resourceCounters.report(resourceStat);
			}
			if (isNormalRequest) {
				addInstanceForFaultDetect(resourceStat.getResource());
			}
		}
		catch (Throwable t) {
			LOG.warn("error occur when report stat with {}", resource);
		}
	}

	private void reloadFaultDetector(Resource resource, ResourceCounters resourceCounters) {
		boolean removeResource = false;
		if (null == resourceCounters) {
			removeResource = true;
		}
		else {
			CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule = resourceCounters.getCurrentActiveRule();
			if (!circuitBreakerRule.hasFaultDetectConfig() || !circuitBreakerRule.getFaultDetectConfig().getEnable()) {
				removeResource = true;
			}
		}

		HealthCheckContainer healthCheckContainer = healthCheckCache.get(resource.getService());
		if (removeResource) {
			if (null == healthCheckContainer) {
				return;
			}
			healthCheckContainer.removeResource(resource);
		}
		else {
			if (null == healthCheckContainer) {
				List<FaultDetectorProto.FaultDetectRule> faultDetectRules = faultDetectRuleDictionary.lookupFaultDetectRule(resource);
				if (CollectionUtils.isNotEmpty(faultDetectRules)) {
					healthCheckContainer = healthCheckCache.computeIfAbsent(resource.getService(), new Function<ServiceKey, HealthCheckContainer>() {
						@Override
						public HealthCheckContainer apply(ServiceKey serviceKey) {
							LOG.info("[CIRCUIT_BREAKER] init health check cache for service {}", serviceKey);
							return new HealthCheckContainer(serviceKey, faultDetectRules, PolarisCircuitBreaker.this);
						}
					});
				}
			}
			if (null != healthCheckContainer) {
				healthCheckContainer.addResource(resource);
			}
		}
	}

	private Optional<ResourceCounters> initResourceCounter(Resource resource) throws ExecutionException {
		CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		if (null == circuitBreakerRule) {
			// pull cb rule
			ServiceEventKey cbEventKey = new ServiceEventKey(resource.getService(),
					ServiceEventKey.EventType.CIRCUIT_BREAKING);
			ServiceRule cbSvcRule;
			try {
				cbSvcRule = getServiceRuleProvider().getServiceRule(cbEventKey);
			}
			catch (Throwable t) {
				LOG.warn("fail to get circuitBreaker rule resource for {}", cbEventKey, t);
				throw t;
			}

			// pull fd rule
			ServiceEventKey fdEventKey = new ServiceEventKey(resource.getService(),
					ServiceEventKey.EventType.FAULT_DETECTING);
			ServiceRule faultDetectRule;
			try {
				faultDetectRule = getServiceRuleProvider().getServiceRule(fdEventKey);
			}
			catch (Throwable t) {
				LOG.warn("fail to get faultDetect rule for {}", fdEventKey, t);
				throw t;
			}

			circuitBreakerRuleDictionary.putServiceRule(resource.getService(), cbSvcRule);
			faultDetectRuleDictionary.putServiceRule(resource.getService(), faultDetectRule);

			// 查询适合的熔断规则
			circuitBreakerRule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		}
		Cache<Resource, Optional<ResourceCounters>> resourceOptionalCache = countersCache.get(resource.getLevel());
		CircuitBreakerProto.CircuitBreakerRule finalCircuitBreakerRule = circuitBreakerRule;
		Resource ruleResource = computeResourceByRule(resource, circuitBreakerRule);
		if (!Objects.equals(ruleResource, resource)) {
			resourceMapping.put(resource, new ResourceWrap(ruleResource, System.currentTimeMillis()));
		}
		return resourceOptionalCache.get(ruleResource, new Callable<Optional<ResourceCounters>>() {
			@Override
			public Optional<ResourceCounters> call() {
				if (null == finalCircuitBreakerRule) {
					return Optional.empty();
				}
				return Optional.of(new ResourceCounters(ruleResource, finalCircuitBreakerRule,
						getStateChangeExecutors(), PolarisCircuitBreaker.this));
			}
		});
	}

	private Resource computeResourceByRule(Resource resource, CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule) {
		if (null == circuitBreakerRule || resource.getLevel() != Level.METHOD) {
			return resource;
		}
		ModelProto.MatchString method = circuitBreakerRule.getRuleMatcher().getDestination().getMethod();
		if (method.getType() == ModelProto.MatchString.MatchStringType.EXACT && !RuleUtils.isMatchAllValue(method.getValue().getValue())) {
			return resource;
		}
		//new path = matchPath + ":" + matchType
		String newPath = method.getValue().getValue() + ":" + method.getType().name();
		MethodResource originalResource = (MethodResource) resource;
		return new MethodResource(originalResource.getService(), newPath, originalResource.getCallerService());
		
	}

	private void addInstanceForFaultDetect(Resource resource) {
		if (!(resource instanceof InstanceResource)) {
			return;
		}
		InstanceResource instanceResource = (InstanceResource) resource;
		HealthCheckContainer healthCheckContainer = healthCheckCache
				.get(instanceResource.getService());
		if (null == healthCheckContainer || instanceResource.getPort() == 0) {
			return;
		}
		healthCheckContainer.addInstance(instanceResource);
	}

	@Override
	public PluginType getType() {
		return PluginTypes.CIRCUIT_BREAKER.getBaseType();
	}

	private static class CounterRemoveListener implements RemovalListener<Resource, Optional<ResourceCounters>> {

		@Override
		public void onRemoval(RemovalNotification<Resource, Optional<ResourceCounters>> removalNotification) {
			Optional<ResourceCounters> value = removalNotification.getValue();
			if (null == value) {
				return;
			}
			value.ifPresent(resourceCounters -> resourceCounters.setDestroyed(true));
		}
	}

	@Override
	public void init(InitContext ctx) throws PolarisException {
		resourceExpireInterval = ctx.getConfig().getConsumer().getCircuitBreaker().getCountersExpireInterval();
		countersCache.put(Level.SERVICE, CacheBuilder.newBuilder().removalListener(new CounterRemoveListener()).build());
		countersCache.put(Level.METHOD, CacheBuilder.newBuilder().removalListener(new CounterRemoveListener()).build());
		countersCache.put(Level.INSTANCE, CacheBuilder.newBuilder().removalListener(new CounterRemoveListener()).build());
		checkPeriod = ctx.getConfig().getConsumer().getCircuitBreaker().getCheckPeriod();
		circuitBreakerConfig = ctx.getConfig().getConsumer().getCircuitBreaker();
		healthCheckInstanceExpireInterval = HealthCheckUtils.CHECK_PERIOD_MULTIPLE * checkPeriod;
	}

	@Override
	public void postContextInit(Extensions extensions) throws PolarisException {
		this.extensions = extensions;
		circuitBreakerRuleDictionary = new CircuitBreakerRuleDictionary(extensions.getFlowCache()::loadOrStoreCompiledRegex);
		faultDetectRuleDictionary = new FaultDetectRuleDictionary();
		serviceResourceProvider = new DefaultServiceResourceProvider(extensions);
		extensions.getLocalRegistry().registerResourceListener(new CircuitBreakerRuleListener(this));
		healthCheckers = extensions.getAllHealthCheckers();
		long expireIntervalMilli = extensions.getConfiguration().getConsumer().getCircuitBreaker()
				.getCountersExpireInterval();
		long cleanupIntervalMilli = Math.max(expireIntervalMilli, CircuitBreakerUtils.MIN_CLEANUP_INTERVAL);
		expiredCleanupExecutors.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				cleanupExpiredResources();
			}
		}, cleanupIntervalMilli, cleanupIntervalMilli, TimeUnit.MILLISECONDS);
	}

	public void cleanupExpiredResources() {
		LOG.info("[CIRCUIT_BREAKER] cleanup expire resources");
		for (Map.Entry<Resource, ResourceWrap> entry : resourceMapping.entrySet()) {
			Resource resource = entry.getKey();
			if (System.currentTimeMillis() - entry.getValue().lastAccessTimeMilli >= resourceExpireInterval) {
				LOG.info("[CIRCUIT_BREAKER] resource {} expired, start to cleanup", resource);
				resourceMapping.remove(resource);
				HealthCheckContainer healthCheckContainer = healthCheckCache.get(resource.getService());
				if (null == healthCheckContainer) {
					continue;
				}
				healthCheckContainer.removeResource(resource);
			}
		}
		for (Map.Entry<Level, Cache<Resource, Optional<ResourceCounters>>> entry : countersCache.entrySet()) {
			Cache<Resource, Optional<ResourceCounters>> values = entry.getValue();
			values.asMap().forEach(new BiConsumer<Resource, Optional<ResourceCounters>>() {
				@Override
				public void accept(Resource resource, Optional<ResourceCounters> resourceCounters) {
					// 每隔一段时间清理占位的缓存数据，避免没规则的情况下，counters无法收敛
					if (!resourceCounters.isPresent()) {
						values.invalidate(resource);
					}
				}
			});
			values.cleanUp();
		}
	}

	// for test
	public void setServiceRuleProvider(ServiceResourceProvider serviceResourceProvider) {
		this.serviceResourceProvider = serviceResourceProvider;
	}

	public long getHealthCheckInstanceExpireInterval() {
		return healthCheckInstanceExpireInterval;
	}

	// for test
	public void setHealthCheckInstanceExpireInterval(long healthCheckInstanceExpireInterval) {
		this.healthCheckInstanceExpireInterval = healthCheckInstanceExpireInterval;
	}

	// for test
	public void setCircuitBreakerRuleDictionary(CircuitBreakerRuleDictionary circuitBreakerRuleDictionary) {
		this.circuitBreakerRuleDictionary = circuitBreakerRuleDictionary;
	}

	public void setFaultDetectRuleDictionary(FaultDetectRuleDictionary faultDetectRuleDictionary) {
		this.faultDetectRuleDictionary = faultDetectRuleDictionary;
	}

	public long getCheckPeriod() {
		return checkPeriod;
	}

	// for test
	public void setCheckPeriod(long checkPeriod) {
		this.checkPeriod = checkPeriod;
	}

	@Override
	protected void doDestroy() {
		stateChangeExecutors.shutdown();
		healthCheckExecutors.shutdown();
		expiredCleanupExecutors.shutdown();
	}

	Extensions getExtensions() {
		return extensions;
	}

	ScheduledExecutorService getStateChangeExecutors() {
		return stateChangeExecutors;
	}

	ScheduledExecutorService getHealthCheckExecutors() {
		return healthCheckExecutors;
	}

	public ServiceResourceProvider getServiceRuleProvider() {
		return serviceResourceProvider;
	}

	public Map<String, HealthChecker> getHealthCheckers() {
		return healthCheckers;
	}

	public Map<Level, Cache<Resource, Optional<ResourceCounters>>> getCountersCache() {
		return Collections.unmodifiableMap(countersCache);
	}

	public Map<ServiceKey, HealthCheckContainer> getHealthCheckCache() {
		return healthCheckCache;
	}

	CircuitBreakerConfig getCircuitBreakerConfig() {
		return circuitBreakerConfig;
	}

	//for test
	public void setCircuitBreakerConfig(CircuitBreakerConfig circuitBreakerConfig) {
		this.circuitBreakerConfig = circuitBreakerConfig;
	}

	int getResourceMappingSize() {
		return resourceMapping.size();
	}

	@Override
	public String getName() {
		return DefaultPlugins.CIRCUIT_BREAKER_COMPOSITE;
	}

	void onCircuitBreakerRuleChanged(ServiceKey serviceKey) {
		circuitBreakerRuleDictionary.onServiceChanged(serviceKey);
		LOG.info("onCircuitBreakerRuleChanged: clear service {} from ResourceCounters", serviceKey);
		for (Map.Entry<Level, Cache<Resource, Optional<ResourceCounters>>> entry : countersCache.entrySet()) {
			Cache<Resource, Optional<ResourceCounters>> cacheValue = entry.getValue();
			for (Resource resource : cacheValue.asMap().keySet()) {
				if (Objects.equals(resource.getService(), serviceKey)) {
					cacheValue.invalidate(resource);
				}
			}
		}
		HealthCheckContainer healthCheckContainer = healthCheckCache.get(serviceKey);
		if (null != healthCheckContainer) {
			for (Map.Entry<Resource, ResourceWrap> entry : resourceMapping.entrySet()) {
				Resource resource = entry.getKey();
				if (Objects.equals(resource.getService(), serviceKey)) {
					LOG.info("onCircuitBreakerRuleChanged: clear resource {} from healthCheckContainer", resource);
					healthCheckContainer.removeResource(resource);
				}
			}
		}
	}

	void onCircuitBreakerRuleAdded(ServiceKey serviceKey) {
		circuitBreakerRuleDictionary.onServiceChanged(serviceKey);
		LOG.info("onCircuitBreakerRuleAdded: clear service {} from ResourceCounters", serviceKey);
		for (Map.Entry<Level, Cache<Resource, Optional<ResourceCounters>>> entry : countersCache.entrySet()) {
			Cache<Resource, Optional<ResourceCounters>> cacheValue = entry.getValue();
			for (Map.Entry<Resource, Optional<ResourceCounters>> entryCache: cacheValue.asMap().entrySet()) {
				Resource resource = entryCache.getKey();
				if (Objects.equals(resource.getService(), serviceKey) && !entryCache.getValue().isPresent()) {
					cacheValue.invalidate(resource);
				}
			}
		}
	}

	void onFaultDetectRuleChanged(ServiceKey svcKey, RegistryCacheValue newValue) {
		ServiceRule serviceRule = (ServiceRule) newValue;
		if (null == serviceRule.getRule()) {
			return;
		}
		FaultDetectorProto.FaultDetector faultDetector = (FaultDetectorProto.FaultDetector) serviceRule.getRule();
		faultDetectRuleDictionary.onFaultDetectRuleChanged(svcKey, faultDetector);
		healthCheckCache.computeIfPresent(svcKey, new BiFunction<ServiceKey, HealthCheckContainer, HealthCheckContainer>() {
			@Override
			public HealthCheckContainer apply(ServiceKey serviceKey, HealthCheckContainer healthCheckContainer) {
				LOG.info("onFaultDetectRuleChanged: clear healthCheckContainer for service: {}", svcKey);
				healthCheckContainer.stop();
				return null;
			}
		});
		for (Map.Entry<Level, Cache<Resource, Optional<ResourceCounters>>> entry : countersCache.entrySet()) {
			Cache<Resource, Optional<ResourceCounters>> cacheValue = entry.getValue();
			for (Map.Entry<Resource, Optional<ResourceCounters>> entryCache : cacheValue.asMap().entrySet()) {
				Resource resource = entryCache.getKey();
				if (Objects.equals(resource.getService(), svcKey)) {
					if (entryCache.getValue().isPresent()) {
						LOG.info("onFaultDetectRuleChanged: ResourceCounters {} setReloadFaultDetect true", svcKey);
						ResourceCounters resourceCounters = entryCache.getValue().get();
						resourceCounters.setReloadFaultDetect(true);
					}

				}
			}
		}
	}

	void onFaultDetectRuleDeleted(ServiceKey svcKey, RegistryCacheValue newValue) {
		ServiceRule serviceRule = (ServiceRule) newValue;
		if (null == serviceRule.getRule()) {
			return;
		}
		faultDetectRuleDictionary.onFaultDetectRuleDeleted(svcKey);
		healthCheckCache.computeIfPresent(svcKey, new BiFunction<ServiceKey, HealthCheckContainer, HealthCheckContainer>() {
			@Override
			public HealthCheckContainer apply(ServiceKey serviceKey, HealthCheckContainer healthCheckContainer) {
				LOG.info("onFaultDetectRuleDeleted: clear healthCheckContainer for service: {}", svcKey);
				healthCheckContainer.stop();
				return null;
			}
		});
	}

	private static class ResourceWrap {
		// target resource, not nullable
		final Resource resource;
		// only record the report time
		long lastAccessTimeMilli;

		ResourceWrap(Resource resource, long lastAccessTimeMilli) {
			this.resource = resource;
			this.lastAccessTimeMilli = lastAccessTimeMilli;
		}
	}
}
