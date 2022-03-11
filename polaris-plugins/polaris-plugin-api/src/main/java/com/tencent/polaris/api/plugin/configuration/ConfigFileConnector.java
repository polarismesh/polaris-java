package com.tencent.polaris.api.plugin.configuration;

import com.tencent.polaris.api.plugin.Plugin;

import java.util.List;

public interface ConfigFileConnector extends Plugin {

    /**
     * 获取配置文件
     *
     * @param configFile 配置文件元信息
     * @return 配置文件信息
     */
    ConfigFileResponse getConfigFile(ConfigFile configFile);

    /**
     * 监听配置文件变更
     *
     * @param configFiles 监听的配置文件列表
     * @return 变更的配置文件
     */
    ConfigFileResponse watchConfigFiles(List<ConfigFile> configFiles);
}
