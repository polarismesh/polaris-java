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

package com.tencent.polaris.factory.config.provider;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.provider.RateLimitConfig;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;

public class RateLimitConfigImpl extends PluginConfigImpl implements RateLimitConfig {

	@JsonProperty
	private Boolean enable;

	@JsonProperty
	private String limiterService;

	@JsonProperty
	private String limiterNamespace;

	@JsonProperty
	private List<String> limiterAddresses;

	@JsonProperty
	private Integer maxWindowCount;

	@JsonProperty
	private Fallback fallbackOnExceedWindowCount;

	@JsonProperty
	private Long remoteSyncTimeoutMilli;

	@JsonProperty
	private Long maxQueuingTime;

	@JsonProperty
	private Boolean reportMetrics;

	public boolean isEnable() {
		if (null == enable) {
			return false;
		}
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	@Override
	public int getMaxWindowCount() {
		if (null == maxWindowCount) {
			return 0;
		}
		return maxWindowCount;
	}

	public void setMaxWindowCount(int maxWindowCount) {
		this.maxWindowCount = maxWindowCount;
	}

	@Override
	public Fallback getFallbackOnExceedWindowCount() {
		return fallbackOnExceedWindowCount;
	}

	public void setFallbackOnExceedWindowCount(
			Fallback fallbackOnExceedWindowCount) {
		this.fallbackOnExceedWindowCount = fallbackOnExceedWindowCount;
	}

	public String getLimiterService() {
		return limiterService;
	}

	public void setLimiterService(String limiterService) {
		this.limiterService = limiterService;
	}

	public String getLimiterNamespace() {
		return limiterNamespace;
	}

	public void setLimiterNamespace(String limiterNamespace) {
		this.limiterNamespace = limiterNamespace;
	}

	public List<String> getLimiterAddresses() {
		return limiterAddresses;
	}

	public void setLimiterAddresses(List<String> limiterAddresses) {
		this.limiterAddresses = limiterAddresses;
	}

	@Override
	public void verify() {
		ConfigUtils.validateNull(enable, "rateLimit.enable");
		if (!enable) {
			return;
		}
		ConfigUtils.validatePositive(maxWindowCount, "rateLimit.maxWindowCount");
		ConfigUtils.validateNull(fallbackOnExceedWindowCount, "rateLimit.fallbackOnExceedWindowCount");
		verifyPluginConfig();
	}

	@Override
	public long getRemoteSyncTimeoutMilli() {
		return remoteSyncTimeoutMilli;
	}

	public void setRemoteSyncTimeoutMilli(long remoteSyncTimeoutMilli) {
		this.remoteSyncTimeoutMilli = remoteSyncTimeoutMilli;
	}

	@Override
	public long getMaxQueuingTime() {
		return maxQueuingTime;
	}

	public void setMaxQueuingTime(Long maxQueuingTime) {
		this.maxQueuingTime = maxQueuingTime;
	}

	@Override
	public boolean isReportMetrics() {
		if (null == reportMetrics) {
			return false;
		}
		return reportMetrics;
	}

	public void setReportMetrics(boolean reportMetrics) {
		this.reportMetrics = reportMetrics;
	}

	@Override
	public void setDefault(Object defaultObject) {
		if (null != defaultObject) {
			RateLimitConfig rateLimitConfig = (RateLimitConfig) defaultObject;
			if (null == enable) {
				setEnable(rateLimitConfig.isEnable());
			}
			if (null == maxWindowCount) {
				setMaxWindowCount(rateLimitConfig.getMaxWindowCount());
			}
			if (null == fallbackOnExceedWindowCount) {
				setFallbackOnExceedWindowCount(rateLimitConfig.getFallbackOnExceedWindowCount());
			}
			if (null == remoteSyncTimeoutMilli) {
				setRemoteSyncTimeoutMilli(rateLimitConfig.getRemoteSyncTimeoutMilli());
			}
			if (null == fallbackOnExceedWindowCount) {
				setFallbackOnExceedWindowCount(rateLimitConfig.getFallbackOnExceedWindowCount());
			}
			if (null == limiterNamespace) {
				setLimiterNamespace(rateLimitConfig.getLimiterNamespace());
			}
			if (null == limiterService) {
				setLimiterService(rateLimitConfig.getLimiterService());
			}
			if (null == maxQueuingTime) {
				setMaxQueuingTime(rateLimitConfig.getMaxQueuingTime());
			}
            if (null == reportMetrics) {
                setReportMetrics(rateLimitConfig.isReportMetrics());
            }
			setDefaultPluginConfig(rateLimitConfig);
		}
	}
}
