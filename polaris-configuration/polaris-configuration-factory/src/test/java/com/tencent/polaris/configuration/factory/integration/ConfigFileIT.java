package com.tencent.polaris.configuration.factory.integration;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFilePublishService;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import com.tencent.polaris.configuration.factory.ConfigFileServicePublishFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author fabian 2023-06-02
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFileIT {

	@Test()
	public  void test() {

		String namespace = "default";
		String fileGroup = "test";
		String fileName = "conf/config.json";
		String content1 = "redis.cache.age=100";
		String content2 = "redis.cache.age=200";

		ConfigFileService configFileService = ConfigFileServiceFactory.createConfigFileService();
		ConfigFilePublishService configFilePublishService = ConfigFileServicePublishFactory.createConfigFilePublishService();

		// 创建配置文件元信息
		DefaultConfigFileMetadata fileMetadata = new DefaultConfigFileMetadata(namespace, fileGroup, fileName)

		configFilePublishService.createConfigFile(fileMetadata, content1);
		configFilePublishService.releaseConfigFile(fileMetadata);

		ConfigFile configFile = configFileService.getConfigFile(fileMetadata);
		Assert.assertEquals(configFile.getContent(), content1);

		configFilePublishService.updateConfigFile(fileMetadata, content2);
		configFilePublishService.releaseConfigFile(fileMetadata);
		Assert.assertEquals(configFile.getContent(), content2);

	}
}
