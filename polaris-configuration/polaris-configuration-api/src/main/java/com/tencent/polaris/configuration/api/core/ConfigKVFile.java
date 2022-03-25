package com.tencent.polaris.configuration.api.core;

import java.lang.reflect.Type;
import java.util.Properties;
import java.util.Set;

/**
 * The config file can be converted as key value set. Such as properties、yaml files.
 *
 * @author lepdou 2022-03-01
 */
public interface ConfigKVFile extends ConfigFile {

    /**
     * Get the value of given key in the configuration file, or {@code defaultValue} if the key doesn't exist
     *
     * @param key          the properties key
     * @param defaultValue the default value when key is not found or any error occurred
     * @return the properties value
     */
    String getProperty(String key, String defaultValue);

    /**
     * Get the value of given key as integer type in the configuration file, or {@code defaultValue} if the key doesn't
     * exist
     *
     * @param key          the properties key
     * @param defaultValue the default value when key is not found or any error occurred
     * @return the properties value
     */
    Integer getIntProperty(String key, Integer defaultValue);

    /**
     * Get the value of given key as long type in the configuration file, or {@code defaultValue} if the key doesn't
     * exist
     *
     * @param key          the properties key
     * @param defaultValue the default value when key is not found or any error occurred
     * @return the properties value
     */
    Long getLongProperty(String key, Long defaultValue);

    /**
     * Get the value of given key as short type in the configuration file, or {@code defaultValue} if the key doesn't
     * exist
     *
     * @param key          the properties key
     * @param defaultValue the default value when key is not found or any error occurred
     * @return the properties value
     */
    Short getShortProperty(String key, Short defaultValue);

    /**
     * Get the value of given key as float type in the configuration file, or {@code defaultValue} if the key doesn't
     * exist
     *
     * @param key          the properties key
     * @param defaultValue the default value when key is not found or any error occurred
     * @return the properties value
     */
    Float getFloatProperty(String key, Float defaultValue);

    /**
     * Get the value of given key as double type in the configuration file, or {@code defaultValue} if the key doesn't
     * exist
     *
     * @param key          the properties key
     * @param defaultValue the default value when key is not found or any error occurred
     * @return the properties value
     */
    Double getDoubleProperty(String key, Double defaultValue);

    /**
     * Get the value of given key as byte type in the configuration file, or {@code defaultValue} if the key doesn't
     * exist
     *
     * @param key          the properties key
     * @param defaultValue the default value when key is not found or any error occurred
     * @return the properties value
     */
    Byte getByteProperty(String key, Byte defaultValue);

    /**
     * Get the value of given key as boolean type in the configuration file, or {@code defaultValue} if the key doesn't
     * exist
     *
     * @param key          the properties key
     * @param defaultValue the default value when key is not found or any error occurred
     * @return the properties value
     */
    Boolean getBooleanProperty(String key, Boolean defaultValue);

    /**
     * Auto parse string to array by given delimiter, or {@code defaultValue} if the key doesn't exist \n For example,
     * the source value is v1,v2,v3. An array ["v1","v2","v3"] will be returned when called with "," delimiter.
     *
     * @param key          the properties key
     * @param defaultValue the default value when key is not found or any error occurred
     * @return parsed array
     */
    String[] getArrayProperty(String key, String delimiter, String[] defaultValue);

    /**
     * Auto parse string to enum. default value will be returned when parsed failed.
     *
     * @param key          the properties key
     * @param enumType     the type of enum
     * @param defaultValue the default value when key is not found or any error occurred
     * @param <T>
     * @return parsed enum
     */
    <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue);

    /**
     * Auto parse string to json object. default value will be returned when parsed failed.
     *
     * @param key          the properties key
     * @param clazz        the type of object
     * @param defaultValue the default value when key is not found or any error occurred
     * @param <T>
     * @return parsed object
     */
    <T> T getJsonProperty(String key, Class<T> clazz, T defaultValue);

    /**
     * Auto parse string to json object. default value will be returned when parsed failed.
     *
     * @param key          the properties key
     * @param typeOfT      the type of object
     * @param defaultValue the default value when key is not found or any error occurred
     * @param <T>
     * @return parsed object
     */
    <T> T getJsonProperty(String key, Type typeOfT, T defaultValue);

    /**
     * Get properties for all keys
     *
     * @return the keys in properties
     */
    Set<String> getPropertyNames();

    /**
     * Adding a config file property change listener, will trigger a callback when the config file is published
     *
     * @param listener the listener will be added
     */
    void addChangeListener(ConfigKVFileChangeListener listener);

    /**
     * Remove a config file property change listener
     *
     * @param listener the listener will be removed
     */
    void removeChangeListener(ConfigKVFileChangeListener listener);
}
