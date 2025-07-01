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

import com.google.common.collect.Lists;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author lepdou 2022-03-01
 */
public abstract class AbstractConfigFileRepo implements ConfigFileRepo {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigFileRepo.class);

    protected ConfigFileMetadata configFileMetadata;
    private final List<ConfigFileRepoChangeListener> listeners = Lists.newCopyOnWriteArrayList();

    protected SDKContext sdkContext;

    protected AbstractConfigFileRepo(SDKContext sdkContext, ConfigFileMetadata configFileMetadata) {
        this.sdkContext = sdkContext;
        this.configFileMetadata = configFileMetadata;
    }

    protected boolean pull() {
        try {
            doPull();
            return true;
        } catch (Throwable t) {
            LOGGER.warn("[Config] load config file failed. config file = {}", configFileMetadata, t);
        }
        return false;
    }

    protected abstract void doPull();

    @Override
    public void addChangeListener(ConfigFileRepoChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeChangeListener(ConfigFileRepoChangeListener listener) {
        listeners.remove(listener);
    }

    protected void fireChangeEvent(ConfigFile configFile) {
        for (ConfigFileRepoChangeListener listener : listeners) {
            try {
                listener.onChange(configFileMetadata, configFile);
            } catch (Throwable t) {
                LOGGER.error("[Config] invoke config file repo change listener failed. config file = {}, listener = {}",
                        configFileMetadata, listener.getClass(), t);
            }
        }
    }

    public ConfigFileMetadata getConfigFileMetadata() {
        return configFileMetadata;
    }
}
