package com.tencent.polaris.configuration.client.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFile extends DefaultConfigFileMetadata implements ConfigFile, ConfigFileRepoChangeListener {

    private static final Logger LOGGER           = LoggerFactory.getLogger(DefaultConfigFile.class);
    private static final Gson   gson             = new Gson();
    private static final String OBJECT_CACHE_KEY = "object";

    protected static ExecutorService notifyExecutorService;

    private final   List<ConfigFileChangeListener> listeners = Lists.newCopyOnWriteArrayList();
    private final   ConfigFileRepo                 configFileRepo;
    protected final ConfigFileConfig               configFileConfig;
    private         String                         content;

    private volatile Cache<String, Object> objectCache;
    private final    AtomicLong            cacheVersion;

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
            LOGGER.error("[Config] convert json file content to given class error. class type = {}", type, t);
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
    public void onChange(ConfigFileMetadata configFileMetadata, String newContent) {
        String oldContent = this.content;
        this.content = newContent;

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

    private void fireChangeEvent(ConfigFileChangeEvent event) {
        for (ConfigFileChangeListener listener : listeners) {
            notifyExecutorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();

                    listener.onChange(event);

                    LOGGER.info("[Config] invoke config file change listener success. listener = {}, duration = {} ms",
                                listener.getClass().getName(), System.currentTimeMillis() - startTime);
                } catch (Throwable t) {
                    LOGGER.error("[Config] failed to invoke config file change listener. listener = {}, event = {}",
                                 listener.getClass().getName(), event, t);
                }
            });
        }
    }
}
