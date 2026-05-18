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

package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.ratelimiter.RemoteQuotaInfo;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindow.WindowStatus;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import com.tencent.polaris.specification.api.v1.traffic.manage.ratelimiter.RateLimitGRPCV2Grpc;
import com.tencent.polaris.specification.api.v1.traffic.manage.ratelimiter.RateLimitGRPCV2Grpc.RateLimitGRPCV2BlockingStub;
import com.tencent.polaris.specification.api.v1.traffic.manage.ratelimiter.RateLimitGRPCV2Grpc.RateLimitGRPCV2Stub;
import com.tencent.polaris.specification.api.v1.traffic.manage.ratelimiter.RateLimiterProto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 与远端的连接资源
 */
public class StreamResource implements StreamObserver<RateLimitResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(StreamResource.class);

    private final AtomicBoolean endStream = new AtomicBoolean(false);

    /**
     * 最后一次链接失败的时间
     */
    private final AtomicLong lastConnectFailTimeMilli = new AtomicLong(0);

    /**
     * 节点的标识
     */
    private final Node hostNode;

    /**
     * 连接
     */
    private final ManagedChannel channel;

    /**
     * GRPC stream客户端
     */
    private final StreamObserver<RateLimitRequest> streamClient;

    /**
     * 同步的客户端
     */
    private final RateLimitGRPCV2BlockingStub client;

    /**
     * 初始化记录
     */
    private final Map<ServiceIdentifier, InitializeRecord> initRecord = new ConcurrentHashMap<>();


    /**
     * report使用的
     */
    private final Map<Integer, DurationBaseCallback> counters = new ConcurrentHashMap<>();

    /**
     * 最近更新时间
     */
    private final AtomicLong lastRecvTime = new AtomicLong(0);

    /**
     * client key
     */
    private int clientKey;

    /**
     * 与服务端的时间差
     */
    private final AtomicLong timeDiffMilli = new AtomicLong();

    /**
     * 最后一次同步的时间戳
     */
    private final AtomicLong lastSyncTimeMilli = new AtomicLong();

    private final AtomicLong syncInterval = new AtomicLong(RateLimitConstants.TIME_ADJUST_INTERVAL_MS);

    public StreamResource(Node node) {
        channel = createConnection(node);
        hostNode = node;
        RateLimitGRPCV2Stub rateLimitGRPCV2Stub = RateLimitGRPCV2Grpc.newStub(channel);
        streamClient = rateLimitGRPCV2Stub.service(this);
        client = RateLimitGRPCV2Grpc.newBlockingStub(channel);
    }

    /**
     * 测试构造器：跳过 gRPC channel/stub 初始化，仅注入节点信息。
     * initRecord/counters 等字段由各自的 final 初始化器置默认值。
     */
    @JustForTest
    StreamResource(Node node, ManagedChannel channel, StreamObserver<RateLimitRequest> streamClient,
                   RateLimitGRPCV2BlockingStub client) {
        this.hostNode = node;
        this.channel = channel;
        this.streamClient = streamClient;
        this.client = client;
    }

    /**
     * 创建连接
     *
     * @return Connection对象
     */
    private ManagedChannel createConnection(Node node) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(node.getHost(), node.getPort())
                .usePlaintext();
        LOG.info("[ServerConnector]connection {} start to connect", node);
        return builder.build();
    }

    public Node getHostNode() {
        return hostNode;
    }

    /**
     * 关闭流
     *
     * @param closeSend 是否发送EOF
     */
    public void closeStream(boolean closeSend) {
        if (endStream.compareAndSet(false, true)) {
            if (closeSend && null != streamClient) {
                LOG.info("[ServerConnector]connection {} start to closeSend", hostNode);
                streamClient.onCompleted();
            }
            if (null != channel) {
                if (closeSend) {
                    channel.shutdown();       // 正常关闭：优雅等待
                } else {
                    channel.shutdownNow();    // 异常关闭：立即断开
                }
            }
            // 重置窗口最后初始化时间，清初始化窗口记录、上报索引记录
            initRecord.forEach((serviceIdentifier, record) -> record.getRateLimitWindow().setLastInitTimeMs(0));
            initRecord.clear();
            counters.clear();
        }
    }

    @Override
    public void onNext(RateLimitResponse rateLimitResponse) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("ratelimit response receive is {}", rateLimitResponse);
        }
        lastRecvTime.set(System.currentTimeMillis());
        if (RateLimitCmd.INIT.equals(rateLimitResponse.getCmd())) {
            handleRateLimitInitResponse(rateLimitResponse.getRateLimitInitResponse());
        } else if (RateLimitCmd.ACQUIRE.equals(rateLimitResponse.getCmd())) {
            if (!handleRateLimitReportResponse(rateLimitResponse.getRateLimitReportResponse())) {
                closeStream(true);
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.error("received error from server {}", hostNode, throwable);
        lastConnectFailTimeMilli.set(System.currentTimeMillis());
        closeStream(false);
    }

    @Override
    public void onCompleted() {
        LOG.error("received EOF from server {}", hostNode);
        closeStream(true);
    }

    public InitializeRecord getInitRecord(ServiceIdentifier serviceIdentifier, RateLimitWindow rateLimitWindow) {
        InitializeRecord record = initRecord.get(serviceIdentifier);
        if (record == null) {
            LOG.info("[RateLimit] add init record for {}, stream is {}", serviceIdentifier, this.hostNode);
            initRecord.putIfAbsent(serviceIdentifier, new InitializeRecord(rateLimitWindow));
            LOG.info("[RateLimit] record is null, write init record for task window is {} {} {}", rateLimitWindow,
                    rateLimitWindow.getUniqueKey(), rateLimitWindow.getStatus());
        } else if (record.getRateLimitWindow() != rateLimitWindow) {  // 存在旧窗口映射关系，说明已经淘汰
            initRecord.put(serviceIdentifier, new InitializeRecord(rateLimitWindow));
            RateLimitWindow oldWindow = record.getRateLimitWindow();
            LOG.warn("[RateLimit] remove init record for window {} {} {}, task window is {} {} {}", oldWindow,
                    oldWindow.getUniqueKey(), oldWindow.getStatus(), rateLimitWindow,
                    rateLimitWindow.getUniqueKey(), rateLimitWindow.getStatus());
        }
        return initRecord.get(serviceIdentifier);
    }

    public InitializeRecord deleteInitRecord(ServiceIdentifier serviceIdentifier) {
        LOG.info("[RateLimit] delete init record for {}, stream is {}", serviceIdentifier, this.hostNode);
        return initRecord.remove(serviceIdentifier);
    }

    // 淘汰时删除窗口初始化映射信息：仅当 record 的 window 与传入 window 匹配时才删
    public InitializeRecord deleteInitRecord(ServiceIdentifier serviceIdentifier, RateLimitWindow rateLimitWindow) {
        InitializeRecord record = initRecord.get(serviceIdentifier);
        if (record != null && record.getRateLimitWindow() == rateLimitWindow) {
            record.getDurationRecord().forEach((duration, counterKey) -> counters.remove(counterKey));
            initRecord.remove(serviceIdentifier);
            LOG.info("[RateLimit] delete init record for {}, window {}", serviceIdentifier, rateLimitWindow.getUniqueKey());
        } else if (record != null && record.getRateLimitWindow() != rateLimitWindow) {
            String recordWindow = record.getRateLimitWindow() != null ? record.getRateLimitWindow().getUniqueKey() : null;
            LOG.warn("[RateLimit] delete init record for {}, window {} failed with {}", serviceIdentifier, rateLimitWindow.getUniqueKey(), recordWindow);
        }
        return record;
    }

    /**
     * 处理初始化请求的response
     *
     * @param rateLimitInitResponse 初始化请求的返回结果
     */
    private void handleRateLimitInitResponse(RateLimitInitResponse rateLimitInitResponse) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("[handleRateLimitInitResponse] response:{}", rateLimitInitResponse);
        }

        if (rateLimitInitResponse.getCode() != ServerCodes.EXECUTE_SUCCESS) {
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
        RateLimitWindow rateLimitWindow = initializeRecord.getRateLimitWindow();
        // window 已 DELETED 时丢弃响应，避免污染已淘汰窗口（写 lastSyncTimeMs / counters / bucket）；
        // 但允许 INITIALIZING（首次 init）和 INITIALIZED（redoInit）两种状态——
        // PolarisRemoteSyncTask.doRemoteInit(true) 在 status 已 INITIALIZED 时仍会发 INIT，
        // 响应必须被接受才能刷新 counters，否则 hasInit 会一直 false 形成死循环。
        // 注：状态读 → 写入之间无锁，存在「读到 INITIALIZED 进入分支后被并发 unInit 标 DELETED」的 race；
        // 此时残留状态只污染已淘汰 window，不影响外部配额计算（getRateLimitWindow 会短路 expired container），
        // 接受这条最终一致性 race 而不引入 initLock 跨类暴露。
        WindowStatus currentStatus = rateLimitWindow.getStatus();
        if (currentStatus != WindowStatus.INITIALIZING && currentStatus != WindowStatus.INITIALIZED) {
            LOG.warn("[handleRateLimitInitResponse] drop response, window not in INITIALIZING/INITIALIZED. "
                            + "window {} {}, status {} ",
                    rateLimitWindow, rateLimitWindow.getUniqueKey(), currentStatus);
            return;
        }
        //重新初始化后，之前的记录就不要了
        initializeRecord.getDurationRecord().forEach((duration, counterKey) -> counters.remove(counterKey));
        initializeRecord.getDurationRecord().clear();
        long remoteQuotaTimeMilli = rateLimitInitResponse.getTimestamp();
        long localQuotaTimeMilli = getLocalTimeMilli(remoteQuotaTimeMilli);
        long currentTimeMs = System.currentTimeMillis();
        rateLimitWindow.setLastSyncTimeMs(currentTimeMs);
        rateLimitWindow.setLastInitTimeMs(0); // 重置上次初始化时间，从而在metric变更或上报失败时可再次立刻再初始化
        countersList.forEach(counter -> {
            initializeRecord.getDurationRecord().putIfAbsent(counter.getDuration(), counter.getCounterKey());
            DurationBaseCallback callback = new DurationBaseCallback(counter.getDuration(), rateLimitWindow);
            // 原实现 putIfAbsent + put 在两次调用之间存在 race，可能让旧 window 的 callback 短暂可见；
            // 用 compute 把「判断是否覆盖」和「写入」放进同一 bin lock，counterKey 路由由概率收敛改为确定收敛。
            // lambda 内仅做状态决策，把 LOG.warn 移到 lambda 外，避免 IO/格式化拖长 bin 锁持有时间。
            String[] replacedFromUniqueKey = new String[1];
            counters.compute(counter.getCounterKey(), (key, prev) -> {
                if (prev != null && prev.getRateLimitWindow() != rateLimitWindow) {
                    replacedFromUniqueKey[0] = prev.getRateLimitWindow().getUniqueKey();
                    return callback;
                }
                return prev != null ? prev : callback;
            });
            if (replacedFromUniqueKey[0] != null) {
                LOG.warn("[handleRateLimitInitResponse] replace counter from window {} to window {}, counterKey {}",
                        replacedFromUniqueKey[0], rateLimitWindow.getUniqueKey(), counter.getCounterKey());
            }
            RemoteQuotaInfo remoteQuotaInfo = new RemoteQuotaInfo(counter.getLeft(), counter.getClientCount(),
                    localQuotaTimeMilli, counter.getDuration() * 1000L);
            rateLimitWindow.getAllocatingBucket().onRemoteUpdate(remoteQuotaInfo);
        });
        // 仅在首次 init 完成时切换到 INITIALIZED；redoInit 场景下保持 INITIALIZED 不变
        if (currentStatus == WindowStatus.INITIALIZING) {
            LOG.info("[handleRateLimitInitResponse] window {} has turn to initialized", rateLimitWindow.getUniqueKey());
            rateLimitWindow.setStatus(WindowStatus.INITIALIZED.ordinal());
        }
    }

    /**
     * 处理acquire的回包
     *
     * @param rateLimitReportResponse report的回包
     */
    boolean handleRateLimitReportResponse(RateLimitReportResponse rateLimitReportResponse) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("[handleRateLimitReportResponse] response:{}", rateLimitReportResponse);
        }
        if (rateLimitReportResponse.getCode() != ServerCodes.EXECUTE_SUCCESS) {
            LOG.error("[handleRateLimitReportResponse] failed. code is {}", rateLimitReportResponse.getCode());
            return false;
        }
        List<QuotaLeft> quotaLeftsList = rateLimitReportResponse.getQuotaLeftsList();
        if (CollectionUtils.isEmpty(quotaLeftsList)) {
            LOG.error("[handleRateLimitReportResponse] quotaLefts is empty.");
            return true;
        }
        long remoteQuotaTimeMilli = rateLimitReportResponse.getTimestamp();
        long localQuotaTimeMilli = getLocalTimeMilli(remoteQuotaTimeMilli);
        quotaLeftsList.forEach(quotaLeft -> {
            DurationBaseCallback callback = counters.get(quotaLeft.getCounterKey());
            if (callback == null) {
                LOG.warn("[handleRateLimitReportResponse] callback not found for counterKey {}, may have been expired",
                        quotaLeft.getCounterKey());
                return;
            }
            RemoteQuotaInfo remoteQuotaInfo = new RemoteQuotaInfo(quotaLeft.getLeft(), quotaLeft.getClientCount(),
                    localQuotaTimeMilli, callback.getDuration() * 1000L);
            callback.getRateLimitWindow().getAllocatingBucket().onRemoteUpdate(remoteQuotaInfo);
            callback.getRateLimitWindow().setLastSyncTimeMs(System.currentTimeMillis());
        });
        return true;
    }

    public int getClientKey() {
        return clientKey;
    }

    public void setClientKey(int clientKey) {
        this.clientKey = clientKey;
    }

    private long getLocalTimeMilli(long remoteTimeMilli) {
        return remoteTimeMilli - timeDiffMilli.get();
    }

    public long getRemoteTimeMilli(long localTimeMilli) {
        return localTimeMilli + timeDiffMilli.get();
    }

    public void adjustTime() {
        long lastSyncTime = lastSyncTimeMilli.get();
        long currentTimeMillis = System.currentTimeMillis();

        //超过间隔时间才需要调整
        if (lastSyncTime > 0
                && currentTimeMillis - lastSyncTime < syncInterval.get()) {
            LOG.debug("[RateLimit] adjustTime need wait.lastSyncTimeMilli:{},sendTimeMilli:{}", lastSyncTimeMilli,
                    currentTimeMillis);
            return;
        }
        long localSendTimeMilli = System.currentTimeMillis();
        TimeAdjustRequest timeAdjustRequest = TimeAdjustRequest.newBuilder().build();
        TimeAdjustResponse timeAdjustResponse;
        try {
            timeAdjustResponse = client.withDeadlineAfter(1, TimeUnit.SECONDS).timeAdjust(timeAdjustRequest);
        } catch (Throwable e) {
            LOG.error("[RateLimit] fail to adjust time, err {}", e.getMessage());
            onError(e);
            return;
        }
        long localReceiveTimeMilli = System.currentTimeMillis();
        lastSyncTimeMilli.set(localReceiveTimeMilli);
        //服务端时间
        long remoteSendTimeMilli = timeAdjustResponse.getServerTimestamp();

        long latency = localReceiveTimeMilli - localSendTimeMilli;

        long remoteReceiveTimeMilli = remoteSendTimeMilli + latency / 3;
        long timeDiff = remoteReceiveTimeMilli - localReceiveTimeMilli;
        if (timeDiffMilli.get() == timeDiff && syncInterval.get() < RateLimitConstants.MAX_TIME_ADJUST_INTERVAL_MS) {
            //不断递增时延
            syncInterval.set(syncInterval.get() + RateLimitConstants.TIME_ADJUST_INTERVAL_MS);
        }
        timeDiffMilli.set(timeDiff);
        LOG.info("[RateLimit] adjust time to server time is {}, latency is {},diff is {}", remoteSendTimeMilli, latency,
                timeDiff);
    }

    public boolean isEndStream() {
        return endStream.get();
    }

    public boolean sendRateLimitRequest(RateLimitRequest rateLimitRequest) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("ratelimit request to send is {}", rateLimitRequest);
        }
        try {
            streamClient.onNext(rateLimitRequest);
            return true;
        } catch (Throwable e) {
            LOG.error("[RateLimit] fail to send request, err {}", e.getMessage());
            onError(e);
            return false;
        }
    }

    public boolean hasInit(ServiceIdentifier serviceIdentifier, RateLimitWindow rateLimitWindow) {
        InitializeRecord record = initRecord.get(serviceIdentifier);
        if (record == null || record.getDurationRecord().isEmpty()) {
            return false;
        }
        if (record.getRateLimitWindow() != rateLimitWindow) {
            record.getDurationRecord().forEach((duration, counterKey) -> counters.remove(counterKey));
            initRecord.remove(serviceIdentifier); // 清理索引触发重新初始化
            // warn 仅打 size，避免 ConcurrentHashMap.toString 在大流量场景下成为热点；debug 开启时打完整 map 便于排查
            if (LOG.isDebugEnabled()) {
                LOG.debug("[hasInit] init record removed for switched. initRecord {}, "
                                + "record window {} {}, param window {} {}",
                        initRecord, record.getRateLimitWindow(), record.getRateLimitWindow().getUniqueKey(),
                        rateLimitWindow, rateLimitWindow.getUniqueKey());
            } else {
                LOG.warn("[hasInit] init record removed for switched. initRecord size {}, "
                                + "record window {} {}, param window {} {}",
                        initRecord.size(), record.getRateLimitWindow(), record.getRateLimitWindow().getUniqueKey(),
                        rateLimitWindow, rateLimitWindow.getUniqueKey());
            }
            return false;
        }
        if (System.currentTimeMillis() - rateLimitWindow.getLastSyncTimeMs()
                > RateLimitConstants.WINDOW_INDEX_EXPIRE_TIME) {
            record.getDurationRecord().forEach((duration, counterKey) -> counters.remove(counterKey));
            initRecord.remove(serviceIdentifier); // 清理索引触发重新初始化
            LOG.warn("[hasInit] init record is removed for expired. last sync time {}. "
                            + "record window {} {}, param window {} {}. ", rateLimitWindow.getLastSyncTimeMs(),
                    record.getRateLimitWindow(), record.getRateLimitWindow().getUniqueKey(), rateLimitWindow,
                    rateLimitWindow.getUniqueKey());
            return false;
        }
        return true;
    }

    public Integer getCounterKey(ServiceIdentifier serviceIdentifier, Integer duration) {
        InitializeRecord initializeRecord = initRecord.get(serviceIdentifier);
        if (null == initializeRecord) {
            return null;
        }
        return initializeRecord.getDurationRecord().get(duration);
    }

    @JustForTest
    Map<ServiceIdentifier, InitializeRecord> getInitRecord() {
        return initRecord;
    }

    @JustForTest
    Map<Integer, DurationBaseCallback> getCounters() {
        return counters;
    }

}
