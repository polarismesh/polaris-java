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

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.provider.RateLimitConfig;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.ratelimiter.InitCriteria;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaBucket;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.FlowControlParam;
import com.tencent.polaris.client.remote.ServiceAddressRepository;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.ratelimit.client.pojo.CommonQuotaRequest;
import com.tencent.polaris.ratelimit.client.sync.PolarisRemoteSyncTask;
import com.tencent.polaris.ratelimit.client.sync.tsf.TsfRemoteSyncTask;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import com.tencent.polaris.ratelimit.client.utils.RateLimiterEventUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Amount;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.RateLimitCluster;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.tencent.polaris.metadata.core.constant.MetadataConstants.LOCAL_NAMESPACE;
import static com.tencent.polaris.metadata.core.constant.MetadataConstants.LOCAL_SERVICE;

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

    private final AtomicLong lastInitTimeMs = new AtomicLong();

    // 执行正式分配的令牌桶
    private final QuotaBucket allocatingBucket;

    private final long expireDurationMs;

    //远程同步的集群名
    private final ServiceKey remoteCluster;

    //限流的服务端地址列表
    private final ServiceAddressRepository serviceAddressRepository;
    //限流规则
    private final Rule rule;

    //限流模式
    private int configMode;

    // 是否是TSF限流模式
    private boolean isTsfCluster = false;


    //限流配置
    private final RateLimitConfig rateLimitConfig;

    private final AtomicReference<QuotaResult.Code> lastCode = new AtomicReference<>(QuotaResult.Code.QuotaResultOk);

    /**
     * 构造函数
     *
     * @param windowSet    集合
     * @param quotaRequest 请求
     * @param labelsStr    标签
     */
    public RateLimitWindow(RateLimitWindowSet windowSet, CommonQuotaRequest quotaRequest, String labelsStr,
                           RateLimitConfig rateLimitConfig, InitCriteria initCriteria) {
        status.set(WindowStatus.CREATED.ordinal());
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
        remoteCluster = getLimiterClusterService(rule.getCluster(), rateLimitConfig);
        serviceAddressRepository = buildServiceAddressRepository(rateLimitConfig.getLimiterAddresses(),
                uniqueKey,  windowSet.getExtensions(), remoteCluster, null, LoadBalanceConfig.LOAD_BALANCE_RING_HASH, "grpc");
        allocatingBucket = getQuotaBucket(initCriteria, windowSet.getRateLimitExtension());
        lastAccessTimeMs.set(System.currentTimeMillis());
        this.rateLimitConfig = rateLimitConfig;
        buildRemoteConfigMode();
    }

    private ServiceAddressRepository buildServiceAddressRepository(List<String> addresses, String hash, Extensions extensions,
            ServiceKey remoteCluster, List<String> routers, String lbPolicy, String protocol) {
        return  new ServiceAddressRepository(addresses, hash, extensions, remoteCluster, routers, lbPolicy, protocol);
    }




    private ServiceKey getLimiterClusterService(RateLimitCluster cluster, RateLimitConfig rateLimitConfig) {
        if (null != cluster && StringUtils.isNotBlank(cluster.getNamespace().getValue()) && StringUtils
                .isNotBlank(cluster.getService().getValue())) {
            return new ServiceKey(cluster.getNamespace().getValue(), cluster.getService().getValue());
        }
        if (StringUtils.isNotBlank(rateLimitConfig.getLimiterNamespace()) && StringUtils
                .isNotBlank(rateLimitConfig.getLimiterService())) {
            return new ServiceKey(rateLimitConfig.getLimiterNamespace(), rateLimitConfig.getLimiterService());
        }
        return null;
    }


    private void buildRemoteConfigMode() {
        //解析限流集群配置
        if (Rule.Type.LOCAL.equals(rule.getType())) {
            this.configMode = RateLimitConstants.CONFIG_QUOTA_LOCAL_MODE;
            return;
        }
        if (null == remoteCluster && 0 == serviceAddressRepository.size()) {
            this.configMode = RateLimitConstants.CONFIG_QUOTA_LOCAL_MODE;
            LOG.warn("remote limiter service or addresses not configured, degrade to local mode");
            return;
        }
        if (rule.getMetadataMap().containsKey("limiter")
                && StringUtils.equalsIgnoreCase("tsf", rule.getMetadataMap().get("limiter"))) {
            // tsf的集群限流也用单机限流的方式执行逻辑，但是需要从远端更新令牌数
            this.configMode = RateLimitConstants.CONFIG_QUOTA_LOCAL_MODE;
            this.isTsfCluster = true;
            return;
        }
        this.configMode = RateLimitConstants.CONFIG_QUOTA_GLOBAL_MODE;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public ServiceAddressRepository getServiceAddressRepository(){
        return serviceAddressRepository;
    }

    private static QuotaBucket getQuotaBucket(InitCriteria criteria, RateLimitExtension extension) {
        String action = criteria.getRule().getAction().getValue();
        Rule.Resource resource = criteria.getRule().getResource();
        ServiceRateLimiter rateLimiter = null;
        if (StringUtils.isNotBlank(action)) {
            rateLimiter = extension.getRateLimiter(resource, action);
        }
        if (null == rateLimiter) {
            rateLimiter = extension.getDefaultRateLimiter();
        }
        return rateLimiter.initQuota(criteria);
    }

    private static long getExpireDurationMs(Rule rule) {
        return getMaxSeconds(rule) * 1000 + RateLimitConstants.EXPIRE_FACTOR_MS;
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
            if (configMode == RateLimitConstants.CONFIG_QUOTA_LOCAL_MODE && !isTsfCluster) {
                //本地限流，则直接可用
                status.set(WindowStatus.INITIALIZED.ordinal());
                return;
            }
            //加入轮询队列，走异步调度
            if (rule.getMetadataMap().containsKey("limiter")
                    && StringUtils.equalsIgnoreCase("tsf", rule.getMetadataMap().get("limiter"))) {
                windowSet.getRateLimitExtension().submitSyncTask(new TsfRemoteSyncTask(this), 0L, 1000L);
            } else {
                windowSet.getRateLimitExtension().submitSyncTask(new PolarisRemoteSyncTask(this));
            }
        }
    }

    public void unInit() {
        synchronized (initLock) {
            if (status.get() == WindowStatus.DELETED.ordinal()) {
                return;
            }
            status.set(WindowStatus.DELETED.ordinal());
            //从轮询队列中剔除
            windowSet.getRateLimitExtension().stopSyncTask(uniqueKey);
        }
    }

    public QuotaResult allocateQuota(CommonQuotaRequest request) {
        long curTimeMs = request.getCurrentTimestamp();
        lastAccessTimeMs.set(curTimeMs);
        QuotaResult quotaResult = allocatingBucket.allocateQuota(curTimeMs, request.getCount());
        if (!Objects.equals(lastCode.get(), quotaResult.getCode())) {
            String sourceNamespace = "";
            String sourceService = "";
            if (Objects.nonNull(request.getMetadataContext())
                    && Objects.nonNull(request.getMetadataContext().getMetadataContainer(MetadataType.APPLICATION, true))) {
                MetadataContainer metadataContainer = request.getMetadataContext().getMetadataContainer(MetadataType.APPLICATION, true);
                sourceNamespace = metadataContainer.getRawMetadataStringValue(LOCAL_NAMESPACE);
                sourceService = metadataContainer.getRawMetadataStringValue(LOCAL_SERVICE);
            }
            RateLimiterEventUtils.reportEvent(windowSet.getExtensions(), svcKey, rule, lastCode.get(),
                    quotaResult.getCode(), sourceNamespace, sourceService, labels, quotaResult.getInfo());
        }
        lastCode.set(quotaResult.getCode());
        return quotaResult;
    }

    public void returnQuota(CommonQuotaRequest request) {
        allocatingBucket.returnQuota(request.getCurrentTimestamp(), request.getCount());
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

    public long getLastInitTimeMs() {
        return lastInitTimeMs.get();
    }

    public void setLastInitTimeMs(long lastInitTimeMs) {
        this.lastInitTimeMs.set(lastInitTimeMs);
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

    public RateLimitConfig getRateLimitConfig() {
        return rateLimitConfig;
    }
}
