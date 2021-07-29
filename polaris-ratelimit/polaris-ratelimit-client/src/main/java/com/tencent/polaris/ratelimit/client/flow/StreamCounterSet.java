package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.plugin.ratelimiter.RemoteQuotaInfo;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindow.WindowStatus;
import com.tencent.polaris.ratelimit.client.pb.RateLimitGRPCV2Grpc;
import com.tencent.polaris.ratelimit.client.pb.RateLimitGRPCV2Grpc.RateLimitGRPCV2BlockingStub;
import com.tencent.polaris.ratelimit.client.pb.RateLimitGRPCV2Grpc.RateLimitGRPCV2Stub;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.LimitTarget;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.QuotaCounter;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.QuotaLeft;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitCmd;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitInitResponse;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitReportResponse;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 计数器对象
 */
public class StreamCounterSet {

    private static final Logger LOG = LoggerFactory.getLogger(StreamCounterSet.class);

    /**
     * 引用的KEY
     */
    private final AtomicInteger reference = new AtomicInteger();

    /**
     * 异步通信器
     */
    private final AsyncRateLimitConnector asyncConnector;

    /**
     * 节点唯一标识
     */
    private final HostIdentifier identifier;

    /**
     * 当前的资源
     */
    private final AtomicReference<StreamResource> currentStreamResource = new AtomicReference<>();

    /**
     * 重连间隔时间
     */
    private final long reconnectInterval;

    /**
     * client key
     */
    private int clientKey;

    /**
     * 初始化记录
     */
    private final Map<ServiceIdentifier, InitializeRecord> initRecord = new HashMap<>();


    /**
     * report使用的
     */
    private final Map<Integer, DurationBaseCallback> counters = new HashMap<>();

    /**
     * 最近更新时间
     */
    private final AtomicLong lastRecvTime = new AtomicLong(0);

    public StreamCounterSet(AsyncRateLimitConnector asyncConnector, HostIdentifier identifier,
            Configuration configuration) {
        this.asyncConnector = asyncConnector;
        this.identifier = identifier;
        this.reconnectInterval = configuration.getGlobal().getServerConnector().getReconnectInterval();
    }

    public HostIdentifier getIdentifier() {
        return identifier;
    }

    private class StreamResource implements StreamObserver<RateLimitResponse> {

        private final AtomicBoolean endStream = new AtomicBoolean(false);

        /**
         * 最后一次链接失败的时间
         */
        private final AtomicLong lastConnectFailTimeMilli = new AtomicLong(0);

        /**
         * 连接
         */
        final ManagedChannel channel;

        /**
         * GRPC stream客户端
         */
        final StreamObserver<RateLimitRequest> streamClient;

        /**
         * 同步的客户端
         */
        final RateLimitGRPCV2BlockingStub client;

        public StreamResource(ManagedChannel channel,
                StreamObserver<RateLimitRequest> streamClient,
                RateLimitGRPCV2BlockingStub client) {
            this.channel = channel;
            this.streamClient = streamClient;
            this.client = client;
        }

        /**
         * 关闭流
         *
         * @param closeSend 是否发送EOF
         */
        public void closeStream(boolean closeSend) {
            if (endStream.compareAndSet(false, true)) {
                if (closeSend && null != streamClient) {
                    LOG.info("[ServerConnector]connection {} start to closeSend", identifier);
                    streamClient.onCompleted();
                }
                if (null != channel) {
                    channel.shutdown();
                }
            }
        }

        @Override
        public void onNext(RateLimitResponse rateLimitResponse) {
            lastRecvTime.set(System.currentTimeMillis());
            if (RateLimitCmd.INIT.equals(rateLimitResponse.getCmd())) {
                handleRateLimitInitResponse(rateLimitResponse.getRateLimitInitResponse());
            } else if (RateLimitCmd.ACQUIRE.equals(rateLimitResponse.getCmd())) {
                handleRateLimitReportResponse(rateLimitResponse.getRateLimitReportResponse());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            LOG.error("received error from server {}", identifier, throwable);
            lastConnectFailTimeMilli.set(System.currentTimeMillis());
            closeStream(false);
        }

        @Override
        public void onCompleted() {
            LOG.error("received EOF from server {}", identifier);
            closeStream(true);
        }
    }

