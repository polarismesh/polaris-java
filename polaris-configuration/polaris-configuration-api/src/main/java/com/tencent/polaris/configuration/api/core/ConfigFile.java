package com.tencent.polaris.configuration.api.core;

import java.lang.reflect.Type;

/**
 * @author lepdou 2022-03-01
 */
public interface ConfigFile extends ConfigFileMetadata {

    /**
     * Get the content of the configuration file. If it has not been published, null will be returned
     *
     * @return the content of the configuration file
     */
    String getContent();

    /**
     * Deserialize to json object with given class type by gson. Default value will be returned when content is blank or
     * some error occurred.
     *
     * @param objectType   the type of class
     * @param defaultValue the default value
     * @param <T>
     * @return Deserialize result of json object.
     */
    <T> T asJson(Class<T> objectType, T defaultValue);

    /**
     * Deserialize to json object with given class type by gson. Default value will be returned when content is blank or
     * some error occurred.
     *
     * @param typeOfT      the type of class
     * @param defaultValue the default value
     * @param <T>
     * @return Deserialize result of json object.
     */
    <T> T asJson(Type typeOfT, T defaultValue);


    /**
     * Whether the configuration file contains content. If it has not been published or content is blank string, false
     * will be returned
     *
     * @return Whether the configuration file contains content
     */
    boolean hasContent();

    /**
     * Adding a config file change listener, will trigger a callback when the config file is published
     *
     * @param listener the listener will be added
     */
    void addChangeListener(ConfigFileChangeListener listener);

    /**
     * remove a config file change listener
     *
     * @param listener the listener will be removed
     */
    void removeChangeListener(ConfigFileChangeListener listener);

}
