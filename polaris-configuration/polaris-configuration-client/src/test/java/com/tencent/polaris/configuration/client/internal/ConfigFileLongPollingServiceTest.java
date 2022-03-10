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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFileLongPollingServiceTest {

    @Mock
    private ConfigFileConnector configFileConnector;
    @Mock
    private SDKContext          sdkContext;


    @Test
    public void testNotReceivedPushEvent() throws InterruptedException {
        //初始化 LongPollingService
        DefaultConfigFileLongPollingService longPollingService =
            new DefaultConfigFileLongPollingService(sdkContext, configFileConnector);

        RemoteConfigFileRepo remoteConfigFileRepo = mock(RemoteConfigFileRepo.class);
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(remoteConfigFileRepo.getConfigFileMetadata()).thenReturn(configFileMetadata);

        longPollingService.addConfigFile(remoteConfigFileRepo);

        //第一次收到变更事件,第二次也没有变化
        when(configFileConnector.watchConfigFiles(anyList()))
            .then(invocation -> {
                return new ConfigFileResponse(ServerCodes.DATA_NO_CHANGE, "", null);
            })
            .then(invocation -> {
                TimeUnit.SECONDS.sleep(6);
                return new ConfigFileResponse(ServerCodes.DATA_NO_CHANGE, "", null);
            });

        //因为LongPolling会在 5s 后开始执行
        TimeUnit.SECONDS.sleep(7);

        verify(configFileConnector, times(2)).watchConfigFiles(anyList());

        //没有触发回调
        verify(remoteConfigFileRepo, times(0)).onLongPollNotified(anyLong());
    }

    @Test
    public void testReceivedPushEvent() throws InterruptedException {
        //初始化 LongPollingService
        DefaultConfigFileLongPollingService longPollingService =
            new DefaultConfigFileLongPollingService(sdkContext, configFileConnector);

        RemoteConfigFileRepo remoteConfigFileRepo = mock(RemoteConfigFileRepo.class);
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(remoteConfigFileRepo.getConfigFileMetadata()).thenReturn(configFileMetadata);

        longPollingService.addConfigFile(remoteConfigFileRepo);

        //构造监听响应对象
        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                               ConfigFileTestUtils.testFileName);
        String content = "hello world";
        long version = 100;
        configFile.setContent(content);
        configFile.setVersion(version);
        ConfigFileResponse configFileResponse = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);

        //第一次收到变更事件,第二次没有变化
        when(configFileConnector.watchConfigFiles(anyList())).thenReturn(configFileResponse).then(invocation -> {
            TimeUnit.SECONDS.sleep(30);
            return new ConfigFileResponse(ServerCodes.DATA_NO_CHANGE, "", null);
        });

        //因为LongPolling会在 5s 后开始执行
        TimeUnit.SECONDS.sleep(6);

        verify(configFileConnector, times(2)).watchConfigFiles(anyList());

        //验证触发回调
        verify(remoteConfigFileRepo).onLongPollNotified(version);
    }

    @Test
    public void testThrowRetryException() throws InterruptedException {
        //初始化 LongPollingService
        DefaultConfigFileLongPollingService longPollingService =
            new DefaultConfigFileLongPollingService(sdkContext, configFileConnector);

        RemoteConfigFileRepo remoteConfigFileRepo = mock(RemoteConfigFileRepo.class);
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(remoteConfigFileRepo.getConfigFileMetadata()).thenReturn(configFileMetadata);

        longPollingService.addConfigFile(remoteConfigFileRepo);

        //模拟抛异常
        when(configFileConnector.watchConfigFiles(anyList()))
            .thenThrow(new RetriableException(ErrorCode.API_TIMEOUT, ""));

        //因为LongPolling会在 5s 后开始执行
        TimeUnit.SECONDS.sleep(7);

        verify(configFileConnector, times(2)).watchConfigFiles(anyList());

        //没有触发回调
        verify(remoteConfigFileRepo, times(0)).onLongPollNotified(anyLong());
    }

    @Test
    public void testSecondReceivedVersionLessThanFirstReceived() throws InterruptedException {
        //初始化 LongPollingService
        DefaultConfigFileLongPollingService longPollingService =
            new DefaultConfigFileLongPollingService(sdkContext, configFileConnector);

        RemoteConfigFileRepo remoteConfigFileRepo = mock(RemoteConfigFileRepo.class);
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(remoteConfigFileRepo.getConfigFileMetadata()).thenReturn(configFileMetadata);

        longPollingService.addConfigFile(remoteConfigFileRepo);

        //第一次版本号为100，第二次版本号为99
        when(configFileConnector.watchConfigFiles(anyList()))
            .then(invocation -> {
                ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                                       ConfigFileTestUtils.testFileName);
                String content = "hello world";
                long version = 100;
                configFile.setContent(content);
                configFile.setVersion(version);
                return new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);
            })
            .then(invocation -> {
                ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                                       ConfigFileTestUtils.testFileName);
                String content = "hello world";
                long version = 99;
                configFile.setContent(content);
                configFile.setVersion(version);
                return new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);
            })
            .then(invocation -> {
                TimeUnit.SECONDS.sleep(30);
                return new ConfigFileResponse(ServerCodes.DATA_NO_CHANGE, "", null);
            });

        //因为LongPolling会在 5s 后开始执行
        TimeUnit.SECONDS.sleep(7);

        verify(configFileConnector, times(3)).watchConfigFiles(anyList());

        //没有触发回调
        verify(remoteConfigFileRepo, times(2)).onLongPollNotified(100);
    }

}
