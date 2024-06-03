/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.plugins.configuration.connector.consul;

import com.ecwid.consul.v1.*;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.plugin.configuration.ConfigPublishFile;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.configuration.connector.consul.utils.ConsulConfigFileUtils;
import com.tencent.polaris.specification.api.v1.model.CodeProto;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.CONSUL_FILE_CONNECTOR_TYPE;

/**
 * Consul config file connector.
 *
 * @author Haotian Zhang
 */
public class ConsulConfigFileConnector implements ConfigFileConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulConfigFileConnector.class);

    /**
     * If server connector initialized.
     */
    private boolean initialized = false;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private ConsulClient consulClient;

    private ScheduledExecutorService scheduledExecutorService;

    private final Map<String, ScheduledFuture<?>> watchFutures = new ConcurrentHashMap<>();

    public final Map<String, Long> consulIndexes = new ConcurrentHashMap<>();

    public final Map<String, Long> consulModifyIndexes = new ConcurrentHashMap<>();

    public final Map<String, ConfigFileResponse> responseCache = new ConcurrentHashMap<>();

    private ConsulConfigContext consulConfigContext;

    /**
     * config file changed queue
     */
    private final BlockingQueue<RefreshEventData> blockingQueue = new ArrayBlockingQueue<>(1024);

    @Override
    public void init(InitContext ctx) throws PolarisException {
        if (!initialized) {
            // init consul client
            ConnectorConfigImpl connectorConfig = ctx.getConfig().getConfigFile().getServerConnector();
            String address = connectorConfig.getAddresses().get(0);
            int lastIndex = address.lastIndexOf(":");
            String agentHost = address.substring(0, lastIndex);
            int agentPort = Integer.parseInt(address.substring(lastIndex + 1));
            LOGGER.info("Connect to consul config server : [{}].", address);
            consulClient = new ConsulClient(new ConsulRawClient(agentHost, agentPort));

            // init consul config context.
            consulConfigContext = new ConsulConfigContext();
            // token
            String tokenStr = connectorConfig.getToken();
            if (StringUtils.isNotBlank(tokenStr)) {
                consulConfigContext.setAclToken(tokenStr);
            }
            Map<String, String> metadata = connectorConfig.getMetadata();
            if (CollectionUtils.isNotEmpty(metadata)) {
                String waitTimeStr = metadata.get(ConsulConfigConstants.WAIT_TIME_KEY);
                if (StringUtils.isNotBlank(waitTimeStr)) {
                    try {
                        int waitTime = Integer.parseInt(waitTimeStr);
                        consulConfigContext.setWaitTime(waitTime);
                    } catch (Exception e) {
                        LOGGER.warn("wait time string {} is not integer.", waitTimeStr, e);
                    }
                }

                String delayStr = metadata.get(ConsulConfigConstants.DELAY_KEY);
                if (StringUtils.isNotBlank(delayStr)) {
                    try {
                        int delay = Integer.parseInt(delayStr);
                        consulConfigContext.setDelay(delay);
                    } catch (Exception e) {
                        LOGGER.warn("delay string {} is not integer.", delayStr, e);
                    }
                }

                String consulErrorSleepStr = metadata.get(ConsulConfigConstants.CONSUL_ERROR_SLEEP_KEY);
                if (StringUtils.isNotBlank(consulErrorSleepStr)) {
                    try {
                        long consulErrorSleep = Long.parseLong(consulErrorSleepStr);
                        consulConfigContext.setConsulErrorSleep(consulErrorSleep);
                    } catch (Exception e) {
                        LOGGER.warn("delay string {} is not integer.", consulErrorSleepStr, e);
                    }
                }
            }

            // init watch executor.
            this.scheduledExecutorService = Executors.newScheduledThreadPool(8, new NamedThreadFactory("consul-configuration-watch"));

            initialized = true;
            LOGGER.info("Consul config file connector is initialized.");
        } else {
            LOGGER.warn("Consul config file connector is already initialized.");
        }
    }

    @Override
    public ConfigFileResponse getConfigFile(ConfigFile configFile) {
        if (this.running.get()) {
            String keyPrefix = ConsulConfigFileUtils.toConsulKVKeyPrefix(configFile);
            if (!watchFutures.containsKey(keyPrefix)) {
                LOGGER.info("Start watching consul config for keyPrefix '{}'", keyPrefix);
                this.watchFutures.put(keyPrefix, this.scheduledExecutorService.scheduleWithFixedDelay(() -> {
                    try {
                        ConfigFileResponse configFileResponse = getKVValues(configFile, keyPrefix);
                        if (configFileResponse != null && configFileResponse.getCode() == CodeProto.Code.ExecuteSuccess.getNumber()) {
                            blockingQueue.offer(new RefreshEventData(keyPrefix, configFile, configFileResponse));
                        } else {
                            if (configFileResponse != null) {
                                LOGGER.debug("Watch consul config '{}' with {}.", keyPrefix, configFileResponse.getMessage());
                            } else {
                                LOGGER.debug("Watch consul config '{}' do nothing.", keyPrefix);
                            }
                        }
                    } catch (Exception exception) {
                        LOGGER.error("Watch consul config '{}' failed.", keyPrefix, exception);
                    }
                }, consulConfigContext.getDelay(), consulConfigContext.getDelay(), TimeUnit.MILLISECONDS));
            }
            if (responseCache.containsKey(keyPrefix)) {
                ConfigFileResponse configFileResponse = responseCache.get(keyPrefix);
                if (StringUtils.equals(configFileResponse.getMessage(), ConsulConfigConstants.CONFIG_FILE_DELETED_MESSAGE)) {
                    responseCache.remove(keyPrefix);
                }
                return configFileResponse;
            }
            return getKVValues(configFile, keyPrefix);
        }
        return new ConfigFileResponse(ServerCodes.NOT_FOUND_RESOURCE, ConsulConfigConstants.CONFIG_FILE_DELETED_MESSAGE, null);
    }

    private ConfigFileResponse getKVValues(ConfigFile configFile, String keyPrefix) {
        // 使用default值逻辑处理
        Long currentIndex = this.consulIndexes.getOrDefault(keyPrefix, ConsulConfigConstants.EMPTY_VALUE_CONSUL_INDEX);
        Long currentModifyIndex = this.consulModifyIndexes.getOrDefault(keyPrefix, ConsulConfigConstants.EMPTY_VALUE_CONSUL_INDEX);
        LOGGER.debug("Get consul config for keyPrefix '{}' with index {} and modify index {}", keyPrefix, currentIndex, currentModifyIndex);

        // use the consul ACL token if found
        String aclToken = consulConfigContext.getAclToken();
        if (StringUtils.isEmpty(aclToken)) {
            aclToken = null;
        }

        try {
            Response<List<GetValue>> response = this.consulClient.getKVValues(keyPrefix, aclToken,
                    new QueryParams(consulConfigContext.getWaitTime(), currentIndex));
            ConfigFile resultConfigFile = new ConfigFile(configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName());
            return handleResponse(resultConfigFile, keyPrefix, currentIndex, currentModifyIndex, response);
        } catch (OperationException operationException) {
            handleOperationException(keyPrefix, currentIndex, currentModifyIndex, operationException);
        } catch (Exception exception) {
            handleException(keyPrefix, currentIndex, currentModifyIndex, exception);
        }
        return null;
    }

    private ConfigFileResponse handleResponse(ConfigFile configFile, String keyPrefix, Long currentIndex,
                                              Long currentModifyIndex, Response<List<GetValue>> response) {
        if (response.getValue() == null) {
            if (responseCache.containsKey(keyPrefix)) {
                Long newIndex = response.getConsulIndex();
                this.consulIndexes.put(keyPrefix, newIndex);
                this.consulModifyIndexes.put(keyPrefix, ConsulConfigConstants.EMPTY_VALUE_CONSUL_INDEX);
                configFile.setVersion(newIndex);
                LOGGER.info("consul config file '{}' has been deleted.", keyPrefix);
                return new ConfigFileResponse(CodeProto.Code.ExecuteSuccess.getNumber(),
                        ConsulConfigConstants.CONFIG_FILE_DELETED_MESSAGE, configFile);
            }
            return new ConfigFileResponse(CodeProto.Code.NotFoundResource.getNumber(), "config file not found.", null);
        }

        int code = CodeProto.Code.ExecuteSuccess.getNumber();
        String message = "execute success";
        if (response.getValue() != null) {
            Long newIndex = response.getConsulIndex();
            // 新增一个ModifyIndex用于记录实际内容变更
            Long newModifyIndex = CollectionUtils.isEmpty(response.getValue()) ? ConsulConfigConstants.EMPTY_VALUE_CONSUL_INDEX
                    : response.getValue().get(0).getModifyIndex();
            // 由于精确监听，直接判断newIndex与currentIndex一致性
            if (newIndex != null && !newIndex.equals(currentIndex)) {
                // 根据currentModifyIndex和newModifyIndex判断内容是否实际发生了变化
                if (!newModifyIndex.equals(currentModifyIndex)) {
                    LOGGER.info("KeyPrefix '{}' has new index {} and new modify index {} with old index {} and old modify index {}",
                            keyPrefix, newIndex, newModifyIndex, currentIndex, currentModifyIndex);
                } else if (LOGGER.isDebugEnabled()) {
                    code = CodeProto.Code.DataNoChange.getNumber();
                    message = "config data is no change";
                    LOGGER.debug("KeyPrefix '{}' not modified with new index {}, index {} and modify index {}",
                            keyPrefix, newIndex, currentIndex, currentModifyIndex);
                }
                // 在Consul中不存在自定义KEY时，此处的逻辑可以避免response实时返回，不断的触发retry
                this.consulIndexes.put(keyPrefix, newIndex);
                this.consulModifyIndexes.put(keyPrefix, newModifyIndex);
            } else if (LOGGER.isDebugEnabled()) {
                code = CodeProto.Code.DataNoChange.getNumber();
                message = "config data is no change";
                LOGGER.debug("KeyPrefix '{}' unchanged with index {} and modify index {}", keyPrefix, currentIndex, currentModifyIndex);
            }
        }
        transferFromGetValueList(configFile, response.getValue());
        ConfigFileResponse configFileResponse = new ConfigFileResponse(code, message, configFile);
        // for first time
        if (!responseCache.containsKey(keyPrefix)) {
            responseCache.put(keyPrefix, configFileResponse);
        }
        return configFileResponse;
    }

    private void transferFromGetValueList(ConfigFile configFile, List<GetValue> getValueList) {
        if (CollectionUtils.isEmpty(getValueList)) {
            return;
        }

        // 只取第一个
        GetValue firstValue = getValueList.get(0);
        String decodedValue = firstValue.getDecodedValue();
        configFile.setContent(decodedValue);
        configFile.setMd5(DigestUtils.md5Hex(decodedValue));
        configFile.setVersion(firstValue.getModifyIndex());
        configFile.setReleaseTime(new Date());
    }

    private void handleOperationException(String keyPrefix, Long currentIndex,
                                          Long currentModifyIndex, OperationException operationException) {
        LOGGER.error("KeyPrefix '{}' with operation exception with index {} and modify index {}.",
                keyPrefix, currentIndex, currentModifyIndex, operationException);
        try {
            Thread.sleep(consulConfigContext.getConsulErrorSleep());
        } catch (Exception e) {
            LOGGER.error("error in sleep, msg: " + e.getMessage());
        }
        throw ServerErrorResponseException.build(CodeProto.Code.ExecuteException.getNumber(), operationException.toString());
    }

    private void handleException(String keyPrefix, Long currentIndex, Long currentModifyIndex, Exception exception) {
        LOGGER.error("KeyPrefix '{}' with exception with index {} and modify index {}.",
                keyPrefix, currentIndex, currentModifyIndex, exception);
        try {
            Thread.sleep(consulConfigContext.getConsulErrorSleep());
        } catch (Exception e) {
            LOGGER.error("error in sleep, msg: " + e.getMessage());
        }
        throw ServerErrorResponseException.build(CodeProto.Code.ExecuteException.getNumber(), exception.toString());
    }

    @Override
    public ConfigFileResponse watchConfigFiles(List<ConfigFile> configFiles) {
        try {
            while (true) {
                RefreshEventData refreshEventData = blockingQueue.poll(30, TimeUnit.SECONDS);
                if (refreshEventData != null) {
                    Optional<ConfigFile> optional = configFiles.stream()
                            .filter(configFile -> StringUtils.equals(refreshEventData.getKeyPrefix(), ConsulConfigFileUtils.toConsulKVKeyPrefix(configFile)))
                            .findFirst();
                    if (optional.isPresent()) {
                        responseCache.put(refreshEventData.getKeyPrefix(), refreshEventData.getConfigFileResponse());
                        return refreshEventData.getConfigFileResponse();
                    }
                }
            }
        } catch (InterruptedException e) {
            LOGGER.warn("watch consul config file interrupt.", e);
        }
        return null;
    }

    @Override
    public String getName() {
        return CONSUL_FILE_CONNECTOR_TYPE;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType();
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        if (!this.running.compareAndSet(false, true)) {
            throw new PolarisException(ErrorCode.PLUGIN_ERROR, "start consul config file connector failed.");
        }
    }

    @Override
    public void destroy() {
        if (this.running.compareAndSet(true, false)) {
            // cancel watch future.
            if (CollectionUtils.isEmpty(this.watchFutures)) {
                for (ScheduledFuture<?> watchFuture : this.watchFutures.values()) {
                    watchFuture.cancel(true);
                }
            }

            // shutdown scheduledExecutorService.
            if (this.scheduledExecutorService != null && !this.scheduledExecutorService.isShutdown()) {
                this.scheduledExecutorService.shutdown();
                try {
                    if (!this.scheduledExecutorService.awaitTermination(2, TimeUnit.SECONDS)) {
                        this.scheduledExecutorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("shutdown scheduledExecutorService failed.", e);
                    this.scheduledExecutorService.shutdownNow();
                }
            }
            LOGGER.info("Consul config file connector is destroyed.");
        } else {
            LOGGER.info("Consul config file connector is not in running state.");
        }
    }

    @Override
    public ConfigFileResponse createConfigFile(ConfigFile configFile) {
        throw new UnsupportedOperationException("not support createConfigFile");
    }

    @Override
    public ConfigFileResponse updateConfigFile(ConfigFile configFile) {
        throw new UnsupportedOperationException("not support updateConfigFile");
    }

    @Override
    public ConfigFileResponse releaseConfigFile(ConfigFile configFile) {
        throw new UnsupportedOperationException("not support releaseConfigFile");
    }

    @Override
    public ConfigFileResponse upsertAndPublishConfigFile(ConfigPublishFile request) {
        throw new UnsupportedOperationException("not support upsertAndPublishConfigFile");
    }
}
