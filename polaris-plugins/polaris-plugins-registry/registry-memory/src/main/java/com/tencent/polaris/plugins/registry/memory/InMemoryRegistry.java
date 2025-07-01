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

package com.tencent.polaris.plugins.registry.memory;

import com.google.protobuf.Message;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.plugin.registry.*;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.plugin.stat.CircuitBreakGauge;
import com.tencent.polaris.api.plugin.stat.DefaultCircuitBreakResult;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.client.pojo.ServiceInstancesByProto;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.client.pojo.ServicesByProto;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

/**
 * 本地缓存,保存服务端返回的实例信息.
 *
 * @author andrewshan, Haotian Zhang
 */
public class InMemoryRegistry extends Destroyable implements LocalRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryRegistry.class);

    /**
     * 默认首次发现discovery服务重试间隔
     */
    private static final long defaultDiscoverServiceRetryIntervalMs = 5000;
    /**
     * 存储同步下来的服务规则信息
     */
    private final Map<ServiceEventKey, CacheObject> resourceMap = new ConcurrentHashMap<>();
    /**
     * 资源变更时间监听器
     */
    private final List<ResourceEventListener> resourceEventListeners = new CopyOnWriteArrayList<>();
    /**
     * 存储使用到的服务列表数据
     */
    private final Map<ServiceKey, Boolean> services = new ConcurrentHashMap<>();
    /**
     * 缓存处理器，通过SPI加载
     */
    private final Map<EventType, CacheHandler> cacheHandlers = new HashMap<>();
    /**
     * 系统服务信息
     */
    private final Map<ServiceKey, ServerServiceInfo> serverServiceMap = new HashMap<>();
    private ServerConnector connector;
    /**
     * 服务数据持久化处理器
     */
    private MessagePersistHandler messagePersistHandler;
    /**
     * 持久化线程
     */
    private ExecutorService persistExecutor;
    /**
     * 超时淘汰线程
     */
    private ScheduledExecutorService expireExecutor;
    /**
     * 拉取系统服务的线程
     */
    private ExecutorService serverServicesDiscoverExecutor;
    /**
     * 服务刷新时延
     */
    private long serviceRefreshIntervalMs;

    /**
     * 拉取服务列表刷新间隔
     */
    private long serviceListRefreshIntervalMs;

    /**
     * 启用本地文件缓存
     */
    private boolean persistEnable;

    /**
     * 缓存是否超时淘汰
     */
    private boolean serviceExpireEnable;

    /**
     * 缓存淘汰时间
     */
    private long serviceExpireTimeMs;

    /**
     * 是否有独立的服务发现集群
     */
    private boolean hasDiscoverCluster = false;

    private FlowCache flowCache;

    private Collection<Plugin> statPlugins;

    @Override
    public Set<ServiceKey> getServices() {
        return services.keySet();
    }

    @Override
    public ServiceRule getServiceRule(ResourceFilter filter) {
        RegistryCacheValue resourceCache = getResource(filter.getSvcEventKey(), filter.isIncludeCache(),
                filter.isInternalRequest());
        if (null == resourceCache) {
            return ServiceRuleByProto.EMPTY_SERVICE_RULE;
        }
        return (ServiceRule) resourceCache;
    }

    private RegistryCacheValue getResource(ServiceEventKey svcEventKey, boolean includeCache, boolean internalRequest) {
        CacheObject cacheObject = resourceMap.get(svcEventKey);
        if (null == cacheObject || Objects.equals("", cacheObject.getRevision())) {
            if (includeCache) {
                // 没有从 remote 正常获取到数据，从本地容灾文件进行数据获取
                loadResourceFromLocal(svcEventKey);
                cacheObject = resourceMap.get(svcEventKey);
                if (Objects.isNull(cacheObject)) {
                    return null;
                }
            }
        }
        if (Objects.isNull(cacheObject)) {
            return null;
        }
        RegistryCacheValue registryCacheValue = cacheObject.loadValue(!internalRequest);
        if (null == registryCacheValue) {
            return null;
        }
        if (cacheObject.isRemoteUpdated() || includeCache) {
            return registryCacheValue;
        }
        return null;
    }

    @Override
    public ServiceInstances getInstances(ResourceFilter filter) {
        RegistryCacheValue resourceCache = getResource(filter.getSvcEventKey(), filter.isIncludeCache(),
                filter.isInternalRequest());
        if (null == resourceCache) {
            return ServiceInstancesByProto.EMPTY_INSTANCES;
        }
        return (ServiceInstances) resourceCache;
    }

    @Override
    public void loadServiceRule(ServiceEventKey svcEventKey, EventCompleteNotifier notifier) throws PolarisException {
        loadRemoteValue(svcEventKey, notifier);
    }

    @Override
    public Services getServices(ResourceFilter filter) {
        RegistryCacheValue resourceCache = getResource(filter.getSvcEventKey(), filter.isIncludeCache(),
                filter.isInternalRequest());
        if (null == resourceCache) {
            return ServicesByProto.EMPTY_SERVICES;
        }
        return (Services) resourceCache;
    }

    @Override
    public void loadServices(ServiceEventKey svcEventKey, EventCompleteNotifier notifier) throws PolarisException {
        loadRemoteValue(svcEventKey, notifier);
    }

    /**
     * 获取connector
     *
     * @return 资源connector
     */
    private ServerConnector getConnector() {
        return connector;
    }

    /**
     * 获取flow cache
     *
     * @return flow cache
     */
    public FlowCache getFlowCache() {
        return flowCache;
    }

    /**
     * 加载资源
     *
     * @param svcEventKey 服务资源名
     * @param notifier    通知器
     * @throws PolarisException 异常
     */
    private void loadRemoteValue(ServiceEventKey svcEventKey, EventCompleteNotifier notifier) throws PolarisException {
        checkDestroyed();
        CacheHandler handler = cacheHandlers.get(svcEventKey.getEventType());
        if (null == handler) {
            throw new PolarisException(ErrorCode.INTERNAL_ERROR,
                    String.format("[LocalRegistry] unRegistered resource type %s", svcEventKey.getEventType()));
        }
        CacheObject cacheObject = resourceMap.computeIfAbsent(svcEventKey,
                serviceEventKey -> new CacheObject(handler, svcEventKey, InMemoryRegistry.this)
        );
        //添加监听器
        cacheObject.addNotifier(notifier);
        //触发往serverConnector注册
        if (cacheObject.startRegister()) {
            LOG.info("[LocalRegistry]start to register service handler for {}", svcEventKey);
            try {
                connector.registerServiceHandler(
                        enhanceServiceEventHandler(new ServiceEventHandler(svcEventKey, cacheObject)));
            } catch (Throwable e) {
                PolarisException polarisException;
                if (e instanceof PolarisException) {
                    polarisException = (PolarisException) e;
                } else {
                    polarisException = new PolarisException(ErrorCode.INTERNAL_ERROR,
                            String.format("exception occurs while registering service handler for %s", svcEventKey));
                }
                cacheObject.resumeUnRegistered(polarisException);
                throw polarisException;
            }
            if (svcEventKey.getEventType() == EventType.INSTANCE) {
                //注册了监听后，认为是被用户需要的服务，加入serviceSet
                services.put(svcEventKey.getServiceKey(), true);
            }
        }
    }

    private ServiceEventHandler enhanceServiceEventHandler(ServiceEventHandler eventHandler) {
        ServiceKey serviceKey = eventHandler.getServiceEventKey().getServiceKey();
        ServerServiceInfo info = serverServiceMap.get(serviceKey);
        if (null != info) {
            //系统服务
            eventHandler.setRefreshInterval(info.getRefreshIntervalMs());
            if (info.getClusterType() != ClusterType.SERVICE_DISCOVER_CLUSTER) {
                eventHandler.setTargetCluster(ClusterType.SERVICE_DISCOVER_CLUSTER);
            } else {
                eventHandler.setTargetCluster(ClusterType.BUILTIN_CLUSTER);
            }
        } else {
            // 拉取服务列表的间隔时间参数跟其它资源独立
            if (eventHandler.getServiceEventKey().getEventType() == EventType.SERVICE) {
                eventHandler.setRefreshInterval(serviceListRefreshIntervalMs);
            } else {
                eventHandler.setRefreshInterval(serviceRefreshIntervalMs);
            }
            eventHandler.setTargetCluster(ClusterType.SERVICE_DISCOVER_CLUSTER);
        }
        return eventHandler;
    }

    @Override
    public void loadInstances(ServiceEventKey svcEventKey, EventCompleteNotifier notifier) throws PolarisException {
        loadRemoteValue(svcEventKey, notifier);
    }

    @SuppressWarnings("unchecked")
    private void onCircuitBreakStatus(Object value, InstanceLocalValue instanceLocalValue, Instance instance) {
        Map<StatusDimension, CircuitBreakerStatus> statusMap = (Map<StatusDimension, CircuitBreakerStatus>) value;
        if (MapUtils.isNotEmpty(statusMap)) {
            for (Map.Entry<StatusDimension, CircuitBreakerStatus> entry : statusMap.entrySet()) {
                instanceLocalValue.setCircuitBreakerStatus(entry.getKey(), entry.getValue());
                reportCircuitStat(entry, instance);
            }
        }
    }

    private void reportCircuitStat(Entry<StatusDimension, CircuitBreakerStatus> dimensionEntry,
                                   Instance instance) {
        if (null != statPlugins) {
            try {
                for (Plugin statPlugin : statPlugins) {
                    if (statPlugin instanceof StatReporter) {
                        StatInfo info = new StatInfo();
                        info.setCircuitBreakGauge(convertToCircuitBreakGauge(dimensionEntry, instance));
                        ((StatReporter) statPlugin).reportStat(info);
                    }
                }
            } catch (Exception ex) {
                LOG.info("circuit breaker report encountered exception, e: {}", ex.getMessage());
            }
        }
    }

    private CircuitBreakGauge convertToCircuitBreakGauge(Entry<StatusDimension, CircuitBreakerStatus> dimensionEntry,
                                                         Instance instance) {
        DefaultCircuitBreakResult result = new DefaultCircuitBreakResult();
        result.setMethod(dimensionEntry.getKey().getMethod());
        result.setCallerService(dimensionEntry.getKey().getCallerService());
        result.setCircuitBreakStatus(dimensionEntry.getValue());
        result.setHost(instance.getHost());
        result.setPort(instance.getPort());
        result.setInstanceId(instance.getId());
        result.setService(instance.getService());
        result.setNamespace(instance.getNamespace());
        return result;
    }

    @Override
    public void updateInstances(ServiceUpdateRequest request) {
        Collection<InstanceProperty> instanceProperties = request.getProperties();
        if (CollectionUtils.isEmpty(instanceProperties)) {
            return;
        }
        ServiceEventKey svcEventKey = new ServiceEventKey(request.getServiceKey(), EventType.INSTANCE);
        RegistryCacheValue cacheValue = getResource(svcEventKey, true, true);
        if (null == cacheValue) {
            //服务不存在，忽略
            return;
        }
        boolean changed = false;
        for (InstanceProperty instanceProperty : instanceProperties) {
            InstanceByProto instance = (InstanceByProto) instanceProperty.getInstance();
            InstanceLocalValue instanceLocalValue = instance.getInstanceLocalValue();
            Map<String, Object> properties = instanceProperty.getProperties();
            LOG.info("update instance properties for instance id:{}, host:{}, port{}, properties {}", instance.getId(),
                    instance.getHost(), instance.getPort(), properties);
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                switch (entry.getKey()) {
                    case InstanceProperty.PROPERTY_DETECT_RESULT:
                        instanceLocalValue.setDetectResult((DetectResult) entry.getValue());
                        changed = true;
                        break;
                    default:
                        break;
                }
            }
        }
        if (changed) {
            // notify changed
            Collection<ResourceEventListener> resourceEventListeners = getResourceEventListeners();
            if (!CollectionUtils.isEmpty(resourceEventListeners)) {
                for (ResourceEventListener listener : resourceEventListeners) {
                    listener.onResourceUpdated(svcEventKey, cacheValue, cacheValue);
                }
            }
        }
    }

    @Override
    public void registerResourceListener(ResourceEventListener listener) {
        resourceEventListeners.add(listener);
    }

    @Override
    public void watchResource(ServiceEventKey svcEventKey) {
        CacheObject cacheObject = resourceMap.get(svcEventKey);
        if (Objects.isNull(cacheObject)) {
            return;
        }
        cacheObject.openWatch();
    }

    @Override
    public void unwatchResource(ServiceEventKey svcEventKey) {
        CacheObject cacheObject = resourceMap.get(svcEventKey);
        if (Objects.isNull(cacheObject)) {
            return;
        }
        cacheObject.cancelWatch();
    }

    public Collection<ResourceEventListener> getResourceEventListeners() {
        return resourceEventListeners;
    }

    @Override
    public String getName() {
        return DefaultPlugins.LOCAL_REGISTRY_IN_MEMORY;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.LOCAL_REGISTRY.getBaseType();
    }


    @Override
    public void init(InitContext ctx) throws PolarisException {
        //获取系统服务配置
        Collection<ServerServiceInfo> serverServices = ctx.getServerServices();
        for (ServerServiceInfo serverServiceInfo : serverServices) {
            if (serverServiceInfo.getClusterType() == ClusterType.SERVICE_DISCOVER_CLUSTER) {
                hasDiscoverCluster = true;
            }
            serverServiceMap.put(serverServiceInfo.getServiceKey(), serverServiceInfo);
        }
        //加载cacheHandler
        ServiceLoader<CacheHandler> handlers = ServiceLoader.load(CacheHandler.class);
        for (CacheHandler handler : handlers) {
            cacheHandlers.put(handler.getTargetEventType(), handler);
        }
        // Load server connector.
        connector = (ServerConnector) ctx.getPlugins().getPlugin(PluginTypes.SERVER_CONNECTOR.getBaseType(),
                ctx.getValueContext().getServerConnectorProtocol());

        //构建基础属性
        String persistDir = ctx.getConfig().getConsumer().getLocalCache().getPersistDir();
        int maxReadRetry = ctx.getConfig().getConsumer().getLocalCache().getPersistMaxReadRetry();
        int maxWriteRetry = ctx.getConfig().getConsumer().getLocalCache().getPersistMaxWriteRetry();
        long retryIntervalMs = ctx.getConfig().getConsumer().getLocalCache().getPersistRetryInterval();
        this.serviceRefreshIntervalMs = ctx.getConfig().getConsumer().getLocalCache().getServiceRefreshInterval();
        this.serviceListRefreshIntervalMs = ctx.getConfig().getConsumer().getLocalCache()
                .getServiceListRefreshInterval();
        boolean configPersistEnable = ctx.getConfig().getConsumer().getLocalCache().isPersistEnable();
        persistEnable = configPersistEnable && StringUtils.isNotBlank(persistDir);
        //启动本地缓存
        if (persistEnable) {
            messagePersistHandler = new MessagePersistHandler(persistDir, maxWriteRetry, maxReadRetry, retryIntervalMs);
            try {
                messagePersistHandler.init();
            } catch (IOException e) {
                throw new PolarisException(ErrorCode.PLUGIN_ERROR,
                        String.format("plugin %s init failed", getName()), e);
            }
            // 这里不能直接一把 Load 所有容灾文件，应该做成按需加载
            // loadFileCache(persistDir);
        }
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory(getName());
        serviceExpireTimeMs = ctx.getConfig().getConsumer().getLocalCache().getServiceExpireTime();
        serviceExpireEnable = ctx.getConfig().getConsumer().getLocalCache().isServiceExpireEnable();
        persistExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);
        expireExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
        if (hasDiscoverCluster) {
            serverServicesDiscoverExecutor = new ThreadPoolExecutor(0, 1,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    namedThreadFactory);
        }
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        if (serviceExpireEnable) {
            expireExecutor.scheduleAtFixedRate(new ExpireTask(), 0, serviceExpireTimeMs, TimeUnit.MILLISECONDS);
        }
        if (null != serverServicesDiscoverExecutor) {
            serverServicesDiscoverExecutor.execute(new WarmupDiscoverServiceTask(extensions));
        }
        statPlugins = extensions.getPlugins().getPlugins(PluginTypes.STAT_REPORTER.getBaseType());
        flowCache = extensions.getFlowCache();
    }

    /**
     * 持久化消息
     *
     * @param svcEventKey 资源KEY
     * @param message     原始消息
     */
    public void saveMessageToFile(ServiceEventKey svcEventKey, Message message) {
        if (!persistEnable) {
            return;
        }
        if (!persistExecutor.isShutdown()) {
            persistExecutor.execute(new SavePersistTask(svcEventKey, message));
        }
    }

    /**
     * 删除文件缓存
     *
     * @param svcEventKey 资源KEY
     */
    private void deleteFileMessage(ServiceEventKey svcEventKey) {
        if (!persistEnable) {
            return;
        }
        if (!persistExecutor.isShutdown()) {
            persistExecutor.execute(new DeletePersistTask(svcEventKey));
        }
    }

    private void loadResourceFromLocal(ServiceEventKey svcEventKey) {
        if (!persistEnable) {
            return;
        }
        CacheHandler cacheHandler = cacheHandlers.get(svcEventKey.getEventType());
        if (null == cacheHandler) {
            LOG.warn("[LocalRegistry]resource type {} not registered, ignore the file", svcEventKey.getEventType());
            return;
        }
        Message message = messagePersistHandler.loadPersistedServices(svcEventKey, ResponseProto.DiscoverResponse::newBuilder);
        if (Objects.isNull(message)) {
            return;
        }
        CacheObject waitSave = new CacheObject(cacheHandler, svcEventKey, InMemoryRegistry.this, message);
        CacheObject saveObj = resourceMap.computeIfAbsent(svcEventKey, eventKey -> waitSave);
        // 这里是直接对比两个对象的地址是否一致，如果不一致，才调用 onEvent
        if (waitSave != saveObj) {
            saveObj.onEventUpdate(new ServerEvent(svcEventKey, message, null));
        }
    }

    /**
     * 清理资源缓存
     *
     * @param serviceEventKey 资源KEY
     */
    public void removeCache(ServiceEventKey serviceEventKey) {
        LOG.info("[LocalRegistry] remove cache for resource {}", serviceEventKey);
        try {
            getConnector().deRegisterServiceHandler(serviceEventKey);
        } catch (PolarisException e) {
            LOG.error("[LocalRegistry] fail to deRegisterServiceHandler", e);
        }
        resourceMap.remove(serviceEventKey);
        if (serviceEventKey.getEventType() == EventType.INSTANCE) {
            services.remove(serviceEventKey.getServiceKey());
        }
        deleteFileMessage(serviceEventKey);
    }

    @Override
    protected void doDestroy() {
        ThreadPoolUtils.waitAndStopThreadPools(
                new ExecutorService[]{serverServicesDiscoverExecutor, persistExecutor, expireExecutor,});
    }

    /**
     * 设置系统服务就绪状态
     *
     * @param serviceEventKey 资源标识
     */
    public void setServerServiceReady(ServiceEventKey serviceEventKey) {
        if (!serverServiceMap.containsKey(serviceEventKey.getServiceKey())) {
            return;
        }
        connector.updateServers(serviceEventKey);
    }

    /**
     * 持久化任务结构
     */
    private class SavePersistTask implements Runnable {

        final ServiceEventKey svcEventKey;
        final Message message;

        SavePersistTask(ServiceEventKey svcEventKey, Message message) {
            this.svcEventKey = svcEventKey;
            this.message = message;
        }

        @Override
        public void run() {
            InMemoryRegistry.this.messagePersistHandler.saveService(svcEventKey, message);
        }
    }

    /**
     * 删除文件任务结构
     */
    private class DeletePersistTask implements Runnable {

        final ServiceEventKey svcEventKey;

        DeletePersistTask(ServiceEventKey svcEventKey) {
            this.svcEventKey = svcEventKey;
        }

        @Override
        public void run() {
            InMemoryRegistry.this.messagePersistHandler.deleteService(svcEventKey);
        }
    }

    /**
     * 缓存淘汰操作
     */
    private class ExpireTask implements Runnable {

        @Override
        public void run() {
            for (Map.Entry<ServiceEventKey, CacheObject> entry : resourceMap.entrySet()) {
                CacheObject cacheObject = entry.getValue();
                // 如果资源被 Watch 了, 则不能淘汰
                if (cacheObject.isWatched()) {
                    continue;
                }
                //如果当前时间减去最新访问时间没有超过expireTime，那么不用淘汰，继续检查下一个服务
                long lastAccessTime = cacheObject.getLastAccessTimeMs();
                if (lastAccessTime == 0) {
                    continue;
                }
                long nowMs = System.currentTimeMillis();
                long diffTimeMs = nowMs - lastAccessTime;
                if (diffTimeMs < 0) {
                    //时间发生倒退，则直接更新最近访问时间
                    cacheObject.setLastAccessTimeMs(nowMs);
                    continue;
                }
                if (diffTimeMs < InMemoryRegistry.this.serviceExpireTimeMs) {
                    continue;
                }
                //执行淘汰
                removeCache(entry.getKey());
            }
        }
    }

    private class WarmupDiscoverServiceTask implements Runnable {

        private final Extensions extensions;

        public WarmupDiscoverServiceTask(Extensions extensions) {
            this.extensions = extensions;
        }

        private void retryTask() {
            Utils.sleepUninterrupted(defaultDiscoverServiceRetryIntervalMs);
            ExecutorService serverServicesDiscoverExecutor = InMemoryRegistry.this.serverServicesDiscoverExecutor;
            if (null != serverServicesDiscoverExecutor && !serverServicesDiscoverExecutor.isShutdown()) {
                serverServicesDiscoverExecutor.execute(new WarmupDiscoverServiceTask(extensions));
            }

        }

        @Override
        public void run() {
            ServiceKey discoverSvcKey = null;
            Map<ServiceKey, ServerServiceInfo> serverServiceMap = InMemoryRegistry.this.serverServiceMap;
            for (Entry<ServiceKey, ServerServiceInfo> entry : serverServiceMap.entrySet()) {
                if (entry.getValue().getClusterType() == ClusterType.SERVICE_DISCOVER_CLUSTER) {
                    discoverSvcKey = entry.getKey();
                    break;
                }
            }
            if (null == discoverSvcKey) {
                LOG.warn("[LocalRegistry] discover service not config");
                return;
            }
            ServiceEventKey svcEventKey = new ServiceEventKey(discoverSvcKey, EventType.INSTANCE);
            DefaultServiceEventKeysProvider provider = new DefaultServiceEventKeysProvider();
            provider.setSvcEventKey(svcEventKey);
            DefaultFlowControlParam defaultFlowControlParam = new DefaultFlowControlParam();
            APIConfig apiConfig = extensions.getConfiguration().getGlobal().getAPI();
            defaultFlowControlParam.setTimeoutMs(apiConfig.getTimeout());
            defaultFlowControlParam.setMaxRetry(apiConfig.getMaxRetryTimes());
            defaultFlowControlParam.setRetryIntervalMs(apiConfig.getRetryInterval());
            ResourcesResponse resourcesResponse;
            try {
                resourcesResponse = BaseFlow
                        .syncGetResources(extensions, false, provider, defaultFlowControlParam);
            } catch (PolarisException e) {
                if (e.getCode() == ErrorCode.INVALID_STATE) {
                    return;
                }
                LOG.error("[LocalRegistry] fail to fetch server service {}", svcEventKey, e);
                retryTask();
                return;
            }
            ServiceInstances serviceInstances = resourcesResponse.getServiceInstances(svcEventKey);
            RegistryCacheValue cacheValue = (RegistryCacheValue) serviceInstances;
            if (!cacheValue.isInitialized()) {
                retryTask();
            }
        }
    }

}
