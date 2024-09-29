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
import java.util.List;
import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;

/**
 * Cache for mapping between metadata and warmup rule.
 * @author Shedfree Wu
 */
public class LosslessRuleDictionary {

	// cache for mapping between metadata and warmup rule. {metadataKey: {metadataValue: warmupRule}}
	private final Cache<ServiceKey, Map<String, Map<String, LosslessProto.Warmup>>> metadataWarmupRuleCache =
			CacheBuilder.newBuilder().build();

	private final Object updateLock = new Object();

	public Map<String, Map<String, LosslessProto.Warmup>> getMetadataWarmupRule(ServiceKey serviceKey) {
		synchronized (updateLock) {
			return metadataWarmupRuleCache.getIfPresent(serviceKey);
		}
	}

	public void clearRules(ServiceKey serviceKey) {
		synchronized (updateLock) {
			metadataWarmupRuleCache.invalidate(serviceKey);
		}
	}

	public void putMetadataWarmupRule(ServiceKey serviceKey, List<LosslessProto.LosslessRule> losslessRuleList)  {
		synchronized (updateLock) {
			if (CollectionUtils.isEmpty(losslessRuleList)) {
				metadataWarmupRuleCache.invalidate(serviceKey);
                return;
			}

			Map<String, Map<String, LosslessProto.Warmup>> metadataWarmupRule = new HashMap<>();
			for (LosslessProto.LosslessRule losslessRule : losslessRuleList) {
				if (CollectionUtils.isNotEmpty(losslessRule.getMetadataMap())) {
					for (Map.Entry<String, String> labelEntry : losslessRule.getMetadataMap().entrySet()) {
						metadataWarmupRule.putIfAbsent(labelEntry.getKey(), new HashMap<>());
						metadataWarmupRule.get(labelEntry.getKey())
								.put(labelEntry.getValue(), losslessRule.getLosslessOnline()
										.getWarmup());
					}
				}
			}
            metadataWarmupRuleCache.put(serviceKey, metadataWarmupRule);
        }
	}
}
