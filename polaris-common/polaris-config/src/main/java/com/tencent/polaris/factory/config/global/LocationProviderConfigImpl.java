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

package com.tencent.polaris.factory.config.global;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.global.LocationProviderConfig;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class LocationProviderConfigImpl implements LocationProviderConfig {

	@JsonProperty
	private String type;

	@JsonProperty
	private Map<String, Object> options = new HashMap<>();

	String getType() {
		return type;
	}

	void setType(String type) {
		this.type = type;
	}

	void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	@Override
	public String getTye() {
		return type;
	}

	@Override
	public Map<String, Object> getOptions() {
		return options;
	}

	@Override
	public void verify() {
		ConfigUtils.validateNull(type, "location.provider.type");
	}

	@Override
	public void setDefault(Object defaultObject) {

	}

	@Override
	public String toString() {
		return "LocationProviderConfigImpl{" +
				"type='" + type + '\'' +
				", options=" + options +
				'}';
	}
}
