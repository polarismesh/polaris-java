package com.tencent.polaris.configuration.factory.integration;

import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.configuration.api.core.ChangeType;
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
	public void testNormalFlow() {
		String namespace = "test";
		String fileGroup = "test";
		String fileName = "conf/1.json";
		String content1 = "redis.cache.age=100";
		String content2 = "redis.cache.age=200";

		ConfigFileService configFileService = ConfigFileServiceFactory.createConfigFileService();
		ConfigFilePublishService configFilePublishService = ConfigFileServicePublishFactory.createConfigFilePublishService();

		// 创建配置文件元信息
		DefaultConfigFileMetadata fileMetadata = new DefaultConfigFileMetadata(namespace, fileGroup, fileName);

		configFilePublishService.createConfigFile(fileMetadata, content1);
		configFilePublishService.releaseConfigFile(fileMetadata);
		ConfigFile configFile = configFileService.getConfigFile(fileMetadata);
		Assert.assertEquals(content1, configFile.getContent());

		//添加变更监听器
		configFile.addChangeListener(event -> {
			Assert.assertEquals(content1, event.getOldValue());
			Assert.assertEquals(content2, event.getNewValue());
			Assert.assertEquals(ChangeType.MODIFIED, event.getChangeType());
		});

		configFilePublishService.updateConfigFile(fileMetadata, content2);
		configFilePublishService.releaseConfigFile(fileMetadata);
	}

	@Test()
	public void testUpdateWithoutCreate() {
		String namespace = "test";
		String fileGroup = "test";
		String fileName = "conf/2.json";
		String content = "redis.cache.age=100";

		ConfigFilePublishService configFilePublishService = ConfigFileServicePublishFactory.createConfigFilePublishService();

		// 创建配置文件元信息
		DefaultConfigFileMetadata fileMetadata = new DefaultConfigFileMetadata(namespace, fileGroup, fileName);

		ConfigFileResponse configFileResponse = configFilePublishService.updateConfigFile(fileMetadata, content);
		Assert.assertEquals(ServerCodes.NOT_FOUND_RESOURCE, configFileResponse.getCode());
	}

	@Test
	public void testReleaseWithoutCreate() {
		String namespace = "test";
		String fileGroup = "test";
		String fileName = "conf/3.json";

		ConfigFilePublishService configFilePublishService = ConfigFileServicePublishFactory.createConfigFilePublishService();

		// 创建配置文件元信息
		DefaultConfigFileMetadata fileMetadata = new DefaultConfigFileMetadata(namespace, fileGroup, fileName);

		ConfigFileResponse configFileResponse = configFilePublishService.releaseConfigFile(fileMetadata);
		Assert.assertEquals(ServerCodes.NOT_FOUND_RESOURCE, configFileResponse.getCode());
	}

	@Test()
	public void testCreateWithoutRelease() {
		String namespace = "test";
		String fileGroup = "test";
		String fileName = "conf/4.json";
		String content = "redis.cache.age=100";

		ConfigFileService configFileService = ConfigFileServiceFactory.createConfigFileService();
		ConfigFilePublishService configFilePublishService = ConfigFileServicePublishFactory.createConfigFilePublishService();

		// 创建配置文件元信息
		DefaultConfigFileMetadata fileMetadata = new DefaultConfigFileMetadata(namespace, fileGroup, fileName);

		configFilePublishService.createConfigFile(fileMetadata, content);

		ConfigFile configFile = configFileService.getConfigFile(fileMetadata);
		Assert.assertNull(configFile.getContent());
	}
}
