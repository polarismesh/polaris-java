package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @author lepdou 2022-03-01
 */
public class RemoteConfigFileRepo extends AbstractConfigFileRepo {

	private static final long INIT_VERSION = 0;

	private static final int PULL_CONFIG_RETRY_TIMES = 3;

	private static final ScheduledExecutorService pullExecutorService;

	private final AtomicReference<ConfigFile> remoteConfigFile;
	//服务端通知的版本号，此版本号有可能落后于服务端
	private final AtomicLong notifiedVersion;
	private final ConfigFileConnector configFileConnector;
	private final RetryPolicy retryPolicy;
	private ConfigFilePersistHandler configFilePersistHandler;

	static {
		pullExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Configuration-Pull"));
	}

	public RemoteConfigFileRepo(SDKContext sdkContext,
			ConfigFileLongPollingService configFileLongPollingService,
			ConfigFileConnector configFileConnector,
			ConfigFileMetadata configFileMetadata) {
		super(sdkContext, configFileMetadata);

		remoteConfigFile = new AtomicReference<>();
		notifiedVersion = new AtomicLong(INIT_VERSION);
		retryPolicy = new ExponentialRetryPolicy(1, 120);

		//获取远程调用插件实现类
		if (configFileConnector != null) {
			this.configFileConnector = configFileConnector;
		}
		else {
			String configFileConnectorType = sdkContext.getConfig().getConfigFile().getServerConnector()
					.getConnectorType();
			this.configFileConnector = (ConfigFileConnector) sdkContext.getExtensions().getPlugins()
					.getPlugin(PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType(), configFileConnectorType);
		}

		initPersistHandle();

		//同步从远程仓库拉取一次
		pull();

		//加入到长轮询的池子里
		addToLongPollingPool(configFileLongPollingService, configFileMetadata);

		startCheckVersionTask();
	}

	private void initPersistHandle() {
		String persistDir = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistDir();

		int maxReadRetry = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistMaxReadRetry();
		int maxWriteRetry = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistMaxWriteRetry();
		long retryIntervalMs = sdkContext.getConfig().getConfigFile().getServerConnector()
				.getPersistRetryInterval();
		this.configFilePersistHandler = new ConfigFilePersistHandler(persistDir, maxWriteRetry,
				maxReadRetry, retryIntervalMs);
		try {
			this.configFilePersistHandler.init();
		}
		catch (IOException e) {
			LOGGER.warn("config file persist handler init fail:" + e.getMessage(), e);
		}
	}

	private boolean isAllowPersistToFile() {
		return sdkContext.getConfig().getConfigFile().getServerConnector().getPersistEnable() &&
				!DefaultValues.LOCAL_FILE_CONNECTOR_TYPE.equals(sdkContext.getConfig()
						.getConfigFile().getServerConnector().getConnectorType());
	}

	private void addToLongPollingPool(ConfigFileLongPollingService configFileLongPollingService,
			ConfigFileMetadata configFileMetadata) {
		ConfigFile configFile = remoteConfigFile.get();

		//理论上，加到长轮询任务之前会同步一次配置文件，但是可能会同步失败。
		if (configFile == null) {
			configFile = new ConfigFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
					configFileMetadata.getFileName());
			//初始版本号为0
			configFile.setVersion(INIT_VERSION);
		}

