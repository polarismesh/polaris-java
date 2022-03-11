package com.tencent.polaris.configuration.example;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;

/**
 * 运行前请修改 polaris.yml 中的北极星服务地址
 *
 * @author lepdou 2022-03-04
 */
public class ConfigFileExample {


    public static void main(String[] args) throws Exception {
        String namespace = "dev";
        String fileGroup = "myGroup";
        String fileName = "application.properties";


        //创建配置中心服务类，一般情况下只需要单例对象
        ConfigFileService configFileService = ConfigFileServiceFactory.createConfigFileService();

        //获取配置文件
        ConfigFile configFile = configFileService.getConfigFile(namespace, fileGroup, fileName);

        //打印配置文件内容
        Utils.print(configFile.getContent());

        //添加变更监听器
        configFile.addChangeListener(new ConfigFileChangeListener() {
            @Override
            public void onChange(ConfigFileChangeEvent event) {
                System.out
                    .printf("Received config file change event. old value = %s, new value = %s, change type = %s%n",
                            event.getOldValue(), event.getNewValue(), event.getChangeType());

                //获取配置文件最新内容
                Utils.print(configFile.getContent());
            }
        });

        //更多 API 用法
        //User user = configFile.asJson(User.class, null);  自动反序列化配置文件成 JSON 对象
        //List<User> users = configFile.asJson(new TypeToken<List<User>>() {}.getType(), null)

        System.in.read();
    }

}
