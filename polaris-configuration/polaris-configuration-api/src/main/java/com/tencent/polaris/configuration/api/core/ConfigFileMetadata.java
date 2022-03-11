package com.tencent.polaris.configuration.api.core;

/**
 * A unique config file located by namespace、file group、file name
 *
 * @author lepdou 2022-03-01
 */
public interface ConfigFileMetadata {

    String getNamespace();

    String getFileGroup();

    String getFileName();
}
