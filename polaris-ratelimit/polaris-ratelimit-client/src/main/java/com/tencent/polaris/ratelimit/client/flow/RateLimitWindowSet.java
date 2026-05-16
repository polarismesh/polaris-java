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
        if (null == windowContainer) {
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
     * 把 (revision, labelsStr) → window 的写入做成一次原子操作，避免 add 与 cleanupContainers 之间的 race：
     * 修复前 add 走 compute 拿到 existing 后释放锁，再调 container.computeLabelWindow 补 label，
     * 这段非锁区间会被 cleanup 抢入并标记 expired + 移除 entry，导致新 window 落在脱链 container 上泄漏。
     * 现在把"取 container + 补齐 label"全部塞进 windowByRule.compute 的 lambda 内，
     * 与 cleanupContainers 的 compute 在同一 bin 锁互斥。
     */
    RateLimitWindow addLabelToRevision(String revision, String labelsStr, boolean newRegexSpread,
                                       Function<String, RateLimitWindow> createRateLimitWindow) {
        WindowContainer container = windowByRule.compute(revision, (key, existing) -> {
            if (existing != null && !existing.isExpired()) {
                if (existing.isRegexSpread() != newRegexSpread) {
                    // 同一 revision 的 regexSpread 由 rule 内容决定，正常情况下不可能发生变化。
                    // 出现不一致说明上游传入的 initCriteria 与 rule 不匹配，记录后仍按 existing 处理，
                    // 避免错位的 mode 切换导致计数走两个不同 container。
                    LOG.error("[RateLimitWindowSet] regexSpread mismatch for revision {} service {}: "
                                    + "existing={}, incoming={}. Keep existing container.",
                            key, serviceKey, existing.isRegexSpread(), newRegexSpread);
                }
                // 在持锁状态下补齐 label 维度的 window，避免离开 compute 后的 race
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
            // 用 compute 与 addRateLimitWindow 的 compute 严格互斥；
            // 在 lambda 内 markExpired + stopSyncTasks，确保任何并发 add 出 compute 后
            // 看到的 container 要么不存在、要么 isExpired() 返回 true（被识别后会替换）。
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
        // 用 compute 在持锁状态下做"判定 + 删除"，避免与 addRateLimitWindow 中
        // computeIfAbsent 的并发竞态把新写入的 window 丢进已被卸载的 container
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
