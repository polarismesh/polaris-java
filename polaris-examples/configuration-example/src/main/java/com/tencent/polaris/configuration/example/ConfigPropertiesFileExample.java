package com.tencent.polaris.configuration.example;

import com.google.common.base.Charsets;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import com.tencent.polaris.logging.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.slf4j.Logger;

/**
 * properties, yaml 格式的配置文件，可以通过 ConfigKVFile 对象提供更高级能力的支持，提高业务开发使用效率。
 *
 * @author lepdou 2022-03-01
 */
public class ConfigPropertiesFileExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPropertiesFileExample.class);

    public static void main(String[] args) throws Exception {
        Utils.InitResult initResult = Utils.initConfiguration(args);
        String namespace = "dev";
        String fileGroup = "myGroup";
        String fileName = "application.properties";

        //创建配置中心服务类，一般情况下只需要单例对象
        ConfigFileService configFileService = Utils.createConfigFileService(initResult.getConfig());

        //获取 properties 格式配置文件对象
        ConfigKVFile configFile = configFileService.getConfigPropertiesFile(namespace, fileGroup, fileName);

        //获取配置文件完整内容
        Utils.print(configFile.getContent());

        //获取特定的 key 的值
        Utils.print(configFile.getProperty("key1", "some default value"));

        //更多基础类型方法
        // getIntProperty、getFloatProperty ...

        //更多高级数据结构方法
        //getEnumProperty、getArrayProperty、getJsonProperty

        //获取 Properties
        // asProperties

        //监听变更事件，kv类型的变更事件可以细化到 key 粒度的变更
        configFile.addChangeListener(new ConfigKVFileChangeListener() {
            @Override
            public void onChange(ConfigKVFileChangeEvent event) {
                for (String key : event.changedKeys()) {
                    ConfigPropertyChangeInfo changeInfo = event.getChangeInfo(key);
                    System.out.printf("\nChange info ：key = %s, old value = %s, new value = %s, change type = %s\n%n",
                            changeInfo.getPropertyName(), changeInfo.getOldValue(),
                            changeInfo.getNewValue(), changeInfo.getChangeType());
                }
            }
        });

        System.err.println("Please input key to get the value. Input quit to exit.");

        while (true) {
            System.out.print("Input key > ");
            String input = new BufferedReader(new InputStreamReader(System.in, Charsets.UTF_8)).readLine();
            if (input == null || input.length() == 0) {
                continue;
            }
            input = input.trim();
            if ("quit".equalsIgnoreCase(input)) {
                System.exit(0);
            }
            Utils.print(configFile.getProperty(input, null));
        }
    }
}
