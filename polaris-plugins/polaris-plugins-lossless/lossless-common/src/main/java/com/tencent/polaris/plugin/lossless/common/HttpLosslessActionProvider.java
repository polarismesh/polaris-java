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

package com.tencent.polaris.plugin.lossless.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.lossless.InstanceProperties;
import com.tencent.polaris.api.plugin.lossless.LosslessActionProvider;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.OkHttpUtil;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;

/**
 * LosslessActionProvider for Http.
 *
 * @author Shedfree Wu
 */
public class HttpLosslessActionProvider implements LosslessActionProvider {

	private final Runnable originalRegisterAction;

	private final Runnable originalDeregisterAction;

	private final BaseInstance instance;

	private final Integer port;

	private final Extensions extensions;

	private final String healthCheckPath;

	private final LosslessProto.DelayRegister.DelayStrategy strategy;

	public HttpLosslessActionProvider(Runnable originalRegisterAction, Runnable originalDeregisterAction,
			Integer port, BaseInstance instance, Extensions extensions) {
		this.originalRegisterAction = originalRegisterAction;
		this.originalDeregisterAction = originalDeregisterAction;
		this.port = port;
		this.instance = instance;

		this.extensions = extensions;
		this.strategy = getStrategy();
		this.healthCheckPath = getHealthCheckPath();
	}

	@Override
	public String getName() {
		return "http";
	}

	@Override
	public void doRegister(InstanceProperties instanceProperties) {
		// use lambda to do original register
		originalRegisterAction.run();
	}

	@Override
	public void doDeregister() {
		// use lambda to do original deregister
        originalDeregisterAction.run();
	}

	/**
	 * Check whether health check is enable.
	 * @return true: register after passing doHealthCheck, false: register after delayRegisterInterval.
	 */
	@Override
	public boolean isEnableHealthCheck() {
		return StringUtils.isNotBlank(healthCheckPath);
	}

	@Override
	public boolean doHealthCheck() {
		Map<String, String> headers = new HashMap<>(1);
		headers.put("User-Agent", "polaris");

		return OkHttpUtil.checkUrl("localhost", port, healthCheckPath, headers);
	}

	private LosslessProto.DelayRegister.DelayStrategy getStrategy() {
		LosslessProto.LosslessRule losslessRule = LosslessUtils.getFirstLosslessRule(extensions,
				instance.getNamespace(), instance.getService());
		return Optional.ofNullable(losslessRule).
				map(LosslessProto.LosslessRule::getLosslessOnline).
				map(LosslessProto.LosslessOnline::getDelayRegister).
				map(LosslessProto.DelayRegister::getStrategy).
				orElse(extensions.getConfiguration().getProvider().getLossless().getStrategy());
	}

	private String getHealthCheckPath() {
		LosslessProto.LosslessRule losslessRule = LosslessUtils.getFirstLosslessRule(extensions,
				instance.getNamespace(), instance.getService());
		return Optional.ofNullable(losslessRule).
				map(LosslessProto.LosslessRule::getLosslessOnline).
				map(LosslessProto.LosslessOnline::getDelayRegister).
				map(LosslessProto.DelayRegister::getHealthCheckPath).
				orElse("");
	}
}
