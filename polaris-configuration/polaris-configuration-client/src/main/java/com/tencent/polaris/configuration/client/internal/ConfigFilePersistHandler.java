/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.configuration.client.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.factory.util.FileUtils;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import static com.tencent.polaris.api.config.verify.DefaultValues.PATTERN_CONFIG_FILE;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * 配置文件持久化处理器
 *
 * @author rod.xu
 * @date 2022/09/26
 */
public class ConfigFilePersistHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigFilePersistHandler.class);

	private final String persistDirPath;
	private final int maxWriteRetry;
	private final int maxReadRetry;
	private final long retryInterval;
	private final boolean isAllowPersist;
	private final String connectorType;
	private static final ExecutorService persistExecutor = Executors
			.newSingleThreadExecutor(new NamedThreadFactory("configFile-persist-handler"));

	public ConfigFilePersistHandler(SDKContext sdkContext) throws IOException {
		String persistDir = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistDir();
		this.maxReadRetry = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistMaxReadRetry();
		this.maxWriteRetry = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistMaxWriteRetry();
		this.retryInterval = sdkContext.getConfig().getConfigFile().getServerConnector()
				.getPersistRetryInterval();
		this.isAllowPersist = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistEnable();
		this.connectorType = sdkContext.getConfig().getConfigFile().getServerConnector().getConnectorType();
		this.persistDirPath = Utils.translatePath(persistDir);
		FileUtils.dirPathCheck(this.persistDirPath);
	}

	private boolean isAllowPersistToFile() {
		return isAllowPersist && !DefaultValues.LOCAL_FILE_CONNECTOR_TYPE.equals(connectorType);
	}

	public void asyncDeleteConfigFile(ConfigFile configFile) {
		if (!persistExecutor.isShutdown() && isAllowPersistToFile()) {
			persistExecutor.execute(new DeleteTask(configFile));
		}
	}

	/**
	 * 删除服务缓存数据
	 *
	 * @param configFile config metadata
	 */
	public void deleteFileConfig(ConfigFile configFile) {
		String fileName = configFileToFileName(configFile);
		String persistFilePath = persistDirPath + File.separator + fileName;
		try {
			Files.deleteIfExists(FileSystems.getDefault().getPath(persistFilePath));
		}
		catch (IOException e) {
			LOG.error("fail to delete cache file {}", persistFilePath);
		}
		String lockFileName = fileName + ".lock";
		String persistFileLockPath = persistDirPath + File.separator + lockFileName;
		try {
			Files.deleteIfExists(FileSystems.getDefault().getPath(persistFileLockPath));
		}
		catch (IOException e) {
			LOG.error("fail to delete cache lock file {}", persistFileLockPath);
		}
	}

	public void asyncSaveConfigFile(ConfigFile configFile) {
		if (!persistExecutor.isShutdown() && isAllowPersistToFile()) {
			persistExecutor.execute(new SaveTask(configFile));
		}
	}

	/**
	 * 持久化配置文件
	 *
	 * @param configFile config file
	 */
	public void saveConfigFile(ConfigFile configFile) {
		int retryTimes = 0;
		LOG.info("start to save config file {}", configFile);
		while (retryTimes <= maxWriteRetry) {
			retryTimes++;
			Path path = doSaveConfigFile(configFile);
			if (null == path) {
				continue;
			}
			LOG.info("end to save config file {} to {}", configFile, path);
			return;
		}
		LOG.error("fail to persist config file {} after retry {}", configFile, retryTimes);
	}

	private static String configFileToFileName(ConfigFile configFile) {
		try {
			String encodedNamespace = URLEncoder.encode(configFile.getNamespace(), "UTF-8");
			String encodedFileGroup = URLEncoder.encode(configFile.getFileGroup(), "UTF-8");
			String encodeFileName = URLEncoder.encode(configFile.getFileName(), "UTF-8");
			return String.format(PATTERN_CONFIG_FILE, encodedNamespace, encodedFileGroup, encodeFileName);
		}
		catch (UnsupportedEncodingException e) {
			throw new AssertionError("UTF-8 is unknown");
		}
	}

	private void writeTmpFile(File persistTmpFile, File persistLockFile, ConfigFile configFile) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(persistLockFile, "rw");
			 FileChannel channel = raf.getChannel()) {
			FileLock lock = channel.tryLock();
			if (lock == null) {
				throw new IOException("fail to lock file " + persistTmpFile
						.getAbsolutePath() + ", ignore and retry later");
			}
			//执行保存
			try {
				doWriteTmpFile(persistTmpFile, configFile);
			}
			finally {
				lock.release();
			}
		}
	}

	private void doWriteTmpFile(File persistTmpFile, ConfigFile configFile) throws IOException {
		if (!persistTmpFile.exists()) {
			if (!persistTmpFile.createNewFile()) {
				LOG.warn("tmp file {} already exists", persistTmpFile.getAbsolutePath());
			}
		}
		try (FileOutputStream outputFile = new FileOutputStream(persistTmpFile)) {
			String jsonAsYaml = new YAMLMapper().writeValueAsString(configFile);
			outputFile.write(jsonAsYaml.getBytes(StandardCharsets.UTF_8));
			outputFile.flush();
		}
	}

	private Path doSaveConfigFile(ConfigFile configFile) {
		String fileName = configFileToFileName(configFile);
		String tmpFileName = fileName + ".tmp";
		String lockFileName = fileName + ".lock";
		String persistFilePathStr = persistDirPath + File.separator + fileName;
		Path persistPath = FileSystems.getDefault().getPath(persistFilePathStr);
		File persistTmpFile = new File(persistDirPath + File.separator + tmpFileName);
		File persistLockFile = new File(persistDirPath + File.separator + lockFileName);
		try {
			if (!persistLockFile.exists()) {
				if (!persistLockFile.createNewFile()) {
					LOG.warn("lock file {} already exists", persistLockFile.getAbsolutePath());
				}
			}
			writeTmpFile(persistTmpFile, persistLockFile, configFile);
			Files.move(FileSystems.getDefault().getPath(persistTmpFile.getAbsolutePath()),
					persistPath, REPLACE_EXISTING, ATOMIC_MOVE);
		}
		catch (IOException e) {
			LOG.error("fail to write file :" + persistTmpFile, e);
			return null;
		}
		return persistPath.toAbsolutePath();
	}

	/**
	 * 从缓存目录加载缓存的配置文件
	 *
	 * @param configFile 配置文件
	 * @return 配置文件
	 */
	public ConfigFile loadPersistedConfigFile(ConfigFile configFile) {
		String fileName = configFileToFileName(configFile);
		String persistFilePathStr = persistDirPath + File.separator + fileName;
		Path persistPath = FileSystems.getDefault().getPath(persistFilePathStr);
		int retryTimes = 0;
		ConfigFile resConfigFile = null;
		while (retryTimes <= maxReadRetry) {
			retryTimes++;
			resConfigFile = loadConfigFile(persistPath.toFile(), configFile);
			if (null == resConfigFile) {
				Utils.sleepUninterrupted(retryInterval);
				continue;
			}
			break;
		}
		if (null == resConfigFile) {
			LOG.debug("fail to read config file from {} after retry {} times", fileName, retryTimes);
			return null;
		}
		return resConfigFile;
	}

	private ConfigFile loadConfigFile(File persistFile, ConfigFile configFile) {
		if (null == persistFile || !persistFile.exists()) {
			return null;
		}
		InputStream inputStream = null;
		InputStreamReader reader = null;
		Yaml yaml = new Yaml();
		try {
			inputStream = new FileInputStream(persistFile);
			reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			ConfigFile resConfigFile = new ConfigFile(configFile.getNamespace(),
					configFile.getFileGroup(), configFile.getFileName());
			Map<String, Object> jsonMap = yaml.load(reader);
			resConfigFile.setContent(jsonMap.get("content").toString());
			resConfigFile.setMd5(jsonMap.get("md5").toString());
			resConfigFile.setVersion(Long.valueOf(String.valueOf(jsonMap.get("version"))));
			return resConfigFile;
		}
		catch (IOException e) {
			LOG.warn("fail to read file :" + persistFile.getAbsoluteFile(), e);
			return null;
		}
		finally {
			if (null != reader) {
				try {
					reader.close();
				}
				catch (IOException e) {
					LOG.warn("fail to close reader for :" + persistFile.getAbsoluteFile(), e);
				}
			}
			if (null != inputStream) {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					LOG.warn("fail to close stream for :" + persistFile.getAbsoluteFile(), e);
				}
			}
		}
	}

	private class DeleteTask implements Runnable {

		private ConfigFile configFile;

		public DeleteTask(ConfigFile configFile) {
			this.configFile = configFile;
		}

		@Override
		public void run() {
			deleteFileConfig(configFile);
		}
	}

	private class SaveTask implements Runnable {

		private ConfigFile configFile;

		public SaveTask(ConfigFile configFile) {
			this.configFile = configFile;
		}

		@Override
		public void run() {
			saveConfigFile(configFile);
		}
	}

}