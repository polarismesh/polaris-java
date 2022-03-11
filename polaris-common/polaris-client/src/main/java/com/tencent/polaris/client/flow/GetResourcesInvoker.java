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

package com.tencent.polaris.client.flow;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.registry.EventCompleteNotifier;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.pojo.Services;
import com.tencent.polaris.api.utils.CollectionUtils;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 获取资源的回调
 */
public class GetResourcesInvoker implements EventCompleteNotifier, Future<ResourcesResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(GetResourcesInvoker.class);

    private final ResourcesResponse resourcesResponse = new ResourcesResponse();

    private final Set<ServiceEventKey> listeningServices = new HashSet<>();

    private final int totalCallback;

    private final AtomicInteger responseIncrement = new AtomicInteger();

    private final Object notifier = new Object();

    private final Extensions extensions;

    private final boolean internalRequest;

    private final boolean useCache;

    public GetResourcesInvoker(ServiceEventKeysProvider paramProvider,
                               Extensions extensions, boolean internalRequest, boolean useCache) throws PolarisException {
        this.extensions = extensions;
        this.internalRequest = internalRequest;
        this.totalCallback = init(paramProvider);
        this.useCache = useCache;
    }

    /**
     * 初始化invoker
     *
     * @param paramProvider
     * @return 等待的数量
     * @throws PolarisException
     */
    private int init(ServiceEventKeysProvider paramProvider) throws PolarisException {
        LocalRegistry localRegistry = extensions.getLocalRegistry();
        int callbacks = 0;
        if (!CollectionUtils.isEmpty(paramProvider.getSvcEventKeys())) {
            for (ServiceEventKey svcEventKey : paramProvider.getSvcEventKeys()) {
                listeningServices.add(svcEventKey);
                callbacks = processSvcEventKey(localRegistry, callbacks, svcEventKey);
            }
        }
        if (null != paramProvider.getSvcEventKey()) {
            listeningServices.add(paramProvider.getSvcEventKey());
            callbacks = processSvcEventKey(localRegistry, callbacks, paramProvider.getSvcEventKey());
        }
        return callbacks;
    }

    private int processSvcEventKey(LocalRegistry localRegistry, int callbacks, ServiceEventKey svcEventKey) {
        ResourceFilter filter = new ResourceFilter(svcEventKey, internalRequest, useCache);
        switch (svcEventKey.getEventType()) {
            case INSTANCE:
                ServiceInstances instances = localRegistry.getInstances(filter);
                if (instances.isInitialized()) {
                    resourcesResponse.addServiceInstances(svcEventKey, instances);
                } else {
                    localRegistry.loadInstances(svcEventKey, this);
                    callbacks++;
                }
                break;
            case SERVICE:
                Services services = localRegistry.getServices(filter);
                if (services.isInitialized()) {
                    resourcesResponse.addServices(svcEventKey, services);
                } else {
                    localRegistry.loadServices(svcEventKey, this);
                    callbacks++;
                }
                break;
            default:
                ServiceRule serviceRule = localRegistry.getServiceRule(filter);
                if (serviceRule.isInitialized()) {
                    resourcesResponse.addServiceRule(svcEventKey, serviceRule);
                } else {
                    localRegistry.loadServiceRule(svcEventKey, this);
                    callbacks++;
                }
                break;
        }
        return callbacks;
    }


    @Override
    public void complete(ServiceEventKey svcEventKey) {
        LocalRegistry localRegistry = extensions.getLocalRegistry();
        ResourceFilter filter = new ResourceFilter(svcEventKey, internalRequest, useCache);
        if (svcEventKey.getEventType() == ServiceEventKey.EventType.INSTANCE) {
            ServiceInstances instances = localRegistry.getInstances(filter);
            resourcesResponse.addServiceInstances(svcEventKey, instances);
        } else if (svcEventKey.getEventType() == EventType.SERVICE) {
            Services services = localRegistry.getServices(filter);
            resourcesResponse.addServices(svcEventKey, services);
        } else {
            ServiceRule serviceRule = localRegistry.getServiceRule(filter);
            resourcesResponse.addServiceRule(svcEventKey, serviceRule);
        }
        synchronized (notifier) {
            int curTotal = responseIncrement.addAndGet(1);
            if (totalCallback == curTotal) {
                notifier.notifyAll();
            }
        }
    }

    @Override
    public void completeExceptionally(ServiceEventKey svcEventKey, Throwable throwable) {
        resourcesResponse.addError(svcEventKey, throwable);
        synchronized (notifier) {
            int curTotal = responseIncrement.addAndGet(1);
            if (totalCallback == curTotal) {
                notifier.notifyAll();
            }
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return totalCallback == 0 || responseIncrement.get() >= totalCallback;
    }

    @Override
    public ResourcesResponse get() throws InterruptedException, ExecutionException {
        if (!isDone()) {
            synchronized (notifier) {
                if (!isDone()) {
                    notifier.wait();
                }
            }
        }
        Map<ServiceEventKey, Throwable> errors = resourcesResponse.getErrors();
        if (!errors.isEmpty()) {
            throw new ExecutionException(combineErrors(errors.values()));
        }
        return resourcesResponse;
    }

    @Override
    public ResourcesResponse get(
            long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long timeoutMs = TimeUnit.MILLISECONDS.convert(timeout, unit);
        if (!isDone()) {
            synchronized (notifier) {
                if (!isDone()) {
                    LOG.debug("start to wait for {}", this.listeningServices);
                    notifier.wait(timeoutMs);
                }
                LOG.debug("end to wait for {}", this.listeningServices);
                if (!isDone()) {
                    LOG.debug("timeout to wait for {}", this.listeningServices);
                    throw new TimeoutException();
                }
            }
        }
        Map<ServiceEventKey, Throwable> errors = resourcesResponse.getErrors();
        if (!errors.isEmpty()) {
            throw new ExecutionException(combineErrors(errors.values()));
        }
        return resourcesResponse;
    }

    /**
     * 多个错误集成一个错误
     *
     * @return 集成的异常
     */
    private PolarisException combineErrors(Collection<Throwable> errors) {
        StringBuilder builder = new StringBuilder();
        int retryCount = 0;
        for (Throwable err : errors) {
            if (err instanceof RetriableException) {
                retryCount++;
            }
            builder.append(err.toString());
            builder.append("\n");
        }
        if (retryCount == errors.size()) {
            //全部都是重试，才进行重试
            return new RetriableException(ErrorCode.SERVER_USER_ERROR, builder.toString());
        }
        return new PolarisException(ErrorCode.SERVER_USER_ERROR, builder.toString());
    }

    /**
     * 资源回调监听
     */
    public interface ResourcesListener {

        /**
         * 当调用完成后回调
         *
         * @param response 应答信息
         */
        void onComplete(ResourcesResponse response);
    }

    ;
}
