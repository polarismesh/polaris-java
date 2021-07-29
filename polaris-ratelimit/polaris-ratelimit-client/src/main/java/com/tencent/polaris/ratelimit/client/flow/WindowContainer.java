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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowContainer {

    private static final Logger LOG = LoggerFactory.getLogger(WindowContainer.class);

    private final ServiceKey serviceKey;

    private final RateLimitWindow mainWindow;

    private final Map<String, RateLimitWindow> windowByLabel;

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
     * 检查并淘汰窗口
     *
     * @return 是否淘汰
     */
    public boolean checkAndExpireWindows() {
        if (null != mainWindow) {
            return mainWindow.isExpired();
        }
        int expiredLabels = 0;
        for (Map.Entry<String, RateLimitWindow> entry : windowByLabel.entrySet()) {
            String labelKey = entry.getKey();
            RateLimitWindow window = entry.getValue();
            if (window.isExpired()) {
                expiredLabels++;
                window = windowByLabel.remove(labelKey);
                if (null != window) {
                    window.unInit();
                }
            }
        }
        if (expiredLabels > 0) {
            LOG.info("[RateLimit]{} labels has been cleanup by expired, service {}", expiredLabels, serviceKey);
        }
        return false;
    }
}
