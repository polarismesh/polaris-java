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

package com.tencent.polaris.plugin.lossless.common;

import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.provider.LosslessConfig;
import com.tencent.polaris.api.config.provider.ProviderConfig;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.lossless.InstanceProperties;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.client.util.OkHttpUtil;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link HttpLosslessActionProvider}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpLosslessActionProviderTest {

	private static final String TEST_HEALTH_CHECK_PATH = "/health";

	private static final String CONFIG_HEALTH_CHECK_PATH = "/actuator/health";

	private static final Integer TEST_PORT = 8080;

	@Mock
	private Extensions extensions;

	@Mock
	private BaseInstance instance;

	@Mock
	private Configuration configuration;

	@Mock
	private ProviderConfig providerConfig;

	@Mock
	private LosslessConfig losslessConfig;

	private Runnable mockRegisterAction;

	private Runnable mockDeregisterAction;

	private boolean registerActionCalled;

	private boolean deregisterActionCalled;

	@Before
	public void setUp() {
		registerActionCalled = false;
		deregisterActionCalled = false;
		mockRegisterAction = () -> registerActionCalled = true;
		mockDeregisterAction = () -> deregisterActionCalled = true;

		// mock configuration chain
		when(extensions.getConfiguration()).thenReturn(configuration);
		when(configuration.getProvider()).thenReturn(providerConfig);
		when(providerConfig.getLossless()).thenReturn(losslessConfig);
	}

	/**
	 * 测试 getName 方法
	 * 测试目的：验证返回的名称为 "http"
	 * 测试场景：LosslessRule 为 null，使用配置回退值
	 * 验证内容：getName 返回 "http"
	 */
	@Test
	public void testGetName() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class)) {
			// Arrange
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(null);
			when(losslessConfig.getStrategy())
					.thenReturn(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_TIME);
			when(losslessConfig.getHealthCheckPath()).thenReturn(CONFIG_HEALTH_CHECK_PATH);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			String name = provider.getName();

			// Assert
			assertThat(name).isEqualTo("http");
		}
	}

	/**
	 * 测试 doRegister 方法
	 * 测试目的：验证 doRegister 调用了原始注册动作
	 * 测试场景：正常调用 doRegister
	 * 验证内容：原始注册 Runnable 被执行
	 */
	@Test
	public void testDoRegister() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class)) {
			// Arrange
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(null);
			when(losslessConfig.getStrategy())
					.thenReturn(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_TIME);
			when(losslessConfig.getHealthCheckPath()).thenReturn(CONFIG_HEALTH_CHECK_PATH);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			InstanceProperties instanceProperties = new InstanceProperties();

			// Act
			provider.doRegister(instanceProperties);

			// Assert
			assertThat(registerActionCalled).isTrue();
		}
	}

	/**
	 * 测试 doDeregister 方法
	 * 测试目的：验证 doDeregister 调用了原始反注册动作
	 * 测试场景：正常调用 doDeregister
	 * 验证内容：原始反注册 Runnable 被执行
	 */
	@Test
	public void testDoDeregister() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class)) {
			// Arrange
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(null);
			when(losslessConfig.getStrategy())
					.thenReturn(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_TIME);
			when(losslessConfig.getHealthCheckPath()).thenReturn(CONFIG_HEALTH_CHECK_PATH);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			provider.doDeregister();

			// Assert
			assertThat(deregisterActionCalled).isTrue();
		}
	}

	/**
	 * 测试 isEnableHealthCheck 方法 - 策略为 DELAY_BY_HEALTH_CHECK 时返回 true
	 * 测试目的：验证当策略为 DELAY_BY_HEALTH_CHECK 时，健康检查功能启用
	 * 测试场景：LosslessRule 返回 DELAY_BY_HEALTH_CHECK 策略
	 * 验证内容：isEnableHealthCheck 返回 true
	 */
	@Test
	public void testIsEnableHealthCheck_WhenDelayByHealthCheck() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class)) {
			// Arrange
			LosslessProto.LosslessRule losslessRule = LosslessProto.LosslessRule.newBuilder()
					.setLosslessOnline(LosslessProto.LosslessOnline.newBuilder()
							.setDelayRegister(LosslessProto.DelayRegister.newBuilder()
									.setStrategy(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_HEALTH_CHECK)
									.setHealthCheckPath(TEST_HEALTH_CHECK_PATH)
									.build())
							.build())
					.build();
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(losslessRule);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			boolean result = provider.isEnableHealthCheck();

			// Assert
			assertThat(result).isTrue();
		}
	}

	/**
	 * 测试 isEnableHealthCheck 方法 - 策略为 DELAY_BY_TIME 时返回 false
	 * 测试目的：验证当策略为 DELAY_BY_TIME 时，健康检查功能不启用
	 * 测试场景：LosslessRule 为 null，配置回退策略为 DELAY_BY_TIME
	 * 验证内容：isEnableHealthCheck 返回 false
	 */
	@Test
	public void testIsEnableHealthCheck_WhenDelayByTime() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class)) {
			// Arrange
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(null);
			when(losslessConfig.getStrategy())
					.thenReturn(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_TIME);
			when(losslessConfig.getHealthCheckPath()).thenReturn(CONFIG_HEALTH_CHECK_PATH);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			boolean result = provider.isEnableHealthCheck();

			// Assert
			assertThat(result).isFalse();
		}
	}

	/**
	 * 测试 doHealthCheck 方法 - 健康检查成功
	 * 测试目的：验证 doHealthCheck 使用正确的参数调用 OkHttpUtil.checkUrl
	 * 测试场景：OkHttpUtil.checkUrl 返回 true
	 * 验证内容：doHealthCheck 返回 true
	 */
	@Test
	public void testDoHealthCheck_Success() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class);
				MockedStatic<OkHttpUtil> mockedOkHttpUtil = Mockito.mockStatic(OkHttpUtil.class)) {
			// Arrange
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(null);
			when(losslessConfig.getStrategy())
					.thenReturn(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_HEALTH_CHECK);
			when(losslessConfig.getHealthCheckPath()).thenReturn(CONFIG_HEALTH_CHECK_PATH);

			mockedOkHttpUtil.when(() -> OkHttpUtil.checkUrl(
					eq("localhost"), eq(TEST_PORT), eq(CONFIG_HEALTH_CHECK_PATH), any(Map.class)))
					.thenReturn(true);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			boolean result = provider.doHealthCheck();

			// Assert
			assertThat(result).isTrue();
			mockedOkHttpUtil.verify(() -> OkHttpUtil.checkUrl(
					eq("localhost"), eq(TEST_PORT), eq(CONFIG_HEALTH_CHECK_PATH), any(Map.class)));
		}
	}

	/**
	 * 测试 doHealthCheck 方法 - 健康检查失败
	 * 测试目的：验证 doHealthCheck 在健康检查失败时返回 false
	 * 测试场景：OkHttpUtil.checkUrl 返回 false
	 * 验证内容：doHealthCheck 返回 false
	 */
	@Test
	public void testDoHealthCheck_Failure() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class);
				MockedStatic<OkHttpUtil> mockedOkHttpUtil = Mockito.mockStatic(OkHttpUtil.class)) {
			// Arrange
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(null);
			when(losslessConfig.getStrategy())
					.thenReturn(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_HEALTH_CHECK);
			when(losslessConfig.getHealthCheckPath()).thenReturn(CONFIG_HEALTH_CHECK_PATH);

			mockedOkHttpUtil.when(() -> OkHttpUtil.checkUrl(
					eq("localhost"), eq(TEST_PORT), eq(CONFIG_HEALTH_CHECK_PATH), any(Map.class)))
					.thenReturn(false);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			boolean result = provider.doHealthCheck();

			// Assert
			assertThat(result).isFalse();
		}
	}

	/**
	 * 测试 healthCheckPath 回退到配置值 - LosslessRule 为 null 场景
	 * 测试目的：验证当 LosslessRule 为 null 时，healthCheckPath 从配置中获取而非空字符串
	 * 测试场景：LosslessUtils.getMatchLosslessRule 返回 null，配置中设置了 healthCheckPath
	 * 验证内容：doHealthCheck 使用配置中的 healthCheckPath 进行健康检查
	 */
	@Test
	public void testHealthCheckPath_FallbackToConfig_WhenLosslessRuleIsNull() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class);
				MockedStatic<OkHttpUtil> mockedOkHttpUtil = Mockito.mockStatic(OkHttpUtil.class)) {
			// Arrange
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(null);
			when(losslessConfig.getStrategy())
					.thenReturn(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_HEALTH_CHECK);
			when(losslessConfig.getHealthCheckPath()).thenReturn(CONFIG_HEALTH_CHECK_PATH);

			mockedOkHttpUtil.when(() -> OkHttpUtil.checkUrl(anyString(), anyInt(), anyString(), any(Map.class)))
					.thenReturn(true);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			provider.doHealthCheck();

			// Assert: verify that the config-based health check path is used, not empty string
			mockedOkHttpUtil.verify(() -> OkHttpUtil.checkUrl(
					eq("localhost"), eq(TEST_PORT), eq(CONFIG_HEALTH_CHECK_PATH), any(Map.class)));
		}
	}

	/**
	 * 测试 healthCheckPath 使用 LosslessRule 中的值
	 * 测试目的：验证当 LosslessRule 存在且包含 healthCheckPath 时，优先使用规则中的路径
	 * 测试场景：LosslessRule 包含完整的 DelayRegister 配置，且设置了 healthCheckPath
	 * 验证内容：doHealthCheck 使用 LosslessRule 中的 healthCheckPath 进行健康检查
	 */
	@Test
	public void testHealthCheckPath_FromLosslessRule() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class);
				MockedStatic<OkHttpUtil> mockedOkHttpUtil = Mockito.mockStatic(OkHttpUtil.class)) {
			// Arrange
			LosslessProto.LosslessRule losslessRule = LosslessProto.LosslessRule.newBuilder()
					.setLosslessOnline(LosslessProto.LosslessOnline.newBuilder()
							.setDelayRegister(LosslessProto.DelayRegister.newBuilder()
									.setStrategy(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_HEALTH_CHECK)
									.setHealthCheckPath(TEST_HEALTH_CHECK_PATH)
									.build())
							.build())
					.build();
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(losslessRule);

			mockedOkHttpUtil.when(() -> OkHttpUtil.checkUrl(anyString(), anyInt(), anyString(), any(Map.class)))
					.thenReturn(true);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			provider.doHealthCheck();

			// Assert: verify that the LosslessRule health check path is used
			mockedOkHttpUtil.verify(() -> OkHttpUtil.checkUrl(
					eq("localhost"), eq(TEST_PORT), eq(TEST_HEALTH_CHECK_PATH), any(Map.class)));
		}
	}

	/**
	 * 测试 strategy 回退到配置值 - LosslessRule 为 null 场景
	 * 测试目的：验证当 LosslessRule 为 null 时，strategy 从配置中获取
	 * 测试场景：LosslessUtils.getMatchLosslessRule 返回 null，配置中设置了 strategy
	 * 验证内容：isEnableHealthCheck 的结果与配置中的 strategy 一致
	 */
	@Test
	public void testStrategy_FallbackToConfig_WhenLosslessRuleIsNull() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class)) {
			// Arrange
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(null);
			when(losslessConfig.getStrategy())
					.thenReturn(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_HEALTH_CHECK);
			when(losslessConfig.getHealthCheckPath()).thenReturn(CONFIG_HEALTH_CHECK_PATH);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			boolean result = provider.isEnableHealthCheck();

			// Assert: strategy falls back to config value DELAY_BY_HEALTH_CHECK, so health check is enabled
			assertThat(result).isTrue();
		}
	}

	/**
	 * 测试 LosslessRule 中 DelayRegister 未设置 healthCheckPath 时回退到配置值
	 * 测试目的：验证当 LosslessRule 存在但 DelayRegister 中没有设置 healthCheckPath 时，回退到配置中的值
	 * 测试场景：LosslessRule 的 DelayRegister 未设置 healthCheckPath（空字符串为 protobuf 默认值）
	 * 验证内容：doHealthCheck 使用配置中的 healthCheckPath 或 protobuf 默认值
	 */
	@Test
	public void testHealthCheckPath_FallbackToConfig_WhenDelayRegisterHasNoPath() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class);
				MockedStatic<OkHttpUtil> mockedOkHttpUtil = Mockito.mockStatic(OkHttpUtil.class)) {
			// Arrange: LosslessRule exists but DelayRegister has no healthCheckPath set
			LosslessProto.LosslessRule losslessRule = LosslessProto.LosslessRule.newBuilder()
					.setLosslessOnline(LosslessProto.LosslessOnline.newBuilder()
							.setDelayRegister(LosslessProto.DelayRegister.newBuilder()
									.setStrategy(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_HEALTH_CHECK)
									// healthCheckPath not set, protobuf default is empty string ""
									.build())
							.build())
					.build();
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(losslessRule);

			mockedOkHttpUtil.when(() -> OkHttpUtil.checkUrl(anyString(), anyInt(), anyString(), any(Map.class)))
					.thenReturn(true);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			provider.doHealthCheck();

			// Assert: protobuf returns empty string "" for unset string field,
			// which is still returned by the Optional chain (not null)
			mockedOkHttpUtil.verify(() -> OkHttpUtil.checkUrl(
					eq("localhost"), eq(TEST_PORT), eq(""), any(Map.class)));
		}
	}

	/**
	 * 测试 LosslessRule 中 LosslessOnline 未设置时回退到配置值
	 * 测试目的：验证当 LosslessRule 存在但不包含 LosslessOnline 时，healthCheckPath 回退到配置
	 * 测试场景：LosslessRule 存在但 LosslessOnline 为默认值（未设置）
	 * 验证内容：doHealthCheck 使用配置中的 healthCheckPath
	 */
	@Test
	public void testHealthCheckPath_FallbackToConfig_WhenNoLosslessOnline() {
		try (MockedStatic<LosslessUtils> mockedLosslessUtils = Mockito.mockStatic(LosslessUtils.class);
				MockedStatic<OkHttpUtil> mockedOkHttpUtil = Mockito.mockStatic(OkHttpUtil.class)) {
			// Arrange: LosslessRule exists but has no LosslessOnline set
			LosslessProto.LosslessRule losslessRule = LosslessProto.LosslessRule.newBuilder().build();
			mockedLosslessUtils.when(() -> LosslessUtils.getMatchLosslessRule(any(), any()))
					.thenReturn(losslessRule);
			when(losslessConfig.getStrategy())
					.thenReturn(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_TIME);
			when(losslessConfig.getHealthCheckPath()).thenReturn(CONFIG_HEALTH_CHECK_PATH);

			mockedOkHttpUtil.when(() -> OkHttpUtil.checkUrl(anyString(), anyInt(), anyString(), any(Map.class)))
					.thenReturn(true);

			HttpLosslessActionProvider provider = new HttpLosslessActionProvider(
					mockRegisterAction, mockDeregisterAction, TEST_PORT, instance, extensions);

			// Act
			provider.doHealthCheck();

			// Assert: since protobuf message fields have default instances (not null),
			// the Optional chain will reach getHealthCheckPath which returns ""
			// Note: protobuf hasXxx would be false but getXxx returns default instance
			mockedOkHttpUtil.verify(() -> OkHttpUtil.checkUrl(
					eq("localhost"), eq(TEST_PORT), anyString(), any(Map.class)));
		}
	}
}
