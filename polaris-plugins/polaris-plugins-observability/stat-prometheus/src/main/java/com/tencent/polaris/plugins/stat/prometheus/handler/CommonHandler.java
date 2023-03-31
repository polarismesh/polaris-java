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

package com.tencent.polaris.plugins.stat.prometheus.handler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.tencent.polaris.api.plugin.stat.CircuitBreakGauge;
import com.tencent.polaris.api.plugin.stat.RateLimitGauge;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.plugins.stat.common.model.AbstractSignatureStatInfoCollector;
import com.tencent.polaris.plugins.stat.common.model.StatMetric;
import com.tencent.polaris.plugins.stat.common.model.StatRevisionMetric;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricLabelOrder;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricName;
import io.prometheus.client.Gauge;

import static com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricValue.NULL_VALUE;

public class CommonHandler {

	public static final int DEFAULT_INTERVAL_MILLI = 30 * 1000;
	public static final int REVISION_MAX_SCOPE = 2;

	public static final String PUSH_GROUP_KEY = "instance";

	public static final String PUSH_DEFAULT_JOB_NAME = "polaris-client";

	public static void putDataFromContainerInOrder(Map<String, Gauge> sampleMapping,
			AbstractSignatureStatInfoCollector<?, ? extends StatMetric> collector,
			long currentRevision,
			String[] order) {
		Collection<? extends StatMetric> values = collector.getCollectedValues();

		for (StatMetric s : values) {
			Gauge gauge = sampleMapping.get(s.getMetricName());
			if (null != gauge) {
				if (s instanceof StatRevisionMetric) {
					StatRevisionMetric rs = (StatRevisionMetric) s;
					if (rs.getRevision() < currentRevision - REVISION_MAX_SCOPE) {
						// 如果连续两个版本还没有数据，就清除该数据
						gauge.remove(CommonHandler.getOrderedMetricLabelValues(s.getLabels(), order));
						collector.getMetricContainer().remove(s.getSignature());
						continue;
					}
					else if (rs.getRevision() < currentRevision) {
						// 如果版本为老版本，则清零数据
						gauge.remove(CommonHandler.getOrderedMetricLabelValues(s.getLabels(), order));
						Gauge.Child child = gauge.labels(CommonHandler.getOrderedMetricLabelValues(s.getLabels(), order));
						if (null != child) {
							child.set(0);
						}
						continue;
					}
				}

				Gauge.Child child = gauge.labels(CommonHandler.getOrderedMetricLabelValues(s.getLabels(), order));
				if (null != child) {
					child.set(s.getValue());
				}
			}
		}
	}

	public static String[] getOrderedMetricLabelValues(Map<String, String> labels, String[] orderedKey) {
		String[] orderValue = new String[orderedKey.length];
		for (int i = 0; i < orderedKey.length; i++) {
			orderValue[i] = labels.getOrDefault(orderedKey[i], NULL_VALUE);
		}
		return orderValue;
	}

	public static Map<String, String> convertInsGaugeToLabels(InstanceGauge insGauge, String sdkIP) {
		Map<String, String> labels = new HashMap<>();
		for (String labelName : SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER) {
			switch (labelName) {
			case SystemMetricName.CALLEE_NAMESPACE:
				addLabel(labelName, insGauge.getNamespace(), labels);
				break;
			case SystemMetricModel.SystemMetricName.CALLEE_SERVICE:
				addLabel(labelName, insGauge.getService(), labels);
				break;
			case SystemMetricName.CALLEE_METHOD:
				addLabel(labelName, insGauge.getMethod(), labels);
				break;
			case SystemMetricName.CALLEE_SUBSET:
				addLabel(labelName, insGauge.getSubset(), labels);
				break;
			case SystemMetricModel.SystemMetricName.CALLEE_INSTANCE:
				addLabel(labelName, buildAddress(insGauge.getHost(), insGauge.getPort()), labels);
				break;
			case SystemMetricName.CALLEE_RET_CODE:
				String retCodeStr =
						null == insGauge.getRetCode() ? null : insGauge.getRetCode().toString();
				addLabel(labelName, retCodeStr, labels);
				break;
			case SystemMetricName.CALLER_LABELS:
				addLabel(labelName, insGauge.getLabels(), labels);
				break;
			case SystemMetricName.CALLER_NAMESPACE:
				String namespace =
						null == insGauge.getCallerService() ? null : insGauge.getCallerService().getNamespace();
				addLabel(labelName, namespace, labels);
				break;
			case SystemMetricName.CALLER_SERVICE:
				String serviceName =
						null == insGauge.getCallerService() ? null : insGauge.getCallerService().getService();
				addLabel(labelName, serviceName, labels);
				break;
			case SystemMetricName.CALLEE_RESULT:
				String retStatusStr =
						null == insGauge.getRetStatus() ? null : insGauge.getRetStatus().getDesc();
				addLabel(labelName, retStatusStr, labels);
				break;
			case SystemMetricName.CALLER_IP:
				String callerIp = Objects.isNull(insGauge.getCallerIp()) ? sdkIP : insGauge.getCallerIp();
				addLabel(labelName, callerIp, labels);
				break;
			case SystemMetricName.RULE_NAME:
				String ruleName = Objects.isNull(insGauge.getRuleName()) ? null : insGauge.getRuleName();
				addLabel(labelName, ruleName, labels);
				break;
			default:
			}
		}

		return labels;
	}

