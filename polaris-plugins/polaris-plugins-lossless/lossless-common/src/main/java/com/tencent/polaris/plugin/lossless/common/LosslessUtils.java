/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.pojo.DefaultServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.Instance;
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
 * Lossless utils for getting lossless rules.
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

	public static Map<String, Map<String, LosslessProto.LosslessRule>> parseMetadataLosslessRules(
			List<LosslessProto.LosslessRule> losslessRuleList) {

		Map<String, Map<String, LosslessProto.LosslessRule>> metadataLosslessRules = new HashMap<>();
		for (LosslessProto.LosslessRule losslessRule : losslessRuleList) {
			if (CollectionUtils.isNotEmpty(losslessRule.getMetadataMap())) {
				for (Map.Entry<String, String> labelEntry : losslessRule.getMetadataMap().entrySet()) {
					metadataLosslessRules.putIfAbsent(labelEntry.getKey(), new HashMap<>());
					metadataLosslessRules.get(labelEntry.getKey()).put(labelEntry.getValue(), losslessRule);
				}
			}
		}
		return metadataLosslessRules;
	}

	/**
	 * if metadata lossless rule is not empty, return the match lossless rule.
	 * if metadata lossless rule is empty, return the first lossless rule.
	 */
	public static LosslessProto.LosslessRule getMatchLosslessRule(BaseInstance baseInstance,
			Map<String, Map<String, LosslessProto.LosslessRule>> metadataLosslessRules,
			List<LosslessProto.LosslessRule> allLosslessRules) {

		if (CollectionUtils.isEmpty(allLosslessRules)) {
			return null;
		}

		if (needMetadataLosslessRule(metadataLosslessRules)) {
			return getMatchMetadataLosslessRule(baseInstance, metadataLosslessRules);
		} else {
			return allLosslessRules.get(0);
		}
	}

	public static LosslessProto.LosslessRule getMatchLosslessRule(Extensions extensions, BaseInstance baseInstance) {
		List<LosslessProto.LosslessRule> allLosslessRules = getLosslessRules(
				extensions, baseInstance.getNamespace(), baseInstance.getService());

		Map<String, Map<String, LosslessProto.LosslessRule>> metadataLosslessRules =
				parseMetadataLosslessRules(allLosslessRules);

		return getMatchLosslessRule(baseInstance, metadataLosslessRules, allLosslessRules);
	}


	public static LosslessProto.LosslessRule getMatchMetadataLosslessRule(BaseInstance baseInstance,
			Map<String, Map<String, LosslessProto.LosslessRule>> metadataLosslessRules) {
		if (!(baseInstance instanceof Instance)) {
			return null;
		}

		Instance instance = (Instance) baseInstance;

		if (CollectionUtils.isEmpty(instance.getMetadata())) {
			return null;
		}

		for (Map.Entry<String, Map<String, LosslessProto.LosslessRule>> metatadaEntry :
				metadataLosslessRules.entrySet()) {
			String instanceMatchValue = instance.getMetadata().get(metatadaEntry.getKey());
			if (metatadaEntry.getValue().containsKey(instanceMatchValue)) {
				return metatadaEntry.getValue().get(instanceMatchValue);
			}
		}
		// not match
		return null;
	}

	private static boolean needMetadataLosslessRule(
			Map<String, Map<String, LosslessProto.LosslessRule>> metadataLosslessRules) {
		return CollectionUtils.isNotEmpty(metadataLosslessRules);
	}
}
