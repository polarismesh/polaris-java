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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.polaris.api.pojo.InstanceWeight;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;

/**
 * Cache for lossless.
 * @author Shedfree Wu
 */
public class LosslessRuleDictionary {

	// cache for mapping between metadata and LosslessRule. {metadataKey: {metadataValue: LosslessRule}}
	private final Cache<ServiceKey, Map<String, Map<String, LosslessProto.LosslessRule>>> metadataLosslessRuleCache =
			CacheBuilder.newBuilder().build();

	private final Cache<ServiceKey, Map<String, InstanceWeight>> instanceWeightCache = CacheBuilder.newBuilder().
			maximumSize(500).expireAfterWrite(5, TimeUnit.SECONDS).build();


	public Map<String, Map<String, LosslessProto.LosslessRule>> getMetadataLosslessRules(ServiceKey serviceKey) {
		return metadataLosslessRuleCache.getIfPresent(serviceKey);
	}

	public Map<String, InstanceWeight> getInstanceWeight(ServiceKey serviceKey) {
		return instanceWeightCache.getIfPresent(serviceKey);
	}

	public void clearRules(ServiceKey serviceKey) {
		metadataLosslessRuleCache.invalidate(serviceKey);
		instanceWeightCache.invalidate(serviceKey);
	}

	public void putMetadataLosslessRules(ServiceKey serviceKey, List<LosslessProto.LosslessRule> losslessRuleList)  {
		if (CollectionUtils.isEmpty(losslessRuleList)) {
			metadataLosslessRuleCache.invalidate(serviceKey);
			return;
		}
		Map<String, Map<String, LosslessProto.LosslessRule>> metadataLosslessRules =
				LosslessUtils.parseMetadataLosslessRules(losslessRuleList);
		metadataLosslessRuleCache.put(serviceKey, metadataLosslessRules);
	}

	public void putInstanceWeight(ServiceKey serviceKey, Map<String, InstanceWeight> instanceWeight) {
		instanceWeightCache.put(serviceKey, instanceWeight);
	}
}
