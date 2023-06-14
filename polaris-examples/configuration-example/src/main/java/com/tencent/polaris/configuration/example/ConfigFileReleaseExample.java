package com.tencent.polaris.configuration.example;

import com.tencent.polaris.configuration.api.core.ConfigFilePublishService;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;

/**
 * 运行前请修改 polaris.yml 中的北极星服务地址
 * http 端口为 8090
 *
 * @author fabian 2023-03-02
 */
public class ConfigFileReleaseExample {

    public static void main(String[] args) throws Exception {
        Utils.InitResult initResult = Utils.initConfiguration(args);
        String namespace = "default";
        String fileGroup = "test";
        String fileName = "conf/config.text";

        String content = "redis.cache.age=100";

        // 创建配置文件元信息
        DefaultConfigFileMetadata fileMetadata = new DefaultConfigFileMetadata(namespace, fileGroup, fileName);

        // 创建配置中心服务发布类，一般情况下只需要单例对象
        ConfigFilePublishService configFilePublishService = Utils.createConfigFilePublishService(initResult.getConfig());

        // 创建配置
        configFilePublishService.createConfigFile(fileMetadata, content);
//        configFileService.createConfigFile(namespace, fileGroup, fileName, content);

        // 更新配置
//        configFilePublishService.updateConfigFile(fileMetadata, content);
//        configFileService.updateConfigFile(namespace, fileGroup, fileName, content);

        // 发布配置
        configFilePublishService.releaseConfigFile(fileMetadata);
//        configFilePublishService.releaseConfigFile(namespace, fileGroup, fileName);

    }
}
