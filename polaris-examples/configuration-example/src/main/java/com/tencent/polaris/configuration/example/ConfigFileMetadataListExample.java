package com.tencent.polaris.configuration.example;

import com.tencent.polaris.configuration.api.core.*;

public class ConfigFileMetadataListExample {
    public static void main(String[] args) throws Exception {
        Utils.InitResult initResult = Utils.initConfiguration(args);
        String namespace = "default";
        String fileGroup = "test";

        ConfigFileService configFileService = Utils.createConfigFileService(initResult.getConfig());
        for (int i = 0; i < 10; i++) {
            ConfigFileGroup configFileGroup = configFileService.getConfigFileGroup(namespace, fileGroup);
            Utils.print(configFileGroup == null? "null": configFileGroup.toString());
        }

        ConfigFileGroup configFileGroup = configFileService.getConfigFileGroup(namespace, fileGroup);
        if (configFileGroup != null) {
            configFileGroup.addChangeListener(new ConfigFileGroupChangeListener() {
                @Override
                public void onChange(ConfigFileGroupChangedEvent event) {
                    Utils.print(event.toString());
                }
            });
        }
    }
}
