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

package com.tencent.polaris.plugins.circuitbreaker.composite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto;

public class FaultDetectRuleDictionary {

	// key is target service, value is list FaultDetectRule
	private final Map<ServiceKey, List<FaultDetectorProto.FaultDetectRule>> serviceRules = new ConcurrentHashMap<>();

	private final Object updateLock = new Object();

	public List<FaultDetectorProto.FaultDetectRule> lookupFaultDetectRule(Resource resource) {
		ServiceKey targetService = resource.getService();
		return serviceRules.get(targetService);
	}

	/**
	 * rule on server has been changed, clear all caches to make it pull again
	 * @param svcKey target service
	 */
	public void onFaultDetectRuleChanged(ServiceKey svcKey, FaultDetectorProto.FaultDetector faultDetector) {
		synchronized (updateLock) {
			Map<String, FaultDetectorProto.FaultDetectRule> changedRules = new HashMap<>();
			for (FaultDetectorProto.FaultDetectRule faultDetectRule : faultDetector.getRulesList()) {
				changedRules.put(faultDetectRule.getId(), faultDetectRule);
			}
			List<FaultDetectorProto.FaultDetectRule> faultDetectRules = serviceRules.get(svcKey);
			List<FaultDetectorProto.FaultDetectRule> rules = faultDetectRules == null ? Collections.emptyList() : faultDetectRules;
			List<FaultDetectorProto.FaultDetectRule> newRules = new ArrayList<>();
			for (FaultDetectorProto.FaultDetectRule rule : rules) {
				FaultDetectorProto.FaultDetectRule faultDetectRule = changedRules.get(rule.getId());
				if (null != faultDetectRule) {
					newRules.add(faultDetectRule);
				}
				else {
					newRules.add(rule);
				}
			}
			serviceRules.put(svcKey, newRules);
		}
	}

	void onFaultDetectRuleDeleted(ServiceKey svcKey, FaultDetectorProto.FaultDetector faultDetector) {
		synchronized (updateLock) {
			List<FaultDetectorProto.FaultDetectRule> faultDetectRules = serviceRules.get(svcKey);
			if (CollectionUtils.isEmpty(faultDetectRules)) {
				return;
			}
			Map<String, FaultDetectorProto.FaultDetectRule> changedRules = new HashMap<>();
			for (FaultDetectorProto.FaultDetectRule faultDetectRule : faultDetector.getRulesList()) {
				changedRules.put(faultDetectRule.getId(), faultDetectRule);
			}
			List<FaultDetectorProto.FaultDetectRule> newRules = new ArrayList<>();
			for (FaultDetectorProto.FaultDetectRule rule : faultDetectRules) {
				if (!changedRules.containsKey(rule.getId())) {
					newRules.add(rule);
				}
			}
			serviceRules.put(svcKey, newRules);
		}
	}

	public void putServiceRule(ServiceKey serviceKey, ServiceRule serviceRule) {
		if (null == serviceRule || null == serviceRule.getRule()) {
			synchronized (updateLock) {
				serviceRules.remove(serviceKey);
			}
			return;
		}
		FaultDetectorProto.FaultDetector faultDetector = (FaultDetectorProto.FaultDetector) serviceRule.getRule();
		List<FaultDetectorProto.FaultDetectRule> rules = faultDetector.getRulesList();
		serviceRules.put(serviceKey, rules);
	}
}
