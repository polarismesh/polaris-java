/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.configuration.example;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigFileService;

/**
 * 运行前请修改 polaris.yml 中的北极星服务地址
 *
 * @author lepdou 2022-03-04
 */
public class ConfigFileExample {


	public static void main(String[] args) throws Exception {
		Utils.InitResult initResult = Utils.initConfiguration(args);
		String namespace = "default";
		String fileGroup = "test";
		String fileName = "conf/a.txt";

		//创建配置中心服务类，一般情况下只需要单例对象
		ConfigFileService configFileService = Utils.createConfigFileService(initResult.getConfig());

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
