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

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class WindowContainer {

    private static final Logger LOG = LoggerFactory.getLogger(WindowContainer.class);

    private final ServiceKey serviceKey;

    private final RateLimitWindow mainWindow;

    private final Map<String, RateLimitWindow> windowByLabel;

    /**
     * 由 cleanup / deleteRules 标记；调用方拿到 container 后须检查此标志，true 表示已淘汰，应重建
     */
    private final AtomicBoolean expired = new AtomicBoolean(false);

    public WindowContainer(ServiceKey serviceKey, String labelStr, RateLimitWindow window, boolean regexSpread) {
        this.serviceKey = serviceKey;
        if (!regexSpread) {
            mainWindow = window;
            windowByLabel = null;
        } else {
            mainWindow = null;
            windowByLabel = new ConcurrentHashMap<>();
            windowByLabel.put(labelStr, window);
        }
    }

    /**
     * @return 该 container 是否为 regexSpread 模式（按 label 维度展开多窗口）
     */
    public boolean isRegexSpread() {
        return mainWindow == null;
    }

    public RateLimitWindow getLabelWindow(String label) {
        if (null != mainWindow) {
            return mainWindow;
        }
        return windowByLabel.get(label);
    }

    public RateLimitWindow computeLabelWindow(String label, Function<String, RateLimitWindow> function) {
        return windowByLabel.computeIfAbsent(label, function);
    }

    public void stopSyncTasks() {
        if (null != mainWindow) {
            mainWindow.unInit();
            return;
        }
        for (RateLimitWindow window : windowByLabel.values()) {
            window.unInit();
        }
    }

    public RateLimitWindow getMainWindow() {
        return mainWindow;
    }

    /**
     * 巡检窗口活跃度，过期则 stopSyncTask 并把 container 标记为 expired。
     * - mainWindow 模式：mainWindow 自身过期即整 container 过期；
     * - regexSpread 模式：先逐 label 清理过期 window，全部清空时整 container 过期。
     *
     * @return container 是否已过期，调用方据此决定是否从上层 windowByRule 中移除
     */
    public boolean checkAndCleanExpiredWindows() {
        if (null != mainWindow) {
            boolean expiredNow = mainWindow.isExpired();
            if (expiredNow) {
                LOG.info("[RateLimit] mainWindow have been cleaned up due to expiration, service {}", serviceKey);
                // 先 markExpired 再 unInit，避免 getRateLimitWindow 拿到 isExpired==false 但已 DELETED 的 window
                expired.set(true);
                mainWindow.unInit();
            }
            return expiredNow;
        }
        int expiredLabels = 0;
        Iterator<Map.Entry<String, RateLimitWindow>> iterator = windowByLabel.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, RateLimitWindow> entry = iterator.next();
            String labelKey = entry.getKey();
            RateLimitWindow window = entry.getValue();
            if (window.isExpired()) {
                expiredLabels++;
                iterator.remove();  // 使用迭代器的 remove 方法删除当前元素
                LOG.info("[WindowContainer] windowByLabel remove label key {} , window {}", labelKey, window);
                window.unInit();
            }
        }
        if (expiredLabels > 0) {
            LOG.info("[RateLimit] {} labels have been cleaned up due to expiration, service {}", expiredLabels,
                    serviceKey);
        }
        boolean shouldRemove = windowByLabel.isEmpty();
        if (shouldRemove) {
            expired.set(true);
        }
        return shouldRemove;
    }

    /**
     * 是否已被 cleanup / deleteRules 标记为过期
     */
    public boolean isExpired() {
        return expired.get();
    }

    /**
     * 标记为已过期；由 cleanup / deleteRules 调用，并发 add 据此重建 container
     */
    public void markExpired() {
        expired.set(true);
    }
}
