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

/**
 * Certificate Config.
 *
 * @author Haotian Zhang
 */
public interface CertificateConfig extends PluginConfig, Verifier {

    /**
     * 是否启用证书管理
     *
     * @return 启用开关
     */
    boolean isEnable();

    /**
     * 默认Common Name
     *
     * @return
     */
    String getCommonName();

    /**
     * 证书有效时间
     *
     * @return
     */
    Long getValidityDuration();

    /**
     * 刷新证书时间
     *
     * @return
     */
    Long getRefreshBefore();

    /**
     * 证书检测周期
     *
     * @return
     */
    Long getWatchInterval();
    /**
     * 证书管理插件名
     *
     * @return 插件名
     */
    String getPluginName();
}
