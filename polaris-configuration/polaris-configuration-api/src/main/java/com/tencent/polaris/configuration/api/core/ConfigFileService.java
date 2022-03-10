package com.tencent.polaris.configuration.api.core;

/**
 * The service of config file.
 * <p>
 * Property and yaml file can be convert to ConfigKVFile which provide a series of common tools and methods.
 *
 * @author lepdou 2022-03-01
 */
public interface ConfigFileService {

    /**
     * Automatically parse the config file into properties format, and provide a series of common tools and methods
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName  file name
     * @return config properties file
     */
    ConfigKVFile getConfigPropertiesFile(String namespace, String fileGroup, String fileName);

    /**
     * Automatically parse the config file into properties format, and provide a series of common tools and methods
     *
     * @param configFileMetadata config file metadata
     * @return properties file
     */
    ConfigKVFile getConfigPropertiesFile(ConfigFileMetadata configFileMetadata);

    /**
     * Automatically parse the configuration file into yaml format, and provide a series of common tools and methods
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName  file name
     * @return config yaml file
     */
    ConfigKVFile getConfigYamlFile(String namespace, String fileGroup, String fileName);

    /**
     * Automatically parse the configuration file into yaml format, and provide a series of common tools and methods
     *
     * @param configFileMetadata config file metadata
     * @return yaml file
     */
    ConfigKVFile getConfigYamlFile(ConfigFileMetadata configFileMetadata);

    /**
     * Get the original configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName  file name
     * @return config file
     */
    ConfigFile getConfigFile(String namespace, String fileGroup, String fileName);

    /**
     * Get the original configuration file
     *
     * @param configFileMetadata config file metadata
     * @return config file
     */
    ConfigFile getConfigFile(ConfigFileMetadata configFileMetadata);
}
