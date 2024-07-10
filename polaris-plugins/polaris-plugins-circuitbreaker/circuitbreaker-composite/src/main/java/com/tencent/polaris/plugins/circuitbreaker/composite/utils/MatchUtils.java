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

import java.util.function.Function;
import java.util.regex.Pattern;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;

public class MatchUtils {

	public static boolean matchService(ServiceKey serviceKey, String namespace, String service) {
		String inputNamespace = "";
		String inputService = "";
		if (null != serviceKey) {
			inputNamespace = serviceKey.getNamespace();
			inputService = serviceKey.getService();
		}
		if (StringUtils.isNotBlank(namespace) && !StringUtils.equals(namespace, RuleUtils.MATCH_ALL) && !StringUtils
				.equals(inputNamespace, namespace)) {
			return false;
		}
		if (StringUtils.isNotBlank(service) && !StringUtils.equals(service, RuleUtils.MATCH_ALL) && !StringUtils
				.equals(inputService, service)) {
			return false;
		}
		return true;
	}

	public static boolean matchMethod(Resource resource, MatchString matchString,
			Function<String, Pattern> regexToPattern) {
		if (resource.getLevel() != Level.METHOD) {
			return true;
		}
		String method = ((MethodResource) resource).getMethod();
		return RuleUtils.matchStringValue(matchString, method, regexToPattern);
	}

	public static boolean isWildcardMatcherSingle(String name) {
		return name.equals(RuleUtils.MATCH_ALL) || StringUtils.isBlank(name);
	}

	public static int compareSingleValue(String value1, String value2) {
		boolean serviceWildcard1 = isWildcardMatcherSingle(value1);
		boolean serviceWildcard2 = isWildcardMatcherSingle(value2);
		if (serviceWildcard1 && serviceWildcard2) {
			return 0;
		}
		if (serviceWildcard1) {
			// 1 before 2
			return 1;
		}
		if (serviceWildcard2) {
			// 1 before 2
			return -1;
		}
		return value1.compareTo(value2);
	}

	public static int compareService(String namespace1, String service1, String namespace2, String service2) {
		int nsResult = compareSingleValue(namespace1, namespace2);
		if (nsResult != 0) {
			return nsResult;
		}
		return compareSingleValue(service1, service2);
	}
}
