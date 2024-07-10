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

package com.tencent.polaris.plugins.circuitbreaker.composite.utils;

import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;

public class CircuitBreakerUtils {

	public static boolean checkRule(CircuitBreakerProto.CircuitBreakerRule rule) {
		return checkLevel(rule.getLevel());
	}

	public static boolean checkLevel(CircuitBreakerProto.Level level) {
		return level == CircuitBreakerProto.Level.SERVICE
				|| level == CircuitBreakerProto.Level.METHOD
				|| level == CircuitBreakerProto.Level.INSTANCE;
	}

	public static long getSleepWindowMilli(CircuitBreakerProto.CircuitBreakerRule currentActiveRule,
			CircuitBreakerConfig circuitBreakerConfig) {
		long sleepWindow = currentActiveRule.getRecoverCondition().getSleepWindow() * 1000L;
		if (sleepWindow == 0) {
			sleepWindow = circuitBreakerConfig.getSleepWindow();
		}
		return sleepWindow;
	}

	public static long getErrorRateIntervalSec(CircuitBreakerProto.TriggerCondition triggerCondition,
			CircuitBreakerConfig circuitBreakerConfig) {
		long interval = triggerCondition.getInterval();
		if (interval == 0) {
			interval = circuitBreakerConfig.getErrorRateInterval() / 1000;
		}
		return interval;
	}

}
