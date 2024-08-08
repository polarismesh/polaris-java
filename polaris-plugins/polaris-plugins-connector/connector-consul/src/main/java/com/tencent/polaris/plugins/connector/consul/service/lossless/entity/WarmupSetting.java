/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.plugins.connector.consul.service.lossless.entity;

/**
 * Warmup setting entity.
 * @author Shedfree Wu
 */
public class WarmupSetting {

	public WarmupSetting() {
	}

	public WarmupSetting(boolean enabled, int warmupTime, int curvature, boolean enabledProtection) {
		this.enabled = enabled;
		this.warmupTime = warmupTime;
		this.curvature = curvature;
		this.enabledProtection = enabledProtection;
	}

	// 是否开启预热
	private boolean enabled = false;

	// 预热窗口时间，单位 秒
	private int warmupTime;

	// 预热曲率，1~5
	private int curvature;

	// 是否开启预热保护，开启预热保护的情况下，预热中节点超过 50%，会中止余下节点预热过程。
	private boolean enabledProtection = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getWarmupTime() {
		return warmupTime;
	}

	public void setWarmupTime(int warmupTime) {
		this.warmupTime = warmupTime;
	}

	public int getCurvature() {
		return curvature;
	}

	public void setCurvature(int curvature) {
		this.curvature = curvature;
	}

	public boolean isEnabledProtection() {
		return enabledProtection;
	}

	public void setEnabledProtection(boolean enabledProtection) {
		this.enabledProtection = enabledProtection;
	}

	@Override
	public String toString() {
		return "WarmupSetting{" +
				"enabled=" + enabled +
				", warmupTime=" + warmupTime +
				", curvature=" + curvature +
				", enabledProtection=" + enabledProtection +
				'}';
	}
}