	public static Map<String, String> convertRateLimitGaugeToLabels(RateLimitGauge rateLimitGauge) {
		Map<String, String> labels = new HashMap<>();
		for (String labelName : SystemMetricModel.SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER) {
			switch (labelName) {
			case SystemMetricName.CALLEE_NAMESPACE:
				addLabel(labelName, rateLimitGauge.getNamespace(), labels);
				break;
			case SystemMetricName.CALLEE_SERVICE:
				addLabel(labelName, rateLimitGauge.getService(), labels);
				break;
			case SystemMetricName.CALLEE_METHOD:
				addLabel(labelName, rateLimitGauge.getMethod(), labels);
				break;
			case SystemMetricName.CALLER_LABELS:
				addLabel(labelName, rateLimitGauge.getLabels(), labels);
				break;
			case SystemMetricName.RULE_NAME:
				addLabel(labelName, rateLimitGauge.getRuleName(), labels);
				break;
			default:
			}
		}

		return labels;
	}

	public static Map<String, String> convertCircuitBreakToLabels(CircuitBreakGauge gauge, String callerIp) {
		Map<String, String> labels = new HashMap<>();
		for (String labelName : SystemMetricModel.SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER) {
			switch (labelName) {
			case SystemMetricName.CALLEE_NAMESPACE:
				addLabel(labelName, gauge.getNamespace(), labels);
				break;
			case SystemMetricName.CALLEE_SERVICE:
				addLabel(labelName, gauge.getService(), labels);
				break;
			case SystemMetricName.CALLEE_METHOD:
				addLabel(labelName, gauge.getMethod(), labels);
				break;
			case SystemMetricName.CALLEE_SUBSET:
				addLabel(labelName, gauge.getSubset(), labels);
				break;
			case SystemMetricName.CALLEE_INSTANCE:
				addLabel(labelName, buildAddress(gauge.getHost(), gauge.getPort()), labels);
				break;
			case SystemMetricName.CALLER_NAMESPACE:
				String namespace =
						null == gauge.getCallerService() ? null : gauge.getCallerService().getNamespace();
				addLabel(labelName, namespace, labels);
				break;
			case SystemMetricName.CALLER_SERVICE:
				String serviceName =
						null == gauge.getCallerService() ? null : gauge.getCallerService().getService();
				addLabel(labelName, serviceName, labels);
				break;
			case SystemMetricName.CALLER_IP:
				addLabel(labelName, callerIp, labels);
				break;
			case SystemMetricName.RULE_NAME:
				addLabel(labelName, gauge.getRuleName(), labels);
				break;
			case SystemMetricName.LEVEL:
				addLabel(labelName, gauge.getLevel(), labels);
				break;
			default:
			}
		}

		return labels;
	}

	public static void addLabel(String key, String value, Map<String, String> target) {
		if (null == key) {
			return;
		}

		if (null == value) {
			value = NULL_VALUE;
		}

		target.put(key, value);
	}

	public static String buildAddress(String host, int port) {
		if (null == host) {
			host = "";
		}

		return host + ":" + port;
	}

}
