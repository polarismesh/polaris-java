package com.tencent.polaris.configuration.client.internal;

import com.google.common.collect.Lists;

import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author lepdou 2022-03-01
 */
public abstract class AbstractConfigFileRepo implements ConfigFileRepo {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigFileRepo.class);

    protected     ConfigFileMetadata                 configFileMetadata;
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

    protected void fireChangeEvent(String newContent) {
        for (ConfigFileRepoChangeListener listener : listeners) {
            try {
                listener.onChange(configFileMetadata, newContent);
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
