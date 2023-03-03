package com.tencent.polaris.configuration.example;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;

/**
 * 运行前请修改 polaris.yml 中的北极星服务地址
 * http 端口为 8090
 *
 * @author fabian 2023-03-02
 */
public class ConfigFileOpenapiExample {

    public static void main(String[] args) throws Exception {
        Utils.InitResult initResult = Utils.initConfiguration(args);
        String namespace = "default";
        String fileGroup = "test";
        String fileName = "test/openapi.json";

        String content = "redis.cache.age=1000";

        // 创建配置中心服务类，一般情况下只需要单例对象
        ConfigFileService configFileService = Utils.createConfigFileService(initResult.getConfig());

        // 创建配置文件元信息
        DefaultConfigFileMetadata fileMetadata = new DefaultConfigFileMetadata(namespace, fileGroup, fileName);

        // 获取配置文件
//        ConfigFile configFile = configFileService.getConfigFile(fileMetadata);
//        ConfigFile configFile = configFileService.getConfigFile(namespace, fileGroup, fileName);

        // 打印配置文件内容
//        Utils.print(configFile.getContent());

        // 创建配置
//        configFileService.createConfigFile(fileMetadata, content);
//        configFileService.createConfigFile(namespace, fileGroup, fileName, content);

        // 更新配置
        configFileService.updateConfigFile(fileMetadata, content);
//        configFileService.updateConfigFile(namespace, fileGroup, fileName, content);

        // 发布配置
        configFileService.releaseConfigFile(fileMetadata);
//        configFileService.releaseConfigFile(namespace, fileGroup, fileName);

    }
}
