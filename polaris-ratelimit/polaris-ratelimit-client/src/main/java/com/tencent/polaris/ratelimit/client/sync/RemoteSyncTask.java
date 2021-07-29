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

package com.tencent.polaris.ratelimit.client.sync;

import com.tencent.polaris.api.plugin.ratelimiter.AmountInfo;
import com.tencent.polaris.api.plugin.ratelimiter.LocalQuotaInfo;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaBucket;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.ratelimit.client.flow.AsyncRateLimitConnector;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindow;
import com.tencent.polaris.ratelimit.client.flow.ServiceIdentifier;
import com.tencent.polaris.ratelimit.client.flow.StreamCounterSet;
import com.tencent.polaris.ratelimit.client.pb.RateLimitGRPCV2Grpc.RateLimitGRPCV2BlockingStub;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.LimitTarget;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.QuotaMode;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.QuotaSum;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.QuotaTotal;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitCmd;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitInitRequest;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitInitRequest.Builder;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitReportRequest;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1、首次调用需要上报
 * 2、出现后台server切换需要上报
 * 3、定时上报上一次上报到现在已经超过上报周期
 * 4、额使用完毕后上报。配额=amount/实例数/时间片数。
 * 每次上报完后，server会返回服务端的时间戳，客户端会用来校正上报的时间段
 */
