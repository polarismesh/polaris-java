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

package com.tencent.polaris.client.pojo;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;

public class RateLimitRuleContainer {

	// all rules in this level
	private final TreeSet<RateLimitProto.Rule> rules;

	// priority : index
	private final TreeMap<Integer, RateLimitRuleIndex> ruleIndexMap = new TreeMap<>();

	private static final int CREATE_RULE_INDEX_THRESHOLD = 20;

	/**
	 * Constructs a <tt>RateLimitRuleContainer</tt> by a rule list.
	 */
	public RateLimitRuleContainer(List<RateLimitProto.Rule> ruleList) {
		rules = new TreeSet<>((o1, o2) -> {
			if (o1.getPriority() != o2.getPriority()) {
				return Integer.compare(o1.getPriority().getValue(), o2.getPriority().getValue());
			}
			return o1.getId().getValue().compareTo(o2.getId().getValue());
		});
		if (ruleList == null || ruleList.isEmpty()) {
			return;
		}
		this.rules.addAll(ruleList);
		if (rules.size() > CREATE_RULE_INDEX_THRESHOLD) {
			for (RateLimitProto.Rule rule : rules) {
				ruleIndexMap.compute(rule.getPriority().getValue(), (k, v) -> {
					if (v == null) {
						v = new RateLimitRuleIndex();
					}
					v.addRule(rule);
					return v;
				});
			}
		}
	}

	public SortedSet<RateLimitProto.Rule> getRules() {
		return Collections.unmodifiableSortedSet(rules);
	}

	public SortedMap<Integer, RateLimitRuleIndex> getRuleIndexMap() {
		return Collections.unmodifiableSortedMap(ruleIndexMap);
	}

}
