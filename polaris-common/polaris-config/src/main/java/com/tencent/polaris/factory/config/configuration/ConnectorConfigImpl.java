package com.tencent.polaris.factory.config.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.configuration.ConnectorConfig;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

import static com.tencent.polaris.api.config.verify.DefaultValues.CONFIG_FILE_DEFAULT_CACHE_PERSIST_DIR;
import static com.tencent.polaris.api.config.verify.DefaultValues.LOCAL_FILE_CONNECTOR_TYPE;

/**
 * 配置中心连接器配置
 *
 * @author lepdou 2022-03-11
 */
public class ConnectorConfigImpl extends ServerConnectorConfigImpl implements ConnectorConfig {

	@JsonProperty
	private String connectorType;

	@JsonProperty
	private Boolean persistEnable = true;

	@JsonProperty
	private String persistDir;

	@JsonProperty
	private Integer persistMaxWriteRetry = 1;

	@JsonProperty
	private Integer persistMaxReadRetry = 0;

	@JsonProperty
	private Boolean fallbackToLocalCache = true;

	@JsonProperty
	@JsonDeserialize(using = TimeStrJsonDeserializer.class)
	private Long persistRetryInterval = 1000L;

	@Override
	public void verify() {
		ConfigUtils.validateString(connectorType, "configConnectorType");
		if (StringUtils.isBlank(persistDir)) {
			persistDir = CONFIG_FILE_DEFAULT_CACHE_PERSIST_DIR;
		}
		if (!LOCAL_FILE_CONNECTOR_TYPE.equals(connectorType)) {
			super.verify();
		}
	}

	@Override
	public void setDefault(Object defaultObject) {
		if (defaultObject == null) {
			return;
		}
		if (defaultObject instanceof ServerConnectorConfig) {
			ServerConnectorConfig serverConnectorConfig = (ServerConnectorConfig) defaultObject;
			super.setDefault(serverConnectorConfig);
		}
		if (defaultObject instanceof ConnectorConfig) {
			ConnectorConfig connectorConfig = (ConnectorConfig) defaultObject;
			if (connectorType == null) {
				this.connectorType = connectorConfig.getConnectorType();
			}
		}
	}

	@Override
	public String getConnectorType() {
		return connectorType;
	}

	public void setConnectorType(String connectorType) {
		this.connectorType = connectorType;
	}

	public Boolean getPersistEnable() {
		return persistEnable;
	}

	public void setPersistEnable(Boolean persistEnable) {
		this.persistEnable = persistEnable;
	}

	public String getPersistDir() {
		return persistDir;
	}

	public void setPersistDir(String persistDir) {
		this.persistDir = persistDir;
	}

	public Integer getPersistMaxWriteRetry() {
		return persistMaxWriteRetry;
	}

	public void setPersistMaxWriteRetry(Integer persistMaxWriteRetry) {
		this.persistMaxWriteRetry = persistMaxWriteRetry;
	}

	public Integer getPersistMaxReadRetry() {
		return persistMaxReadRetry;
	}

	public void setPersistMaxReadRetry(Integer persistMaxReadRetry) {
		this.persistMaxReadRetry = persistMaxReadRetry;
	}

	public Long getPersistRetryInterval() {
		return persistRetryInterval;
	}

	public void setPersistRetryInterval(Long persistRetryInterval) {
		this.persistRetryInterval = persistRetryInterval;
	}

	public Boolean getFallbackToLocalCache() {
		return fallbackToLocalCache;
	}

	public void setFallbackToLocalCache(Boolean fallbackToLocalCache) {
		this.fallbackToLocalCache = fallbackToLocalCache;
	}
}
