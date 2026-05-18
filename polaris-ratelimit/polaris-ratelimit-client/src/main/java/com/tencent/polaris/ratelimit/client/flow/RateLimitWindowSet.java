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
import com.tencent.polaris.api.config.provider.RateLimitConfig;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.ratelimiter.InitCriteria;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.ratelimit.client.pojo.CommonQuotaRequest;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class RateLimitWindowSet {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitWindowSet.class);

    /**
     * 被限流服务的serviceKey
     */
    private final ServiceKey serviceKey;

    private final Extensions extensions;

    private final RateLimitExtension rateLimitExtension;

    /**
     * 不同规则版本对应不同的WindowContainer
     * WindowContainer 里面放的是根据label做为key的Window集合
     */
    private final Map<String, WindowContainer> windowByRule = new ConcurrentHashMap<>();

    /**
     * 限流与集群通信的客户端
     */
    AsyncRateLimitConnector asyncRateLimitConnector;

    /**
     * 客户端唯一标识
     */
    String clientId;

    /**
     * syncFlow
     *
     * @param serviceKey         服务名
     * @param rateLimitExtension 扩展插件
     * @param clientId           客户端唯一标识
     */
    public RateLimitWindowSet(ServiceKey serviceKey, Extensions extensions, RateLimitExtension rateLimitExtension, String clientId) {
        this.clientId = clientId;
        this.serviceKey = serviceKey;
        this.extensions = extensions;
        this.rateLimitExtension = rateLimitExtension;
        this.asyncRateLimitConnector = new AsyncRateLimitConnector();
    }

    public RateLimitWindow getRateLimitWindow(Rule rule, String labelsStr) {
        WindowContainer windowContainer = windowByRule.get(rule.getRevision().getValue());
        // 已 markExpired 的 container 内 window 处于 DELETED，返回 null 让上层重建
        if (null == windowContainer || windowContainer.isExpired()) {
            return null;
        }
        return windowContainer.getLabelWindow(labelsStr);
    }

    /**
     * 增加限流窗口
     *
     * @param request   请求
     * @param labelsStr 标签
     * @return window
     */
    public RateLimitWindow addRateLimitWindow(CommonQuotaRequest request, String labelsStr,
                                              RateLimitConfig rateLimitConfig, InitCriteria initCriteria) {
        Rule targetRule = initCriteria.getRule();
        String revision = targetRule.getRevision().getValue();
        Function<String, RateLimitWindow> createRateLimitWindow = label ->
                new RateLimitWindow(RateLimitWindowSet.this, request, label, rateLimitConfig, initCriteria);
        return addLabelToRevision(revision, labelsStr, initCriteria.isRegexSpread(), createRateLimitWindow);
    }

    /**
     * 在 windowByRule.compute 内完成 container 创建 + label 维度补齐，
     * 与 cleanupContainers / deleteRules 在同一 bin 锁互斥，避免 add 出 compute 后 race。
     * createRateLimitWindow 仅在真正需要新建 container 或新增 label 时调用，避免预创建后被丢弃浪费。
     */
    RateLimitWindow addLabelToRevision(String revision, String labelsStr, boolean newRegexSpread,
                                       Function<String, RateLimitWindow> createRateLimitWindow) {
        WindowContainer container = windowByRule.compute(revision, (key, existing) -> {
            if (existing != null && !existing.isExpired()) {
                if (existing.isRegexSpread() != newRegexSpread) {
                    // 同一 revision 的 regexSpread 由 rule 内容决定，不一致说明上游传错 initCriteria
                    LOG.error("[RateLimitWindowSet] regexSpread mismatch for revision {} service {}: "
                                    + "existing={}, incoming={}. Keep existing container.",
                            key, serviceKey, existing.isRegexSpread(), newRegexSpread);
                }
                // 持锁内补齐 label，避免离开 compute 后被 cleanup 抢入；仅在缺失时调 factory
                if (existing.isRegexSpread() && existing.getLabelWindow(labelsStr) == null) {
                    existing.computeLabelWindow(labelsStr, createRateLimitWindow);
                }
                return existing;
            }
            RateLimitWindow window = createRateLimitWindow.apply(labelsStr);
            return new WindowContainer(serviceKey, labelsStr, window, newRegexSpread);
        });
        return container.getLabelWindow(labelsStr);
    }

    public Extensions getExtensions() {
        return extensions;
    }

    public RateLimitExtension getRateLimitExtension() {
        return rateLimitExtension;
    }

    public void deleteRules(Set<String> rules) {
        for (String rule : rules) {
            // 用 compute 与 add 严格互斥；markExpired 让并发 add 通过 isExpired() 重建 container
            windowByRule.compute(rule, (key, container) -> {
                if (container == null) {
                    return null;
                }
                container.markExpired();
                container.stopSyncTasks();
                LOG.info("[RateLimit]container {} for service {} has been stopped", key, serviceKey);
                return null;
            });
        }
    }

    /**
     * 过期清理单个rule下所有WindowContainer
     */
    public void cleanupContainers() {
        AtomicInteger rulesExpired = new AtomicInteger(0);
        // 用 compute 让"判定 + 移除"与 add 在同一 bin 锁内原子完成
        for (String revision : windowByRule.keySet()) {
            windowByRule.compute(revision, (key, container) -> {
                if (container == null) {
                    return null;
                }
                if (!container.checkAndCleanExpiredWindows()) {
                    return container;
                }
                rulesExpired.incrementAndGet();
                LOG.info("[RateLimitWindowSet] rule {} for service {} has been expired, window container {}",
                        key, serviceKey, container);
                return null;
            });
        }
        if (rulesExpired.get() > 0) {
            LOG.info("[RateLimitWindowSet] {} rules have been cleaned up due to expiration, service {}",
                    rulesExpired, serviceKey);
        }
    }

    public AsyncRateLimitConnector getAsyncRateLimitConnector() {
        return asyncRateLimitConnector;
    }

    public String getClientId() {
        return clientId;
    }

    @JustForTest
    Map<String, WindowContainer> getWindowByRule() {
        return windowByRule;
    }
}
