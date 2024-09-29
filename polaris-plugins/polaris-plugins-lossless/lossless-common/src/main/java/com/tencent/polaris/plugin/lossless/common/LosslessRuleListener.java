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

import com.tencent.polaris.api.plugin.registry.AbstractResourceEventListener;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;

/**
 * Resource event listener for lossless rule
 * @author Shedfree Wu
 */
public class LosslessRuleListener extends AbstractResourceEventListener {

	private final LosslessRuleDictionary losslessRuleDictionary;

	public LosslessRuleListener(LosslessRuleDictionary losslessRuleDictionary) {
		this.losslessRuleDictionary = losslessRuleDictionary;
	}

	@Override
	public void onResourceAdd(ServiceEventKey svcEventKey, RegistryCacheValue newValue) {
		if (svcEventKey.getEventType() != ServiceEventKey.EventType.LOSSLESS) {
			return;
		}

		ServiceRule serviceRule = (ServiceRule) newValue;
		if (null == serviceRule.getRule()) {
			losslessRuleDictionary.clearRules(svcEventKey.getServiceKey());
			return;
		}

		ResponseProto.DiscoverResponse discoverResponse = (ResponseProto.DiscoverResponse) serviceRule.getRule();
		losslessRuleDictionary.putMetadataWarmupRule(svcEventKey.getServiceKey(), discoverResponse.getLosslessRulesList());
	}

	@Override
	public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue, RegistryCacheValue newValue) {
		if (svcEventKey.getEventType() != ServiceEventKey.EventType.LOSSLESS) {
			return;
		}

		ServiceRule serviceRule = (ServiceRule) newValue;
		if (null == serviceRule.getRule()) {
			losslessRuleDictionary.clearRules(svcEventKey.getServiceKey());
			return;
		}

		ResponseProto.DiscoverResponse discoverResponse = (ResponseProto.DiscoverResponse) serviceRule.getRule();
		losslessRuleDictionary.putMetadataWarmupRule(svcEventKey.getServiceKey(), discoverResponse.getLosslessRulesList());
	}

	@Override
	public void onResourceDeleted(ServiceEventKey svcEventKey, RegistryCacheValue oldValue) {
		if (svcEventKey.getEventType() != ServiceEventKey.EventType.LOSSLESS) {
			return;
		}

		losslessRuleDictionary.clearRules(svcEventKey.getServiceKey());
	}
}
