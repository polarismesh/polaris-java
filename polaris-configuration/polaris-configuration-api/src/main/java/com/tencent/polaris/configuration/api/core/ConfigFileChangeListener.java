package com.tencent.polaris.configuration.api.core;

/**
 * The listener of config file change.
 *
 * @author lepdou 2022-03-01
 */
public interface ConfigFileChangeListener {

    /**
     * onChange method will be invoked, when config file published
     *
     * @param event publish event, contain change info
     */
    void onChange(ConfigFileChangeEvent event);
}
