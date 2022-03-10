package com.tencent.polaris.configuration.api.core;

/**
 * @author lepdou 2022-03-01
 */
public interface ConfigKVFileChangeListener {

    /**
     * onChange method will be invoked, when config file published
     *
     * @param event publish event, contain change info
     */
    void onChange(ConfigKVFileChangeEvent event);
}
