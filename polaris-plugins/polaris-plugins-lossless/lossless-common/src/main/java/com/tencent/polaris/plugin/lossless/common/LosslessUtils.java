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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.DefaultServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;

/**
 * Lossless utils for getting lossless rules
 * @author Shedfree Wu
 */
public class LosslessUtils {

	private LosslessUtils() {

	}

	private static ServiceRule getServiceRule(Extensions extensions, String dstNamespace, String dstServiceName) {
		DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();
		BaseFlow.buildFlowControlParam(new RequestBaseEntity(), extensions.getConfiguration(), engineFlowControlParam);
		Set<ServiceEventKey> losslessKeys = new HashSet<>();
		ServiceEventKey dstSvcEventKey = new ServiceEventKey(new ServiceKey(dstNamespace, dstServiceName),
				ServiceEventKey.EventType.LOSSLESS);
		losslessKeys.add(dstSvcEventKey);
		DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
		svcKeysProvider.setSvcEventKeys(losslessKeys);
		ResourcesResponse resourcesResponse = BaseFlow
				.syncGetResources(extensions, false, svcKeysProvider, engineFlowControlParam);
		return resourcesResponse.getServiceRule(dstSvcEventKey);
	}

	public static List<LosslessProto.LosslessRule> getLosslessRules(Extensions extensions,
			String dstNamespace, String dstServiceName) {
		ServiceRule serviceRule = getServiceRule(extensions, dstNamespace, dstServiceName);
		if (serviceRule == null || serviceRule.getRule() == null) {
			return Collections.emptyList();
		}
		ResponseProto.DiscoverResponse discoverResponse = (ResponseProto.DiscoverResponse) serviceRule.getRule();
		return discoverResponse.getLosslessRulesList();
	}

	public static LosslessProto.LosslessRule getFirstLosslessRule(Extensions extensions, String dstNamespace, String dstServiceName) {
		List<LosslessProto.LosslessRule> losslessRules = getLosslessRules(extensions, dstNamespace, dstServiceName);
		if (CollectionUtils.isEmpty(losslessRules)) {
			return null;
		}
		return losslessRules.get(0);
	}
}
