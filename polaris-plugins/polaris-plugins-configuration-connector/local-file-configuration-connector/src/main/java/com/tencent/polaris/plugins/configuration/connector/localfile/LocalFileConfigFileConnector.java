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

package com.tencent.polaris.plugins.configuration.connector.localfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.util.FileUtils;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import static com.tencent.polaris.api.config.verify.DefaultValues.LOCAL_FILE_CONNECTOR_TYPE;
import static com.tencent.polaris.api.config.verify.DefaultValues.PATTERN_CONFIG_FILE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * @author DoubleLuXu 2022-09-20
 */
public class LocalFileConfigFileConnector implements ConfigFileConnector {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileConfigFileConnector.class);

	private ExecutorService executorService;
	private WatchService watcher;
	private String persistDirPath;
	private Path dir;
	/**
	 * config file changed queue
	 */
	private BlockingQueue<ConfigFileChange> blockingQueue = new ArrayBlockingQueue(1024);

	@Override
	public void init(InitContext ctx) throws PolarisException {
		String dirPath = ctx.getConfig().getConfigFile().getServerConnector().getPersistDir();
		if (StringUtils.isBlank(dirPath)) {
			dirPath = DefaultValues.CONFIG_FILE_DEFAULT_CACHE_PERSIST_DIR;
		}
		this.persistDirPath = Utils.translatePath(dirPath);
		this.dir = Paths.get(this.persistDirPath);
		try {
			FileUtils.dirPathCheck(this.persistDirPath);
		}
		catch (IOException ex) {
			throw new PolarisException(ErrorCode.INVALID_CONFIG, ex.getMessage(), ex);
		}
		this.executorService = Executors.newSingleThreadExecutor();
		try {
			this.watcher = FileSystems.getDefault().newWatchService();
			this.dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			LOGGER.info("init local file config connector,watch dir:[{}].", persistDirPath);
		}
		catch (IOException e) {
			LOGGER.error("file system watch path: " + persistDirPath + " error.", e);
			throw new PolarisException(ErrorCode.UNKNOWN_SERVER_ERROR, "");
		}
		watchFileChange();
	}

	@Override
	public ConfigFileResponse getConfigFile(ConfigFile configFile) {
		String configFileName = configFileToFileName(configFile);
		ConfigFile configFileRes = loadConfigFile(configFileName);
		if (configFileRes != null) {
			return new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "success.", configFileRes);
		}
		return new ConfigFileResponse(ServerCodes.NOT_FOUND_RESOURCE, "config file not found.", null);
	}

	@Override
	public ConfigFileResponse watchConfigFiles(List<ConfigFile> configFiles) {
		try {
			while (true) {
				ConfigFileChange configFileChange = blockingQueue.take();
				Optional<ConfigFile> optional = configFiles.stream().filter(item -> configFileToFileName(item)
						.equals(configFileChange.getFileName())).findFirst();
				if (optional.isPresent()) {
					return getConfigFile(optional.get());
				}
			}
		}
		catch (InterruptedException e) {
			LOGGER.warn("config file watch interrupt " + e.getMessage());
		}
		return null;
	}

	@Override
	public String getName() {
		return LOCAL_FILE_CONNECTOR_TYPE;
	}

	@Override
	public PluginType getType() {
		return PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType();
	}

	@Override
	public void postContextInit(Extensions extensions) throws PolarisException {

	}

	@Override
	public void destroy() {
		if (this.watcher != null) {
			try {
				this.watcher.close();
				LOGGER.info("watcher close success.");
			}
			catch (IOException e) {
				LOGGER.error("watcher close error.", e);
			}
		}
		if (this.executorService != null) {
			this.executorService.shutdown();
		}
	}

	private void watchFileChange() {
		executorService.execute(() -> {
			while (true) {
				WatchKey key = null;
				try {
					key = watcher.take();
				}
				catch (InterruptedException e) {
					LOGGER.error("file watcher take interrupted.", e);
				}
				catch (ClosedWatchServiceException e) {
					LOGGER.warn("file watcher closed.", e);
					return;
				}
				List<WatchEvent<?>> watchEvents = key.pollEvents();
				for (WatchEvent<?> event : watchEvents) {
					LOGGER.info("watched file event:{}:{}/{}.", event.kind(), this.dir.toAbsolutePath(),
							event.context());
					if (StandardWatchEventKinds.ENTRY_CREATE == event.kind()) {
						blockingQueue.offer(new ConfigFileChange(ConfigFileChange.ChangeType.CREATE,
								event.context().toString()));
					}
					if (StandardWatchEventKinds.ENTRY_MODIFY == event.kind()) {
						blockingQueue.offer(new ConfigFileChange(ConfigFileChange.ChangeType.UPDATE,
								event.context().toString()));
					}
					if (StandardWatchEventKinds.ENTRY_DELETE == event.kind()) {
						blockingQueue.offer(new ConfigFileChange(ConfigFileChange.ChangeType.DELETE,
								event.context().toString()));
					}
				}
				key.reset();
			}
		});
	}

	public ConfigFile loadConfigFile(String fileName) {
		String persistFilePathStr = persistDirPath + File.separator + fileName;
		Path persistPath = FileSystems.getDefault().getPath(persistFilePathStr);
		File persistFile = persistPath.toFile();
		if (null == persistFile || !persistFile.exists()) {
			return null;
		}
		return loadConfigFile(persistPath.toFile(), fileNameToConfigFile(fileName));
	}

	private static final String CACHE_SUFFIX = ".yaml";

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

	private ConfigFile fileNameToConfigFile(String fileName) {
		fileName = fileName.substring(0, fileName.length() - CACHE_SUFFIX.length());
		String[] pieces = fileName.split("#");
		try {
			String namespace = URLDecoder.decode(pieces[0], "UTF-8");
			String fileGroup = URLDecoder.decode(pieces[1], "UTF-8");
			String configFileName = URLDecoder.decode(pieces[2], "UTF-8");
			return new ConfigFile(namespace, fileGroup, configFileName);
		}
		catch (UnsupportedEncodingException e) {
			throw new AssertionError("UTF-8 is unknown");
		}
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
			LOGGER.warn("fail to read file :" + persistFile.getAbsoluteFile(), e);
			return null;
		}
		finally {
			if (null != reader) {
				try {
					reader.close();
				}
				catch (IOException e) {
					LOGGER.warn("fail to close reader for :" + persistFile.getAbsoluteFile(), e);
				}
			}
			if (null != inputStream) {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					LOGGER.warn("fail to close stream for :" + persistFile.getAbsoluteFile(), e);
				}
			}
		}
	}

}
