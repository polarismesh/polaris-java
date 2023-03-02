package com.tencent.polaris.configuration.example;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileService;

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
        String fileName = "aa/plugin.json";

        //创建配置中心服务类，一般情况下只需要单例对象
        ConfigFileService configFileService = Utils.createConfigFileService(initResult.getConfig());

        //获取配置文件
        ConfigFile configFile = configFileService.getConfigFile(namespace, fileGroup, fileName);

        //打印配置文件内容
        Utils.print(configFile.getContent());

        // 创建新配置
//        configFileService.createConfigFileAndRelease(namespace, fileGroup, fileName, "redis.cache.age=100");

        // 更新配置
//        configFileService.updateConfigFileAndRelease(namespace, fileGroup, fileName, "redis.cache.age=10000");
    }
}
