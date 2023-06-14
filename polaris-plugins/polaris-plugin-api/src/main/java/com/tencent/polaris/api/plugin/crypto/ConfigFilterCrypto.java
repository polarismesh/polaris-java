package com.tencent.polaris.api.plugin.crypto;

import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;

/**
 * ConfigFilterCrypto 插件接口
 *
 * @author fabian4
 * @date 2023/6/13
 */
public interface ConfigFilterCrypto extends Plugin {

    ConfigFile doBefore(ConfigFile configFile);

    ConfigFileResponse doAfter(ConfigFileResponse configFileResponse);
}
