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

import com.tencent.polaris.api.plugin.ratelimiter.InitCriteria;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaBucket;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.FlowControlParam;
import com.tencent.polaris.client.pb.RateLimitProto.Amount;
import com.tencent.polaris.client.pb.RateLimitProto.RateLimitCluster;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.client.pb.RateLimitProto.Rule.Type;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.ratelimit.client.pojo.CommonQuotaRequest;
import com.tencent.polaris.ratelimit.client.sync.RemoteSyncTask;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

/**
 * 限流窗口
 */
public class RateLimitWindow {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitWindow.class);

    public enum WindowStatus {
        /**
         * 刚创建， 无需进行后台调度
         */
        CREATED,
        /**
         * 已获取调度权，准备开始调度
         */
        INITIALIZING,
        /**
         * 已经在远程初始化结束
         */
        INITIALIZED,
        /**
         * 已经删除
         */
        DELETED,
    }

    private final RateLimitWindowSet windowSet;

    private final ServiceKey svcKey;

    private final String labels;

    private final String uniqueKey;

    private final int hashValue;

    private final FlowControlParam syncParam;

    private final Object initLock = new Object();

    private final AtomicInteger status = new AtomicInteger();

    private final AtomicLong lastAccessTimeMs = new AtomicLong();

    // 执行正式分配的令牌桶
    private final QuotaBucket allocatingBucket;

    private final long expireDurationMs;

    //远程同步的集群名
    private final ServiceKey remoteCluster;

    //限流规则
    private final Rule rule;

    //限流模式
    private int configMode;

    /**
     * 构造函数
     *
     * @param windowSet 集合
     * @param quotaRequest 请求
     * @param labelsStr 标签
     */
    public RateLimitWindow(RateLimitWindowSet windowSet, CommonQuotaRequest quotaRequest, String labelsStr) {
        status.set(WindowStatus.CREATED.ordinal());
        InitCriteria initCriteria = quotaRequest.getInitCriteria();
        Rule rule = initCriteria.getRule();
        this.rule = rule;
        this.windowSet = windowSet;
        this.svcKey = quotaRequest.getSvcEventKey().getServiceKey();
        this.labels = labelsStr;
        this.uniqueKey = buildQuotaUniqueKey(rule.getRevision().getValue());
        initCriteria.setWindowKey(this.uniqueKey);
        this.hashValue = uniqueKey.hashCode();
        this.expireDurationMs = getExpireDurationMs(rule);
        this.syncParam = quotaRequest.getFlowControlParam();
        RateLimitCluster cluster = rule.getCluster();
        if (null != cluster && StringUtils.isNotBlank(cluster.getNamespace().getValue()) && StringUtils
                .isNotBlank(cluster.getService().getValue())) {
            remoteCluster = new ServiceKey(cluster.getNamespace().getValue(), cluster.getService().getValue());
        } else {
            remoteCluster = null;
        }
        allocatingBucket = getQuotaBucket(initCriteria, windowSet.getRateLimitExtension());
        lastAccessTimeMs.set(System.currentTimeMillis());

        buildRemoteConfigMode();
    }

    private void buildRemoteConfigMode() {
        //解析限流集群配置
        if (Type.LOCAL.equals(rule.getType())) {
            this.configMode = RateLimitConstants.CONFIG_QUOTA_LOCAL_MODE;
            return;
        }
        if (StringUtils.isBlank(rule.getNamespace().getValue()) || StringUtils.isBlank(rule.getService().getValue())) {
            this.configMode = RateLimitConstants.CONFIG_QUOTA_LOCAL_MODE;
            return;
        }
        this.configMode = RateLimitConstants.CONFIG_QUOTA_GLOBAL_MODE;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    private static QuotaBucket getQuotaBucket(InitCriteria criteria, RateLimitExtension extension) {
        String action = criteria.getRule().getAction().getValue();
        ServiceRateLimiter rateLimiter = null;
        if (StringUtils.isNotBlank(action)) {
            rateLimiter = extension.getRateLimiter(action);
        }
        if (null == rateLimiter) {
            rateLimiter = extension.getDefaultRateLimiter();
        }
        return rateLimiter.initQuota(criteria);
    }

    private static long getExpireDurationMs(Rule rule) {
        return getMaxSeconds(rule) + RateLimitConstants.EXPIRE_FACTOR_MS;
    }

    private static long getMaxSeconds(Rule rule) {
        long maxSeconds = 0;
        for (Amount amount : rule.getAmountsList()) {
            long seconds = amount.getValidDuration().getSeconds();
            if (maxSeconds == 0 || seconds > maxSeconds) {
                maxSeconds = seconds;
            }
        }
        return maxSeconds;
    }

    private String buildQuotaUniqueKey(String ruleRevision) {
        StringBuilder builder = new StringBuilder();
        builder.append(ruleRevision).append(RateLimitConstants.DEFAULT_NAMES_SEPARATOR);
        builder.append(svcKey.getService()).append(RateLimitConstants.DEFAULT_NAMES_SEPARATOR);
        builder.append(svcKey.getNamespace());
        if (StringUtils.isNotBlank(labels)) {
            builder.append(RateLimitConstants.DEFAULT_NAMES_SEPARATOR);
            builder.append(labels);
        }
        return builder.toString();
    }

    public void init() {
        synchronized (initLock) {
            if (!status.compareAndSet(WindowStatus.CREATED.ordinal(), WindowStatus.INITIALIZING.ordinal())) {
                //确保初始化一次
                return;
            }
            if (configMode == RateLimitConstants.CONFIG_QUOTA_LOCAL_MODE) {
                //本地限流，则直接可用
                status.set(WindowStatus.INITIALIZED.ordinal());
                return;
            }
            //加入轮询队列，走异步调度
            windowSet.getRateLimitExtension().submitSyncTask(new RemoteSyncTask(this));
        }
    }

    public void unInit() {
        synchronized (initLock) {
            if (status.get() == WindowStatus.DELETED.ordinal()) {
                return;
            }
            status.set(WindowStatus.DELETED.ordinal());
            //从轮询队列中剔除
            if (null == remoteCluster) {
                return;
            }
            windowSet.getRateLimitExtension().stopSyncTask(uniqueKey);
        }
    }

    public QuotaResult allocateQuota(int count) {
        long curTimeMs = System.currentTimeMillis();
        lastAccessTimeMs.set(curTimeMs);
        return allocatingBucket.allocateQuota(curTimeMs, count);
    }

    /**
     * 窗口已经过期
     *
     * @return boolean
     */
    public boolean isExpired() {
        long curTimeMs = System.currentTimeMillis();
        boolean expired = curTimeMs - lastAccessTimeMs.get() > expireDurationMs;
        if (expired) {
            LOG.info("[RateLimit]window has expired, expireDurationMs {}, uniqueKey {}", expireDurationMs, uniqueKey);
        }
        return expired;
    }

    /**
     * 获取当前窗口的状态
     *
     * @return 窗口状态
     */
    public WindowStatus getStatus() {
        return WindowStatus.class.getEnumConstants()[this.status.get()];
    }

    public void setStatus(int value) {
        this.status.set(value);
    }

    public RateLimitWindowSet getWindowSet() {
        return windowSet;
    }

    public QuotaBucket getAllocatingBucket() {
        return allocatingBucket;
    }

    public ServiceKey getRemoteCluster() {
        return remoteCluster;
    }

    public ServiceKey getSvcKey() {
        return svcKey;
    }

    public String getLabels() {
        return labels;
    }

    public Rule getRule() {
        return rule;
    }

}
