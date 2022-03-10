package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.ConfigFileTestUtils;
import com.tencent.polaris.configuration.client.factory.ConfigFileFactory;
import com.tencent.polaris.configuration.client.factory.ConfigFileFactoryManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFileManagerTest {

    @Mock
    private ConfigFileFactory        configFileFactory;
    @Mock
    private ConfigFileFactoryManager configFileFactoryManager;
    @InjectMocks
    private DefaultConfigFileManager defaultConfigFileManager;

    @Before
    public void before() {
        defaultConfigFileManager.setConfigFileFactoryManager(configFileFactoryManager);
    }
    @Test
    public void testGetConfigFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile mockedConfigFile = mock(ConfigFile.class);

        when(configFileFactoryManager.getFactory(any())).thenReturn(configFileFactory);
        when(configFileFactory.createConfigFile(configFileMetadata)).thenReturn(mockedConfigFile);

        //第一次获取
        ConfigFile configFile = defaultConfigFileManager.getConfigFile(configFileMetadata);

        verify(configFileFactoryManager).getFactory(configFileMetadata);
        verify(configFileFactory).createConfigFile(configFileMetadata);
        Assert.assertEquals(mockedConfigFile, configFile);

        //第二次获取，经过缓存
        ConfigFile configFile2 = defaultConfigFileManager.getConfigFile(configFileMetadata);
        verify(configFileFactoryManager).getFactory(configFileMetadata);
        verify(configFileFactory).createConfigFile(configFileMetadata);
        Assert.assertEquals(mockedConfigFile, configFile2);

    }

    @Test
    public void testGetConfigPropertiesFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigKVFile mockedConfigFile = mock(ConfigKVFile.class);

        when(configFileFactoryManager.getFactory(any())).thenReturn(configFileFactory);
        when(configFileFactory.createConfigKVFile(configFileMetadata, ConfigFileFormat.Properties)).thenReturn(mockedConfigFile);

        //第一次获取
        ConfigKVFile configFile = defaultConfigFileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);

        verify(configFileFactoryManager).getFactory(configFileMetadata);
        verify(configFileFactory).createConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
        Assert.assertEquals(mockedConfigFile, configFile);

        //第二次获取，经过缓存
        ConfigKVFile configFile2 = defaultConfigFileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
        verify(configFileFactoryManager).getFactory(configFileMetadata);
        verify(configFileFactory).createConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
        Assert.assertEquals(mockedConfigFile, configFile2);

    }
}
