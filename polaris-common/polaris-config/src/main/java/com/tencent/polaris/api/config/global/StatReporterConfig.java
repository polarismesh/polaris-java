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

package com.tencent.polaris.api.config.global;

import com.tencent.polaris.api.config.plugin.PluginConfig;
import com.tencent.polaris.api.config.verify.Verifier;
import java.util.List;

/**
 * 数据上报配置
 */
public interface StatReporterConfig extends PluginConfig, Verifier {

    String DEFAULT_REPORTER_PROMETHEUS = "prometheus";

    /**
     * 是否启用数据上报
     *
     * @return 启用开关
     */
    boolean isEnable();

    /**
     * 启用的数据上报插件链
     *
     * @return 插件名
     */
    List<String> getChain();
}
