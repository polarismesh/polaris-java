package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

/**
 * 连接器，单线程调用，不考虑并发
 */
public class AsyncRateLimitConnector {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncRateLimitConnector.class);

    /**
     * 节点到客户端连接
     */
    private final Map<HostIdentifier, StreamCounterSet> hostToStream = new HashMap<>();

    /**
     * uniqueKey到客户端连接
     */
    private final Map<String, StreamCounterSet> uniqueKeyToStream = new HashMap<>();

    /**
     * 配置信息
     */
    private final Configuration configuration;

    /**
     * 与服务端的时间差
     */
    private final AtomicLong timeDiff = new AtomicLong();

    /**
     * 最后一次同步的时间戳
     */
    private final AtomicLong lastSyncTimeMilli = new AtomicLong();

    private final List<String> coreRouters = new ArrayList<>();


    public AsyncRateLimitConnector(Configuration configuration) {
        this.configuration = configuration;
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
    public StreamCounterSet getStreamCounterSet(Extensions extensions, ServiceKey remoteCluster, String uniqueKey,
            ServiceIdentifier serviceIdentifier) {
        HostIdentifier hostIdentifier = getServiceInstance(extensions, remoteCluster, uniqueKey);
        if (hostIdentifier == null) {
            LOG.error("[getStreamCounterSet] rate limit cluster service not found.");
            return null;
        }
        StreamCounterSet streamCounterSet = uniqueKeyToStream.get(uniqueKey);
        if (null != streamCounterSet) {
            if (streamCounterSet.getIdentifier().equals(hostIdentifier)) {
                return streamCounterSet;
            }
            //切换了节点，去掉初始化记录
            Map<ServiceIdentifier, InitializeRecord> initRecord = streamCounterSet.getInitRecord();
            if (null != initRecord) {
                initRecord.remove(serviceIdentifier);
            }
            //切换了节点，老的不再使用
            if (streamCounterSet.decreaseReference()) {
                hostToStream.remove(hostIdentifier);
            }
        }
        streamCounterSet = hostToStream.get(hostIdentifier);
        if (null == streamCounterSet) {
            streamCounterSet = new StreamCounterSet(this, hostIdentifier, configuration);
        }
        streamCounterSet.addReference();
        return streamCounterSet;
    }

    /**
     * 一致性hash保证被调正常的情况下，拿到的都是同一个节点
     *
     * @param remoteCluster 远程集群信息
     * @return 节点标识
     */
    private HostIdentifier getServiceInstance(Extensions extensions, ServiceKey remoteCluster, String hashValue) {
        Instance instance = BaseFlow
                .commonGetOneInstance(extensions, remoteCluster, coreRouters, LoadBalanceConfig.LOAD_BALANCE_RING_HASH,
                        "grpc", hashValue);
        if (instance == null) {
            LOG.error("can not found any instance by serviceKye:{}", remoteCluster);
            return null;
        }
        String host = instance.getHost();
        int port = instance.getPort();
        return new HostIdentifier(host, port);
    }

    public AtomicLong getTimeDiff() {
        return timeDiff;
    }

    public AtomicLong getLastSyncTimeMilli() {
        return lastSyncTimeMilli;
    }
}
