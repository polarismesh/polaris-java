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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;

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

}
