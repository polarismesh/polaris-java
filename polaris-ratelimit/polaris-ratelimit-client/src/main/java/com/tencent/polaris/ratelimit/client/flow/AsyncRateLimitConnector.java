package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 * 连接器，单线程调用，不考虑并发
 */
public class AsyncRateLimitConnector {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncRateLimitConnector.class);

    private final Object counterSetLock = new Object();

    /**
     * 节点到客户端连接
     */
    private final Map<HostIdentifier, StreamCounterSet> hostToStream = new HashMap<>();

    /**
     * uniqueKey到客户端连接
     */
    private final Map<String, StreamCounterSet> uniqueKeyToStream = new HashMap<>();

    private final List<String> coreRouters = new ArrayList<>();


    public AsyncRateLimitConnector() {
        coreRouters.add(ServiceRouterConfig.DEFAULT_ROUTER_METADATA);
    }

    /**
     * 获取连接流对象
     *
     * @param extensions 插件容器
     * @param remoteCluster 远程限流集群名
     * @param uniqueKey 唯一主键
     * @param serviceIdentifier 服务标识
     * @return 连接流对象
     */
    public StreamCounterSet getStreamCounterSet(Extensions extensions, ServiceKey remoteCluster,
            ServiceInstances remoteAddresses, String uniqueKey, ServiceIdentifier serviceIdentifier) {
        HostIdentifier hostIdentifier = getServiceInstance(extensions, remoteCluster, remoteAddresses, uniqueKey);
        if (hostIdentifier == null) {
            LOG.error("[getStreamCounterSet] ratelimit cluster service not found.");
            return null;
        }
        StreamCounterSet streamCounterSet = uniqueKeyToStream.get(uniqueKey);
        if (null != streamCounterSet && streamCounterSet.getIdentifier().equals(hostIdentifier)) {
            return streamCounterSet;
        }
        synchronized (counterSetLock) {
            if (null != streamCounterSet && streamCounterSet.getIdentifier().equals(hostIdentifier)) {
                return streamCounterSet;
            }
            if (null != streamCounterSet) {
                //切换了节点，去掉初始化记录
                streamCounterSet.deleteInitRecord(serviceIdentifier);
                //切换了节点，老的不再使用
                if (streamCounterSet.decreaseReference()) {
                    hostToStream.remove(hostIdentifier);
                }
            }
            streamCounterSet = hostToStream.get(hostIdentifier);
            if (null == streamCounterSet) {
                streamCounterSet = new StreamCounterSet(hostIdentifier);
            }
            streamCounterSet.addReference();
            hostToStream.put(hostIdentifier, streamCounterSet);
            uniqueKeyToStream.put(uniqueKey, streamCounterSet);
            return streamCounterSet;
        }
    }

    /**
     * 一致性hash保证被调正常的情况下，拿到的都是同一个节点
     *
     * @param remoteCluster 远程集群信息
     * @return 节点标识
     */
    private HostIdentifier getServiceInstance(Extensions extensions, ServiceKey remoteCluster,
            ServiceInstances remoteAddresses, String hashValue) {
        Instance instance;
        if (null != remoteCluster) {
            instance = BaseFlow
                    .commonGetOneInstance(extensions, remoteCluster, coreRouters,
                            LoadBalanceConfig.LOAD_BALANCE_RING_HASH,
                            "grpc", hashValue);
        } else {
            LoadBalancer loadBalancer = (LoadBalancer) extensions.getPlugins()
                    .getPlugin(PluginTypes.LOAD_BALANCER.getBaseType(), LoadBalanceConfig.LOAD_BALANCE_RING_HASH);
            Criteria criteria = new Criteria();
            criteria.setHashKey(hashValue);
            instance = BaseFlow.processLoadBalance(loadBalancer, criteria, remoteAddresses,
                    extensions.getWeightAdjusters());
        }
        if (instance == null) {
            LOG.error("can not found any instance by serviceKye:{}", remoteCluster);
            return null;
        }
        String host = instance.getHost();
        int port = instance.getPort();
        return new HostIdentifier(host, port);
    }

}
