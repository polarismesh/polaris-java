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

package com.tencent.polaris.plugins.connector.nacos;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.common.utils.MD5Utils;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.server.CommonProviderRequest;
import com.tencent.polaris.api.plugin.server.CommonProviderResponse;
import com.tencent.polaris.api.plugin.server.ReportClientRequest;
import com.tencent.polaris.api.plugin.server.ReportClientResponse;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.Services;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.ServicesByProto;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceInstancesResponse;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;

/**
 * An implement of {@link ServerConnector} to connect to Nacos Server.
 *
 * @author Palmer Xu
 */
public class NacosConnector extends DestroyableServerConnector {

	/**
	 * Logger Instance.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(NacosConnector.class);

	/**
	 * Service Instance Name Format.
	 */
	private static final String INSTANCE_NAME = "%s$%s@@%s#%s#%d";

	/**
	 * If server connector initialized .
	 */
	private final AtomicBoolean initialized = new AtomicBoolean();

	/**
	 * Connector id .
	 */
	private String id;

	/**
	 * Marking service registration as enabled or not .
	 */
	private boolean isRegisterEnable = true;

	/**
	 * Marking service discovery as enabled or not .
	 */
	private boolean isDiscoveryEnable = true;

	/**
	 * Nacos Config Properties .
	 */
	private Properties nacosProperties = new Properties();

	/**
	 * Nacos namespace & NamingService mappings .
	 */
	private final Map<String, NamingService> namingServices = new ConcurrentHashMap<>();

	/**
	 * Nacos namespace & NacosServiceMerger mappings .
	 */
	private final Map<String, NacosServiceMerger> mergers = new ConcurrentHashMap<>();

	private final Object lock = new Object();

	private static final int NACOS_SERVICE_PAGESIZE = 10;

	@Override
	public String getName() {
		return DefaultPlugins.SERVER_CONNECTOR_NACOS;
	}

	@Override
	public PluginType getType() {
		return PluginTypes.SERVER_CONNECTOR.getBaseType();
	}

