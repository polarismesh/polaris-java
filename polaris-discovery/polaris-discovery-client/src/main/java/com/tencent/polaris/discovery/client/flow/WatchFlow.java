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
import com.tencent.polaris.client.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class WatchFlow {

    private static final Logger LOG = LoggerFactory.getLogger(SyncFlow.class);

    private Extensions extensions;

    private SyncFlow syncFlow;

    private static final Map<ServiceKey, Set<ServiceListener>> watchers = new ConcurrentHashMap<>();

    public void init(Extensions extensions, SyncFlow syncFlow) {
        this.extensions = extensions;
        this.syncFlow = syncFlow;
    }

    /**
     * 监听服务下的实例变化
     *
     * @return
     */
    public WatchServiceResponse commonWatchService(CommonWatchServiceRequest request) throws PolarisException{
        if (request.isWatch()) {
            return watchService(request);
        } else {
            return unWatchService(request);
        }
    }

    private WatchServiceResponse watchService(CommonWatchServiceRequest request) throws PolarisException {

        ServiceKey serviceKey = request.getSvcEventKey().getServiceKey();

        InstancesResponse response = syncFlow.commonSyncGetAllInstances(request.getAllRequest());

        watchers.computeIfAbsent(request.getSvcEventKey().getServiceKey(),
                key -> Collections.synchronizedSet(new HashSet<>()));
        boolean result = watchers.get(serviceKey).addAll(request.getWatchServiceRequest().getListeners());

        extensions.getLocalRegistry().registerResourceListener(new InstanceChangeListener(serviceKey));

        return new WatchServiceResponse(response, result);
    }

    private WatchServiceResponse unWatchService(CommonWatchServiceRequest request) {
        boolean result = true;

        Set<ServiceListener> listeners = watchers.get(request.getSvcEventKey().getServiceKey());

        if (CollectionUtils.isNotEmpty(listeners)) {
            result = listeners.removeAll(request.getWatchServiceRequest().getListeners());
        }

        return new WatchServiceResponse(null, result);
    }

    private static class InstanceChangeListener extends AbstractResourceEventListener {

        private final ServiceKey serviceKey;

        public InstanceChangeListener(ServiceKey serviceKey) {
            this.serviceKey = serviceKey;
        }

        @Override
        public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue, RegistryCacheValue newValue) {
            if (newValue.getEventType() != ServiceEventKey.EventType.INSTANCE) {
                return;
            }
            if (oldValue instanceof ServiceInstancesByProto && newValue instanceof ServiceInstancesByProto) {
                ServiceInstancesByProto oldIns = (ServiceInstancesByProto) oldValue;
                ServiceInstancesByProto newIns = (ServiceInstancesByProto) newValue;
                ServiceChangeEvent event = ServiceChangeEvent.Builder()
                        .addInstances(Utils.checkAddInstances(oldIns, newIns))
                        .updateInstances(Utils.checkUpdateInstances(oldIns, newIns))
                        .deleteInstances(Utils.checkDeleteInstances(oldIns, newIns))
                        .build();

                watchers.getOrDefault(serviceKey, Collections.emptySet())
                        .forEach(serviceListener -> serviceListener.onEvent(event));
            }
        }

        @Override
        public void onResourceDeleted(ServiceEventKey svcEventKey, RegistryCacheValue oldValue) {
            if (oldValue.getEventType() != ServiceEventKey.EventType.INSTANCE) {
                return;
            }

            if (oldValue instanceof ServiceInstancesByProto) {

            }
        }
    }
}
