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

package com.tencent.polaris.router.api.rpc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.SourceService;
import org.junit.Assert;
import org.junit.Test;

public class ProcessRoutersRequestTests {

	@Test
	public void testGetRouterArguments() {
		ProcessRoutersRequest request = new ProcessRoutersRequest();
		request.getRouterArguments();
		request.getRouterArguments("ruleRouter");
		request.getRouterMetadata();
		request.getRouterMetadata("ruleRouter");
	}

	@Test
	public void testSetSourceService() {
		ProcessRoutersRequest request = new ProcessRoutersRequest();
		ServiceInfo serviceInfo = new ServiceInfo();

		Map<String, String> metadata = new HashMap<>();
		metadata.put("$header.uid", "1234");
		metadata.put("uid", "123");
		serviceInfo.setMetadata(metadata);
		request.setSourceService(serviceInfo);

		SourceService sourceService = (SourceService) request.getSourceService();
		Assert.assertEquals(2, sourceService.getArguments().size());
		Assert.assertEquals(2, request.getRouterArguments("ruleRouter").size());
		Assert.assertEquals(1, request.getRouterMetadata().size());
		Assert.assertEquals(2, request.getRouterMetadata("ruleRouter").size());
	}

	@Test
	public void testSetSourceService2() {
		ProcessRoutersRequest request = new ProcessRoutersRequest();
		SourceService serviceInfo = new SourceService();
		serviceInfo.appendArguments(RouteArgument.fromLabel("$header.uid", "123"));
		serviceInfo.setArguments(new HashSet<>(Arrays.asList(
				RouteArgument.fromLabel("uid", "123")
		)));

		request.setSourceService(serviceInfo);

		SourceService sourceService = (SourceService) request.getSourceService();
		Assert.assertEquals(2, sourceService.getArguments().size());
		Assert.assertEquals(2, request.getRouterArguments("ruleRouter").size());
		Assert.assertEquals(1, request.getRouterMetadata().size());
		Assert.assertEquals(2, request.getRouterMetadata("ruleRouter").size());
	}

	@Test
	public void testSetSourceService3() {
		ProcessRoutersRequest request = new ProcessRoutersRequest();
		SourceService serviceInfo = new SourceService();
		serviceInfo.appendArguments(RouteArgument.fromLabel("$header.uid", "123"));
		serviceInfo.setArguments(new HashSet<>(Arrays.asList(
				RouteArgument.fromLabel("uid", "123")
		)));

		request.setSourceService(serviceInfo);

		SourceService sourceService = (SourceService) request.getSourceService();
		Assert.assertEquals(2, sourceService.getArguments().size());
		Assert.assertEquals(2, request.getRouterArguments("ruleRouter").size());
		Assert.assertEquals(1, request.getRouterMetadata().size());
		Assert.assertEquals(2, request.getRouterMetadata("ruleRouter").size());

		((SourceService) request.getSourceService()).appendArguments(RouteArgument.fromLabel("$header.env", "prod"));
		Assert.assertEquals(3, sourceService.getArguments().size());
		Assert.assertEquals(3, request.getRouterArguments("ruleRouter").size());
		Assert.assertEquals(1, request.getRouterMetadata().size());
		Assert.assertEquals(3, request.getRouterMetadata("ruleRouter").size());
	}
}