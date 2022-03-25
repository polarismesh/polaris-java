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

package com.tencent.polaris.discovery.client.flow;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.listener.ServiceListener;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.registry.AbstractResourceEventListener;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceChangeEvent;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.WatchServiceResponse;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.ServiceInstancesByProto;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.client.util.Utils;
import org.slf4j.Logger;
import com.tencent.polaris.logging.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class WatchFlow {

    private static final Logger LOG = LoggerFactory.getLogger(SyncFlow.class);

    private Extensions extensions;

    private SyncFlow syncFlow;

    private DispatchExecutor executor;

    private final AtomicBoolean initialize = new AtomicBoolean(false);

    private static final Map<ServiceKey, Set<ServiceListener>> watchers = new ConcurrentHashMap<>();

    public void init(Extensions extensions, SyncFlow syncFlow) {
        this.extensions = extensions;
        this.syncFlow = syncFlow;
    }

    /**
     * 监听服务下的实例变化
     *
     * @return WatchServiceResponse
     */
    public WatchServiceResponse commonWatchService(CommonWatchServiceRequest request) throws PolarisException {
        initFlow();
        ServiceKey serviceKey = request.getSvcEventKey().getServiceKey();
        InstancesResponse response = syncFlow.commonSyncGetAllInstances(request.getAllRequest());
        watchers.computeIfAbsent(request.getSvcEventKey().getServiceKey(),
                key -> Collections.synchronizedSet(new HashSet<>()));
        boolean result = watchers.get(serviceKey).addAll(request.getWatchServiceRequest().getListeners());
        return new WatchServiceResponse(response, result);
    }

    /**
     * 取消服务监听
     * case 1. 移除对 service 的所有 Listener
     * case 2. 只移除部分对 service 的 Listener
     *
     * @param request CommonUnWatchServiceRequest
     * @return WatchServiceResponse
     * @throws PolarisException
     */
    public WatchServiceResponse commonUnWatchService(CommonUnWatchServiceRequest request) throws PolarisException {
        initFlow();
        boolean result = true;

        Set<ServiceListener> listeners = watchers.get(request.getSvcEventKey().getServiceKey());

        if (request.getRequest().isRemoveAll()) {
            watchers.remove(request.getSvcEventKey().getServiceKey());
        } else {
            if (CollectionUtils.isNotEmpty(listeners)) {
                result = listeners.removeAll(request.getRequest().getListeners());
            }
        }

        return new WatchServiceResponse(null, result);
    }

    private void initFlow() {
        if (initialize.compareAndSet(false, true)) {
            extensions.getLocalRegistry().registerResourceListener(new InstanceChangeListener());
            executor = new DispatchExecutor(extensions.getConfiguration().getConsumer().getSubscribe()
                    .getCallbackConcurrency());
        }
    }

    /**
     * 监听实例变化的Listener
     */
    private class InstanceChangeListener extends AbstractResourceEventListener {

        private final BiConsumer<ServiceChangeEvent, ServiceListener> consumer = (event, listener) -> {
            WatchFlow.this.executor.execute(event.getServiceKey(), () -> {
                listener.onEvent(event);
            });
        };

        @Override
        public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue,
                                      RegistryCacheValue newValue) {
            if (newValue.getEventType() != ServiceEventKey.EventType.INSTANCE) {
                return;
            }
            if (oldValue instanceof ServiceInstancesByProto && newValue instanceof ServiceInstancesByProto) {
                LOG.debug("receive service={} change event", svcEventKey);
                ServiceInstancesByProto oldIns = (ServiceInstancesByProto) oldValue;
                ServiceInstancesByProto newIns = (ServiceInstancesByProto) newValue;
                ServiceChangeEvent event = ServiceChangeEvent.builder()
                        .serviceKey(svcEventKey.getServiceKey())
                        .addInstances(Utils.checkAddInstances(oldIns, newIns))
                        .updateInstances(Utils.checkUpdateInstances(oldIns, newIns))
                        .deleteInstances(Utils.checkDeleteInstances(oldIns, newIns))
                        .allInstances(newIns.getInstances())
                        .build();

                Set<ServiceListener> listeners = watchers.getOrDefault(svcEventKey.getServiceKey(), Collections.emptySet());
                listeners.forEach(serviceListener -> consumer.accept(event, serviceListener));
            }
        }
    }

    /**
     * 由于使用移步处理ServiceChangeEvent，为了保证同一个Service下的Event按事件发生顺序通知给Listener，需要保证同一个Service
     * 的Event是由同一个Thread进行处理
     */
    private static class DispatchExecutor {

        private final Executor[] executors;

        public DispatchExecutor(int nThread) {
            if (nThread < 1) {
                nThread = 1;
            }

            this.executors = new Executor[nThread];

            for (int i = 0; i < nThread; i++) {
                this.executors[i] = Executors.newFixedThreadPool(1,
                        new NamedThreadFactory("service-watch-dispatch" + i));
            }
        }

        public void execute(ServiceKey serviceKey, Runnable command) {
            int code = serviceKey.hashCode();
            Executor executor = executors[code % executors.length];
            executor.execute(command);
        }
    }
}
