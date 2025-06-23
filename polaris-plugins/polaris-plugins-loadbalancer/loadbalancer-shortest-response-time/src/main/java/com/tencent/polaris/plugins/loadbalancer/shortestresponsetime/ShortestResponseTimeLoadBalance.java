package com.tencent.polaris.plugins.loadbalancer.shortestresponsetime;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceStatistic;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
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

    LocalRegistry localRegistry;

    private long slidePeriod = 10000;

    private final ConcurrentMap<Instance, SlideWindowData> instanceMap = new ConcurrentHashMap<>();

    private final AtomicBoolean onResetSlideWindow = new AtomicBoolean(false);

    private volatile long lastUpdateTime = System.currentTimeMillis();

    private ExecutorService executorService;

    protected class SlideWindowData {

        private long succeededOffset;
        private long succeededElapsedOffset;


        public SlideWindowData() {
            this.succeededOffset = 0;
            this.succeededElapsedOffset = 0;
        }

        public void reset(InstanceStatistic instanceStatistic) {
            if (instanceStatistic != null) {
                this.succeededOffset = instanceStatistic.getSucceededCount();
                this.succeededElapsedOffset = instanceStatistic.getSucceededElapsed();
            }

        }
        public void update(InstanceStatistic instanceStatistic) {
            if (instanceStatistic != null) {
                long avgElapsed = getSucceededAverageElapsed(instanceStatistic);
                this.succeededOffset++;
                this.succeededElapsedOffset += avgElapsed;
            }
        }
        public long getSucceededAverageElapsed(InstanceStatistic instanceStatistic) {
            if (instanceStatistic != null) {
                long succeed = instanceStatistic.getSucceededCount() - this.succeededOffset;
                if (succeed == 0) {
                    return 0;
                }
                return (instanceStatistic.getSucceededElapsed() - this.succeededElapsedOffset) / succeed;
            }else{
                return 0;
            }

        }
    }

    @Override
    public Instance chooseInstance(Criteria criteria, ServiceInstances instances) throws PolarisException {
        ServiceKey serviceKey = instances.getServiceKey();
        List<Instance> requestInstanceList = instances.getInstances();
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.INSTANCE);
        List<Instance> localInstanceList = localRegistry.getInstances(new ResourceFilter(serviceEventKey,true,true)).getInstances();

        // 使用Set优化交集查找
        Set<String> instanceKeys = requestInstanceList.stream()
            .map(instance -> instance.getHost() + ":" + instance.getPort())
            .collect(Collectors.toSet());

        List<Instance> instanceList = localInstanceList.stream()
            .filter(instance -> instanceKeys.contains(instance.getHost() + ":" + instance.getPort()))
            .collect(Collectors.toList());

        int length = instanceList.size();
        long[] instanceElapsed = new long[length];
        long[] instanceCount = new long[length];
        for (int i = 0; i < length; i++) {
            InstanceByProto instance = (InstanceByProto) instanceList.get(i);
            InstanceStatistic instanceStatistic = Optional.ofNullable(instance.getInstanceLocalValue().getInstanceStatistic())
                .orElse(new InstanceStatistic());
            SlideWindowData slideWindowData = instanceMap.computeIfAbsent(instance, k -> new SlideWindowData());
            instanceElapsed[i] = slideWindowData.getSucceededAverageElapsed(instanceStatistic);
            instanceCount[i] = instanceStatistic.getSucceededCount() - slideWindowData.succeededOffset;
        }


        long totalWeight = 0;
        long[] instanceWeight = new long[length];
        long base = 100000;
        for(int i = 0; i < length; i++) {
            instanceWeight[i] = (instanceElapsed[i] == 0) ? base * 100 : base / instanceElapsed[i];
            totalWeight += instanceWeight[i];
        }
        long randomWeight = (long) (Math.random() * totalWeight);
        long currentWeight = 0;
        Instance targetInstance = instanceList.get(0);
        for (int i = 0; i < length; i++) {
            currentWeight += instanceWeight[i];
            if (currentWeight >= randomWeight) {
                targetInstance = instanceList.get(i);
                break;
            }
        }

        if (System.currentTimeMillis() - lastUpdateTime > slidePeriod
                && onResetSlideWindow.compareAndSet(false, true)) {

            // reset slideWindowData in async way
            executorService.execute(() -> {
                LOG.info(String.format("[ShortestResponseTimeLoadBalance] Refresh windows. \nIn last window: Instances called: %s , avg elapsed :%s",Arrays.toString(instanceCount), Arrays.toString(instanceElapsed)));
                instanceMap.forEach((instance, slideWindowData) -> {
                    InstanceStatistic instanceStatistic = ((InstanceByProto)instance).getInstanceLocalValue().getInstanceStatistic();
                    slideWindowData.reset(instanceStatistic != null ? instanceStatistic : new InstanceStatistic());
                });
                lastUpdateTime = System.currentTimeMillis();
                onResetSlideWindow.set(false);
            });
        }

        return targetInstance;
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
        localRegistry = ctx.getLocalRegistry();
        slidePeriod = ctx.getConfiguration().getConsumer().getLoadbalancer().getPluginConfig(getName(),
                ShortestResponseTimeLoadBalanceConfig.class).getSlidePeriod();

    }
}
