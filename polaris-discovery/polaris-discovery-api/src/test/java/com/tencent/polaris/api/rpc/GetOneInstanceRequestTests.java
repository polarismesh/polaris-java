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

package com.tencent.polaris.api.rpc;

import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.SourceService;
import junit.framework.TestCase;

public class GetOneInstanceRequestTests extends TestCase {

	public void testSetServiceInfo_ServiceInfo() {
		GetOneInstanceRequest request = new GetOneInstanceRequest();
		ServiceInfo serviceInfo = new ServiceInfo();

		Map<String, String> metadata = new HashMap<>();

		RouteArgument argument = RouteArgument.buildHeader("uid", "uid");
		argument.toLabel(metadata);
		serviceInfo.setMetadata(metadata);

		request.setServiceInfo(serviceInfo);

		ServiceInfo ret = request.getServiceInfo();
		Map<String, String> retMeta = ret.getMetadata();

		assertEquals(metadata, retMeta);
	}

	public void testSetServiceInfo_SourceInfo() {
		GetOneInstanceRequest request = new GetOneInstanceRequest();
		SourceService serviceInfo = new SourceService();
		RouteArgument argument = RouteArgument.buildHeader("uid", "uid");
		serviceInfo.appendArguments(argument);

		request.setServiceInfo(serviceInfo);
		ServiceInfo ret = request.getServiceInfo();

		assertEquals(serviceInfo.getLabels(), ret.getMetadata());
		assertEquals(serviceInfo.getMetadata(), ret.getMetadata());

		Map<String, String> map = new HashMap<>();
		argument.toLabel(map);
		assertEquals(serviceInfo.getLabels(), map);
		assertEquals(serviceInfo.getMetadata(), map);
	}

}