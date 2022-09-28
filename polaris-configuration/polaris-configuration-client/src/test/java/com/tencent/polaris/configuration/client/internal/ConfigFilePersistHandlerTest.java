package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * test for ConfigFilePersistHandler.
 * @author rod.xu
 * @date 2022/9/27 10:32 上午
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFilePersistHandlerTest {

	private ConfigFilePersistHandler configFilePersistHandler;

	@Before
	public void setUp() throws Exception {
		configFilePersistHandler = new ConfigFilePersistHandler("polaris/backup/config",
				3, 3, 2);
		configFilePersistHandler.init();
	}

	@Test
	public void testDeleteFileConfig() {
		configFilePersistHandler.deleteFileConfig(new ConfigFile("default",
				"rodGroup", "conf/test.json"));
	}

	@Test
	public void testSaveConfigFile() {
		ConfigFile configFile = new ConfigFile("default", "rodGroup",
				"conf/test.json");
		configFile.setContent("{}");
		configFile.setVersion(1L);
		configFile.setMd5("1234567");
		configFilePersistHandler.saveConfigFile(configFile);
	}

	@Test
	public void testLoadPersistedConfigFile() {
		ConfigFile configFile = new ConfigFile("default", "rodGroup",
				"conf/test.json");
		ConfigFile configFileResponse = configFilePersistHandler.loadPersistedConfigFile(configFile);
		System.out.println(configFileResponse);
	}

}
