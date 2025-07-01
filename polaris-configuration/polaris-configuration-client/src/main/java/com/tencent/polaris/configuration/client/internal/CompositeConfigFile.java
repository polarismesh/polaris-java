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

import com.tencent.polaris.configuration.api.core.ConfigFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.client.util.ConfigFileUtils;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The composite file consists of many config files.
 *
 * @author Haotian Zhang
 */
public class CompositeConfigFile implements ConfigKVFile {

    private final List<ConfigKVFile> configKVFiles;

    private final AtomicReference<Properties> properties = new AtomicReference<>();

    public CompositeConfigFile(List<ConfigKVFile> configKVFiles) {
        this.configKVFiles = Collections.unmodifiableList(configKVFiles);
        Properties properties = new Properties();
        for (ConfigKVFile configKVFile : configKVFiles) {
            for (String name : configKVFile.getPropertyNames()) {
                if (!properties.containsKey(name)) {
                    properties.put(name, configKVFile.getProperty(name, null));
                }
            }
        }
        this.properties.set(properties);
    }

    public List<ConfigKVFile> getConfigKVFiles() {
        return configKVFiles;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = properties.get().getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public Integer getIntProperty(String key, Integer defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getLongProperty(String key, Long defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Short getShortProperty(String key, Short defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Float getFloatProperty(String key, Float defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Double getDoubleProperty(String key, Double defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Byte getByteProperty(String key, Byte defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean getBooleanProperty(String key, Boolean defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getArrayProperty(String key, String delimiter, String[] defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getJsonProperty(String key, Class<T> clazz, T defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getJsonProperty(String key, Type typeOfT, T defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getPropertyNames() {
        return ConfigFileUtils.stringPropertyNames(properties.get());
    }

    @Override
    public void addChangeListener(ConfigKVFileChangeListener listener) {
        for (ConfigKVFile configKVFile : configKVFiles) {
            configKVFile.addChangeListener(listener);
        }
    }

    @Override
    public void removeChangeListener(ConfigKVFileChangeListener listener) {
        for (ConfigKVFile configKVFile : configKVFiles) {
            configKVFile.removeChangeListener(listener);
        }
    }

    @Override
    public String getContent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T asJson(Class<T> objectType, T defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T asJson(Type typeOfT, T defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasContent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMd5() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChangeListener(ConfigFileChangeListener listener) {
        for (ConfigKVFile configKVFile : configKVFiles) {
            configKVFile.addChangeListener(listener);
        }
    }

    @Override
    public void removeChangeListener(ConfigFileChangeListener listener) {
        for (ConfigKVFile configKVFile : configKVFiles) {
            configKVFile.removeChangeListener(listener);
        }
    }

    @Override
    public String getNamespace() {
        return "";
    }

    @Override
    public String getFileGroup() {
        return "";
    }

    @Override
    public String getFileName() {
        return "";
    }

    @Override
    public String getFileVersion() {
        return "";
    }
}
