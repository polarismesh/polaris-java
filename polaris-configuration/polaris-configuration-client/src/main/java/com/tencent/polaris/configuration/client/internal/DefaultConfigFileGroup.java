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
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupChangedEvent;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultConfigFileGroup extends DefaultConfigFileGroupMetadata implements ConfigFileGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfigFileGroup.class);

    protected List<ConfigFileMetadata> configFileMetadataList;

    protected static ExecutorService notifyExecutorService = Executors.newSingleThreadExecutor(new NamedThreadFactory(DefaultConfigFileGroup.class.getSimpleName()));
    private final List<ConfigFileGroupChangeListener> listeners = Lists.newCopyOnWriteArrayList();

    public DefaultConfigFileGroup(String namespace, String fileGroupName, List<ConfigFileMetadata> configFileMetadataList) {
        super(namespace, fileGroupName);
        this.configFileMetadataList = configFileMetadataList;
    }

    @Override
    public List<ConfigFileMetadata> getConfigFileMetadataList() {
        return configFileMetadataList;
    }

    @Override
    public void addChangeListener(ConfigFileGroupChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeChangeListener(ConfigFileGroupChangeListener listener) {
        listeners.remove(listener);
    }

    protected void trigger(ConfigFileGroupChangedEvent event) {
        for (ConfigFileGroupChangeListener listener : listeners) {
            notifyExecutorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    listener.onChange(event);
                    LOGGER.info("[Config] invoke config file group change listener success. listener = {}, duration = {} ms",
                            listener.getClass().getName(), System.currentTimeMillis() - startTime);
                } catch (Throwable t) {
                    LOGGER.error("[Config] failed to invoke config file group change listener. listener = {}, event = {}",
                            listener.getClass().getName(), event, t);
                }
            });
        }
    }

    @Override
    public String toString() {
        return "DefaultConfigFileGroup{" +
                "configFileMetadataList=" + configFileMetadataList +
                ", listeners=" + listeners +
                "} " + super.toString();
    }
}
