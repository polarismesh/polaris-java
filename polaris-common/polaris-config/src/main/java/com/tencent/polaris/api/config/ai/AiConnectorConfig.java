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

package com.tencent.polaris.api.config.ai;

import com.tencent.polaris.api.config.global.ServerConnectorConfig;

/**
 * Skill connector configuration including persist options.
 */
public interface AiConnectorConfig extends ServerConnectorConfig {

    /**
     * Connector plugin name.
     *
     * @return connector type
     */
    String getConnectorType();

    /**
     * Whether persist is enabled.
     *
     * @return persist enable
     */
    Boolean getPersistEnable();

    /**
     * Persist directory.
     *
     * @return persist dir
     */
    String getPersistDir();

    /**
     * Max write retry times.
     *
     * @return write retry
     */
    Integer getPersistMaxWriteRetry();

    /**
     * Max read retry times.
     *
     * @return read retry
     */
    Integer getPersistMaxReadRetry();

    /**
     * Persist retry interval in milliseconds.
     *
     * @return retry interval
     */
    Long getPersistRetryInterval();

    /**
     * Whether fallback to local cache when remote fails.
     *
     * @return fallback switch
     */
    Boolean getFallbackToLocalCache();
}
