package com.tencent.polaris.configuration.api.core;

/**
 * @author fabian4 2023-03-08
 */
public interface ConfigFilePublishService {

    /**
     * Create the configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     */
    void createConfigFile(String namespace, String fileGroup, String fileName, String content);

    /**
     * Create the configuration file
     *
     * @param configFileMetadata config file metadata
     */
    void createConfigFile(ConfigFileMetadata configFileMetadata, String content);

    /**
     * Update the configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     */
    void updateConfigFile(String namespace, String fileGroup, String fileName, String content);

    /**
     * Update the configuration file
     *
     * @param configFileMetadata config file metadata
     */
    void updateConfigFile(ConfigFileMetadata configFileMetadata, String content);

    /**
     * Release the configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     */
    void releaseConfigFile(String namespace, String fileGroup, String fileName);

    /**
     * Release the configuration file
     *
     * @param configFileMetadata config file metadata
     */
    void releaseConfigFile(ConfigFileMetadata configFileMetadata);
}
