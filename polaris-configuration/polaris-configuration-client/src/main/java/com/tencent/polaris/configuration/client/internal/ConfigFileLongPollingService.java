package com.tencent.polaris.configuration.client.internal;


/**
 * @author lepdou 2022-03-02
 */
public interface ConfigFileLongPollingService {

    void addConfigFile(RemoteConfigFileRepo configFileRepo);
}
