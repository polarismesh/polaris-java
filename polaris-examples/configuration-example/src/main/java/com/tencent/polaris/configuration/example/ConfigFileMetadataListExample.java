package com.tencent.polaris.configuration.example;

import com.tencent.polaris.configuration.api.core.*;

import java.util.List;

public class ConfigFileMetadataListExample {
    public static void main(String[] args) throws Exception {
        Utils.InitResult initResult = Utils.initConfiguration(args);
        String namespace = "default";
        String fileGroup = "test";

        //创建配置中心服务类，一般情况下只需要单例对象
        ConfigFileService configFileService = Utils.createConfigFileService(initResult.getConfig());

        ConfigFileGroup configFileGroup = configFileService.getConfigFileGroup(namespace, fileGroup);

        Utils.print(configFileGroup.toString());
        configFileGroup.addChangeListener(new ConfigFileGroupChangeListener() {
            @Override
            public void onChange(ConfigFileGroupChangedEvent event) {
                Utils.print(event.toString());
            }
        });
    }
}
