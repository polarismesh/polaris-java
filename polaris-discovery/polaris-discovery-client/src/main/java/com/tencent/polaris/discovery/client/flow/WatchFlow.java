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

import static com.tencent.polaris.logging.LoggingConsts.LOGGING_UPDATE_EVENT_ASYNC;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.listener.ServiceListener;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.registry.AbstractResourceEventListener;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceChangeEvent;
import com.tencent.polaris.api.pojo.ServiceChangeEvent.OneInstanceUpdate;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.WatchServiceResponse;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.ServiceInstancesByProto;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.client.util.Utils;
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
import org.slf4j.Logger;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 * @author Haotian Zhang
 */
public class WatchFlow {

    private static final Logger LOG = LoggerFactory.getLogger(SyncFlow.class);
    private static final Logger UPDATE_EVENT_LOG = LoggerFactory.getLogger(LOGGING_UPDATE_EVENT_ASYNC);
    private static final Map<ServiceKey, Set<ServiceListener>> watchers = new ConcurrentHashMap<>();
    private final AtomicBoolean initialize = new AtomicBoolean(false);
    private Extensions extensions;
    private SyncFlow syncFlow;
    private DispatchExecutor executor;

    public void init(Extensions extensions, SyncFlow syncFlow) {
        this.extensions = extensions;
        this.syncFlow = syncFlow;
        initFlow();
    }

    /**
     * 监听服务下的实例变化
     *
     * @return WatchServiceResponse
     */
    public WatchServiceResponse commonWatchService(CommonWatchServiceRequest request) throws PolarisException {
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
            int code = Math.abs(serviceKey.hashCode());
            Executor executor = executors[code % executors.length];
            executor.execute(command);
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
                logChangeInstances(svcEventKey, event, oldIns, newIns);
                Set<ServiceListener> listeners = watchers.getOrDefault(svcEventKey.getServiceKey(),
                        Collections.emptySet());
                listeners.forEach(serviceListener -> consumer.accept(event, serviceListener));
            }
        }

        /**
         * Print change of instances.
         *
         * @param svcEventKey
         * @param event
         * @param oldIns
         * @param newIns
         */
        private void logChangeInstances(ServiceEventKey svcEventKey, ServiceChangeEvent event,
                ServiceInstancesByProto oldIns, ServiceInstancesByProto newIns) {
            UPDATE_EVENT_LOG.info(
                    "service instances of {} change, oldRevision {}, newRevision {}, oldCount {}, newCount {}.",
                    svcEventKey, oldIns.getRevision(), newIns.getRevision(), oldIns.getInstances().size(),
                    newIns.getInstances().size());
            // Added instances.
            for (Instance addInst : event.getAddInstances()) {
                UPDATE_EVENT_LOG.info("add instance of {}: [{}:{}, status: {}].",
                        svcEventKey, addInst.getHost(), addInst.getPort(), totalInstanceInfo(addInst));
            }
            // Updated instances.
            for (OneInstanceUpdate oneInstanceUpdate : event.getUpdateInstances()) {
                UPDATE_EVENT_LOG.info("modify instance of {} from [{}:{}, status: {}] to [{}:{}, status: {}].",
                        svcEventKey, oneInstanceUpdate.getBefore().getHost(), oneInstanceUpdate.getBefore().getPort(),
                        totalInstanceInfo(oneInstanceUpdate.getBefore()), oneInstanceUpdate.getAfter().getHost(),
                        oneInstanceUpdate.getAfter().getPort(), totalInstanceInfo(oneInstanceUpdate.getAfter()));
            }
            // Deleted instances.
            for (Instance delInst : event.getDeleteInstances()) {
                UPDATE_EVENT_LOG.info("delete instance of {}: [{}:{}, status: {}].",
                        svcEventKey, delInst.getHost(), delInst.getPort(), totalInstanceInfo(delInst));
            }
        }

        /**
         * Generate info of instance.
         *
         * @param instance
         * @return
         */
        private String totalInstanceInfo(Instance instance) {
            return String.format("healthy:%s;isolate:%s;weight:%s", instance.isHealthy(), instance.isIsolated(),
                    instance.getWeight());
        }
    }
}
