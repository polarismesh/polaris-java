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

package com.tencent.polaris.plugins.circuitbraker.composite.trigger;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.ServiceResource;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.plugins.circuitbreaker.composite.HealthCheckInstanceProvider;
import com.tencent.polaris.plugins.circuitbreaker.composite.PolarisCircuitBreaker;
import com.tencent.polaris.plugins.circuitbreaker.composite.ResourceHealthChecker;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.DestinationService;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.Protocol;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import org.junit.Assert;
import org.junit.Test;

public class ResourceHealthCheckerTest {

	@Test
	public void testSelectFdRule() {
		// match one service rules
		FaultDetectRule.Builder builderDefaultSvc1 = FaultDetectRule.newBuilder();
		builderDefaultSvc1.setId("test_cb_default_svc1");
		builderDefaultSvc1.setName("test_cb_default_svc1");
		builderDefaultSvc1.setTargetService(
				FaultDetectRule.DestinationService.newBuilder().setNamespace("default").setService("svc1").build());
		builderDefaultSvc1.setProtocol(Protocol.HTTP);

		FaultDetectRule.Builder builderDefaultSvcFoo1 = FaultDetectRule.newBuilder();
		builderDefaultSvcFoo1.setId("test_cb_default_svc_foo1");
		builderDefaultSvcFoo1.setName("test_cb_default_svc_foo1");
		builderDefaultSvcFoo1.setTargetService(
				DestinationService.newBuilder().setNamespace("default").setService("svc1").setMethod(
								MatchString.newBuilder().setValue(StringValue.newBuilder().setValue("foo1").build()).build())
						.build());
		builderDefaultSvcFoo1.setProtocol(Protocol.TCP);

		FaultDetectRule.Builder builderAllNsAllSvc = FaultDetectRule.newBuilder();
		builderAllNsAllSvc.setName("test_cb_all_ns_all_svc");
		builderAllNsAllSvc.setTargetService(
				FaultDetectRule.DestinationService.newBuilder().setNamespace("*").setService("*").build());
		builderAllNsAllSvc.setProtocol(Protocol.HTTP);

		HealthCheckInstanceProvider healthCheckInstanceProvider = () -> null;

		ResourceHealthChecker resourceHealthCheckerDefaultSvc1 = new ResourceHealthChecker(builderDefaultSvc1.build(),
				healthCheckInstanceProvider, new PolarisCircuitBreaker());
		ResourceHealthChecker resourceHealthCheckerDefaultSvcFoo1 = new ResourceHealthChecker(builderDefaultSvcFoo1.build(),
				healthCheckInstanceProvider, new PolarisCircuitBreaker());
		ResourceHealthChecker resourceHealthCheckerAllNsAllSvc = new ResourceHealthChecker(builderAllNsAllSvc.build(),
				healthCheckInstanceProvider, new PolarisCircuitBreaker());

		ServiceResource svcResource = new ServiceResource(new ServiceKey("default", "svc1"));
		Assert.assertTrue(resourceHealthCheckerDefaultSvc1.matchResource(svcResource));
		Assert.assertFalse(resourceHealthCheckerDefaultSvcFoo1.matchResource(svcResource));
		Assert.assertTrue(resourceHealthCheckerAllNsAllSvc.matchResource(svcResource));
	}

}
