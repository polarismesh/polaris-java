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

import com.google.gson.JsonParser;
import com.tencent.polaris.api.plugin.server.ReportClientRequest;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ConfigWatchReportRequestCustomizer}.
 *
 * @author fishtailfu
 */
public class ConfigWatchReportRequestCustomizerTest {

    private ConfigWatchReportRequestCustomizer customizer;

    @Before
    public void setUp() {
        customizer = new ConfigWatchReportRequestCustomizer();
    }

    private RemoteConfigFileRepo mockRepo(String namespace, String group, String fileName,
                                          long version, String md5) {
        RemoteConfigFileRepo repo = mock(RemoteConfigFileRepo.class);
        ConfigFileMetadata metadata = new DefaultConfigFileMetadata(namespace, group, fileName);
        when(repo.getConfigFileMetadata()).thenReturn(metadata);
        when(repo.getSnapshot()).thenReturn(new ConfigFileSnapshot(version, md5));
        return repo;
    }

    @Test
    public void testGetName() {
        assertThat(customizer.getName()).isEqualTo("config-watch");
    }

    @Test
    public void testGetType() {
        assertThat(customizer.getType()).isNotNull();
    }

    @Test
    public void testGetPluginConfigClazz() {
        assertThat(customizer.getPluginConfigClazz()).isEqualTo(ConfigWatchReportRequestCustomizerConfig.class);
    }

    @Test
    public void testCustomizeDisabled() {
        ReportClientRequest request = new ReportClientRequest();
        customizer.register(mockRepo("ns", "g", "f", 1, "abc"));
        customizer.customize(request);

        assertThat(request.getConfigEnabled()).isNull();
        assertThat(request.getConfigMetadata()).isNull();
    }

    @Test
    public void testCustomizeEnabledEmptyList() throws Exception {
        enableCustomizer();
        ReportClientRequest request = new ReportClientRequest();
        customizer.customize(request);

        assertThat(request.getConfigEnabled()).isTrue();
        assertThat(request.getConfigMetadata()).isNotNull();
        assertThat(JsonParser.parseString(request.getConfigMetadata())
                .getAsJsonObject().getAsJsonArray("config_watch")).isEmpty();
    }

    @Test
    public void testCustomizeSingleFile() throws Exception {
        enableCustomizer();
        customizer.register(mockRepo("default", "scg-test", "application.yaml", 3,
                "e10adc3949ba59abbe56e057f20f883e"));

        ReportClientRequest request = new ReportClientRequest();
        customizer.customize(request);

        assertThat(request.getConfigEnabled()).isTrue();
        String json = request.getConfigMetadata();
        assertThat(json).contains("default");
        assertThat(json).contains("scg-test");
        assertThat(json).contains("application.yaml");
        assertThat(json).contains("\"version\":3");
        assertThat(json).contains("e10adc3949ba59abbe56e057f20f883e");
    }

    @Test
    public void testCustomizeMultipleFiles() throws Exception {
        enableCustomizer();
        customizer.register(mockRepo("default", "g1", "f1", 1, "md5_1"));
        customizer.register(mockRepo("default", "g2", "f2", 2, "md5_2"));

        ReportClientRequest request = new ReportClientRequest();
        customizer.customize(request);

        assertThat(request.getConfigEnabled()).isTrue();
        int count = JsonParser.parseString(request.getConfigMetadata())
                .getAsJsonObject().getAsJsonArray("config_watch").size();
        assertThat(count).isEqualTo(2);
    }

    @Test
    public void testCustomizeNullMd5OutputEmptyString() throws Exception {
        enableCustomizer();
        RemoteConfigFileRepo repo = mockRepo("ns", "g", "f", 0, null);
        customizer.register(repo);

        ReportClientRequest request = new ReportClientRequest();
        customizer.customize(request);

        assertThat(request.getConfigMetadata()).contains("\"md5\":\"\"");
    }

    @Test
    public void testCustomizeVersionZeroWhenNotPulled() throws Exception {
        enableCustomizer();
        RemoteConfigFileRepo repo = mockRepo("ns", "g", "f", 0, "");
        customizer.register(repo);

        ReportClientRequest request = new ReportClientRequest();
        customizer.customize(request);

        assertThat(request.getConfigMetadata()).contains("\"version\":0");
        assertThat(request.getConfigMetadata()).contains("\"md5\":\"\"");
    }

    @Test
    public void testRegisterDuplicateIgnored() throws Exception {
        enableCustomizer();
        RemoteConfigFileRepo repo1 = mockRepo("ns", "g", "f", 1, "md5_1");
        RemoteConfigFileRepo repo2 = mockRepo("ns", "g", "f", 2, "md5_2");
        customizer.register(repo1);
        customizer.register(repo2);

        ReportClientRequest request = new ReportClientRequest();
        customizer.customize(request);

        int count = JsonParser.parseString(request.getConfigMetadata())
                .getAsJsonObject().getAsJsonArray("config_watch").size();
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testUnregister() throws Exception {
        enableCustomizer();
        RemoteConfigFileRepo repo = mockRepo("ns", "g", "f", 1, "md5");
        customizer.register(repo);

        ConfigFileMetadata metadata = repo.getConfigFileMetadata();
        customizer.unregister(metadata);

        ReportClientRequest request = new ReportClientRequest();
        customizer.customize(request);

        int count = JsonParser.parseString(request.getConfigMetadata())
                .getAsJsonObject().getAsJsonArray("config_watch").size();
        assertThat(count).isZero();
    }

    @Test
    public void testDestroyClearsWatchedFiles() {
        enableCustomizer();
        customizer.register(mockRepo("ns", "g", "f", 1, "md5"));
        customizer.destroy();

        ReportClientRequest request = new ReportClientRequest();
        customizer.customize(request);

        assertThat(request.getConfigMetadata()).contains("\"config_watch\":[]");
    }

    private void enableCustomizer() {
        ConfigWatchReportRequestCustomizerConfig config = new ConfigWatchReportRequestCustomizerConfig();
        config.setEnable(true);
        try {
            java.lang.reflect.Field field = ConfigWatchReportRequestCustomizer.class.getDeclaredField("enable");
            field.setAccessible(true);
            field.setBoolean(customizer, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
