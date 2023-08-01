package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileGroupConnector;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultConfigFileGroupPullService implements RevisableConfigFileGroupPullService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfigFileGroupPullService.class);

    private final AtomicReference<Boolean> started;
    private final Map<ConfigFileGroupMetadata, RevisableConfigFileGroup> configFileGroupCache;
    private final ExecutorService pullExecutorPool;
    private final ExecutorService taskStarter;
    private final RetryableConfigFileGroupConnector rpcConnector;

    public DefaultConfigFileGroupPullService(SDKContext sdkContext,
                                             Map<ConfigFileGroupMetadata, RevisableConfigFileGroup> configFileGroupCache, ConfigFileGroupConnector connector) {
        this.started = new AtomicReference<>(false);
        this.configFileGroupCache = configFileGroupCache;
        this.rpcConnector = new RetryableConfigFileGroupConnector(connector, getPullFailedRetryStrategy());

        NamedThreadFactory threadFactory = new NamedThreadFactory(getClass().getSimpleName());
        int threadNum = sdkContext.getConfig().getConfigFile().getServerConnector().getConfigFileGroupThreadNum();
        this.pullExecutorPool = Executors.newFixedThreadPool(threadNum, threadFactory);
        this.taskStarter = Executors.newSingleThreadExecutor(threadFactory);
    }

    public RetryableConfigFileGroupConnector.RetryableValidator getPullFailedRetryStrategy() {
        return response -> {
            switch (response.getCode()) {
                case ServerCodes.DATA_NO_CHANGE:
                case ServerCodes.EXECUTE_SUCCESS:
                    return false;
                default:
                    return true;
            }
        };
    }

    public void pullConfigFileGroup(RevisableConfigFileGroup fileGroup) {
        if (!started.get()) {
            startPullingTask();
        }
    }

    private void startPullingTask() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        try {
            taskStarter.submit(() -> {
                Random random = new Random();
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        TimeUnit.SECONDS.sleep(5 + random.nextInt(5));
                    } catch (InterruptedException e) {
                        //ignore
                    }

                    pullAll();
                }
            });
        } catch (Throwable t) {
            started.set(false);
        }
    }

    private void pullAll() {
        for (Map.Entry<ConfigFileGroupMetadata, RevisableConfigFileGroup> entry :
                configFileGroupCache.entrySet()) {
            ConfigFileGroupMetadata metadata = entry.getKey();
            RevisableConfigFileGroup oldConfigFileGroup = entry.getValue();

            pullExecutorPool.submit(() -> {
                com.tencent.polaris.api.plugin.configuration.ConfigFileGroupMetadata metadataRPCObj = new
                        com.tencent.polaris.api.plugin.configuration.ConfigFileGroupMetadata();
                metadataRPCObj.setFileGroupName(metadata.getFileGroupName());
                metadataRPCObj.setNamespace(metadata.getNamespace());
                com.tencent.polaris.api.plugin.configuration.ConfigFileGroupResponse response =
                        rpcConnector.GetConfigFileMetadataList(metadataRPCObj, oldConfigFileGroup.getRevision());
                if (response == null) {
                    return;
                }

                switch (response.getCode()) {
                    case ServerCodes.EXECUTE_SUCCESS:
                        com.tencent.polaris.api.plugin.configuration.ConfigFileGroup configFileGroupObj =
                                response.getConfigFileGroup();
                        String newlyRevision = response.getRevision();

                        List<ConfigFile> configFileList = configFileGroupObj.getConfigFileList();
                        configFileList.sort(Comparator.comparing(ConfigFile::getReleaseTime));
                        List<ConfigFileMetadata> configFileMetadataList = new ArrayList<>();
                        for (ConfigFile configFile : configFileList) {
                            configFileMetadataList.add(new ConfigFileGroupManager.InternalConfigFileMetadata(configFile));
                        }
                        ConfigFileGroup configFileGroup = new DefaultConfigFileGroup(configFileGroupObj.getNamespace(),
                                configFileGroupObj.getFileGroupName(), configFileMetadataList);
                        RevisableConfigFileGroup revisableConfigFileGroup = new RevisableConfigFileGroup(configFileGroup, newlyRevision);
                        configFileGroupCache.put(metadata, revisableConfigFileGroup);
                        LOGGER.info("[Config] pull result: success. namespace = {}, fileGroupName = {}, oldRevision = {}, newlyRevision = {}",
                                metadata.getNamespace(), metadata.getFileGroupName(), oldConfigFileGroup.getRevision(), newlyRevision);
                        return;
                    case ServerCodes.DATA_NO_CHANGE:
                        LOGGER.debug("[Config] pull result: data no change. namespace = {}, fileGroupName = {}",
                                metadata.getNamespace(), metadata.getFileGroupName());
                        return;
                    default:
                        // Unreachable
                        return;
                }
            });
        }
    }

}
