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
     * 创建连接
     *
     * @return Connection对象
     */
    private ManagedChannel createConnection(Node node) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(node.getHost(), node.getPort())
                .usePlaintext();
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
                channel.shutdown();
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
        } else if (record.getRateLimitWindow() != rateLimitWindow) {  // 存在旧窗口映射关系，说明已经淘汰
            initRecord.put(serviceIdentifier, new InitializeRecord(rateLimitWindow));
            RateLimitWindow oldWindow = record.getRateLimitWindow();
            LOG.info("remove init record for window {} {}", oldWindow.getUniqueKey(), oldWindow.getStatus());
        }
        return initRecord.get(serviceIdentifier);
    }

    public void deleteInitRecord(ServiceIdentifier serviceIdentifier) {
        LOG.info("[RateLimit] delete init record for {}, stream is {}", serviceIdentifier, this.hostNode);
        initRecord.remove(serviceIdentifier);
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
        //重新初始化后，之前的记录就不要了
        initializeRecord.getDurationRecord().clear();
        long remoteQuotaTimeMilli = rateLimitInitResponse.getTimestamp();
        long localQuotaTimeMilli = getLocalTimeMilli(remoteQuotaTimeMilli);
        RateLimitWindow rateLimitWindow = initializeRecord.getRateLimitWindow();
        rateLimitWindow.setLastInitTimeMs(0); // 重置上次初始化时间，从而在metric变更或上报失败时可再次立刻再初始化
        countersList.forEach(counter -> {
            initializeRecord.getDurationRecord().putIfAbsent(counter.getDuration(), counter.getCounterKey());
            counters.putIfAbsent(counter.getCounterKey(),
                    new DurationBaseCallback(counter.getDuration(), rateLimitWindow));
            RemoteQuotaInfo remoteQuotaInfo = new RemoteQuotaInfo(counter.getLeft(), counter.getClientCount(),
                    localQuotaTimeMilli, counter.getDuration() * 1000);
            rateLimitWindow.getAllocatingBucket().onRemoteUpdate(remoteQuotaInfo);
        });
        LOG.info("[RateLimit] window {} has turn to initialized", rateLimitWindow.getUniqueKey());
        rateLimitWindow.setStatus(WindowStatus.INITIALIZED.ordinal());
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
            RemoteQuotaInfo remoteQuotaInfo = new RemoteQuotaInfo(quotaLeft.getLeft(), quotaLeft.getClientCount(),
                    localQuotaTimeMilli, callback.getDuration() * 1000);
            callback.getRateLimitWindow().getAllocatingBucket().onRemoteUpdate(remoteQuotaInfo);
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
            timeAdjustResponse = client.timeAdjust(timeAdjustRequest);
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

    public boolean hasInit(ServiceIdentifier serviceIdentifier) {
        return initRecord.containsKey(serviceIdentifier);
    }

    public Integer getCounterKey(ServiceIdentifier serviceIdentifier, Integer duration) {
        InitializeRecord initializeRecord = initRecord.get(serviceIdentifier);
        if (null == initializeRecord) {
            return null;
        }
        return initializeRecord.getDurationRecord().get(duration);
    }

}
