package com.tencent.polaris.configuration.client.internal;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import com.tencent.polaris.configuration.client.util.ConvertFunctions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The properties file.
 *
 * @author lepdou 2022-03-04
 */
public class ConfigPropertiesFile extends DefaultConfigFile implements ConfigKVFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPropertiesFile.class);

    private final List<ConfigKVFileChangeListener> listeners = Lists.newCopyOnWriteArrayList();
    private       AtomicReference<Properties>      properties;

    private volatile Cache<String, Integer>               integerCache;
    private volatile Cache<String, Long>                  longCache;
    private volatile Cache<String, Short>                 shortCache;
    private volatile Cache<String, Float>                 floatCache;
    private volatile Cache<String, Double>                doubleCache;
    private volatile Cache<String, Byte>                  byteCache;
    private volatile Cache<String, Boolean>               booleanCache;
    private final    Map<String, Cache<String, String[]>> arrayCache;
    private final    List<Cache>                          allCaches;
    private final    AtomicLong                           cacheVersion;

    public ConfigPropertiesFile(String namespace, String fileGroup, String fileName,
                                ConfigFileRepo configFileRepo, ConfigFileConfig configFileConfig) {
        super(namespace, fileGroup, fileName, configFileRepo, configFileConfig);

        arrayCache = Maps.newConcurrentMap();
        allCaches = Lists.newArrayList();
        cacheVersion = new AtomicLong();
    }

    @Override
    protected void initialize() {
        properties = new AtomicReference<>();

        super.initialize();

        Properties properties = convertToProperties(getContent());

        this.properties.set(properties);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        //-D 启动参数优先级高于配置中心
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }

        value = properties.get().getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    @Override
    public Integer getIntProperty(String key, Integer defaultValue) {
        try {
            if (integerCache == null) {
                synchronized (this) {
                    if (integerCache == null) {
                        integerCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, ConvertFunctions.TO_INT_FUNCTION, integerCache, defaultValue);
        } catch (Throwable ex) {
            LOGGER.error("[Config] convert to int error. return default value.", ex);
        }
        return defaultValue;
    }

    @Override
    public Long getLongProperty(String key, Long defaultValue) {
        try {
            if (longCache == null) {
                synchronized (this) {
                    if (longCache == null) {
                        longCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, ConvertFunctions.TO_LONG_FUNCTION, longCache, defaultValue);
        } catch (Throwable ex) {
            LOGGER.error("[Config] convert to long error. return default value.", ex);
        }
        return defaultValue;
    }

    @Override
    public Short getShortProperty(String key, Short defaultValue) {
        try {
            if (shortCache == null) {
                synchronized (this) {
                    if (shortCache == null) {
                        shortCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, ConvertFunctions.TO_SHORT_FUNCTION, shortCache, defaultValue);
        } catch (Throwable ex) {
            LOGGER.error("[Config] convert to short error. return default value.", ex);
        }
        return defaultValue;
    }

    @Override
    public Float getFloatProperty(String key, Float defaultValue) {
        try {
            if (floatCache == null) {
                synchronized (this) {
                    if (floatCache == null) {
                        floatCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, ConvertFunctions.TO_FLOAT_FUNCTION, floatCache, defaultValue);
        } catch (Throwable ex) {
            LOGGER.error("[Config] convert to float error. return default value.", ex);
        }
        return defaultValue;
    }

    @Override
    public Double getDoubleProperty(String key, Double defaultValue) {
        try {
            if (doubleCache == null) {
                synchronized (this) {
                    if (doubleCache == null) {
                        doubleCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, ConvertFunctions.TO_DOUBLE_FUNCTION, doubleCache, defaultValue);
        } catch (Throwable ex) {
            LOGGER.error("[Config] convert to double error. return default value.", ex);
        }
        return defaultValue;
    }

    @Override
    public Byte getByteProperty(String key, Byte defaultValue) {
        try {
            if (byteCache == null) {
                synchronized (this) {
                    if (byteCache == null) {
                        byteCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, ConvertFunctions.TO_BYTE_FUNCTION, byteCache, defaultValue);
        } catch (Throwable ex) {
            LOGGER.error("[Config] convert to byte error. return default value.", ex);
        }
        return defaultValue;
    }

    @Override
    public Boolean getBooleanProperty(String key, Boolean defaultValue) {
        try {
            if (booleanCache == null) {
                synchronized (this) {
                    if (booleanCache == null) {
                        booleanCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, ConvertFunctions.TO_BOOLEAN_FUNCTION, booleanCache, defaultValue);
        } catch (Throwable ex) {
            LOGGER.error("[Config] convert to boolean error. return default value.", ex);
        }
        return defaultValue;
    }

    @Override
    public String[] getArrayProperty(String key, String delimiter, String[] defaultValue) {
        try {
            if (!arrayCache.containsKey(delimiter)) {
                synchronized (this) {
                    if (!arrayCache.containsKey(delimiter)) {
                        arrayCache.put(delimiter, this.<String[]>newCache());
                    }
                }
            }

            Cache<String, String[]> cache = arrayCache.get(delimiter);
            String[] result = cache.getIfPresent(key);

            if (result != null) {
                return result;
            }

            return getValueAndStoreToCache(key, new Function<String, String[]>() {
                @Override
                public String[] apply(String input) {
                    return input.split(delimiter);
                }
            }, cache, defaultValue);
        } catch (Throwable ex) {
            LOGGER.error("[Config] convert to array error. return default value.", ex);
        }
        return defaultValue;
    }

    @Override
    public <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue) {
        try {
            String value = getProperty(key, null);

            if (value != null) {
                return Enum.valueOf(enumType, value);
            }
        } catch (Throwable ex) {
            LOGGER.error("[Config] convert to enum error. return default value.", ex);
        }

        return defaultValue;
    }

    @Override
    public <T> T getJsonProperty(String key, Class<T> clazz, T defaultValue) {
        try {
            String json = getProperty(key, null);
            if (json != null) {
                return convertToJson(key, json, null, clazz, defaultValue);
            }
        } catch (Throwable t) {
            LOGGER.error("[Config] convert to json object error. return default value. clazz = {}", clazz.getTypeName(),
                         t);
        }

        return defaultValue;
    }

    @Override
    public <T> T getJsonProperty(String key, Type typeOfT, T defaultValue) {
        try {
            String json = getProperty(key, null);
            if (json != null) {
                return convertToJson(key, json, typeOfT, null, defaultValue);
            }
        } catch (Throwable t) {
            LOGGER.error("[Config] convert to json object error. return default value. clazz = {}", typeOfT,
                         t);
        }

        return defaultValue;
    }

    @Override
    public <T> T asJson(Type typeOfT, T defaultValue) {
        throw new IllegalStateException("Properties or yaml file can not convert to json");
    }

    @Override
    public <T> T asJson(Class<T> objectType, T defaultValue) {
        throw new IllegalStateException("Properties or yaml file can not convert to json");
    }

    @Override
    public Set<String> getPropertyNames() {
        return stringPropertyNames(properties.get());
    }

    @Override
    public void addChangeListener(ConfigKVFileChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeChangeListener(ConfigKVFileChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onChange(ConfigFileMetadata configFileMetadata, String newContent) {
        super.onChange(configFileMetadata, newContent);

        Properties oldProperties = this.properties.get();
        if (oldProperties == null) {
            oldProperties = new Properties();
        }
        Properties newProperties = convertToProperties(newContent);

        //更新缓存的 properties 值
        this.properties.set(newProperties);

        Map<String, ConfigPropertyChangeInfo> changeInfos = Maps.newHashMap();

        // 计算变更
        for (Map.Entry<Object, Object> entry : newProperties.entrySet()) {
            String key = entry.getKey().toString();
            String newValue = entry.getValue().toString();
            String oldValue = oldProperties.getProperty(key);

            if (oldValue == null) {
                changeInfos.put(key, new ConfigPropertyChangeInfo(key, null, newValue, ChangeType.ADDED));
            } else if (!StringUtils.equals(oldValue, newValue)) {
                changeInfos.put(key, new ConfigPropertyChangeInfo(key, oldValue, newValue, ChangeType.MODIFIED));
            }
        }

        for (Map.Entry<Object, Object> entry : oldProperties.entrySet()) {
            String key = entry.getKey().toString();
            String oldValue = entry.getValue().toString();
            if (!newProperties.containsKey(key)) {
                changeInfos.put(key, new ConfigPropertyChangeInfo(key, oldValue, null, ChangeType.DELETED));
            }
        }

        clearConfigCache();

        ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos);

        fireChangeEvent(event);
    }

    protected Properties convertToProperties(String content) {
        Properties properties = new Properties();
        if (content == null) {
            return properties;
        }

        //默认用 properties 格式解析
        convertToProperties(properties, content);

        return properties;
    }

    protected void convertToProperties(Properties properties, String content) {
        try {
            properties.load(new ByteArrayInputStream(content.getBytes()));
        } catch (IOException e) {
            String msg = String.format("[Config] failed to convert content to properties. namespace = %s, "
                                       + "file group = %s, file name = %s",
                                       getNamespace(), getFileGroup(), getFileName());
            LOGGER.error(msg, e);
            throw new IllegalStateException(msg);
        }
    }

    private void fireChangeEvent(ConfigKVFileChangeEvent event) {
        for (ConfigKVFileChangeListener listener : listeners) {
            notifyExecutorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();

                    listener.onChange(event);

                    LOGGER.info("[Config] invoke config file change listener success. listener = {}, duration = {} ms",
                                listener.getClass().getName(), System.currentTimeMillis() - startTime);
                } catch (Throwable t) {
                    LOGGER.error("[Config] ailed to invoke config file change listener. listener = {}, event = {}",
                                 listener.getClass().getName(), event, t);
                }
            });
        }
    }

    private <T> T getValueFromCache(String key, Function<String, T> parser, Cache<String, T> cache, T defaultValue) {
        T result = cache.getIfPresent(key);

        if (result != null) {
            return result;
        }

        return getValueAndStoreToCache(key, parser, cache, defaultValue);
    }

    private <T> T getValueAndStoreToCache(String key, Function<String, T> parser, Cache<String, T> cache,
                                          T defaultValue) {
        long currentCacheVersion = cacheVersion.get();
        String value = getProperty(key, null);

        if (value != null) {
            T result = parser.apply(value);

            if (result != null) {
                synchronized (this) {
                    if (cacheVersion.get() == currentCacheVersion) {
                        cache.put(key, result);
                    }
                }
                return result;
            }
        }

        return defaultValue;
    }

    private <T> Cache<String, T> newCache() {
        Cache<String, T> cache = CacheBuilder.newBuilder()
            .maximumSize(configFileConfig.getPropertiesValueCacheSize())
            .expireAfterAccess(configFileConfig.getPropertiesValueExpireTime(), TimeUnit.MINUTES)
            .build();
        allCaches.add(cache);
        return cache;
    }

    protected void clearConfigCache() {
        synchronized (this) {
            for (Cache c : allCaches) {
                if (c != null) {
                    c.invalidateAll();
                }
            }
            cacheVersion.incrementAndGet();
        }
    }

    private Set<String> stringPropertyNames(Properties properties) {
        if (properties == null) {
            return Collections.emptySet();
        }
        Map<String, Object> map = Maps.newLinkedHashMapWithExpectedSize(properties.size());
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            if (k instanceof String) {
                map.put((String) k, v);
            }
        }
        return map.keySet();
    }
}
