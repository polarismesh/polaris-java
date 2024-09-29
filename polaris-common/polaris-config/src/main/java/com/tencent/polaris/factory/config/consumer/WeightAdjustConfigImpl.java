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

package com.tencent.polaris.factory.config.consumer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.consumer.WeightAdjustConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;

public class WeightAdjustConfigImpl extends PluginConfigImpl implements WeightAdjustConfig {

	@JsonProperty
	private Boolean enable;

	@JsonProperty
	private List<String> chain;

	@Override
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
	public List<String> getChain() {
		return chain;
	}

	public void setChain(List<String> chain) {
		this.chain = chain;
	}

	@Override
	public void verify() {
		ConfigUtils.validateNull(enable, "weightAdjust.enable");
		if (!enable) {
			return;
		}
		verifyPluginConfig();
	}

	@Override
	public void setDefault(Object defaultObject) {
		if (null != defaultObject) {
			WeightAdjustConfig defaultConfig = (WeightAdjustConfig) defaultObject;
            if (null == enable) {
                enable = defaultConfig.isEnable();
            }
            if (null == chain) {
                chain = defaultConfig.getChain();
            }
			if (enable) {
				setDefaultPluginConfig(defaultConfig);
			}
		}
	}

	@Override
	public String toString() {
		return "WeightAdjustConfigImpl{" +
				"enable=" + enable +
				", chain=" + chain +
				"} ";
	}
}
