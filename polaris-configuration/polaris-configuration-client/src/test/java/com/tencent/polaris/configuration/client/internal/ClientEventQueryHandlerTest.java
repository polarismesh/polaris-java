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

package com.tencent.polaris.configuration.client.internal;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueProvider;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKeyConflict;
import com.tencent.polaris.configuration.api.core.EffectiveValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ClientEventQueryHandler}.
 *
 * @author evelynwei
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientEventQueryHandlerTest {

    private static final Gson GSON = new Gson();

    private ConfigWatchReportRequestCustomizer watchRegistry;

    private ClientEventQueryHandler handler;

    @Before
    public void setUp() {
        watchRegistry = new ConfigWatchReportRequestCustomizer();
        handler = new ClientEventQueryHandler(watchRegistry);
    }

    private String pushJson(String namespace, String group, String fileName) {
        return "{\"kind\":\"config\",\"config\":{\"namespace\":\"" + namespace + "\",\"group\":\"" + group
                + "\",\"file_name\":\"" + fileName + "\"}}";
    }

    private JsonObject ackOf(String ackJson) {
        return JsonParser.parseString(ackJson).getAsJsonObject();
    }

    private RemoteConfigFileRepo registerWatched(String namespace, String group, String fileName,
            String content, long version, String md5, long effectiveTime) {
        RemoteConfigFileRepo repo = mock(RemoteConfigFileRepo.class);
        ConfigFileMetadata metadata = new DefaultConfigFileMetadata(namespace, group, fileName);
        when(repo.getConfigFileMetadata()).thenReturn(metadata);
        when(repo.getSnapshot()).thenReturn(new ConfigFileSnapshot(version, md5, content, effectiveTime));
        watchRegistry.register(repo);
        return repo;
    }

    /**
     * 测试目的：合法 config 查询且文件已监听时，返回 applied=true 且带 version/md5/content/effective_time。
     * 测试场景：注册监听文件后查询。
     * 验证内容：applied、version、md5、content、effective_time、无 reason、无 properties。
     */
    @Test
    public void testHandleConfigQuerySuccess() {
        registerWatched("ns", "g", "f.yaml", "server:\n  port: 8080", 12, "md5abc", 1785900000000L);

        JsonObject ack = ackOf(handler.onPush(1, pushJson("ns", "g", "f.yaml")));

        assertThat(ack.get("applied").getAsBoolean()).isTrue();
        assertThat(ack.get("kind").getAsString()).isEqualTo("config");
        assertThat(ack.get("version").getAsLong()).isEqualTo(12);
        assertThat(ack.get("md5").getAsString()).isEqualTo("md5abc");
        assertThat(ack.get("effective_time").getAsLong()).isEqualTo(1785900000000L);
        assertThat(ack.get("content").getAsString()).isEqualTo("server:\n  port: 8080");
        assertThat(ack.has("reason")).isFalse();
        assertThat(ack.has("properties")).isFalse();
    }

    /**
     * 测试目的：effective_time 为 0（从未成功拉取）时该字段省略，输出 null 而非 0。
     * 测试场景：监听文件但 effectiveTime=0。
     * 验证内容：effective_time 字段不存在。
     */
    @Test
    public void testEffectiveTimeOmittedWhenZero() {
        registerWatched("ns", "g", "f.yaml", "content", 1, "md5", 0);

        JsonObject ack = ackOf(handler.onPush(1, pushJson("ns", "g", "f.yaml")));

        assertThat(ack.get("applied").getAsBoolean()).isTrue();
        assertThat(ack.has("effective_time")).isFalse();
    }

    /**
     * 测试目的：非法 JSON 返回 bad_content 且 content 为空串（不省略）。
     * 测试场景：PUSH content 非 JSON。
     * 验证内容：applied=false、reason=bad_content、content 为空串。
     */
    @Test
    public void testBadContent() {
        JsonObject ack = ackOf(handler.onPush(1, "not-a-json"));

        assertThat(ack.get("applied").getAsBoolean()).isFalse();
        assertThat(ack.get("reason").getAsString()).isEqualTo("bad_content");
        assertThat(ack.get("content").getAsString()).isEmpty();
    }

    /**
     * 测试目的：kind 非 config 返回 unknown_kind 并回带原 kind。
     * 测试场景：kind=service。
     * 验证内容：applied=false、reason=unknown_kind、kind 回带。
     */
    @Test
    public void testUnknownKind() {
        JsonObject ack = ackOf(handler.onPush(1, "{\"kind\":\"service\",\"config\":{}}"));

        assertThat(ack.get("applied").getAsBoolean()).isFalse();
        assertThat(ack.get("reason").getAsString()).isEqualTo("unknown_kind");
        assertThat(ack.get("kind").getAsString()).isEqualTo("service");
    }

    /**
     * 测试目的：文件未监听返回 not_watched。
     * 测试场景：查询未注册的文件。
     * 验证内容：applied=false、reason=not_watched、回带 config 坐标。
     */
    @Test
    public void testNotWatched() {
        JsonObject ack = ackOf(handler.onPush(1, pushJson("ns", "g", "notwatch.yaml")));

        assertThat(ack.get("applied").getAsBoolean()).isFalse();
        assertThat(ack.get("reason").getAsString()).isEqualTo("not_watched");
        assertThat(ack.get("config").getAsJsonObject().get("file_name").getAsString()).isEqualTo("notwatch.yaml");
    }

    /**
     * 测试目的：watchRegistry 为 null（配置中心未启用）时返回 config_disabled。
     * 测试场景：构造 handler 传 null registry。
     * 验证内容：applied=false、reason=config_disabled。
     */
    @Test
    public void testConfigDisabled() {
        ClientEventQueryHandler disabledHandler = new ClientEventQueryHandler(null);

        JsonObject ack = ackOf(disabledHandler.onPush(1, pushJson("ns", "g", "f.yaml")));

        assertThat(ack.get("applied").getAsBoolean()).isFalse();
        assertThat(ack.get("reason").getAsString()).isEqualTo("config_disabled");
    }

    /**
     * 测试目的：注册 Provider 后 ACK 带 properties[]，含 file_value/effective_value/property_source/conflicts。
     * 测试场景：注入 mock Provider。
     * 验证内容：properties 数组元素各字段正确。
     */
    @Test
    public void testPropertiesWithProvider() {
        registerWatched("ns", "g", "f.yaml", "server.port=8080", 12, "md5abc", 1785900000000L);
        ConfigEffectiveValueProvider provider = mock(ConfigEffectiveValueProvider.class);
        when(provider.getKeys(any())).thenReturn(Collections.singletonList("server.port"));
        when(provider.resolve(any(String.class), any()))
                .thenReturn(new EffectiveValue("8080", "9090", "commandLineArgs"));
        ConfigKeyConflict conflict = new ConfigKeyConflict("ns", "common", "common.yaml", "8081");
        when(provider.resolveConflicts(any(String.class), any()))
                .thenReturn(Collections.singletonList(conflict));
        handler.registerProvider(provider);

        JsonObject ack = ackOf(handler.onPush(1, pushJson("ns", "g", "f.yaml")));

        assertThat(ack.get("applied").getAsBoolean()).isTrue();
        JsonObject prop = ack.getAsJsonArray("properties").get(0).getAsJsonObject();
        assertThat(prop.get("key").getAsString()).isEqualTo("server.port");
        assertThat(prop.get("file_value").getAsString()).isEqualTo("8080");
        assertThat(prop.get("effective_value").getAsString()).isEqualTo("9090");
        assertThat(prop.get("property_source").getAsString()).isEqualTo("commandLineArgs");
        JsonObject conflictJson = prop.getAsJsonArray("conflicts").get(0).getAsJsonObject();
        assertThat(conflictJson.get("file_name").getAsString()).isEqualTo("common.yaml");
        assertThat(conflictJson.get("value").getAsString()).isEqualTo("8081");
    }

    /**
     * 测试目的：Provider 单 key 解析抛异常时该 key 降级，整体仍 applied=true。
     * 测试场景：resolve 抛异常。
     * 验证内容：applied=true、properties 元素无 effective_value。
     */
    @Test
    public void testProviderResolveFailureDegradesSingleKey() {
        registerWatched("ns", "g", "f.yaml", "k=v", 1, "md5", 100L);
        ConfigEffectiveValueProvider provider = mock(ConfigEffectiveValueProvider.class);
        when(provider.getKeys(any())).thenReturn(Collections.singletonList("k"));
        when(provider.resolve(any(String.class), any())).thenThrow(new RuntimeException("boom"));
        when(provider.resolveConflicts(any(String.class), any())).thenReturn(Collections.emptyList());
        handler.registerProvider(provider);

        JsonObject ack = ackOf(handler.onPush(1, pushJson("ns", "g", "f.yaml")));

        assertThat(ack.get("applied").getAsBoolean()).isTrue();
        JsonObject prop = ack.getAsJsonArray("properties").get(0).getAsJsonObject();
        assertThat(prop.has("effective_value")).isFalse();
    }

    /**
     * 测试目的：超长 content 按字节截断并标记 content_truncated、content_length 为原始字节数。
     * 测试场景：构造超过 512KB 的内容。
     * 验证内容：content_truncated=true、content_length 为原始字节数、content 变短。
     */
    @Test
    public void testContentTruncated() {
        char[] chars = new char[600 * 1024];
        Arrays.fill(chars, 'a');
        String bigContent = new String(chars);
        registerWatched("ns", "g", "big.yaml", bigContent, 1, "md5", 100L);

        JsonObject ack = ackOf(handler.onPush(1, pushJson("ns", "g", "big.yaml")));

        assertThat(ack.get("applied").getAsBoolean()).isTrue();
        assertThat(ack.get("content_truncated").getAsBoolean()).isTrue();
        assertThat(ack.get("content_length").getAsInt()).isEqualTo(bigContent.getBytes().length);
        assertThat(ack.get("content").getAsString().length()).isLessThanOrEqualTo(512 * 1024);
    }

    /**
     * 测试目的：未超长 content 不输出 content_length 字段。
     * 测试场景：普通长度内容。
     * 验证内容：无 content_length、无 content_truncated。
     */
    @Test
    public void testContentLengthOmittedWhenNotTruncated() {
        registerWatched("ns", "g", "f.yaml", "short", 1, "md5", 100L);

        JsonObject ack = ackOf(handler.onPush(1, pushJson("ns", "g", "f.yaml")));

        assertThat(ack.has("content_length")).isFalse();
        assertThat(ack.has("content_truncated")).isFalse();
    }
}
