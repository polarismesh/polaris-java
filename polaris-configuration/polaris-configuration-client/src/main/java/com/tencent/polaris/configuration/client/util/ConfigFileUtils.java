package com.tencent.polaris.configuration.client.util;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

/**
 * @author lepdou 2022-03-04
 */
public class ConfigFileUtils {

    public static void checkConfigFileMetadata(ConfigFileMetadata configFileMetadata) {
        if (StringUtils.isBlank(configFileMetadata.getNamespace())) {
            throw new IllegalArgumentException("namespace cannot be empty.");
        }
        if (StringUtils.isBlank(configFileMetadata.getFileGroup())) {
            throw new IllegalArgumentException("file group cannot be empty.");
        }
        if (StringUtils.isBlank(configFileMetadata.getFileName())) {
            throw new IllegalArgumentException("file name cannot be empty.");
        }
    }
}
