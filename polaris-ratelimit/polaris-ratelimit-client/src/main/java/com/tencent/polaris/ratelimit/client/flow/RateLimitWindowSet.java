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

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.ratelimit.client.pojo.CommonQuotaRequest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;

public class RateLimitWindowSet {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitWindowSet.class);

    /**
     * 被限流服务的serviceKey
     */
    private final ServiceKey serviceKey;

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
     * @param serviceKey 服务名
     * @param rateLimitExtension 扩展插件
     * @param clientId 客户端唯一标识
     */
    public RateLimitWindowSet(ServiceKey serviceKey, RateLimitExtension rateLimitExtension, String clientId) {
        this.clientId = clientId;
        this.serviceKey = serviceKey;
        this.rateLimitExtension = rateLimitExtension;
        this.asyncRateLimitConnector = new AsyncRateLimitConnector(
                rateLimitExtension.getExtensions().getConfiguration());
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
     * @param request 请求
     * @param labelsStr 标签
     * @return window
     */
    public RateLimitWindow addRateLimitWindow(CommonQuotaRequest request, String labelsStr) {
        Rule targetRule = request.getInitCriteria().getRule();
        String revision = targetRule.getRevision().getValue();
        Function<String, RateLimitWindow> createRateLimitWindow = new Function<String, RateLimitWindow>() {
            @Override
            public RateLimitWindow apply(String label) {
                return new RateLimitWindow(RateLimitWindowSet.this, request, label);
            }
        };
        WindowContainer container = windowByRule.computeIfAbsent(revision, new Function<String, WindowContainer>() {
            @Override
            public WindowContainer apply(String s) {
                RateLimitWindow window = createRateLimitWindow.apply(labelsStr);
                return new WindowContainer(serviceKey, labelsStr, window, request.isRegexSpread());
            }
        });
        RateLimitWindow mainWindow = container.getLabelWindow(labelsStr);
        if (null != mainWindow) {
            return mainWindow;
        }
        return container.computeLabelWindow(labelsStr, createRateLimitWindow);
    }

    public RateLimitExtension getRateLimitExtension() {
        return rateLimitExtension;
    }

    public void deleteRules(Set<String> rules) {
        for (String rule : rules) {
            WindowContainer container = windowByRule.remove(rule);
            if (null == container) {
                continue;
            }
            LOG.info("[RateLimit]container {} for service {} has been stopped", rule, serviceKey);
            container.stopSyncTasks();
        }
    }

    public void cleanupContainers() {
        int rulesExpired = 0;
        for (Map.Entry<String, WindowContainer> entry : windowByRule.entrySet()) {
            String revision = entry.getKey();
            boolean expired = entry.getValue().checkAndExpireWindows();
            if (expired) {
                rulesExpired++;
                WindowContainer container = windowByRule.remove(revision);
                if (null != container) {
                    container.getMainWindow().unInit();
                }
            }
        }
        if (rulesExpired > 0) {
            LOG.info("[RateLimit]{} rules has been cleanup by expired, service {}", rulesExpired, serviceKey);
        }
    }

    public AsyncRateLimitConnector getAsyncRateLimitConnector() {
        return asyncRateLimitConnector;
    }

    public String getClientId() {
        return clientId;
    }
}
