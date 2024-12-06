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

package com.tencent.polaris.assembly.factory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.api.pojo.GetOneInstanceRequest;
import com.tencent.polaris.assembly.api.pojo.GetReachableInstancesRequest;
import com.tencent.polaris.assembly.api.pojo.TraceAttributes;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static com.tencent.polaris.test.common.Consts.ITERATE_COUNT;
import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;

public class AssemblyTest {

	private static final Logger LOG = LoggerFactory.getLogger(AssemblyTest.class);
	private static final Map<Operation, ValidParam> validParams = new HashMap<>();
	private static final String SERVICE_TEST_NORMAL = "java_test_normal";
	private static final String SERVICE_TEST_ABNORMAL = "java_test_abnormal";
	private static final String NOT_EXISTS_SERVICE = "java_test_not_exists";

	private NamingServer namingServer;

	static {
		validParams.put(Operation.ALL_HEALTHY,
				new ValidParam(SERVICE_TEST_NORMAL, 6, 6, 6));
		validParams.put(Operation.HAS_UNHEALTHY,
				new ValidParam(SERVICE_TEST_ABNORMAL, 10, 4, 8));
	}

	private enum Operation {
		ALL_HEALTHY, HAS_UNHEALTHY
	}

	private static class ValidParam {

		final String serviceName;

		final int countAll;

		final int countHealth;

		final int countHasWeight;

		public ValidParam(String serviceName, int countAll, int countHealth, int countHasWeight) {
			this.serviceName = serviceName;
			this.countAll = countAll;
			this.countHealth = countHealth;
			this.countHasWeight = countHasWeight;
		}

		public String getServiceName() {
			return serviceName;
		}

		public int getCountAll() {
			return countAll;
		}

		public int getCountHealth() {
			return countHealth;
		}

		public int getCountHasWeight() {
			return countHasWeight;
		}
	}

	@Before
	public void before() {
		try {
			namingServer = NamingServer.startNamingServer(-1);
			System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
		for (ValidParam validParam : validParams.values()) {
			NamingService.InstanceParameter instanceParameter = new NamingService.InstanceParameter();
			instanceParameter.setHealthy(true);
			instanceParameter.setIsolated(false);
			instanceParameter.setWeight(100);
			ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, validParam.getServiceName());
			List<Node> nodes = namingServer.getNamingService().batchAddInstances(serviceKey, 10000,
					validParam.getCountAll(), instanceParameter);
			if (validParam.getCountAll() > validParam.getCountHealth()) {
				int abnormalCount = validParam.getCountAll() - validParam.getCountHealth();
				int unhealthyCount = abnormalCount / 2;
				int isolatedCount = abnormalCount - unhealthyCount;
				for (int i = 0; i < unhealthyCount; i++) {
					namingServer.getNamingService().setInstanceHealthyStatus(
							serviceKey, nodes.get(i), false, null, null);
				}
				for (int i = 0; i < isolatedCount; i++) {
					namingServer.getNamingService().setInstanceHealthyStatus(
							serviceKey, nodes.get(nodes.size() - 1 - i), null, true, null);
				}
			}
			if (validParam.getCountAll() > validParam.getCountHasWeight()) {
				int weightZeroCount = validParam.getCountAll() - validParam.getCountHasWeight();
				for (int i = 0; i < weightZeroCount; i++) {
					namingServer.getNamingService().setInstanceHealthyStatus(
							serviceKey, nodes.get(i), null, null, 0);
				}
			}
		}
	}

	@After
	public void after() {
		if (null != namingServer) {
			namingServer.terminate();
		}
	}

	@Test
	public void testSyncGetOneInstanceNormal() {
		commonTestSyncGetOneInstance(Operation.ALL_HEALTHY);
	}

	@Test
	public void testSyncGetOneInstanceAbnormal() {
		commonTestSyncGetOneInstance(Operation.HAS_UNHEALTHY);
	}

	@Test
	public void testSyncGetReachableInstancesNormal() {
		commonTestSyncGetReachableInstances(Operation.ALL_HEALTHY);
	}

	@Test
	public void testSyncGetReachableInstancesAbnormal() {
		commonTestSyncGetReachableInstances(Operation.HAS_UNHEALTHY);
	}

	@Test
	public void testInitService() {
		ValidParam validParam = validParams.get(Operation.ALL_HEALTHY);
		Configuration configuration = TestUtils.configWithEnvAddress();
		try (AssemblyAPI assemblyAPI = AssemblyAPIFactory.createAssemblyAPIByConfig(configuration)) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				LOG.error("test fail: testInitService", e);
			}
			assemblyAPI.initService(new ServiceKey(NAMESPACE_TEST, validParam.getServiceName()));
		}
	}

	private void commonTestSyncGetOneInstance(Operation operation) {
		ValidParam validParam = validParams.get(operation);
		Configuration configuration = TestUtils.configWithEnvAddress();
		try (AssemblyAPI assemblyAPI = AssemblyAPIFactory.createAssemblyAPIByConfig(configuration)) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				LOG.error("test fail: commonTestSyncGetOneInstance", e);
			}
			for (int i = 0; i < ITERATE_COUNT; i++) {
				GetOneInstanceRequest request = new GetOneInstanceRequest();
				request.setNamespace(NAMESPACE_TEST);
				request.setService(validParam.getServiceName());

				Instance instance = assemblyAPI.getOneInstance(request);
				Assert.assertNotNull(instance);

				Assert.assertTrue(instance.isHealthy());
				Assert.assertFalse(instance.isIsolated());
				Assert.assertEquals(100, instance.getWeight());
			}
		}
	}

	private void commonTestSyncGetReachableInstances(Operation operation) {
		ValidParam validParam = validParams.get(operation);
		Configuration configuration = TestUtils.configWithEnvAddress();
		try (AssemblyAPI assemblyAPI = AssemblyAPIFactory.createAssemblyAPIByConfig(configuration)) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				LOG.error("test fail: commonTestSyncGetReachableInstances", e);
			}
			for (int i = 0; i < ITERATE_COUNT; i++) {
				GetReachableInstancesRequest request = new GetReachableInstancesRequest();
				request.setNamespace(NAMESPACE_TEST);
				request.setService(validParam.getServiceName());

				List<Instance> instances = assemblyAPI.getReachableInstances(request);
				Assert.assertEquals(validParam.getCountHealth(), instances.size());
			}
		}
	}

	@Test
	public void testTraceSpanAttributes() {
		Configuration configuration = TestUtils.configWithEnvAddress();
		try (AssemblyAPI assemblyAPI = AssemblyAPIFactory.createAssemblyAPIByConfig(configuration)) {
			try {
				Thread.sleep(3000);
			}
			catch (InterruptedException e) {
				LOG.error("test fail: testTraceSpanAttributes", e);
			}
			Map<String, String> values = new HashMap<>();
			values.put("testKey1", "testValue1");
			values.put("testKey2", "testValue2");
			TraceAttributes traceAttributes = new TraceAttributes();
			traceAttributes.setAttributeLocation(TraceAttributes.AttributeLocation.SPAN);
			traceAttributes.setAttributes(values);
			assemblyAPI.updateTraceAttributes(traceAttributes);
			traceAttributes.setAttributeLocation(TraceAttributes.AttributeLocation.BAGGAGE);
			assemblyAPI.updateTraceAttributes(traceAttributes);
		}
	}
}
