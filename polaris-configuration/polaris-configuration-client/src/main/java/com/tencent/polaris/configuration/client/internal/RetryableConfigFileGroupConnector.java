package com.tencent.polaris.configuration.client.internal;

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

                LOGGER.error("[Config] get config file metadata list hits retry strategy. retry times = {}, namespace = {}, fileGroupName = {}",
                        retryTimes, configFileGroupMetadata.getNamespace(), configFileGroupMetadata.getFileGroupName());
                retryPolicy.fail();
                retryTimes++;
                retryPolicy.executeDelay();
            } catch (Throwable t) {
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
