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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.configuration.api.core.*;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFile extends DefaultConfigFileMetadata implements ConfigFile, ConfigFileRepoChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfigFile.class);
    private static final Gson gson = new Gson();
    private static final String OBJECT_CACHE_KEY = "object";

    protected static ExecutorService notifyExecutorService;

    private final List<ConfigFileChangeListener> listeners = Lists.newCopyOnWriteArrayList();
    private final ConfigFileRepo configFileRepo;
    protected final ConfigFileConfig configFileConfig;
    private String content;
    //与 content 同一次发布的版本名，随 content 一起更新
    private volatile String fileVersion;

    private volatile Cache<String, Object> objectCache;
    private final AtomicLong cacheVersion;

    static {
        notifyExecutorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("Configuration-Notify"));
    }

    public DefaultConfigFile(String namespace, String fileGroup, String fileName, ConfigFileRepo configFileRepo,
            ConfigFileConfig configFileConfig) {
        super(namespace, fileGroup, fileName);

        this.configFileRepo = configFileRepo;
        this.configFileConfig = configFileConfig;
        this.cacheVersion = new AtomicLong();

        initialize();
    }

    protected void initialize() {
        content = configFileRepo.getContent();
        fileVersion = configFileRepo.getVersionName();

        //监听远端变更事件
        configFileRepo.addChangeListener(this);
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public <T> T asJson(Class<T> objectType, T defaultValue) {
        return convertToJson(OBJECT_CACHE_KEY, this.content, null, objectType, defaultValue);
    }

    @Override
    public <T> T asJson(Type typeOfT, T defaultValue) {
        return convertToJson(OBJECT_CACHE_KEY, this.content, typeOfT, null, defaultValue);
    }

    protected <T> T convertToJson(String key, String json, Type typeOfT, Class<T> clazz, T defaultValue) {
        if (StringUtils.isBlank(json)) {
            return defaultValue;
        }

        String type = null;

        try {
            createObjectCacheIfAbsent();

            Object cachedObject = objectCache.getIfPresent(key);
            if (cachedObject != null) {
                return (T) cachedObject;
            }

            long currentCacheVersion = cacheVersion.get();

            T result;
            if (clazz != null) {
                result = gson.fromJson(json, clazz);
                type = clazz.getTypeName();
            } else {
                result = gson.fromJson(json, typeOfT);
                type = typeOfT.getTypeName();
            }

            synchronized (this) {
                if (result != null && currentCacheVersion == cacheVersion.get()) {
                    objectCache.put(OBJECT_CACHE_KEY, result);
                }
            }

            return result;
        } catch (Throwable t) {
            // 不把异常对象交给 logger：反序列化异常的消息可能带上配置正文片段（如非法数值会
            // 原样回显 For input string: "..."），加密配置在此已是解密后的明文
            LOGGER.error("[Config] convert json file content to given class error. class type = {}, error = {}",
                    type, t.getClass().getName());
        }
        return defaultValue;
    }

    private void createObjectCacheIfAbsent() {
        if (objectCache == null) {
            synchronized (this) {
                if (objectCache == null) {
                    objectCache = CacheBuilder.newBuilder()
                            .maximumSize(configFileConfig.getPropertiesValueCacheSize())
                            .expireAfterAccess(configFileConfig.getPropertiesValueExpireTime(), TimeUnit.MINUTES)
                            .build();
                }
            }
        }
    }

    @Override
    public boolean hasContent() {
        return !StringUtils.isBlank(content);
    }

    @Override
    public String getMd5() {
        return configFileRepo.getMd5();
    }

    @Override
    public boolean isEncrypted() {
        return configFileRepo.isEncrypted();
    }

    /**
     * 返回当前生效配置的版本名。
     *
     * <p>父类元数据里的版本在构造时固定为 null，这里改为跟随生效的配置，
     * 便于上层框架在没有变更事件时（如启动期间补齐错过的变更）也能拿到版本名。
     * 版本名在 {@link #onChange} 中与 content 一起更新，而不是直接读仓库：仓库先替换配置再通知变更，
     * 直接读仓库会在通知完成前拿到新版本名，与仍是旧值的 content 不一致。
     *
     * @return 版本名，内存中没有配置时返回 null
     */
    @Override
    public String getFileVersion() {
        return fileVersion;
    }

    @Override
    public void addChangeListener(ConfigFileChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeChangeListener(ConfigFileChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onChange(ConfigFileMetadata configFileMetadata, com.tencent.polaris.api.plugin.configuration.ConfigFile configFile) {
        String oldContent = this.content;
        String newContent = Optional.ofNullable(configFile).map(com.tencent.polaris.api.plugin.configuration.ConfigFile::getContent).orElse(null);
        this.content = newContent;
        publishFileVersion(configFile);

        ChangeType changeType = ChangeType.MODIFIED;
        if (oldContent == null) {
            changeType = ChangeType.ADDED;
        }
        if (newContent == null) {
            changeType = ChangeType.DELETED;
        }
        if (Objects.equals(oldContent, newContent)) {
            changeType = ChangeType.NOT_CHANGED;
        }

        if (objectCache != null) {
            objectCache.invalidateAll();
            cacheVersion.incrementAndGet();
        }

        fireChangeEvent(new ConfigFileChangeEvent(configFileMetadata, oldContent, newContent, changeType));
    }

    /**
     * 发布新配置的版本名，须在该配置的内容对读取方可见之后调用，保证读到新版本名时内容也已是新的。
     *
     * @param configFile 新配置，删除时为 null
     */
    protected void publishFileVersion(com.tencent.polaris.api.plugin.configuration.ConfigFile configFile) {
        this.fileVersion = Optional.ofNullable(configFile)
                .map(com.tencent.polaris.api.plugin.configuration.ConfigFile::getName).orElse(null);
    }

    private void fireChangeEvent(ConfigFileChangeEvent event) {
        for (ConfigFileChangeListener listener : listeners) {
            notifyExecutorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();

                    listener.onChange(event);

                    LOGGER.info("[Config] invoke config file change listener success. listener = {}, duration = {} ms",
                            listener.getClass().getName(), System.currentTimeMillis() - startTime);
                } catch (Throwable t) {
                    // 只输出监听器与文件坐标、变更类型，不输出变更前后的配置正文
                    LOGGER.error("[Config] failed to invoke config file change listener. listener = {}, "
                                    + "file = {}/{}/{}, changeType = {}", listener.getClass().getName(),
                            getNamespace(), getFileGroup(), getFileName(), event.getChangeType(), t);
                }
            });
        }
    }
}
