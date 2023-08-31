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

package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.api.exception.UnimplementedException;
import com.tencent.polaris.api.plugin.configuration.ConfigFileGroupConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileGroupMetadata;
import com.tencent.polaris.api.plugin.configuration.ConfigFileGroupResponse;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * RetryableConfigFileGroupConnector will retry until MAX_RETRY_TIMES is reached.
 * It will return null instead if MAX_RETRY_TIMES is reached.
 */
public class RetryableConfigFileGroupConnector {
    protected static final Logger LOGGER = LoggerFactory.getLogger(RetryableConfigFileGroupConnector.class);

    private static final int MAX_RETRY_TIMES = 3;

    private final ConfigFileGroupConnector rpcConnector;
    private final RetryPolicy retryPolicy;
    private final RetryableValidator retryableValidator;

    public RetryableConfigFileGroupConnector(ConfigFileGroupConnector rpcConnector, RetryableValidator retryableValidator) {
        this.rpcConnector = rpcConnector;
        this.retryableValidator = retryableValidator;
        this.retryPolicy = new ExponentialRetryPolicy(1, 120);
    }

    public ConfigFileGroupResponse GetConfigFileMetadataList(ConfigFileGroupMetadata configFileGroupMetadata, String revision) {
        int retryTimes = 0;
        while (retryTimes < MAX_RETRY_TIMES) {
            try {
                ConfigFileGroupResponse response = rpcConnector.GetConfigFileMetadataList(configFileGroupMetadata, revision);
                if (!retryableValidator.shouldRetry(response)) {
                    retryPolicy.success();
                    return response;
                }

                LOGGER.error("[Config] get config file metadata list hits retry strategy. retry times = {}, namespace = {}, fileGroupName = {}, responseCod = {}",
                        retryTimes, configFileGroupMetadata.getNamespace(), configFileGroupMetadata.getFileGroupName(), response.getCode());
                retryPolicy.fail();
                retryTimes++;
                retryPolicy.executeDelay();
            } catch (Throwable t) {
                if (t instanceof UnimplementedException) {
                    LOGGER.error("[Config] polaris server does not implement, please upgrade. ", t);
                    return null;
                }

                LOGGER.error("[Config] failed to get config file metadata list. retry times = {}, namespace = {}, fileGroupName = {}, exception = {}",
                        retryTimes, configFileGroupMetadata.getNamespace(), configFileGroupMetadata.getFileGroupName(), t);
                retryPolicy.fail();
                retryTimes++;
                retryPolicy.executeDelay();
            }
        }
        return null;
    }

    public interface RetryableValidator {
        boolean shouldRetry(ConfigFileGroupResponse response);
    }
}
