package com.tencent.polaris.configuration.client.internal;

/**
 * @author lepdou 2022-03-02
 */
public interface ConfigFileRepo {

    String getContent();

    void addChangeListener(ConfigFileRepoChangeListener listener);

    void removeChangeListener(ConfigFileRepoChangeListener listener);

}