public class RemoteSyncTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteSyncTask.class);

    /**
     * 限流窗口
     */
    private final RateLimitWindow window;

    /**
     * 与限流集群通信的连接类
     */
    private final AsyncRateLimitConnector asyncRateLimitConnector;

    /**
     * 被限流的服务信息，一个窗口的唯一标识
     */
    private final ServiceIdentifier serviceIdentifier;

    public RemoteSyncTask(RateLimitWindow window) {
        this.window = window;
        this.asyncRateLimitConnector = window.getWindowSet().getAsyncRateLimitConnector();
        this.serviceIdentifier = new ServiceIdentifier(window.getSvcKey().getService(),
                window.getSvcKey().getNamespace(), window.getLabels());
    }

    public RateLimitWindow getWindow() {
        return window;
    }


    @Override
    public void run() {
        switch (window.getStatus()) {
            case CREATED:
            case DELETED://todo 已经是删除态了，还有必要在轮询吗？
                break;
            case INITIALIZING:
                doRemoteInit();
                break;
            default:
                doRemoteAcquire();
                break;
        }
    }

    /**
     * 发送初始化请求
     */
    private void doRemoteInit() {
        StreamCounterSet streamCounterSet = asyncRateLimitConnector
                .getStreamCounterSet(window.getWindowSet().getRateLimitExtension().getExtensions(),
                        window.getRemoteCluster(), window.getUniqueKey(), serviceIdentifier);
        //拿不到限流集群的实例的时候
        if (streamCounterSet == null) {
            LOG.error("[doRemoteInit] failed, stream counter is null. remote cluster:{},",
                    window.getRemoteCluster());
            return;
        }
        //调整时间
        if (!adjustTime(streamCounterSet)) {
            LOG.error("[doRemoteInit] adjustTime failed.remote cluster:{},svcKey:{}",
                    window.getRemoteCluster(), window.getSvcKey());
            return;
        }
        StreamObserver<RateLimitRequest> streamClient = streamCounterSet.preCheckAsync(serviceIdentifier, window);
        if (streamClient == null) {
            LOG.error("[doRemoteInit] failed, stream client is null. remote cluster:{},svcKey:{}",
                    window.getRemoteCluster(), window.getSvcKey());
            return;
        }
        //clientId
        Builder initRequest = RateLimitInitRequest.newBuilder();
        initRequest.setClientId(window.getWindowSet().getClientId());

        //target
        LimitTarget.Builder target = LimitTarget.newBuilder();
        target.setNamespace(window.getSvcKey().getNamespace());
        target.setService(window.getSvcKey().getService());
        target.setLabels(window.getLabels());
        initRequest.setTarget(target);

        //QuotaTotal
        QuotaMode quotaMode = QuotaMode.forNumber(window.getRule().getAmountModeValue());
        QuotaBucket allocatingBucket = window.getAllocatingBucket();
        Map<Integer, AmountInfo> amountInfos = allocatingBucket.getAmountInfo();
        if (MapUtils.isNotEmpty(amountInfos)) {
            for (Map.Entry<Integer, AmountInfo> entry : amountInfos.entrySet()) {
                QuotaTotal.Builder total = QuotaTotal.newBuilder();
                total.setDuration(entry.getKey());
                total.setMode(quotaMode);
                total.setMaxAmount((int) entry.getValue().getMaxAmount());
                initRequest.addTotals(total.build());
            }
        }
        RateLimitRequest rateLimitInitRequest = RateLimitRequest.newBuilder().setCmd(RateLimitCmd.INIT)
                .setRateLimitInitRequest(initRequest).build();

        streamClient.onNext(rateLimitInitRequest);
    }


    /**
     * 上报
     */
    private void doRemoteAcquire() {
        StreamCounterSet streamCounterSet = asyncRateLimitConnector
                .getStreamCounterSet(window.getWindowSet().getRateLimitExtension().getExtensions(),
                        window.getRemoteCluster(), window.getUniqueKey(), serviceIdentifier);
        if (streamCounterSet == null) {
            LOG.error("[doRemoteAcquire] failed, stream counter is null. remote cluster:{},",
                    window.getRemoteCluster());
            return;
        }
        if (!streamCounterSet.hasInit(serviceIdentifier)) {
            LOG.warn("[doRemoteAcquire] has not inited. serviceKey:{}", window.getSvcKey());
            doRemoteInit();
            return;
        }
        //调整时间
        if (!adjustTime(streamCounterSet)) {
            LOG.error("[doRemoteAcquire] adjustTime failed.remote cluster:{},svcKey:{}",
                    window.getRemoteCluster(), window.getSvcKey());
            return;
        }
        StreamObserver<RateLimitRequest> streamClient = streamCounterSet.preCheckAsync(serviceIdentifier, window);
        if (streamClient == null) {
            LOG.error("[doRemoteAcquire] failed, stream client is null. remote cluster:{}", window.getRemoteCluster());
            return;
        }
        RateLimitReportRequest.Builder rateLimitReportRequest = RateLimitReportRequest.newBuilder();
        //clientKey
        rateLimitReportRequest.setClientKey(streamCounterSet.getClientKey());
        //timestamp
        long curTimeMilli = System.currentTimeMillis();
        long serverTimeMilli = curTimeMilli + asyncRateLimitConnector.getTimeDiff().get();
        rateLimitReportRequest.setTimestamp(serverTimeMilli);

        //quotaUses
        Map<Integer, LocalQuotaInfo> localQuotaInfos = window.getAllocatingBucket().fetchLocalUsage(serverTimeMilli);

        for (Map.Entry<Integer, LocalQuotaInfo> entry : localQuotaInfos.entrySet()) {
            QuotaSum.Builder quotaSum = QuotaSum.newBuilder();
            quotaSum.setUsed((int) entry.getValue().getQuotaUsed());
            quotaSum.setLimited((int) entry.getValue().getQuotaLimited());
            quotaSum.setCounterKey(streamCounterSet.getInitRecord().get(serviceIdentifier).getDurationRecord()
                    .get(entry.getKey()));
            rateLimitReportRequest.addQuotaUses(quotaSum.build());
        }

        RateLimitRequest rateLimitRequest = RateLimitRequest.newBuilder().setCmd(RateLimitCmd.ACQUIRE)
                .setRateLimitReportRequest(rateLimitReportRequest).build();
        streamClient.onNext(rateLimitRequest);
    }

    /**
     * 调整时间
     *
     * @param streamCounterSet streamCounterSet
     */
    private boolean adjustTime(StreamCounterSet streamCounterSet) {

        long lastSyncTimeMilli = asyncRateLimitConnector.getLastSyncTimeMilli().get();
        long sendTimeMilli = System.currentTimeMillis();

        //超过间隔时间才需要调整
        if (lastSyncTimeMilli > 0 && sendTimeMilli - lastSyncTimeMilli < RateLimitConstants.STARTUP_DELAY_MS) {
            LOG.info("adjustTime need wait.lastSyncTimeMilli:{},sendTimeMilli:{}", lastSyncTimeMilli, sendTimeMilli);
            return true;
        }

        RateLimitGRPCV2BlockingStub client = streamCounterSet.preCheckSync(serviceIdentifier, window);
        if (client == null) {
            LOG.error("[adjustTime] can not get connection {}", window.getRemoteCluster());
            return false;
        }

        TimeAdjustRequest timeAdjustRequest = TimeAdjustRequest.newBuilder().build();
        TimeAdjustResponse timeAdjustResponse = client.timeAdjust(timeAdjustRequest);

        long receiveClientTimeMilli = System.currentTimeMillis();
        asyncRateLimitConnector.getLastSyncTimeMilli().set(receiveClientTimeMilli);
        //服务端时间
        long serverTimestamp = timeAdjustResponse.getServerTimestamp();

        long latency = receiveClientTimeMilli - sendTimeMilli;

        long timeDiff = serverTimestamp + latency / 2 - receiveClientTimeMilli;
        asyncRateLimitConnector.getTimeDiff().set(timeDiff);
        LOG.info("[RateLimit]adjust time to server time is {}, latency is {},diff is {}", serverTimestamp, latency,
                timeDiff);
        return true;
    }
}
