package com.tencent.polaris.discovery.client.stat;

import static com.tencent.polaris.api.plugin.registry.InstanceProperty.PROPERTY_INSTANCE_STATISTIC;
import static com.tencent.polaris.api.pojo.RetStatus.RetSuccess;

import com.tencent.polaris.api.plugin.registry.InstanceProperty;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.plugin.registry.ServiceUpdateRequest;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.InstanceLocalValue;
import com.tencent.polaris.api.pojo.InstanceStatistic;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class InstancesStatisticUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(InstancesDetectTask.class);

    private final LocalRegistry localRegistry;

    public InstancesStatisticUpdater(LocalRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }
    public void updateInstanceStatistic(InstanceGauge result) {
        ServiceKey serviceKey = new ServiceKey(result.getNamespace(), result.getService());
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.INSTANCE);
        ServiceInstances serviceInstances = localRegistry.getInstances
                (new ResourceFilter(serviceEventKey,true,true));
        List<Instance> instances = serviceInstances.getInstances();
        InstanceByProto targetInstance = null;
        for (Instance instance : instances) {
            if (instance.getHost().equals(result.getHost())&&instance.getPort()==result.getPort()) {
                targetInstance = (InstanceByProto) instance;
                break;
            }
        }
        if (targetInstance!=null) {
            InstanceLocalValue localValue = targetInstance.getInstanceLocalValue();

            InstanceStatistic instanceStatistic = localValue.getInstanceStatistic();
            if (instanceStatistic == null){
                instanceStatistic = new InstanceStatistic();
                instanceStatistic.count(result.getDelay(), RetSuccess.equals(result.getRetStatus()));
                List<InstanceProperty> instanceProperties = new ArrayList<>();
                Map<String, Object> properties = new HashMap<>();
                properties.put(PROPERTY_INSTANCE_STATISTIC, instanceStatistic);
                InstanceProperty instanceProperty = new InstanceProperty(targetInstance, properties);
                instanceProperties.add(instanceProperty);
                ServiceUpdateRequest request = new ServiceUpdateRequest(serviceKey, instanceProperties);
                localRegistry.updateInstances(request);
            }
            else{
                instanceStatistic.count(result.getDelay(), RetSuccess.equals(result.getRetStatus()));
            }
            LOG.debug("[InstanceStatisticUpdater]: "+targetInstance.getHost()+":"+targetInstance.getPort()+":"+result.getPort()+ ": Delay: "+
                    result.getDelay()+ "TotalCount" + instanceStatistic.getTotalCount()+ "TotalElapsed" + instanceStatistic.getTotalElapsed());

        }
    }
}
