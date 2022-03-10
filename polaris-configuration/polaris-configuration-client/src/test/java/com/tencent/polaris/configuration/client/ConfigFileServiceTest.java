package com.tencent.polaris.configuration.client;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.client.internal.ConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFileServiceTest {

    @Mock
    private ConfigFileManager configFileManager;
    @Mock
    private ConfigFile        configFile;
    @InjectMocks
    private DefaultConfigFileService defaultConfigFileService;

    @Test(expected = IllegalArgumentException.class)
    public void testNamespaceBlank() {
        defaultConfigFileService.getConfigFile("", "somegroup", "application.yaml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGroupBlank() {
        defaultConfigFileService.getConfigFile("somenamespace", "", "application.yaml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileNameBlank() {
        defaultConfigFileService.getConfigFile("somenamespace", "somegroup", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNamespaceBlank2() {
        defaultConfigFileService.getConfigFile(new DefaultConfigFileMetadata("", "somegroup", "application.yaml"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGroupBlank2() {
        defaultConfigFileService.getConfigFile(new DefaultConfigFileMetadata("somenamespace", "", "application.yaml"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileNameBlank2() {
        defaultConfigFileService.getConfigFile(new DefaultConfigFileMetadata("somenamespace", "somegroup", ""));
    }

    @Test
    public void testGetNormalConfigFile() {
        when(configFileManager.getConfigFile(any())).thenReturn(configFile);

        ConfigFile configFile2 = defaultConfigFileService.getConfigFile("somenamespace", "somegroup", "application.yaml");

        verify(configFileManager).getConfigFile(any());
        Assert.assertEquals(configFile2, configFile2);
    }

}
