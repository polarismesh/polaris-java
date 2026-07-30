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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
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
 * Test for {@link ConfigWatchClientReporter}.
 *
 * @author polaris
 */
public class ConfigWatchClientReporterTest {

    private ConfigWatchClientReporter reporter;

    @Before
    public void setUp() {
        reporter = new ConfigWatchClientReporter();
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
        assertThat(reporter.getName()).isEqualTo("config-watch");
    }

    @Test
    public void testGetType() {
        assertThat(reporter.getType()).isNotNull();
    }

    @Test
    public void testGetPluginConfigClazz() {
        assertThat(reporter.getPluginConfigClazz()).isEqualTo(ConfigWatchReporterConfig.class);
    }

    @Test
    public void testContributeDisabled() {
        ReportClientRequest request = new ReportClientRequest();
        reporter.register(mockRepo("ns", "g", "f", 1, "abc"));
        reporter.contribute(request);

        assertThat(request.getConfigEnabled()).isNull();
        assertThat(request.getConfigMetadata()).isNull();
    }

    @Test
    public void testContributeEnabledEmptyList() throws Exception {
        enableReporter();
        ReportClientRequest request = new ReportClientRequest();
        reporter.contribute(request);

        assertThat(request.getConfigEnabled()).isTrue();
        assertThat(request.getConfigMetadata()).isNotNull();
        assertThat(JsonParser.parseString(request.getConfigMetadata())
                .getAsJsonObject().getAsJsonArray("config_watch")).isEmpty();
    }

    @Test
    public void testContributeSingleFile() throws Exception {
        enableReporter();
        reporter.register(mockRepo("default", "scg-test", "application.yaml", 3,
                "e10adc3949ba59abbe56e057f20f883e"));

        ReportClientRequest request = new ReportClientRequest();
        reporter.contribute(request);

        assertThat(request.getConfigEnabled()).isTrue();
        String json = request.getConfigMetadata();
        assertThat(json).contains("default");
        assertThat(json).contains("scg-test");
        assertThat(json).contains("application.yaml");
        assertThat(json).contains("\"version\":3");
        assertThat(json).contains("e10adc3949ba59abbe56e057f20f883e");
    }

    @Test
    public void testContributeMultipleFiles() throws Exception {
        enableReporter();
        reporter.register(mockRepo("default", "g1", "f1", 1, "md5_1"));
        reporter.register(mockRepo("default", "g2", "f2", 2, "md5_2"));

        ReportClientRequest request = new ReportClientRequest();
        reporter.contribute(request);

        assertThat(request.getConfigEnabled()).isTrue();
        int count = JsonParser.parseString(request.getConfigMetadata())
                .getAsJsonObject().getAsJsonArray("config_watch").size();
        assertThat(count).isEqualTo(2);
    }

    @Test
    public void testContributeNullMd5OutputEmptyString() throws Exception {
        enableReporter();
        RemoteConfigFileRepo repo = mockRepo("ns", "g", "f", 0, null);
        reporter.register(repo);

        ReportClientRequest request = new ReportClientRequest();
        reporter.contribute(request);

        assertThat(request.getConfigMetadata()).contains("\"md5\":\"\"");
    }

    @Test
    public void testContributeVersionZeroWhenNotPulled() throws Exception {
        enableReporter();
        RemoteConfigFileRepo repo = mockRepo("ns", "g", "f", 0, "");
        reporter.register(repo);

        ReportClientRequest request = new ReportClientRequest();
        reporter.contribute(request);

        assertThat(request.getConfigMetadata()).contains("\"version\":0");
        assertThat(request.getConfigMetadata()).contains("\"md5\":\"\"");
    }

    @Test
    public void testRegisterDuplicateIgnored() throws Exception {
        enableReporter();
        RemoteConfigFileRepo repo1 = mockRepo("ns", "g", "f", 1, "md5_1");
        RemoteConfigFileRepo repo2 = mockRepo("ns", "g", "f", 2, "md5_2");
        reporter.register(repo1);
        reporter.register(repo2);

        ReportClientRequest request = new ReportClientRequest();
        reporter.contribute(request);

        int count = JsonParser.parseString(request.getConfigMetadata())
                .getAsJsonObject().getAsJsonArray("config_watch").size();
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testUnregister() throws Exception {
        enableReporter();
        RemoteConfigFileRepo repo = mockRepo("ns", "g", "f", 1, "md5");
        reporter.register(repo);

        ConfigFileMetadata metadata = repo.getConfigFileMetadata();
        reporter.unregister(metadata);

        ReportClientRequest request = new ReportClientRequest();
        reporter.contribute(request);

        int count = JsonParser.parseString(request.getConfigMetadata())
                .getAsJsonObject().getAsJsonArray("config_watch").size();
        assertThat(count).isZero();
    }

    @Test
    public void testDestroyClearsWatchedFiles() {
        enableReporter();
        reporter.register(mockRepo("ns", "g", "f", 1, "md5"));
        reporter.destroy();

        ReportClientRequest request = new ReportClientRequest();
        reporter.contribute(request);

        assertThat(request.getConfigMetadata()).contains("\"config_watch\":[]");
    }

    private void enableReporter() {
        ConfigWatchReporterConfig config = new ConfigWatchReporterConfig();
        config.setEnable(true);
        try {
            java.lang.reflect.Field field = ConfigWatchClientReporter.class.getDeclaredField("enable");
            field.setAccessible(true);
            field.setBoolean(reporter, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
