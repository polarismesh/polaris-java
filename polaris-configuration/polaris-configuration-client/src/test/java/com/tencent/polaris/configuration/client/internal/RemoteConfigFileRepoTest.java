package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.client.ConfigFileTestUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class RemoteConfigFileRepoTest {

    @Mock
    private ConfigFileConnector          configFileConnector;
    @Mock
    private ConfigFileLongPollingService configFileLongPollingService;
    @Mock
    private SDKContext                   sdkContext;

    @Test
    public void testPullSuccess() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                               ConfigFileTestUtils.testFileName);
        String content = "hello world";
        long version = 100;
        configFile.setContent(content);
        configFile.setVersion(version);
        ConfigFileResponse configFileResponse = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);

        when(configFileConnector.getConfigFile(any())).thenReturn(configFileResponse);

        RemoteConfigFileRepo remoteConfigFileRepo =
            new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileConnector, configFileMetadata);

        verify(configFileConnector).getConfigFile(any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertEquals(content, remoteConfigFileRepo.getContent());
        Assert.assertEquals(version, remoteConfigFileRepo.getConfigFileVersion());
    }

    @Test
    public void testPullNotFoundConfigFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        ConfigFileResponse configFileResponse = new ConfigFileResponse(ServerCodes.NOT_FOUND_RESOURCE, "", null);

        when(configFileConnector.getConfigFile(any())).thenReturn(configFileResponse);

        RemoteConfigFileRepo remoteConfigFileRepo =
            new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileConnector, configFileMetadata);

        verify(configFileConnector).getConfigFile(any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertNull(remoteConfigFileRepo.getContent());
        Assert.assertEquals(0, remoteConfigFileRepo.getConfigFileVersion());
    }

    @Test
    public void testPullWithUnexpectedResponseCode() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        ConfigFileResponse configFileResponse = new ConfigFileResponse(50000, "", null);

        when(configFileConnector.getConfigFile(any())).thenReturn(configFileResponse);

        RemoteConfigFileRepo remoteConfigFileRepo =
            new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileConnector, configFileMetadata);

        //重试三次
        verify(configFileConnector, times(3)).getConfigFile(any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertNull(remoteConfigFileRepo.getContent());
        Assert.assertEquals(0, remoteConfigFileRepo.getConfigFileVersion());
    }

    @Test
    public void testPullWithRetryException() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        when(configFileConnector.getConfigFile(any())).thenThrow(new RetriableException(ErrorCode.API_TIMEOUT, ""));

        RemoteConfigFileRepo remoteConfigFileRepo =
            new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileConnector, configFileMetadata);

        //重试三次
        verify(configFileConnector, times(3)).getConfigFile(any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertNull(remoteConfigFileRepo.getContent());
        Assert.assertEquals(0, remoteConfigFileRepo.getConfigFileVersion());
    }

    @Test
    public void testNotifyAndPullSecondTime() throws InterruptedException {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                               ConfigFileTestUtils.testFileName);
        String content = "hello world";
        long version = 100;
        configFile.setContent(content);
        configFile.setVersion(version);
        ConfigFileResponse configFileResponse = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);

        when(configFileConnector.getConfigFile(any())).thenReturn(configFileResponse);

        RemoteConfigFileRepo remoteConfigFileRepo =
            new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileConnector, configFileMetadata);

        AtomicInteger cbCnt = new AtomicInteger();
        //增加两个listener
        remoteConfigFileRepo.addChangeListener((configFileMetadata1, newContent) -> {
            cbCnt.getAndIncrement();
        });
        remoteConfigFileRepo.addChangeListener((configFileMetadata1, newContent) -> {
            cbCnt.getAndIncrement();
        });

        TimeUnit.MILLISECONDS.sleep(500);

        //第一次初始化拉取配置
        verify(configFileConnector).getConfigFile(any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertEquals(0, cbCnt.get());
        Assert.assertEquals(content, remoteConfigFileRepo.getContent());
        Assert.assertEquals(version, remoteConfigFileRepo.getConfigFileVersion());

        //变更通知，重新拉取配置
        long newVersion = version + 1;
        String newContent = "hello world2";
        configFile.setContent(newContent);
        configFile.setVersion(newVersion);

        ConfigFileResponse configFileResponse2 = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);

        when(configFileConnector.getConfigFile(any())).thenReturn(configFileResponse2);

        remoteConfigFileRepo.onLongPollNotified(newVersion);

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            //ignore
        }

        verify(configFileConnector, times(2)).getConfigFile(any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertEquals(2, cbCnt.get()); //触发回调
        Assert.assertEquals(newContent, remoteConfigFileRepo.getContent());
        Assert.assertEquals(newVersion, remoteConfigFileRepo.getConfigFileVersion());

        //变更通知的版本号小于内存里缓存的版本号，则不会触发重新拉取配置
        long smallVersion = 100;
        remoteConfigFileRepo.onLongPollNotified(smallVersion);

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            //ignore
        }

        verify(configFileConnector, times(2)).getConfigFile(any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertEquals(2, cbCnt.get()); //不触发回调，所以还是2次
        Assert.assertEquals(newContent, remoteConfigFileRepo.getContent());
        Assert.assertEquals(newVersion, remoteConfigFileRepo.getConfigFileVersion());
    }
}
