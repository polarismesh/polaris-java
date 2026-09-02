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

package com.tencent.polaris.ai.client.internal;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.util.FileUtils;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Persist skill payloads to local files, keyed by namespace/name/version.
 */
public class SkillPersistentHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SkillPersistentHandler.class);

    private static final String DIR_GET = "get";

    private static final String DIR_DOWNLOAD = "download";

    private static final String DIR_ACTIVE = "active";

    private static final String EXT_YAML = ".yaml";

    private static final String EXT_ZIP = ".zip";

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    private final ExecutorService persistExecutor = Executors
            .newSingleThreadExecutor(new NamedThreadFactory("skill-persistent-handler"));

    private final String persistDirPath;

    private final boolean persistEnable;

    private final int maxWriteRetry;

    private final int maxReadRetry;

    private final long retryInterval;

    public SkillPersistentHandler(String persistDir, boolean persistEnable, int maxWriteRetry,
            int maxReadRetry, long retryInterval) throws IOException {
        this.persistEnable = persistEnable;
        this.maxWriteRetry = maxWriteRetry;
        this.maxReadRetry = maxReadRetry;
        this.retryInterval = retryInterval;
        this.persistDirPath = Utils.translatePath(persistDir);
        FileUtils.dirPathCheck(this.persistDirPath);
    }

    /**
     * Persist GetSkill response under an explicit version.
     *
     * @param namespace namespace
     * @param name skill name
     * @param version skill version
     * @param response response
     */
    public void asyncSaveGetSkill(String namespace, String name, String version, SkillGetResponse response) {
        if (persistEnable) {
            persistExecutor.execute(new SaveGetTask(namespace, name, version, response));
        }
    }

    /**
     * Persist download response. Zip bytes go to a sibling .zip file.
     *
     * @param namespace namespace
     * @param name skill name
     * @param version skill version
     * @param format markdown or zip
     * @param response response
     */
    public void asyncSaveDownload(String namespace, String name, String version, String format,
            SkillDownloadResponse response) {
        if (persistEnable) {
            persistExecutor.execute(new SaveDownloadTask(namespace, name, version, format, response));
        }
    }

    /**
     * Persist active version pointer when request version is empty.
     *
     * @param namespace namespace
     * @param name skill name
     * @param version resolved version
     */
    public void asyncSaveActiveVersion(String namespace, String name, String version) {
        if (persistEnable) {
            persistExecutor.execute(new SaveActiveTask(namespace, name, version));
        }
    }

    /**
     * Load persisted GetSkill response.
     *
     * @param namespace namespace
     * @param name skill name
     * @param version skill version
     * @return cached response or null
     */
    public SkillGetResponse loadGetSkill(String namespace, String name, String version) {
        SkillGetResponse result = null;
        if (persistEnable) {
            result = readYaml(buildGetPath(namespace, name, version), SkillGetResponse.class);
        }
        return result;
    }

    /**
     * Load persisted download response.
     *
     * @param namespace namespace
     * @param name skill name
     * @param version skill version
     * @param format markdown or zip
     * @return cached response or null
     */
    public SkillDownloadResponse loadDownload(String namespace, String name, String version, String format) {
        SkillDownloadResponse result = null;
        if (persistEnable) {
            result = readYaml(buildDownloadYamlPath(namespace, name, version, format), SkillDownloadResponse.class);
            fillZipContent(result, namespace, name, version, format);
        }
        return result;
    }

    /**
     * Load last active version pointer.
     *
     * @param namespace namespace
     * @param name skill name
     * @return version or null
     */
    public String loadActiveVersion(String namespace, String name) {
        String result = null;
        if (persistEnable) {
            result = readText(buildActivePath(namespace, name));
        }
        return result;
    }

    /**
     * Stop persist executor.
     */
    public void doDestroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{persistExecutor});
    }

    /**
     * Persist GetSkill response synchronously. Used by tests and fallback warmup.
     *
     * @param namespace namespace
     * @param name skill name
     * @param version skill version
     * @param response response
     */
    public void saveGetSkill(String namespace, String name, String version, SkillGetResponse response) {
        if (persistEnable) {
            writeYaml(buildGetPath(namespace, name, version), response);
        }
    }

    /**
     * Persist download response synchronously.
     *
     * @param namespace namespace
     * @param name skill name
     * @param version skill version
     * @param format markdown or zip
     * @param response response
     */
    public void saveDownload(String namespace, String name, String version, String format,
            SkillDownloadResponse response) {
        if (persistEnable) {
            SkillDownloadResponse meta = copyDownloadMeta(response);
            writeYaml(buildDownloadYamlPath(namespace, name, version, format), meta);
            writeZipIfPresent(namespace, name, version, format, response);
        }
    }

    /**
     * Persist active version pointer synchronously.
     *
     * @param namespace namespace
     * @param name skill name
     * @param version resolved version
     */
    public void saveActiveVersion(String namespace, String name, String version) {
        if (persistEnable) {
            writeText(buildActivePath(namespace, name), version);
        }
    }

    private void fillZipContent(SkillDownloadResponse result, String namespace, String name, String version,
            String format) {
        if (result != null && "zip".equals(format)) {
            result.setZipContent(readBytes(buildZipPath(namespace, name, version)));
        }
    }

    private void writeZipIfPresent(String namespace, String name, String version, String format,
            SkillDownloadResponse response) {
        if ("zip".equals(format) && response.getZipContent() != null) {
            writeBytes(buildZipPath(namespace, name, version), response.getZipContent());
        }
    }

    private SkillDownloadResponse copyDownloadMeta(SkillDownloadResponse response) {
        SkillDownloadResponse meta = new SkillDownloadResponse();
        meta.setCode(response.getCode());
        meta.setInfo(response.getInfo());
        meta.setVersion(response.getVersion());
        meta.setContentType(response.getContentType());
        meta.setContent(response.getContent());
        meta.setTotalSize(response.getTotalSize());
        meta.setFilename(response.getFilename());
        return meta;
    }

    private Path buildGetPath(String namespace, String name, String version) {
        return new File(persistDirPath + File.separator + DIR_GET,
                encodeKey(namespace, name, version) + EXT_YAML).toPath();
    }

    private Path buildDownloadYamlPath(String namespace, String name, String version, String format) {
        return new File(persistDirPath + File.separator + DIR_DOWNLOAD,
                encodeKey(namespace, name, version) + "." + format + EXT_YAML).toPath();
    }

    private Path buildZipPath(String namespace, String name, String version) {
        return new File(persistDirPath + File.separator + DIR_DOWNLOAD,
                encodeKey(namespace, name, version) + EXT_ZIP).toPath();
    }

    private Path buildActivePath(String namespace, String name) {
        return new File(persistDirPath + File.separator + DIR_ACTIVE,
                encodePair(namespace, name)).toPath();
    }

    private String encodeKey(String namespace, String name, String version) {
        return encodePair(namespace, name) + "#" + encodeToken(version);
    }

    private String encodePair(String namespace, String name) {
        return encodeToken(namespace) + "#" + encodeToken(name);
    }

    private String encodeToken(String value) {
        String encoded = value;
        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.warn("fail to encode persist key {}", value, e);
        }
        return encoded;
    }

    private void writeYaml(Path path, Object value) {
        int retryTimes = 0;
        boolean success = false;
        while (retryTimes <= maxWriteRetry && !success) {
            retryTimes++;
            success = doWriteYaml(path, value);
        }
        if (!success) {
            LOG.error("fail to persist skill file {} after retry {}", path, retryTimes);
        }
    }

    private boolean doWriteYaml(Path path, Object value) {
        boolean success = false;
        try {
            ensureParent(path);
            Path tmpPath = path.resolveSibling(path.getFileName().toString() + ".tmp");
            YAML_MAPPER.writeValue(tmpPath.toFile(), value);
            Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            success = true;
        } catch (IOException e) {
            LOG.error("fail to write skill yaml {}", path, e);
        }
        return success;
    }

    private void writeBytes(Path path, byte[] data) {
        try {
            ensureParent(path);
            Files.write(path, data);
        } catch (IOException e) {
            LOG.error("fail to write skill zip {}", path, e);
        }
    }

    private void writeText(Path path, String text) {
        try {
            ensureParent(path);
            Files.write(path, text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.error("fail to write skill active pointer {}", path, e);
        }
    }

    private <T> T readYaml(Path path, Class<T> type) {
        T result = null;
        int retryTimes = 0;
        while (retryTimes <= maxReadRetry && result == null) {
            retryTimes++;
            result = doReadYaml(path, type);
            if (result == null && retryTimes <= maxReadRetry) {
                Utils.sleepUninterrupted(retryInterval);
            }
        }
        return result;
    }

    private <T> T doReadYaml(Path path, Class<T> type) {
        T result = null;
        if (Files.exists(path)) {
            try {
                result = YAML_MAPPER.readValue(path.toFile(), type);
            } catch (IOException e) {
                LOG.warn("fail to read skill yaml {}", path, e);
            }
        }
        return result;
    }

    private byte[] readBytes(Path path) {
        byte[] result = null;
        if (Files.exists(path)) {
            try {
                result = Files.readAllBytes(path);
            } catch (IOException e) {
                LOG.warn("fail to read skill zip {}", path, e);
            }
        }
        return result;
    }

    private String readText(Path path) {
        String result = null;
        if (Files.exists(path)) {
            try {
                result = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                if (StringUtils.isBlank(result)) {
                    result = null;
                }
            } catch (IOException e) {
                LOG.warn("fail to read skill active pointer {}", path, e);
            }
        }
        return result;
    }

    private void ensureParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private class SaveGetTask implements Runnable {

        private final String namespace;

        private final String name;

        private final String version;

        private final SkillGetResponse response;

        SaveGetTask(String namespace, String name, String version, SkillGetResponse response) {
            this.namespace = namespace;
            this.name = name;
            this.version = version;
            this.response = response;
        }

        @Override
        public void run() {
            saveGetSkill(namespace, name, version, response);
        }
    }

    private class SaveDownloadTask implements Runnable {

        private final String namespace;

        private final String name;

        private final String version;

        private final String format;

        private final SkillDownloadResponse response;

        SaveDownloadTask(String namespace, String name, String version, String format,
                SkillDownloadResponse response) {
            this.namespace = namespace;
            this.name = name;
            this.version = version;
            this.format = format;
            this.response = response;
        }

        @Override
        public void run() {
            saveDownload(namespace, name, version, format, response);
        }
    }

    private class SaveActiveTask implements Runnable {

        private final String namespace;

        private final String name;

        private final String version;

        SaveActiveTask(String namespace, String name, String version) {
            this.namespace = namespace;
            this.name = name;
            this.version = version;
        }

        @Override
        public void run() {
            saveActiveVersion(namespace, name, version);
        }
    }
}
