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

package com.tencent.polaris.api.plugin.stat;

import com.tencent.polaris.api.plugin.Plugin;

import java.util.Map;

/**
 * 【扩展点接口】上报调用链
 *
 * @author andrewshan
 * @date 2024/6/2
 */
public interface TraceReporter extends Plugin {

    /**
     * if the reporter is enabled
     */
    boolean isEnabled();

    /**
     * set the attributes in trace span
     *
     * @param attributes span attributes
     */
    void setSpanAttributes(Map<String, String> attributes);

    /**
     * set the attributes in baggage span
     *
     * @param attributes baggage attributes
     */
    Object setBaggageAttributes(Map<String, String> attributes);
}
