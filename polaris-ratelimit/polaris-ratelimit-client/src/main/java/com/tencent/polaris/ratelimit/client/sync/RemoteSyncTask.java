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
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.ratelimit.client.flow.AsyncRateLimitConnector;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindow;
import com.tencent.polaris.ratelimit.client.flow.ServiceIdentifier;
import com.tencent.polaris.ratelimit.client.flow.StreamCounterSet;
import com.tencent.polaris.ratelimit.client.flow.StreamResource;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.LimitTarget;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.QuotaMode;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.QuotaSum;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.QuotaTotal;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitCmd;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitInitRequest;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitInitRequest.Builder;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitReportRequest;
import com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest;
import java.util.Map;
import org.slf4j.Logger;

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
            case DELETED:
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
                        window.getRemoteCluster(), window.getRemoteAddresses(), window.getUniqueKey(),
                        serviceIdentifier);
        //拿不到限流集群的实例的时候
        if (streamCounterSet == null) {
            LOG.error("[doRemoteInit] failed, stream counter is null. remote cluster:{}, remote addresses: {}",
                    window.getRemoteCluster(), window.getRemoteAddresses());
            return;
        }
        StreamResource streamResource = streamCounterSet.checkAndCreateResource(serviceIdentifier, window);
        //调整时间
        adjustTime(streamResource);
        //执行同步操作
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
        if (!streamResource.sendRateLimitRequest(rateLimitInitRequest)) {
            LOG.warn("fail to init token request by {}", window.getUniqueKey());
        }
    }


    /**
     * 上报
     */
    private void doRemoteAcquire() {
        StreamCounterSet streamCounterSet = asyncRateLimitConnector
                .getStreamCounterSet(window.getWindowSet().getRateLimitExtension().getExtensions(),
                        window.getRemoteCluster(), window.getRemoteAddresses(), window.getUniqueKey(),
                        serviceIdentifier);
        if (streamCounterSet == null) {
            LOG.error("[doRemoteAcquire] failed, stream counter is null. remote cluster:{},",
                    window.getRemoteCluster());
            return;
        }
        StreamResource streamResource = streamCounterSet.checkAndCreateResource(serviceIdentifier, window);

        if (!streamResource.hasInit(serviceIdentifier)) {
            LOG.warn("[doRemoteAcquire] has not inited. serviceKey:{}", window.getSvcKey());
            doRemoteInit();
            return;
        }
        //调整时间
        streamResource.adjustTime();
        RateLimitReportRequest.Builder rateLimitReportRequest = RateLimitReportRequest.newBuilder();
        //clientKey
        rateLimitReportRequest.setClientKey(streamResource.getClientKey());
        //timestamp
        long curTimeMilli = System.currentTimeMillis();
        long serverTimeMilli = streamResource.getRemoteTimeMilli(curTimeMilli);
        rateLimitReportRequest.setTimestamp(serverTimeMilli);

        //quotaUses
        Map<Integer, LocalQuotaInfo> localQuotaInfos = window.getAllocatingBucket().fetchLocalUsage(curTimeMilli);

        for (Map.Entry<Integer, LocalQuotaInfo> entry : localQuotaInfos.entrySet()) {
            QuotaSum.Builder quotaSum = QuotaSum.newBuilder();
            quotaSum.setUsed((int) entry.getValue().getQuotaUsed());
            quotaSum.setLimited((int) entry.getValue().getQuotaLimited());
            Integer counterKey = streamResource.getCounterKey(serviceIdentifier, entry.getKey());
            if (null == counterKey) {
                LOG.warn("[doRemoteAcquire] counterKey for {}, duration {} not found", window.getUniqueKey(),
                        entry.getKey());
                doRemoteInit();
                return;
            }
            quotaSum.setCounterKey(counterKey);
            rateLimitReportRequest.addQuotaUses(quotaSum.build());
        }

        RateLimitRequest rateLimitRequest = RateLimitRequest.newBuilder().setCmd(RateLimitCmd.ACQUIRE)
                .setRateLimitReportRequest(rateLimitReportRequest).build();
        if (!streamResource.sendRateLimitRequest(rateLimitRequest)) {
            LOG.warn("fail to acquire token request by {}", window.getUniqueKey());
        }
    }

    /**
     * 调整时间
     *
     * @param streamResource streamCounterSet
     */
    private void adjustTime(StreamResource streamResource) {
        streamResource.adjustTime();
    }
}
