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

package com.tencent.polaris.plugin.location.cloud;

import java.io.IOException;
import java.lang.reflect.Field;

import com.tencent.polaris.api.plugin.location.LocationProvider;
import com.tencent.polaris.plugin.location.base.BaseLocationProvider;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CloudLocationProvider}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudLocationProviderTest {

    private static final String REGION = "huanan";

    private static final String ZONE = "ap-guangzhou";

    private static final String CAMPUS = "ap-guangzhou-6";

    private MockWebServer server;

    private CloudLocationProvider provider;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        provider = new CloudLocationProvider();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    /**
     * 测试 getProviderType 返回 CLOUD 类型
     * 测试目的：验证 getProviderType() 返回正确的枚举值
     * 测试场景：直接调用 getProviderType()
     * 验证内容：返回值为 LocationProvider.ProviderType.CLOUD
     */
    @Test
    public void testGetProviderType() {
        // Act
        LocationProvider.ProviderType type = provider.getProviderType();

        // Assert
        assertThat(type).isEqualTo(LocationProvider.ProviderType.CLOUD);
    }

    /**
     * 测试 doGet 三项地理信息均成功获取
     * 测试目的：验证 HTTP 请求均返回 200 时，doGet 能正确构建 Location 对象
     * 测试场景：mock 服务依次返回 region/zone/campus 的值
     * 验证内容：返回的 Location 包含正确的 region、zone、campus 字段
     */
    @Test
    public void testDoGet_AllSuccess() {
        // Arrange
        server.enqueue(new MockResponse().setResponseCode(200).setBody(REGION));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(ZONE));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(CAMPUS));
        String baseUrl = server.url("/").toString();
        BaseLocationProvider.GetOption option = buildOption(baseUrl, baseUrl, baseUrl);

        // Act
        ModelProto.Location location = provider.doGet(option);

        // Assert
        assertThat(location).isNotNull();
        assertThat(location.getRegion().getValue()).isEqualTo(REGION);
        assertThat(location.getZone().getValue()).isEqualTo(ZONE);
        assertThat(location.getCampus().getValue()).isEqualTo(CAMPUS);
    }

    /**
     * 测试 doGet 全部 URL 为空时返回 null
     * 测试目的：验证所有 URL 均为空时，getResponse 直接返回空字符串，doGet 最终返回 null
     * 测试场景：option 中 region/zone/campus URL 均置为空字符串
     * 验证内容：返回值为 null
     */
    @Test
    public void testDoGet_AllEmpty() {
        // Arrange
        BaseLocationProvider.GetOption option = buildOption("", "", "");

        // Act
        ModelProto.Location location = provider.doGet(option);

        // Assert
        assertThat(location).isNull();
    }

    /**
     * 测试 doGet region/campus URL 为空时字段降级为空字符串
     * 测试目的：验证 option 中 region URL 为空时（无默认 URL）region 字段为空字符串，
     *          campus URL 为空且默认 URL 不可达时 campus 字段为空字符串
     * 测试场景：option 仅设置 zone URL，region/campus 留空；mock server 返回 zone 值
     * 验证内容：zone 有值，region/campus 为空字符串，整体 Location 非 null
     */
    @Test
    public void testDoGet_RegionAndCampusEmpty_OnlyZoneReturned() {
        // Arrange
        server.enqueue(new MockResponse().setResponseCode(200).setBody(ZONE));
        String baseUrl = server.url("/").toString();
        BaseLocationProvider.GetOption option = buildOption("", baseUrl, "");

        // Act
        ModelProto.Location location = provider.doGet(option);

        // Assert
        assertThat(location).isNotNull();
        assertThat(location.getRegion().getValue()).isEqualTo("");
        assertThat(location.getZone().getValue()).isEqualTo(ZONE);
        assertThat(location.getCampus().getValue()).isEqualTo("");
    }

    /**
     * 测试 doGet HTTP 响应码非 200 时对应字段为空字符串
     * 测试目的：验证 HTTP 响应码非 200 时字段降级为空字符串，其他字段不受影响
     * 测试场景：zone 请求返回 500，region/campus 返回 200
     * 验证内容：zone 为空字符串，region 和 campus 有正确值
     */
    @Test
    public void testDoGet_NonOkResponseCode() {
        // Arrange
        server.enqueue(new MockResponse().setResponseCode(200).setBody(REGION));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(CAMPUS));
        String baseUrl = server.url("/").toString();
        BaseLocationProvider.GetOption option = buildOption(baseUrl, baseUrl, baseUrl);

        // Act
        ModelProto.Location location = provider.doGet(option);

        // Assert
        assertThat(location).isNotNull();
        assertThat(location.getRegion().getValue()).isEqualTo(REGION);
        assertThat(location.getZone().getValue()).isEqualTo("");
        assertThat(location.getCampus().getValue()).isEqualTo(CAMPUS);
    }

    /**
     * 测试 doGet 仅 campus 有效时仍返回 Location
     * 测试目的：验证只要有一个字段非空，doGet 就不返回 null
     * 测试场景：仅 campus URL 指向 mock server，region/zone URL 为空
     * 验证内容：返回非 null 的 Location，campus 有值
     */
    @Test
    public void testDoGet_OnlyCampusValid() {
        // Arrange
        server.enqueue(new MockResponse().setResponseCode(200).setBody(CAMPUS));
        String baseUrl = server.url("/").toString();
        BaseLocationProvider.GetOption option = buildOption("", "", baseUrl);

        // Act
        ModelProto.Location location = provider.doGet(option);

        // Assert
        assertThat(location).isNotNull();
        assertThat(location.getCampus().getValue()).isEqualTo(CAMPUS);
    }

    /**
     * 测试 doGet 所有 HTTP 请求均返回非 200 时返回 null
     * 测试目的：验证所有字段均获取失败（空字符串）时 doGet 返回 null
     * 测试场景：所有 URL 请求均返回 500
     * 验证内容：返回值为 null
     */
    @Test
    public void testDoGet_AllNon200_ReturnsNull() {
        // Arrange
        server.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
        String baseUrl = server.url("/").toString();
        BaseLocationProvider.GetOption option = buildOption(baseUrl, baseUrl, baseUrl);

        // Act
        ModelProto.Location location = provider.doGet(option);

        // Assert
        assertThat(location).isNull();
    }

    // ---- 辅助方法 ----

    private BaseLocationProvider.GetOption buildOption(String regionUrl, String zoneUrl, String campusUrl) {
        BaseLocationProvider.GetOption option = new BaseLocationProvider.GetOption();
        setField(option, "region", regionUrl);
        setField(option, "zone", zoneUrl);
        setField(option, "campus", campusUrl);
        return option;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
}
