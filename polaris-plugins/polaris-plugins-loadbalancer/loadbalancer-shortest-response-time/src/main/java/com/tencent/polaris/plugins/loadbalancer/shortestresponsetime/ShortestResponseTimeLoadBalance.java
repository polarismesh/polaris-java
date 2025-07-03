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

package com.tencent.polaris.plugins.loadbalancer.shortestresponsetime;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.pojo.DefaultServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceStatistic;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public class ShortestResponseTimeLoadBalance extends Destroyable implements LoadBalancer, PluginConfigProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ShortestResponseTimeLoadBalance.class);

    Extensions extensions;

    LocalRegistry localRegistry;

    private long slidePeriod;

    private final ConcurrentMap<Instance, SlideWindowData> instanceMap = new ConcurrentHashMap<>();

    private final AtomicBoolean onResetSlideWindow = new AtomicBoolean(false);

    private volatile long lastUpdateTime = System.currentTimeMillis();

    private ExecutorService executorService;

    /**
     * Sliding window data class, used to record instance statistic offsets
     */
    protected class SlideWindowData {

        /**
         * Offset of successful calls
         */
        private long succeededOffset;
        /**
         * Offset of successful call elapsed time
         */
        private long succeededElapsedOffset;

        /**
         * Constructor, initializes offsets to 0
         */
        public SlideWindowData() {
            this.succeededOffset = 0;
            this.succeededElapsedOffset = 0;
        }

        /**
         * Reset sliding window data
         */
        public void reset(InstanceStatistic instanceStatistic) {
            if (instanceStatistic != null) {
                this.succeededOffset = instanceStatistic.getSucceededCount();
                this.succeededElapsedOffset = instanceStatistic.getSucceededElapsed();
            }
        }

        /**
         * Calculate average elapsed time within the sliding window
         */
        public long getSucceededAverageElapsed(InstanceStatistic instanceStatistic) {
            if (instanceStatistic.getSucceededCount() - this.succeededOffset == 0) {
                return 0;  // Returns 0 if there are no requests
            }
            return (instanceStatistic.getSucceededElapsed() - this.succeededElapsedOffset) / (
                    instanceStatistic.getSucceededCount() - this.succeededOffset);
        }
    }

    @Override
    public Instance chooseInstance(Criteria criteria, ServiceInstances instances) throws PolarisException {
        if (instances == null || CollectionUtils.isEmpty(instances.getInstances())) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND, "instances is empty");
        }
        ServiceKey serviceKey = instances.getServiceKey();
        List<Instance> requestInstanceList = instances.getInstances();
        Map<String, Instance> requestInstanceMap = new HashMap<>();
        requestInstanceList.forEach(
                instance -> requestInstanceMap.put(instance.getHost() + ":" + instance.getPort(), instance));
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.INSTANCE);
        DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
        Set<ServiceEventKey> serviceEventKeySet = new HashSet<>();
        serviceEventKeySet.add(serviceEventKey);
        svcKeysProvider.setSvcEventKeys(serviceEventKeySet);
        DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();

        ResourcesResponse resourcesResponse = BaseFlow.syncGetResources(extensions, false, svcKeysProvider,
                engineFlowControlParam);
        ServiceInstances serviceInstances = resourcesResponse.getServiceInstances(serviceEventKey);
        List<Instance> localInstanceList = serviceInstances.getInstances();
        if (CollectionUtils.isEmpty(localInstanceList)) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND, "local instances is empty");
        }
        // intersection lookup
        List<Instance> instanceList = localInstanceList.stream()
                .filter(instance -> requestInstanceMap.containsKey(instance.getHost() + ":" + instance.getPort()))
                .collect(Collectors.toList());
        if (instanceList.isEmpty()) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND,
                    "No instance found. serviceKey=" + serviceKey.toString());
        }
        int length = instanceList.size();
        long[] instanceElapsed = new long[length];
        long[] instanceCount = new long[length];

        // calculate instance avg elapsed time and count in slide window
        for (int i = 0; i < length; i++) {
            InstanceByProto instance = (InstanceByProto) instanceList.get(i);
            InstanceStatistic instanceStatistic = Optional.ofNullable(
                            instance.getInstanceLocalValue().getInstanceStatistic())
                    .orElse(new InstanceStatistic());
            SlideWindowData slideWindowData = instanceMap.computeIfAbsent(instance, k -> new SlideWindowData());
            instanceElapsed[i] = slideWindowData.getSucceededAverageElapsed(instanceStatistic);
            instanceCount[i] = instanceStatistic.getSucceededCount() - slideWindowData.succeededOffset;
        }
        // calculate instance weight
        long totalWeight = 0;
        long[] instanceWeight = new long[length];

        long base = 100000;
        for (int i = 0; i < length; i++) {
            // weight = base * 100 if elapsed = 0
            // weight = base / elapsed if elapsed > 0
            // if an instance elapsed over base time, weight will be 0
            instanceWeight[i] = (instanceElapsed[i] == 0) ? base * 100 : base / instanceElapsed[i];
            totalWeight += instanceWeight[i];
        }
        // random select instance based on calculated weight
        long randomWeight = (long) (Math.random() * totalWeight);
        long currentWeight = 0;
        Instance targetInstance = instanceList.get(0);
        for (int i = 0; i < length; i++) {
            if (instanceWeight[i] == 0) {
                continue;
            }
            currentWeight += instanceWeight[i];
            if (currentWeight >= randomWeight) {
                targetInstance = instanceList.get(i);
                break;
            }
        }
        if (System.currentTimeMillis() - lastUpdateTime > slidePeriod
                && onResetSlideWindow.compareAndSet(false, true)) {
            // Reset slideWindowData asynchronously
            executorService.execute(() -> {
                LOG.info(String.format(
                        "[ShortestResponseTimeLoadBalance] Refresh windows. \nIn last window: Instances called: %s , avg elapsed :%s",
                        Arrays.toString(instanceCount), Arrays.toString(instanceElapsed)));
                instanceMap.forEach((instance, slideWindowData) -> {
                    InstanceStatistic instanceStatistic = ((InstanceByProto) instance).getInstanceLocalValue()
                            .getInstanceStatistic();
                    slideWindowData.reset(instanceStatistic);
                });
                lastUpdateTime = System.currentTimeMillis();
                onResetSlideWindow.set(false);
            });
        }
        return requestInstanceMap.get(targetInstance.getHost() + ":" + targetInstance.getPort());
    }

    @Override
    public PluginType getType() {
        return PluginTypes.LOAD_BALANCER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        this.executorService = Executors.newSingleThreadExecutor();

    }

    @Override
    public String getName() {
        return LoadBalanceConfig.LOAD_BALANCE_SHORTEST_RESPONSE_TIME;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return ShortestResponseTimeLoadBalanceConfig.class;
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        extensions = ctx;
        localRegistry = ctx.getLocalRegistry();
        slidePeriod = ctx.getConfiguration().getConsumer().getLoadbalancer().getPluginConfig(getName(),
                ShortestResponseTimeLoadBalanceConfig.class).getSlidePeriod();

    }
}