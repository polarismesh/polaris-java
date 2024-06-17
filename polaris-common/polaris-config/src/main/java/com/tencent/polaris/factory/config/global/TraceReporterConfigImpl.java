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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.global.TraceReporterConfig;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.util.ConfigUtils;

public class TraceReporterConfigImpl implements TraceReporterConfig {

	@JsonProperty
	private Boolean enable;

	@JsonProperty
	private String reporter;

	@Override
	public boolean isEnable() {
		if (null == enable) {
			return false;
		}
		return enable;
	}

	@Override
	public String getReporter() {
		return reporter;
	}

	void setEnable(Boolean enable) {
		this.enable = enable;
	}

	void setReporter(String reporter) {
		this.reporter = reporter;
	}

	public void verify() {
		ConfigUtils.validateNull(enable, "traceReporter.enable");
		if (isEnable()) {
			ConfigUtils.validateString(reporter, "traceReporter.reporter");
		}
	}

	@Override
	public void setDefault(Object defaultObject) {
		if (null != defaultObject) {
			TraceReporterConfig traceReporterConfig = (TraceReporterConfig) defaultObject;
			if (null == enable) {
				setEnable(traceReporterConfig.isEnable());
			}
			if (StringUtils.isBlank(reporter)) {
				setReporter(traceReporterConfig.getReporter());
			}
		}
	}

	@Override
	public String toString() {
		return "TraceReporterConfigImpl{" +
				"enable=" + enable +
				", reporter='" + reporter + '\'' +
				'}';
	}
}
