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

package com.tencent.polaris.configuration.client.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.client.ReportClientRequestCustomizer;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.server.ReportClientRequest;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 配置监听画像请求定制器，向 ReportClientRequest 追加 config_enabled 和 config_metadata 字段。
 *
 * @author fishtailfu
 */
public class ConfigWatchReportRequestCustomizer implements ReportClientRequestCustomizer, PluginConfigProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigWatchReportRequestCustomizer.class);

    public static final String NAME = "config-watch";

    private final ConcurrentMap<ConfigFileMetadata, RemoteConfigFileRepo> watchedFiles = new ConcurrentHashMap<>();

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private boolean enable;

    @Override
    public void init(InitContext ctx) throws PolarisException {
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        ConfigWatchReportRequestCustomizerConfig config = extensions.getConfiguration().getGlobal()
                .getReportClientRequestCustomizer()
                .getPluginConfig(getName(), ConfigWatchReportRequestCustomizerConfig.class);
        this.enable = config.isEnable();
    }

    public void register(RemoteConfigFileRepo repo) {
        watchedFiles.putIfAbsent(toMetadataKey(repo.getConfigFileMetadata()), repo);
    }

    public void unregister(ConfigFileMetadata metadata) {
        watchedFiles.remove(toMetadataKey(metadata));
    }

    /**
     * 按 (namespace, group, fileName) 查询单个监听配置文件的仓库。
     *
     * @param metadata 配置文件坐标
     * @return 命中返回仓库，未监听返回 null
     */
    public RemoteConfigFileRepo getWatchedFile(ConfigFileMetadata metadata) {
        return watchedFiles.get(toMetadataKey(metadata));
    }

    private ConfigFileMetadata toMetadataKey(ConfigFileMetadata metadata) {
        return new DefaultConfigFileMetadata(metadata.getNamespace(), metadata.getFileGroup(), metadata.getFileName());
    }

    @Override
    public void customize(ReportClientRequest request) {
        if (!enable) {
            return;
        }
        request.setConfigEnabled(Boolean.TRUE);
        request.setConfigMetadata(buildSnapshotJson());
        if (LOG.isDebugEnabled()) {
            LOG.debug("customize config_metadata: {}", request.getConfigMetadata());
        }
    }

    private String buildSnapshotJson() {
        List<ConfigWatchInfo> watchList = new ArrayList<>(watchedFiles.size());
        for (Map.Entry<ConfigFileMetadata, RemoteConfigFileRepo> entry : watchedFiles.entrySet()) {
            ConfigFileMetadata metadata = entry.getKey();
            RemoteConfigFileRepo repo = entry.getValue();
            ConfigFileSnapshot snapshot = repo.getSnapshot();
            String md5 = snapshot == null ? "" : snapshot.getMd5();
            if (md5 == null) {
                md5 = "";
            }
            watchList.add(new ConfigWatchInfo(
                    metadata.getNamespace(),
                    metadata.getFileGroup(),
                    metadata.getFileName(),
                    snapshot == null ? 0 : snapshot.getVersion(),
                    md5));
        }
        return gson.toJson(java.util.Collections.singletonMap("config_watch", watchList));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.REPORT_CLIENT_REQUEST_CUSTOMIZER.getBaseType();
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return ConfigWatchReportRequestCustomizerConfig.class;
    }

    @Override
    public void destroy() {
        watchedFiles.clear();
    }
}
