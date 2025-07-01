/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.plugins.connector.nacos;

import org.junit.Assert;
import org.junit.Test;

public class NacosConnectorTests {

	@Test
	public void testNacosAnalyze() {
		String serviceName;

		serviceName = "GROUP__svc";
		Assert.assertEquals("svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("GROUP", NacosConnector.analyzeNacosGroup(serviceName));

		serviceName = "GROUP__svc_svc";
		Assert.assertEquals("svc_svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("GROUP", NacosConnector.analyzeNacosGroup(serviceName));

		serviceName = "GROUP_123__svc_svc";
		Assert.assertEquals("svc_svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("GROUP_123", NacosConnector.analyzeNacosGroup(serviceName));

		serviceName = "GROUP_123__svc__svc";
		Assert.assertEquals("svc__svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("GROUP_123", NacosConnector.analyzeNacosGroup(serviceName));

		serviceName = "__GROUP_123__svc__svc";
		Assert.assertEquals("GROUP_123__svc__svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("DEFAULT_GROUP", NacosConnector.analyzeNacosGroup(serviceName));
	}

}