    /**
     * 获取流式的异步客户端
     *
     * @param serviceIdentifier 服务标识
     * @param rateLimitWindow 限流窗口
     * @return 异步客户端
     */
    public StreamObserver<RateLimitRequest> preCheckAsync(ServiceIdentifier serviceIdentifier,
            RateLimitWindow rateLimitWindow) {
        StreamResource streamResource = checkAndCreateResource(serviceIdentifier, rateLimitWindow);
        if (null != streamResource) {
            return streamResource.streamClient;
        }
        return null;
    }

    /**
     * 获取阻塞的同步客户端
     *
     * @param serviceIdentifier 服务标识
     * @param rateLimitWindow 限流窗口
     * @return 同步客户端
     */
    public RateLimitGRPCV2BlockingStub preCheckSync(ServiceIdentifier serviceIdentifier,
            RateLimitWindow rateLimitWindow) {
        StreamResource streamResource = checkAndCreateResource(serviceIdentifier, rateLimitWindow);
        if (null != streamResource) {
            return streamResource.client;
        }
        return null;
    }

    /**
     * 获取同步阻塞的客户端
     *
     * @return 同步阻塞的客户端
     */
    private StreamResource checkAndCreateResource(ServiceIdentifier serviceIdentifier,
            RateLimitWindow rateLimitWindow) {
        StreamResource streamResource = currentStreamResource.get();
        if (null != streamResource && !streamResource.endStream.get()) {
            return streamResource;
        }
        long lastConnectFailTimeMilli = 0;
        if (null != streamResource) {
            lastConnectFailTimeMilli = streamResource.lastConnectFailTimeMilli.get();
        }
        ManagedChannel channel = createConnection(lastConnectFailTimeMilli);
        if (null == channel) {
            return null;
        }
        RateLimitGRPCV2Stub rateLimitGRPCV2Stub = RateLimitGRPCV2Grpc.newStub(channel);
        StreamObserver<RateLimitRequest> streamClient = rateLimitGRPCV2Stub.service(streamResource);
        RateLimitGRPCV2BlockingStub rateLimitGRPCV2BlockingStub = RateLimitGRPCV2Grpc.newBlockingStub(channel);
        streamResource = new StreamResource(channel, streamClient, rateLimitGRPCV2BlockingStub);
        currentStreamResource.set(streamResource);
        if (initRecord.get(serviceIdentifier) == null) {
            initRecord.putIfAbsent(serviceIdentifier, new InitializeRecord(rateLimitWindow));
        }
        return streamResource;
    }

    /**
     * 创建连接
     *
     * @return Connection对象
     */
    private ManagedChannel createConnection(long lastConnectFailTimeMilli) {
        long curTimeMilli = System.currentTimeMillis();
        long timePassed = curTimeMilli - lastConnectFailTimeMilli;
        if (lastConnectFailTimeMilli > 0 && timePassed > 0 && timePassed < this.reconnectInterval) {
            //未达到重连的时间间隔
            LOG.debug("reconnect interval should exceed {}", this.reconnectInterval);
            return null;
        }
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(identifier.getHost(), identifier.getPort())
                .usePlaintext();
        return builder.build();
    }

    /**
     * 是否已经初始哈
     *
     * @param serviceIdentifier 服务标识
     * @return hasInit
     */
    public boolean hasInit(ServiceIdentifier serviceIdentifier) {
        if (!initRecord.containsKey(serviceIdentifier)) {
            return false;
        }
        return initRecord.get(serviceIdentifier).getDurationRecord().size() > 0;
    }

