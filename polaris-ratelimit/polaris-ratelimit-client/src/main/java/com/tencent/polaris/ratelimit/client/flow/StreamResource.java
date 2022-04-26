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

package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.ratelimiter.RemoteQuotaInfo;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.logging.LoggerFactory;
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
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

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
    private final HostIdentifier hostIdentifier;

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

    public StreamResource(HostIdentifier identifier) {
        channel = createConnection(identifier);
        hostIdentifier = identifier;
        RateLimitGRPCV2Stub rateLimitGRPCV2Stub = RateLimitGRPCV2Grpc.newStub(channel);
        streamClient = rateLimitGRPCV2Stub.service(this);
        client = RateLimitGRPCV2Grpc.newBlockingStub(channel);
    }

    /**
     * 创建连接
     *
     * @return Connection对象
     */
    private ManagedChannel createConnection(HostIdentifier identifier) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(identifier.getHost(), identifier.getPort())
                .usePlaintext();
        return builder.build();
    }

    /**
     * 关闭流
     *
     * @param closeSend 是否发送EOF
     */
    public void closeStream(boolean closeSend) {
        if (endStream.compareAndSet(false, true)) {
            if (closeSend && null != streamClient) {
                LOG.info("[ServerConnector]connection {} start to closeSend", hostIdentifier);
                streamClient.onCompleted();
            }
            if (null != channel) {
                channel.shutdown();
            }
        }
    }

    @Override
    public void onNext(RateLimitResponse rateLimitResponse) {
        LOG.debug("ratelimit response receive is {}", rateLimitResponse);
        lastRecvTime.set(System.currentTimeMillis());
        if (RateLimitCmd.INIT.equals(rateLimitResponse.getCmd())) {
            handleRateLimitInitResponse(rateLimitResponse.getRateLimitInitResponse());
        } else if (RateLimitCmd.ACQUIRE.equals(rateLimitResponse.getCmd())) {
            handleRateLimitReportResponse(rateLimitResponse.getRateLimitReportResponse());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.error("received error from server {}", hostIdentifier, throwable);
        lastConnectFailTimeMilli.set(System.currentTimeMillis());
        closeStream(false);
    }

    @Override
    public void onCompleted() {
        LOG.error("received EOF from server {}", hostIdentifier);
        closeStream(true);
    }

    public InitializeRecord addInitRecord(ServiceIdentifier serviceIdentifier, RateLimitWindow rateLimitWindow) {
        if (!initRecord.containsKey(serviceIdentifier)) {
            LOG.info("[RateLimit] add init record for {}, stream is {}", serviceIdentifier, this.hostIdentifier);
            initRecord.putIfAbsent(serviceIdentifier, new InitializeRecord(rateLimitWindow));
        }
        return initRecord.get(serviceIdentifier);
    }

    public void deleteInitRecord(ServiceIdentifier serviceIdentifier) {
        LOG.info("[RateLimit] delete init record for {}, stream is {}", serviceIdentifier, this.hostIdentifier);
        initRecord.remove(serviceIdentifier);
    }

    /**
     * 处理初始化请求的response
     *
     * @param rateLimitInitResponse 初始化请求的返回结果
     */
    private void handleRateLimitInitResponse(RateLimitInitResponse rateLimitInitResponse) {
        LOG.debug("[handleRateLimitInitResponse] response:{}", rateLimitInitResponse);

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
    void handleRateLimitReportResponse(RateLimitReportResponse rateLimitReportResponse) {
        LOG.debug("[handleRateLimitReportRequest] response:{}", rateLimitReportResponse);
        if (rateLimitReportResponse.getCode() != ServerCodes.EXECUTE_SUCCESS) {
            LOG.error("[handleRateLimitReportRequest] failed. code is {}", rateLimitReportResponse.getCode());
            return;
        }
        List<QuotaLeft> quotaLeftsList = rateLimitReportResponse.getQuotaLeftsList();
        if (CollectionUtils.isEmpty(quotaLeftsList)) {
            LOG.error("[handleRateLimitReportRequest] quotaLefts is empty.");
            return;
        }
        long remoteQuotaTimeMilli = rateLimitReportResponse.getTimestamp();
        long localQuotaTimeMilli = getLocalTimeMilli(remoteQuotaTimeMilli);
        quotaLeftsList.forEach(quotaLeft -> {
            DurationBaseCallback callback = counters.get(quotaLeft.getCounterKey());
            RemoteQuotaInfo remoteQuotaInfo = new RemoteQuotaInfo(quotaLeft.getLeft(), quotaLeft.getClientCount(),
                    localQuotaTimeMilli, callback.getDuration() * 1000);
            callback.getRateLimitWindow().getAllocatingBucket().onRemoteUpdate(remoteQuotaInfo);
        });
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
                && currentTimeMillis - lastSyncTime < RateLimitConstants.TIME_ADJUST_INTERVAL_MS) {
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
        timeDiffMilli.set(timeDiff);
        LOG.info("[RateLimit] adjust time to server time is {}, latency is {},diff is {}", remoteSendTimeMilli, latency,
                timeDiff);
    }

    public boolean isEndStream() {
        return endStream.get();
    }

    public boolean sendRateLimitRequest(RateLimitRequest rateLimitRequest) {
        LOG.debug("ratelimit request to send is {}", rateLimitRequest);
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

    public InitializeRecord getInitRecord(ServiceIdentifier serviceIdentifier) {
        return initRecord.get(serviceIdentifier);
    }

    public Integer getCounterKey(ServiceIdentifier serviceIdentifier, Integer duration) {
        InitializeRecord initializeRecord = initRecord.get(serviceIdentifier);
        if (null == initializeRecord) {
            return null;
        }
        return initializeRecord.getDurationRecord().get(duration);
    }

}
