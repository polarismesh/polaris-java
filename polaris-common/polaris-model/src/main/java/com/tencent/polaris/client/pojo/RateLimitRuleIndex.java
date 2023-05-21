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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;

import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;

public class RateLimitRuleIndex {

	// rules without exact labels
	private final List<RateLimitProto.Rule> fuzzyRules = new ArrayList<>();

	// first label key : sub index
	private final TreeMap<String, RateLimitRuleSubIndex> indexMap = new TreeMap<>();

	public RateLimitRuleIndex() {
	}

	public void addRule(RateLimitProto.Rule rule) {
		TreeMap<String, ModelProto.MatchString> exactLabelMap = new TreeMap<>();
		List<RateLimitProto.MatchArgument> arguments = rule.getArgumentsList();
		for (RateLimitProto.MatchArgument argument : arguments) {
			ModelProto.MatchString matchValue = argument.getValue();
			if (Objects.equals(matchValue.getType(), ModelProto.MatchString.MatchStringType.EXACT)) {
				exactLabelMap.put(RateLimitConsts.toLabelKey(argument.getType(), argument.getKey()), argument.getValue());
			}
		}
		if (exactLabelMap.isEmpty()) {
			fuzzyRules.add(rule);
			return;
		}
		addToSubIndex(rule, exactLabelMap);
	}

	private void addToSubIndex(RateLimitProto.Rule rule, TreeMap<String, ModelProto.MatchString> exactLabelMap) {
		Map.Entry<String, ModelProto.MatchString> first = exactLabelMap.firstEntry();
		final String labelKey = first.getKey();
		final String labelValue = first.getValue().getValue().getValue();
		indexMap.compute(labelKey, (k, v) -> {
			if (v == null) {
				v = new RateLimitRuleSubIndex();
			}
			exactLabelMap.remove(labelKey, first.getValue());
			v.addRule(rule, labelValue, exactLabelMap);
			return v;
		});
	}

	/**
	 * 检索第一条与标签字典匹配的规则
	 *
	 * @param arguments    标签字典
	 * @param method 规则匹配函数
	 * @param matcher
	 * @return 匹配的规则
	 */
	public RateLimitProto.Rule searchOne(
			Map<Integer, Map<String, String>> arguments, String method,
			RateLimitMatcher matcher) {
		if (MapUtils.isEmpty(arguments)) {
			// 请求未带标签时匹配模糊规则
			for (RateLimitProto.Rule rule : fuzzyRules) {
				if (matcher.match(rule, method, arguments)) {
					return rule;
				}
			}
			return null;
		}
		// 优先检索带有精确标签的规则
		for (Map.Entry<String, RateLimitRuleSubIndex> entry : indexMap.entrySet()) {
			RateLimitRuleSubIndex subIndex = entry.getValue();
			for (Map.Entry<Integer, Map<String, String>> argumentLabels : arguments.entrySet()) {
				Map<String, String> labelMap = argumentLabels.getValue();
				if (!labelMap.containsKey(entry.getKey())) {
					continue;
				}
				String labelValue = labelMap.get(entry.getKey());
				RateLimitProto.Rule rule = subIndex.searchOne(arguments, method, labelValue, matcher);
				if (Objects.nonNull(rule)) {
					return rule;
				}
			}
		}
		// 再顺序匹配模糊规则
		for (RateLimitProto.Rule rule : fuzzyRules) {
			if (matcher.match(rule, method, arguments)) {
				return rule;
			}
		}
		return null;
	}

	private static class RateLimitRuleSubIndex {
		// last label value : rule list
		private final HashMap<String, List<RateLimitProto.Rule>> rules = new HashMap<>();

		// label value : < next label key, sub index >
		private final TreeMap<String, TreeMap<String, RateLimitRuleSubIndex>> subIndexMap = new TreeMap<>();

		public RateLimitRuleSubIndex() {
		}

		public void addRule(
				RateLimitProto.Rule rule,
				String value,
				TreeMap<String, ModelProto.MatchString> exactLabelMap) {
			if (exactLabelMap.isEmpty()) {
				rules.compute(value, (k, v) -> {
					if (v == null) {
						v = new ArrayList<>();
					}
					v.add(rule);
					return v;
				});
				return;
			}
			subIndexMap.compute(value, (k, v) -> {
				if (v == null) {
					v = new TreeMap<>();
				}
				Map.Entry<String, ModelProto.MatchString> next = exactLabelMap.firstEntry();
				exactLabelMap.remove(next.getKey(), next.getValue());
				v.compute(next.getKey(), (k1, v1) -> {
					if (v1 == null) {
						v1 = new RateLimitRuleSubIndex();
					}
					v1.addRule(rule, next.getValue().getValue().getValue(), exactLabelMap);
					return v1;
				});
				return v;
			});
		}

		private final BiFunction<Map<Integer, Map<String, String>>, String, String> labelValueSearcher = (arguments, key) -> {
			for (Map.Entry<Integer, Map<String, String>> entry : arguments.entrySet()) {
				Map<String, String> labels = entry.getValue();
				if (labels.containsKey(key)) {
					return labels.get(key);
				}
			}
			return null;
		};

		public RateLimitProto.Rule searchOne(
				Map<Integer, Map<String, String>> arguments,
				String method,
				String labelValue,
				RateLimitMatcher matcher) {

			if (subIndexMap.containsKey(labelValue)) {
				for (Map.Entry<String, RateLimitRuleSubIndex> entry : subIndexMap.get(labelValue).entrySet()) {
					RateLimitRuleSubIndex subIndex = entry.getValue();
					String nextValue = labelValueSearcher.apply(arguments, entry.getKey());
					if (Objects.nonNull(nextValue)) {
						RateLimitProto.Rule rule = subIndex.searchOne(arguments, method, nextValue, matcher);
						if (rule != null) {
							return rule;
						}
					}
				}
			}
			List<RateLimitProto.Rule> ruleList = rules.get(labelValue);
			if (ruleList != null) {
				for (RateLimitProto.Rule rule : ruleList) {
					if (matcher.match(rule, method, arguments)) {
						return rule;
					}
				}
			}
			return null;
		}
	}

}
