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

package com.tencent.polaris.configuration.client;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFilePublishService;
import com.tencent.polaris.configuration.api.rpc.ConfigPublishRequest;
import com.tencent.polaris.configuration.api.rpc.CreateConfigFileRequest;
import com.tencent.polaris.configuration.api.rpc.ReleaseConfigFileRequest;
import com.tencent.polaris.configuration.api.rpc.UpdateConfigFileRequest;
import com.tencent.polaris.configuration.client.internal.ConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import com.tencent.polaris.configuration.client.util.ConfigFileUtils;

/**
 * @author fabian4 2022-03-08
 */
public class DefaultConfigFilePublishService extends BaseEngine implements ConfigFilePublishService {

    private ConfigFileManager configFileManager;

    public DefaultConfigFilePublishService(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() throws PolarisException {
        configFileManager = new ConfigFileManager(sdkContext);
    }

    @Override
    public ConfigFileResponse createConfigFile(String namespace, String fileGroup, String fileName, String content) {
        return createConfigFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName), content);
    }

    @Override
    public ConfigFileResponse createConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        CreateConfigFileRequest request = new CreateConfigFileRequest();
        request.setFilename(configFileMetadata.getFileName());
        request.setGroup(configFileMetadata.getFileGroup());
        request.setNamespace(configFileMetadata.getNamespace());
        request.setContent(content);
        return configFileManager.createConfigFile(request);
    }

    @Override
    public ConfigFileResponse updateConfigFile(String namespace, String fileGroup, String fileName, String content) {
        return updateConfigFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName), content);
    }

    @Override
    public ConfigFileResponse updateConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        UpdateConfigFileRequest request = new UpdateConfigFileRequest();
        request.setFilename(configFileMetadata.getFileName());
        request.setGroup(configFileMetadata.getFileGroup());
        request.setNamespace(configFileMetadata.getNamespace());
        request.setContent(content);
        return configFileManager.updateConfigFile(request);
    }

    @Override
    public ConfigFileResponse releaseConfigFile(String namespace, String fileGroup, String fileName) {
        return releaseConfigFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName));
    }

    @Override
    public ConfigFileResponse releaseConfigFile(ConfigFileMetadata configFileMetadata) {
        ReleaseConfigFileRequest request = new ReleaseConfigFileRequest();
        request.setFilename(configFileMetadata.getFileName());
        request.setGroup(configFileMetadata.getFileGroup());
        request.setNamespace(configFileMetadata.getNamespace());
        return release(request);
    }

    @Override
    public ConfigFileResponse create(CreateConfigFileRequest request) {
        request.verify();
        return configFileManager.createConfigFile(request);
    }

    @Override
    public ConfigFileResponse update(UpdateConfigFileRequest request) {
        request.verify();
        return configFileManager.updateConfigFile(request);
    }

    @Override
    public ConfigFileResponse release(ReleaseConfigFileRequest request) {
        request.verify();
        return configFileManager.releaseConfigFile(request);
    }

    @Override
    public ConfigFileResponse upsertAndPublish(ConfigPublishRequest request) {
        request.verify();
        return configFileManager.upsertAndPublish(request);
    }
}
