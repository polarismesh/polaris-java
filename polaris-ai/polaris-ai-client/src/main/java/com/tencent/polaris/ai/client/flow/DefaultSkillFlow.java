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

package com.tencent.polaris.ai.client.flow;

import com.tencent.polaris.ai.api.flow.SkillFlow;
import com.tencent.polaris.ai.client.internal.SkillPersistentHandler;
import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.api.config.skill.SkillConnectorConfig;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.skill.SkillConnector;
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.plugin.skill.SkillListResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default skill flow with persist fallback.
 */
public class DefaultSkillFlow implements SkillFlow {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSkillFlow.class);

    private static final String FORMAT_MARKDOWN = "markdown";

    private SkillConnector skillConnector;

    private SkillPersistentHandler persistentHandler;

    private boolean fallbackToLocalCache;

    private final ConcurrentMap<String, SkillGetResponse> getSkillCache = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, SkillDownloadResponse> markdownCache = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, String> activeVersionCache = new ConcurrentHashMap<>();

    public DefaultSkillFlow() {
    }

    @JustForTest
    public DefaultSkillFlow(SkillConnector skillConnector, SkillPersistentHandler persistentHandler,
            boolean fallbackToLocalCache) {
        this.skillConnector = skillConnector;
        this.persistentHandler = persistentHandler;
        this.fallbackToLocalCache = fallbackToLocalCache;
    }

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        SkillConnectorConfig connectorConfig = sdkContext.getConfig().getSkill().getServerConnector();
        this.skillConnector = (SkillConnector) sdkContext.getExtensions().getPlugins()
                .getPlugin(PluginTypes.SKILL_CONNECTOR.getBaseType(), connectorConfig.getConnectorType());
        this.fallbackToLocalCache = Boolean.TRUE.equals(connectorConfig.getFallbackToLocalCache());
        this.persistentHandler = createPersistentHandler(connectorConfig);
        registerDestroyHook(sdkContext);
    }

    @Override
    public SkillGetResponse getSkill(SkillGetRequest request) throws PolarisException {
        SkillGetResponse result;
        try {
            result = skillConnector.getSkill(request);
            persistGetSuccess(request, result);
        } catch (PolarisException exception) {
            result = fallbackGetSkill(request, exception);
            if (result == null) {
                throw exception;
            }
        }
        return result;
    }

    @Override
    public SkillListResponse listSkills(SkillListRequest request) throws PolarisException {
        return skillConnector.listSkills(request);
    }

    @Override
    public SkillDownloadResponse downloadSkill(SkillDownloadRequest request) throws PolarisException {
        SkillDownloadResponse result;
        try {
            result = skillConnector.downloadSkill(request);
            persistDownloadSuccess(request, result);
        } catch (PolarisException exception) {
            result = fallbackDownload(request, exception);
            if (result == null) {
                throw exception;
            }
        }
        return result;
    }

    private void persistGetSuccess(SkillGetRequest request, SkillGetResponse response) {
        if (isExecuteSuccess(response.getCode()) && persistentHandler != null) {
            String version = resolveGetVersion(request, response);
            if (StringUtils.isNotBlank(version)) {
                getSkillCache.put(cacheKey(request.getNamespace(), request.getName(), version), response);
                persistentHandler.asyncSaveGetSkill(request.getNamespace(), request.getName(), version, response);
                saveActiveIfNeeded(request.getNamespace(), request.getName(), request.getVersion(), version);
            }
        }
    }

    private void persistDownloadSuccess(SkillDownloadRequest request, SkillDownloadResponse response) {
        if (isExecuteSuccess(response.getCode()) && persistentHandler != null) {
            String version = resolveDownloadVersion(request, response);
            if (StringUtils.isNotBlank(version)) {
                String format = normalizeFormat(request.getFormat());
                if (FORMAT_MARKDOWN.equals(format)) {
                    markdownCache.put(cacheKey(request.getNamespace(), request.getName(), version), response);
                }
                persistentHandler.asyncSaveDownload(request.getNamespace(), request.getName(), version, format,
                        response);
                saveActiveIfNeeded(request.getNamespace(), request.getName(), request.getVersion(), version);
            }
        }
    }

    private SkillGetResponse fallbackGetSkill(SkillGetRequest request, PolarisException exception) {
        SkillGetResponse result = null;
        if (allowFallback(exception)) {
            String version = resolveFallbackVersion(request.getNamespace(), request.getName(), request.getVersion());
            if (StringUtils.isNotBlank(version)) {
                result = getSkillCache.get(cacheKey(request.getNamespace(), request.getName(), version));
                if (result == null) {
                    result = persistentHandler.loadGetSkill(request.getNamespace(), request.getName(), version);
                }
            }
        }
        return result;
    }

    private SkillDownloadResponse fallbackDownload(SkillDownloadRequest request, PolarisException exception) {
        SkillDownloadResponse result = null;
        if (allowFallback(exception)) {
            String version = resolveFallbackVersion(request.getNamespace(), request.getName(), request.getVersion());
            String format = normalizeFormat(request.getFormat());
            if (StringUtils.isNotBlank(version)) {
                result = loadDownloadCache(request.getNamespace(), request.getName(), version, format);
            }
        }
        return result;
    }

    private SkillDownloadResponse loadDownloadCache(String namespace, String name, String version, String format) {
        SkillDownloadResponse result = null;
        if (FORMAT_MARKDOWN.equals(format)) {
            result = markdownCache.get(cacheKey(namespace, name, version));
        }
        if (result == null) {
            result = persistentHandler.loadDownload(namespace, name, version, format);
        }
        return result;
    }

    private boolean allowFallback(PolarisException exception) {
        return fallbackToLocalCache && persistentHandler != null && ErrorCode.NETWORK_ERROR.equals(exception.getCode());
    }

    private void saveActiveIfNeeded(String namespace, String name, String requestVersion, String resolvedVersion) {
        if (StringUtils.isBlank(requestVersion) && StringUtils.isNotBlank(resolvedVersion)) {
            activeVersionCache.put(activeKey(namespace, name), resolvedVersion);
            persistentHandler.asyncSaveActiveVersion(namespace, name, resolvedVersion);
        }
    }

    private String resolveFallbackVersion(String namespace, String name, String requestVersion) {
        String version = requestVersion;
        if (StringUtils.isBlank(version)) {
            version = activeVersionCache.get(activeKey(namespace, name));
            if (StringUtils.isBlank(version)) {
                version = persistentHandler.loadActiveVersion(namespace, name);
            }
        }
        return version;
    }

    private String resolveGetVersion(SkillGetRequest request, SkillGetResponse response) {
        String version = request.getVersion();
        if (StringUtils.isBlank(version) && response.getResourceVersion() != null) {
            version = response.getResourceVersion().getVersion();
        }
        return version;
    }

    private String resolveDownloadVersion(SkillDownloadRequest request, SkillDownloadResponse response) {
        String version = request.getVersion();
        if (StringUtils.isBlank(version)) {
            version = response.getVersion();
        }
        return version;
    }

    private String normalizeFormat(String format) {
        String result = FORMAT_MARKDOWN;
        if (StringUtils.isNotBlank(format)) {
            result = format;
        }
        return result;
    }

    private boolean isExecuteSuccess(int code) {
        return code == ServerCodes.EXECUTE_SUCCESS || code == 0;
    }

    private String cacheKey(String namespace, String name, String version) {
        return namespace + "#" + name + "#" + version;
    }

    private String activeKey(String namespace, String name) {
        return namespace + "#" + name;
    }

    private SkillPersistentHandler createPersistentHandler(SkillConnectorConfig connectorConfig) {
        SkillPersistentHandler handler = null;
        try {
            boolean persistEnable = Boolean.TRUE.equals(connectorConfig.getPersistEnable());
            handler = new SkillPersistentHandler(connectorConfig.getPersistDir(), persistEnable,
                    connectorConfig.getPersistMaxWriteRetry(), connectorConfig.getPersistMaxReadRetry(),
                    connectorConfig.getPersistRetryInterval());
        } catch (IOException e) {
            LOG.warn("skill persist handler init fail: {}", e.getMessage());
        }
        return handler;
    }

    private void registerDestroyHook(SDKContext sdkContext) {
        sdkContext.registerDestroyHook(new Destroyable() {
            @Override
            protected void doDestroy() {
                if (persistentHandler != null) {
                    persistentHandler.doDestroy();
                }
            }
        });
    }
}