    /**
     * 处理初始化请求的response
     *
     * @param rateLimitInitResponse 初始化请求的返回结果
     */
    void handleRateLimitInitResponse(RateLimitInitResponse rateLimitInitResponse) {
        LOG.debug("[handleRateLimitInitResponse] response:{}", rateLimitInitResponse);

        if (rateLimitInitResponse.getCode() != RateLimitConstants.SUCCESS) {
            LOG.error("[handleRateLimitInitResponse] failed. code is {}", rateLimitInitResponse.getCode());
            return;
        }
        LimitTarget target = rateLimitInitResponse.getTarget();
        ServiceIdentifier serviceIdentifier = new ServiceIdentifier(target.getService(), target.getNamespace(),
                target.getLabels());
        InitializeRecord initializeRecord = initRecord.get(serviceIdentifier);
        if (initializeRecord == null) {
            LOG.error("[handleRateLimitInitResponse] can not find init record:{}", serviceIdentifier);
            return;
        }

        //client key
        setClientKey(rateLimitInitResponse.getClientKey());

        List<QuotaCounter> countersList = rateLimitInitResponse.getCountersList();
        if (CollectionUtils.isEmpty(countersList)) {
            LOG.error("[handleRateLimitInitResponse] countersList is empty.");
            return;
        }
        //重新初始化后，之前的记录就不要了
        initializeRecord.getDurationRecord().clear();
        long serverTimeMilli = rateLimitInitResponse.getTimestamp() + asyncConnector.getTimeDiff().get();
        countersList.forEach(counter -> {
            initializeRecord.getDurationRecord().putIfAbsent(counter.getDuration(), counter.getCounterKey());
            getCounters().putIfAbsent(counter.getCounterKey(),
                    new DurationBaseCallback(counter.getDuration(), initializeRecord.getRateLimitWindow()));
            RemoteQuotaInfo remoteQuotaInfo = new RemoteQuotaInfo(counter.getLeft(), counter.getClientCount(),
                    serverTimeMilli, counter.getDuration() * 1000);
            initializeRecord.getRateLimitWindow().getAllocatingBucket().onRemoteUpdate(remoteQuotaInfo);
        });

        initializeRecord.getRateLimitWindow().setStatus(WindowStatus.INITIALIZED.ordinal());
    }

    /**
     * 处理acquire的回包
     *
     * @param rateLimitReportResponse report的回包
     */
    void handleRateLimitReportResponse(RateLimitReportResponse rateLimitReportResponse) {
        LOG.debug("[handleRateLimitReportRequest] response:{}", rateLimitReportResponse);
        if (rateLimitReportResponse.getCode() != RateLimitConstants.SUCCESS) {
            LOG.error("[handleRateLimitReportRequest] failed. code is {}", rateLimitReportResponse.getCode());
            return;
        }

        long serverTimeMilli = rateLimitReportResponse.getTimestamp();
        List<QuotaLeft> quotaLeftsList = rateLimitReportResponse.getQuotaLeftsList();
        if (CollectionUtils.isEmpty(quotaLeftsList)) {
            LOG.error("[handleRateLimitReportRequest] quotaLefts is empty.");
            return;
        }
        quotaLeftsList.forEach(quotaLeft -> {
            DurationBaseCallback callback = getCounters().get(quotaLeft.getCounterKey());
            RemoteQuotaInfo remoteQuotaInfo = new RemoteQuotaInfo(quotaLeft.getLeft(), quotaLeft.getClientCount(),
                    serverTimeMilli, callback.getDuration() * 1000);
            callback.getRateLimitWindow().getAllocatingBucket().onRemoteUpdate(remoteQuotaInfo);
        });
    }

    public int getClientKey() {
        return clientKey;
    }

    public void setClientKey(int clientKey) {
        this.clientKey = clientKey;
    }

    public Map<Integer, DurationBaseCallback> getCounters() {
        return counters;
    }

    public Map<ServiceIdentifier, InitializeRecord> getInitRecord() {
        return initRecord;
    }

    public void addReference() {
        reference.incrementAndGet();
    }

    public boolean decreaseReference() {
        int value = reference.decrementAndGet();
        if (value == 0) {
            StreamResource streamResource = currentStreamResource.get();
            if (null != streamResource) {
                streamResource.closeStream(true);
            }
            return true;
        }
        return false;
    }


}