		configFileLongPollingService.addConfigFile(this);
	}

	@Override
	public String getContent() {
		return remoteConfigFile.get() != null ? remoteConfigFile.get().getContent() : null;
	}

	public long getConfigFileVersion() {
		if (remoteConfigFile.get() == null) {
			return INIT_VERSION;
		}
		return remoteConfigFile.get().getVersion();
	}

	@Override
	protected void doPull() {
		long startTime = System.currentTimeMillis();

		ConfigFile pullConfigFileReq = new ConfigFile(configFileMetadata.getNamespace(),
				configFileMetadata.getFileGroup(),
				configFileMetadata.getFileName());
		pullConfigFileReq.setVersion(notifiedVersion.get());

		LOGGER.info("[Config] start pull config file. config file = {}, version = {}",
				configFileMetadata, notifiedVersion.get());

		int retryTimes = 0;
		while (retryTimes < PULL_CONFIG_RETRY_TIMES) {
			try {
				ConfigFileResponse response = configFileConnector.getConfigFile(pullConfigFileReq);
				retryPolicy.success();

				//打印请求信息
				long pulledConfigFileVersion =
						response.getConfigFile() != null ? response.getConfigFile().getVersion() : -1;
				LOGGER.info("[Config] pull config file finished. config file = {}, code = {}, version = {}, duration = {} ms",
						configFileMetadata, response.getCode(), pulledConfigFileVersion,
						System.currentTimeMillis() - startTime);

				if (response.getCode() == ServerCodes.EXECUTE_SUCCESS) {
					ConfigFile pulledConfigFile = response.getConfigFile();
					// update local file cache
					updateLocalCacheFile(pulledConfigFile);
					//本地配置文件落后，更新内存缓存
					if (remoteConfigFile.get() == null ||
							pulledConfigFile.getVersion() >= remoteConfigFile.get().getVersion()) {
						ConfigFile copiedConfigFile = deepCloneConfigFile(pulledConfigFile);
						remoteConfigFile.set(copiedConfigFile);

						//配置有更新，触发回调
						fireChangeEvent(copiedConfigFile.getContent());
					}
					return;
				}

				//远端没有此配置文件
				if (response.getCode() == ServerCodes.NOT_FOUND_RESOURCE) {
					LOGGER.warn("[Config] config file not found, please check whether config file released. {}",
							configFileMetadata);
					//delete local file cache
					deleteLocalFile();
					//删除配置文件
					if (remoteConfigFile.get() != null) {
						remoteConfigFile.set(null);

						//删除配置文件也需要触发通知
						fireChangeEvent(null);
					}
					return;
				}

				//预期之外的状态码，重试
				LOGGER.error("[Config] pull response without expected code. retry times = {}, code = {}", retryTimes,
						response.getCode());
				retryPolicy.fail();

				retryTimes++;
				retryPolicy.executeDelay();
				if (isNeedFallback(retryTimes)) {
					fallback(pullConfigFileReq);
				}
			}
			catch (Throwable t) {
				LOGGER.error("[Config] failed to pull config file. retry times = " + retryTimes, t);
				retryPolicy.fail();

				retryTimes++;
				retryPolicy.executeDelay();
				if (isNeedFallback(retryTimes)) {
					fallback(pullConfigFileReq);
				}
			}
		}
	}

	private boolean isNeedFallback(final int retryTimes) {
		return retryTimes >= PULL_CONFIG_RETRY_TIMES &&
				sdkContext.getConfig().getConfigFile().getServerConnector().getFallbackToLocalCache();
	}

	private void fallback(ConfigFile configFileReq) {
		ConfigFile configFileRes = configFilePersistHandler.loadPersistedConfigFile(configFileReq);
		if (configFileRes != null) {
			LOGGER.info("[Config] failed to pull config file from remote,fallback to local cache success.{}.", configFileRes);
			remoteConfigFile.set(configFileRes);
			//配置有更新，触发回调
			fireChangeEvent(configFileRes.getContent());
			return;
		}
		LOGGER.info("[Config] failed to pull config file from remote,fallback to local cache fail.{}.", configFileReq);
	}

	public void onLongPollNotified(long newVersion) {
		if (remoteConfigFile.get() != null && remoteConfigFile.get().getVersion() >= newVersion) {
			return;
		}

		notifiedVersion.set(newVersion);

		//版本落后，从服务端拉取最新的配置文件
		pullExecutorService.submit((Runnable) this::pull);
	}

	private void deleteLocalFile() {
		if (isAllowPersistToFile()) {
			this.configFilePersistHandler.deleteFileConfig(new ConfigFile(configFileMetadata.getNamespace(),
					configFileMetadata.getFileGroup(), configFileMetadata.getFileName()));
		}
	}

	private void updateLocalCacheFile(ConfigFile configFile) {
		if (isAllowPersistToFile()) {
			this.configFilePersistHandler.saveConfigFile(configFile);
		}
	}

	// 有可能出现收到通知时，重新拉取配置失败。此时 notifiedVersion 大于 remoteConfigFile.version，需要定时重试
	private void startCheckVersionTask() {
		pullExecutorService.scheduleAtFixedRate(() -> {
			//没有通知的版本号
			if (notifiedVersion == null || notifiedVersion.get() == 0) {
				return;
			}

			long pulledVersion =
					remoteConfigFile.get() != null ? remoteConfigFile.get().getVersion() : INIT_VERSION;

			//版本落后，需要重新拉取
			if (notifiedVersion.get() > pulledVersion) {
				LOGGER.info("[Config] notified version greater than pulled version, will pull config file."
								+ "file = {}, notified version = {}, pulled version = {}", getConfigFileMetadata(),
						notifiedVersion, pulledVersion);
				pull();
			}
		}, 1, 1, TimeUnit.MINUTES);
	}

	private ConfigFile deepCloneConfigFile(ConfigFile sourceConfigFile) {
		ConfigFile configFile =
				new ConfigFile(sourceConfigFile.getNamespace(), sourceConfigFile.getFileGroup(),
						sourceConfigFile.getFileName());
		configFile.setContent(sourceConfigFile.getContent());
		configFile.setVersion(sourceConfigFile.getVersion());
		configFile.setMd5(sourceConfigFile.getMd5());
		return configFile;
	}

}
