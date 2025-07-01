/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.global.LocationConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class LocationConfigImpl implements LocationConfig {

	@JsonProperty
	private List<LocationProviderConfigImpl> providers = new ArrayList<>();

	@Override
	public List<LocationProviderConfigImpl> getProviders() {
		return providers;
	}

	void setProviders(List<LocationProviderConfigImpl> providers) {
		this.providers = providers;
	}

	@Override
	public void verify() {

	}

	public LocationProviderConfigImpl getByType(String type) {
		for (LocationProviderConfigImpl config : providers) {
			if (StringUtils.equals(config.getType(), type)) {
				return config;
			}
		}

		return null;
	}

	@Override
	public void setDefault(Object defaultObject) {
		if (null != defaultObject) {
			LocationConfig locationConfig = (LocationConfig) defaultObject;
			if (CollectionUtils.isEmpty(providers)) {
				setProviders((List<LocationProviderConfigImpl>) locationConfig.getProviders());
			}
		}
	}

	@Override
	public String toString() {
		return "LocationConfigImpl{" +
				"providers=" + providers +
				'}';
	}
}
