package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.configuration.client.util.YamlParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * The yaml/yml file.
 *
 * @author lepdou 2022-03-08
 */
public class ConfigYamlFile extends ConfigPropertiesFile {

    private static final YamlParser YAML_PARSER = new YamlParser();

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigYamlFile.class);


    public ConfigYamlFile(String namespace, String fileGroup, String fileName,
                          ConfigFileRepo configFileRepo,
                          ConfigFileConfig configFileConfig) {
        super(namespace, fileGroup, fileName, configFileRepo, configFileConfig);
    }

    @Override
    protected Properties convertToProperties(String content) {
        Properties properties = new Properties();
        if (content == null) {
            return properties;
        }

        try {
            properties = YAML_PARSER.yamlToProperties(content);
        } catch (Throwable t) {
            String msg = String.format("[Config] failed to convert content to properties. namespace = %s, "
                                       + "file group = %s, file name = %s",
                                       getNamespace(), getFileGroup(), getFileName());
            LOGGER.error(msg, t);
            throw new IllegalStateException(msg);
        }

        return properties;
    }
}