	@Override
	public void init(InitContext ctx) throws PolarisException {
		if (initialized.compareAndSet(false, true)) {
			List<ServerConnectorConfigImpl> serverConnectorConfigs = ctx.getConfig().getGlobal().getServerConnectors();
			if (CollectionUtils.isNotEmpty(serverConnectorConfigs)) {
				for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectorConfigs) {
					if (DefaultPlugins.SERVER_CONNECTOR_NACOS.equals(serverConnectorConfig.getProtocol())) {
						initActually(ctx, serverConnectorConfig);
					}
				}
			}
		}
	}

	private void initActually(InitContext ctx, ServerConnectorConfig connectorConfig) {
		this.id = connectorConfig.getId();
		if (ctx.getConfig().getProvider().getRegisterConfigMap().containsKey(id)) {
			isRegisterEnable = ctx.getConfig().getProvider().getRegisterConfigMap().get(id).isEnable();
		}
		if (ctx.getConfig().getConsumer().getDiscoveryConfigMap().containsKey(id)) {
			isDiscoveryEnable = ctx.getConfig().getConsumer().getDiscoveryConfigMap().get(id).isEnable();
		}

		this.nacosProperties = this.decodeNacosConfigProperties(connectorConfig);
	}

	private Properties decodeNacosConfigProperties(ServerConnectorConfig config) {
		Properties properties = new Properties();
		Map<String, String> metadata = Optional.ofNullable(config.getMetadata()).orElse(new HashMap<>());
		if (Objects.nonNull(metadata.get(PropertyKeyConst.USERNAME))) {
			properties.put(PropertyKeyConst.USERNAME, metadata.get(PropertyKeyConst.USERNAME));
		}
		if (Objects.nonNull(metadata.get(PropertyKeyConst.PASSWORD))) {
			properties.put(PropertyKeyConst.PASSWORD, metadata.get(PropertyKeyConst.PASSWORD));
		}
		if (Objects.nonNull(metadata.get(PropertyKeyConst.CONTEXT_PATH))) {
			properties.put(PropertyKeyConst.CONTEXT_PATH, metadata.get(PropertyKeyConst.CONTEXT_PATH));
		}
		if (Objects.nonNull(metadata.get(PropertyKeyConst.NAMESPACE))) {
			properties.put(PropertyKeyConst.NAMESPACE, metadata.get(PropertyKeyConst.NAMESPACE));
		}
		properties.put(PropertyKeyConst.SERVER_ADDR, String.join(",", config.getAddresses()));
		return properties;
	}

	private NamingService getOrCreateNamingService(String namespace) {
		if(StringUtils.isNotEmpty(this.nacosProperties.getProperty(PropertyKeyConst.NAMESPACE))){
			namespace = this.nacosProperties.getProperty(PropertyKeyConst.NAMESPACE);
		}

		NamingService namingService = namingServices.get(namespace);
		if (namingService != null) {
			return namingService;
		}

		synchronized (lock) {
			Properties properties = new Properties(nacosProperties);
			if (!Objects.equals(namespace, "default")) {
				properties.setProperty(PropertyKeyConst.NAMESPACE, namespace);
			}

			try {
				namingService = NacosFactory.createNamingService(properties);
			}
			catch (NacosException e) {
				LOG.error("[Connector][Nacos] fail to create naming service to {}, namespace {}",
						properties.get(PropertyKeyConst.SERVER_ADDR), namespace, e);
				return null;
			}
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}

			namingServices.put(namespace, namingService);
			mergers.put(namespace, new NacosServiceMerger(namingService));
			return namingService;
		}
	}

	@Override
	public void postContextInit(Extensions ctx) throws PolarisException {
		// do nothing
	}

	@Override
	public void registerServiceHandler(ServiceEventHandler handler) throws PolarisException {
		// do nothing
	}

	@Override
	public void deRegisterServiceHandler(ServiceEventKey eventKey) throws PolarisException {
		// do nothing
	}

	@Override
	public CommonProviderResponse registerInstance(CommonProviderRequest req, Map<String, String> customHeader) throws PolarisException {
		CommonProviderResponse response = new CommonProviderResponse();

		if (isRegisterEnable()) {
			NamingService namingService = getOrCreateNamingService(req.getNamespace());

			if (namingService == null) {
				LOG.error("[Nacos] fail to lookup namingService for service {}", req.getService());
				return null;
			}

			try {
				Instance instance = buildRegisterNacosInstance(req, analyzeNacosGroup(req.getService()));
				namingService.registerInstance(analyzeNacosService(req.getService()), analyzeNacosGroup(req.getService()), instance);
				response.setInstanceID(instance.getInstanceId());
			}
			catch (NacosException e) {
				throw new RetriableException(ErrorCode.NETWORK_ERROR,
						String.format("[Connector][Nacos] fail to register host %s:%d service %s", req.getHost(), req.getPort(),
								req.getService()), e);
			}
		}
		return response;
	}

	@Override
	public void deregisterInstance(CommonProviderRequest req) throws PolarisException {

		try {
			NamingService service = getOrCreateNamingService(req.getNamespace());

			if (service == null) {
				LOG.error("[Nacos] fail to lookup namingService for service {}", req.getService());
				return;
			}

			Instance instance = buildDeregisterNacosInstance(req, analyzeNacosGroup(req.getService()));

			// register with nacos naming service
			service.deregisterInstance(analyzeNacosService(req.getService()), analyzeNacosGroup(req.getService()), instance);
		}
		catch (NacosException e) {
			throw new RetriableException(ErrorCode.NETWORK_ERROR,
					String.format("[Connector][Nacos] fail to deregister host %s:%d service %s", req.getHost(), req.getPort(),
							req.getService()), e);
		}
	}

	@Override
	public void heartbeat(CommonProviderRequest req) throws PolarisException {
		// do nothing
	}

	@Override
	public ReportClientResponse reportClient(ReportClientRequest req) throws PolarisException {
		return null;
	}

	@Override
	public void updateServers(ServiceEventKey svcEventKey) {
		// do nothing
	}

	@Override
	public ServiceInstancesResponse syncGetServiceInstances(ServiceUpdateTask serviceUpdateTask) {
		List<DefaultInstance> instanceList = new ArrayList<>();
		try {

			String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
			NamingService namingService = getOrCreateNamingService(namespace);
			NacosServiceMerger merger = mergers.get(namespace);

			if (namingService == null || merger == null) {
				LOG.error("[Connector][Nacos] fail to lookup namingService for service {}", namespace);
				return null;
			}

			NacosServiceMerger.NacosService serviceValue = merger.createIfAbsent(serviceUpdateTask.getServiceEventKey()
					.getServiceKey());

			for (Instance service : serviceValue.getInstances()) {
				DefaultInstance instance = new DefaultInstance();
				instance.setId(service.getInstanceId());
				instance.setService(service.getServiceName());
				instance.setHost(service.getIp());
				instance.setPort(service.getPort());
				instance.setHealthy(service.isHealthy());
				instance.setMetadata(Optional.ofNullable(service.getMetadata()).orElse(new HashMap<>()));
				instance.setIsolated(!service.isEnabled());
				instance.setWeight((int) (100 * service.getWeight()));

				String protocol = instance.getMetadata().getOrDefault("protocol", "");
				String version = instance.getMetadata().getOrDefault("version", "");
				if (StringUtils.isNotEmpty(protocol)) {
					instance.setProtocol(protocol);
				}
				if (StringUtils.isNotEmpty(version)) {
					instance.setVersion(version);
				}

				String region = instance.getMetadata().getOrDefault("region", "");
				String zone = instance.getMetadata().getOrDefault("zone", "");
				String campus = instance.getMetadata().getOrDefault("campus", "");

				if (StringUtils.isNotEmpty(region)) {
					instance.setRegion(region);
				}
				if (StringUtils.isNotEmpty(zone)) {
					instance.setRegion(zone);
				}
				if (StringUtils.isNotEmpty(campus)) {
					instance.setRegion(campus);
				}

				instanceList.add(instance);
			}
			return new ServiceInstancesResponse(serviceValue.getRevision(), instanceList);
		}
		catch (Exception e) {
			throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
					String.format("[Connector][Nacos] Get service instances of %s sync failed.",
							serviceUpdateTask.getServiceEventKey().getServiceKey()));
		}
	}

	@Override
	public Services syncGetServices(ServiceUpdateTask serviceUpdateTask) {
		Services services = new ServicesByProto(new ArrayList<>());
		try {

			String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
			NamingService namingService = getOrCreateNamingService(namespace);

			if (namingService == null) {
				LOG.error("[Connector][Nacos] fail to lookup namingService for service {}", namespace);
				return null;
			}

			int pageIndex = 1;
			ListView<String> listView = namingService.getServicesOfServer(pageIndex, NACOS_SERVICE_PAGESIZE, DEFAULT_GROUP);
			final Set<String> serviceNames = new LinkedHashSet<>(listView.getData());
			int count = listView.getCount();
			int pageNumbers = count / NACOS_SERVICE_PAGESIZE;
			int remainder = count % NACOS_SERVICE_PAGESIZE;
			if (remainder > 0) {
				pageNumbers += 1;
			}
			// If more than 1 page
			while (pageIndex < pageNumbers) {
				listView = namingService.getServicesOfServer(++pageIndex, NACOS_SERVICE_PAGESIZE, DEFAULT_GROUP);
				serviceNames.addAll(listView.getData());
			}

			serviceNames.forEach(name -> {
				ServiceInfo serviceInfo = new ServiceInfo();
				serviceInfo.setNamespace(namespace);
				serviceInfo.setService(name);
				services.getServices().add(serviceInfo);
			});

		}
		catch (NacosException e) {
			throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
					String.format("[Connector][Nacos] Get services of %s instances sync failed.",
							serviceUpdateTask.getServiceEventKey().getServiceKey()));
		}
		return services;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean isRegisterEnable() {
		return this.isRegisterEnable;
	}

	@Override
	public boolean isDiscoveryEnable() {
		return this.isDiscoveryEnable;
	}

	@Override
	public boolean isInitialized() {
		return this.initialized.get();
	}

	@Override
	public void retryServiceUpdateTask(ServiceUpdateTask updateTask) {
		// do nothing
	}

	@Override
	protected void submitServiceHandler(ServiceUpdateTask updateTask, long delayMs) {
		// do nothing
	}

	@Override
	public void addLongRunningTask(ServiceUpdateTask serviceUpdateTask) {
		// do nothing
	}

	@Override
	protected void doDestroy() {
		if (initialized.compareAndSet(true, false)) {
			// unsubscribe service listener
			if (CollectionUtils.isNotEmpty(mergers)) {
				mergers.forEach((s, serviceMerger) -> {
					try {
						serviceMerger.shutdown();
					}
					catch (Exception ignore) {
					}
				});
			}

			// shutdown naming service
			if (CollectionUtils.isNotEmpty(namingServices)) {
				namingServices.forEach((s, namingService) -> {
					try {
						namingService.shutDown();
					}
					catch (NacosException ignore) {
					}
				});
			}
		}
	}

	private Instance buildRegisterNacosInstance(CommonProviderRequest req, String group) {
		String instanceId = String.format(INSTANCE_NAME, req.getNamespace(), group,
				analyzeNacosService(req.getService()), req.getHost(), req.getPort());
		Instance instance = new Instance();
		instance.setInstanceId(instanceId);
		instance.setEnabled(true);
		instance.setEphemeral(true);
		instance.setPort(req.getPort());
		instance.setIp(req.getHost());
		instance.setHealthy(true);
		if (Objects.nonNull(req.getWeight())) {
			instance.setWeight(req.getWeight());
		}
		instance.setServiceName(analyzeNacosService(req.getService()));

		if (CollectionUtils.isNotEmpty(req.getMetadata())) {
			if (req.getMetadata().containsKey("nacos.cluster")) {
				instance.setClusterName(req.getMetadata().get("nacos.cluster"));
			}
		}

		Map<String, String> metadata = new HashMap<>(Optional.ofNullable(req.getMetadata())
				.orElse(Collections.emptyMap()));

		// 填充默认 protocol 以及 version 属性信息
		if (StringUtils.isNotEmpty(req.getProtocol())) {
			metadata.put("protocol", req.getProtocol());
		}
		if (StringUtils.isNotEmpty(req.getVersion())) {
			metadata.put("version", req.getVersion());
		}

		// 填充地域信息
		if (StringUtils.isNotEmpty(req.getRegion())) {
			metadata.put("region", req.getRegion());
		}
		if (StringUtils.isNotEmpty(req.getZone())) {
			metadata.put("zone", req.getZone());
		}
		if (StringUtils.isNotEmpty(req.getCampus())) {
			metadata.put("campus", req.getCampus());
		}

		instance.setMetadata(metadata);
		return instance;
	}

	private Instance buildDeregisterNacosInstance(CommonProviderRequest req, String group) {
		String instanceId = String.format(INSTANCE_NAME, req.getNamespace(), group,
				analyzeNacosService(req.getService()), req.getHost(), req.getPort());
		Instance instance = new Instance();
		instance.setInstanceId(instanceId);
		instance.setEnabled(true);
		instance.setEphemeral(true);
		instance.setPort(req.getPort());
		instance.setIp(req.getHost());
		instance.setHealthy(true);

		if (CollectionUtils.isNotEmpty(req.getMetadata())) {
			if (req.getMetadata().containsKey("nacos.cluster")) {
				instance.setClusterName(req.getMetadata().get("nacos.cluster"));
			}
		}

		return instance;
	}

	protected static String analyzeNacosService(String service) {
		String[] detail = service.split("__");
		if (detail.length == 1) {
			return service;
		}

		return service.replaceFirst(detail[0] + "__", "");
	}

	protected static String analyzeNacosGroup(String service) {
		String[] detail = service.split("__");
		if (detail.length == 1 || Objects.equals(detail[0], "")) {
			return DEFAULT_GROUP;
		}
		return detail[0];
	}

	private static class NacosServiceMerger {

		private final NamingService namingService;

		private NacosServiceMerger(NamingService service) {
			this.namingService = service;
		}

		private final Map<ServiceKey, NacosService> services = new ConcurrentHashMap<>(8);

		public void shutdown() {
			try {
				services.values().forEach(entry -> {
					try {
						namingService.unsubscribe(entry.serviceName, entry.group, entry);
					}
					catch (NacosException ignore) {
					}
				});

				services.clear();
			}
			catch (Exception ignore) {
			}
		}

		public synchronized NacosService createIfAbsent(ServiceKey key) throws Exception {
			if (!services.containsKey(key)) {
				NacosService service = new NacosService(namingService, analyzeNacosService(key.getService()),
						analyzeNacosGroup(key.getService()));

				service.init();

				services.put(key, service);
			}

			return services.get(key);
		}

		private static class NacosService implements EventListener {

			private final String serviceName;

			private final String group;

			private String revision;

			private List<Instance> instances;

			private final NamingService namingService;

			NacosService(NamingService namingService, String serviceName, String group) {
				this.namingService = namingService;
				this.serviceName = serviceName;
				this.group = group;
			}

			private void init() throws Exception {
				instances = namingService.getAllInstances(serviceName, group);

				// subscribe service instance change .
				try {
					namingService.subscribe(serviceName, group, this);
				}
				catch (NacosException e) {
					LOG.warn("[Connector][Nacos] service subscribe failed, service name: {}, group: {}", serviceName, group, e);
				}
			}

			private String buildRevision(List<Instance> instances) throws Exception {
				StringBuilder revisionStr = new StringBuilder("NacosServiceInstances");
				for (Instance instance : instances) {
					revisionStr.append("|").append(instance.toString());
				}
				return MD5Utils.md5Hex(revisionStr.toString().getBytes(StandardCharsets.UTF_8));
			}

			public void rebuild(List<Instance> instances) throws Exception {
				this.instances = instances;
				this.revision = buildRevision(instances);
			}


			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (o == null || getClass() != o.getClass()) {
					return false;
				}
				NacosService that = (NacosService) o;
				return Objects.equals(serviceName, that.serviceName) && Objects.equals(group, that.group);
			}

			@Override
			public int hashCode() {
				return Objects.hash(serviceName, group);
			}

			@Override
			public void onEvent(Event event) {
				if (event instanceof NamingEvent) {
					NamingEvent namingEvent = (NamingEvent) event;
					// rebuild instance cache
					try {
						rebuild(namingEvent.getInstances());
					}
					catch (Exception e) {
						LOG.warn("[Connector][Nacos] service revision build failed, service name: {}, group: {}", serviceName, group, e);
					}
				}
			}

			List<Instance> getInstances() {
				return instances;
			}

			String getRevision() {
				return revision;
			}
		}
	}
}
