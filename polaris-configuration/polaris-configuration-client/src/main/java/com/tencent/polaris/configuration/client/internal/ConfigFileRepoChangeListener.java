package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

/**
 * @author lepdou 2022-03-02
 */
public interface ConfigFileRepoChangeListener {

    void onChange(ConfigFileMetadata configFileMetadata, String newContent);

}
