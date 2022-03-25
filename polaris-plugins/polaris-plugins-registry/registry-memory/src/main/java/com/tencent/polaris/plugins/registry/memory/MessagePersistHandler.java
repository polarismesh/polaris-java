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

package com.tencent.polaris.plugins.registry.memory;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.logging.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * 消息持久化处理器，用于持久化PB对象
 *
 * @author andrewshan
 * @date 2019/9/5
 */
public class MessagePersistHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MessagePersistHandler.class);

    private static final String CACHE_SUFFIX = ".yaml";

    private static final Pattern REGEX_PATTERN_SERVICE = Pattern.compile("^svc#.+#.+#.+\\.yaml$");

    private static final String PATTERN_SERVICE = "svc#%s#%s#%s.yaml";

    private final File persistDirFile;

    private final String persistDirPath;

    private final int maxWriteRetry;

    private final int maxReadRetry;

    private final long retryInterval;

    private final JsonFormat.Printer printer = JsonFormat.printer();

    private final JsonFormat.Parser parser = JsonFormat.parser();

    public MessagePersistHandler(
            String persistDirPath, int maxWriteRetry, int maxReadRetry, long retryInterval) {
        this.maxReadRetry = maxReadRetry;
        this.maxWriteRetry = maxWriteRetry;
        this.retryInterval = retryInterval;
        this.persistDirPath = Utils.translatePath(persistDirPath);
        persistDirFile = new File(this.persistDirPath);
    }

    public void init() throws IOException {
        try {
            if (!persistDirFile.exists() && !persistDirFile.mkdirs()) {
                throw new IOException(String.format("fail to create dir %s", persistDirPath));
            }
            //检查文件夹是否具备写权限
            if (!Files.isWritable(FileSystems.getDefault().getPath(persistDirPath))) {
                throw new IOException(String.format("fail to check permission for dir %s", persistDirPath));
            }
        } catch (Throwable e) {
            throw new IOException(String.format("fail to check permission for dir %s", persistDirPath), e);
        }
    }

    /**
     * 删除服务缓存数据
     *
     * @param svcEventKey 服务标识
     */
    public void deleteService(ServiceEventKey svcEventKey) {
        String fileName = serviceKeyToFileName(svcEventKey);
        String persistFilePath = persistDirPath + File.separator + fileName;
        try {
            Files.deleteIfExists(FileSystems.getDefault().getPath(persistFilePath));
        } catch (IOException e) {
            LOG.error("fail to delete cache file {}", persistFilePath);
        }
        String lockFileName = fileName + ".lock";
        String persistFileLockPath = persistDirPath + File.separator + lockFileName;
        try {
            Files.deleteIfExists(FileSystems.getDefault().getPath(persistFileLockPath));
        } catch (IOException e) {
            LOG.error("fail to delete cache lock file {}", persistFileLockPath);
        }
    }

    /**
     * 持久化单个服务实例数据
     *
     * @param svcEventKey 服务标识
     * @param message 数据内容
     */
    public void saveService(ServiceEventKey svcEventKey, Message message) {
        int retryTimes = 0;
        LOG.info("start to save service {}", svcEventKey);
        while (retryTimes <= maxWriteRetry) {
            retryTimes++;
            Path path = doSaveService(svcEventKey, message);
            if (null == path) {
                continue;
            }
            LOG.info("end to save service {} to {}", svcEventKey, path);
            return;
        }
        LOG.error("fail to persist service {} after retry {}", svcEventKey, retryTimes);
    }

    private static String serviceKeyToFileName(ServiceEventKey svcEventKey) {
        try {
            String encodedNamespace = URLEncoder.encode(svcEventKey.getServiceKey().getNamespace(), "UTF-8");
            String encodedService = URLEncoder.encode(svcEventKey.getServiceKey().getService(), "UTF-8");
            String eventType = URLEncoder.encode(svcEventKey.getEventType().toString().toLowerCase(), "UTF-8");
            return String.format(PATTERN_SERVICE, encodedNamespace, encodedService, eventType);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
            // or 'throw new AssertionError("Impossible things are happening today. " +
            //                              "Consider buying a lottery ticket!!");'
        }
    }

    private static ServiceEventKey fileNameToServiceKey(String fileName) {
        fileName = fileName.substring(0, fileName.length() - CACHE_SUFFIX.length());
        String[] pieces = fileName.split("#");
        try {
            String namespace = URLDecoder.decode(pieces[1], "UTF-8");
            String service = URLDecoder.decode(pieces[2], "UTF-8");
            String eventTypeStr = URLDecoder.decode(pieces[3], "UTF-8");
            ServiceEventKey.EventType eventType = ServiceEventKey.EventType.valueOf(eventTypeStr.toUpperCase());
            return new ServiceEventKey(new ServiceKey(namespace, service), eventType);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
            // or 'throw new AssertionError("Impossible things are happening today. " +
            //                              "Consider buying a lottery ticket!!");'
        }
    }

    private void writeTmpFile(File persistTmpFile, File persistLockFile, Message message) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(persistLockFile, "rw");
                FileChannel channel = raf.getChannel()) {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                throw new IOException(
                        "fail to lock file " + persistTmpFile.getAbsolutePath() + ", ignore and retry later");
            }
            //执行保存
            try {
                doWriteTmpFile(persistTmpFile, message);
            } finally {
                lock.release();
            }
        }
    }

    private void doWriteTmpFile(File persistTmpFile, Message message) throws IOException {
        if (!persistTmpFile.exists()) {
            if (!persistTmpFile.createNewFile()) {
                LOG.warn("tmp file {} already exists", persistTmpFile.getAbsolutePath());
            }
        }
        try (FileOutputStream outputFile = new FileOutputStream(persistTmpFile)) {
            String jsonStr = printer.print(message);
            JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonStr);
            String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
            outputFile.write(jsonAsYaml.getBytes(StandardCharsets.UTF_8));
            outputFile.flush();
        }
    }

    private Path doSaveService(ServiceEventKey svcEventKey, Message message) {
        String fileName = serviceKeyToFileName(svcEventKey);
        String tmpFileName = fileName + ".tmp";
        String lockFileName = fileName + ".lock";
        String persistFilePathStr = persistDirPath + File.separator + fileName;
        Path persistPath = FileSystems.getDefault().getPath(persistFilePathStr);
        File persistTmpFile = new File(persistDirPath + File.separator + tmpFileName);
        File persistLockFile = new File(persistDirPath + File.separator + lockFileName);
        try {
            if (!persistLockFile.exists()) {
                if (!persistLockFile.createNewFile()) {
                    LOG.warn("lock file {} already exists", persistLockFile.getAbsolutePath());
                }
            }
            writeTmpFile(persistTmpFile, persistLockFile, message);
            Files.move(FileSystems.getDefault().getPath(persistTmpFile.getAbsolutePath()),
                    persistPath, REPLACE_EXISTING, ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("fail to write file {}", persistTmpFile, e);
            return null;
        }
        return persistPath.toAbsolutePath();
    }

    /**
     * 遍历缓存目录并加载之前缓存的服务信息
     *
     * @param message 消息对象
     * @return 服务标识-消息对象的集合
     */
    public Map<ServiceEventKey, Message> loadPersistedServices(Message message) {
        Path curDir = Paths.get(persistDirPath);
        Map<ServiceEventKey, Message> result = new HashMap<>();
        try {
            Files.walkFileTree(curDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                    Path fileNamePath = filePath.getFileName();
                    if (null == fileNamePath) {
                        return FileVisitResult.CONTINUE;
                    }
                    String fileName = fileNamePath.toString();
                    if (!REGEX_PATTERN_SERVICE.matcher(fileName).matches()) {
                        return FileVisitResult.CONTINUE;
                    }
                    ServiceEventKey svcEventKey = fileNameToServiceKey(fileName);
                    int retryTimes = 0;
                    Message readMessage = null;
                    while (retryTimes <= maxReadRetry) {
                        retryTimes++;
                        Message.Builder builder = message.newBuilderForType();
                        readMessage = loadMessage(filePath.toFile(), builder);
                        if (null == readMessage) {
                            Utils.sleepUninterrupted(retryInterval);
                            continue;
                        }
                        break;
                    }
                    if (null == readMessage) {
                        LOG.debug("fail to read service from {} after retry {} times", fileName, retryTimes);
                        return FileVisitResult.CONTINUE;
                    }
                    result.put(svcEventKey, readMessage);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.error("fail to visit cache directory {}", persistDirPath);
        }
        return result;
    }

    private Message loadMessage(File persistFile, Message.Builder builder) {
        if (null == persistFile || !persistFile.exists()) {
            return null;
        }
        InputStream inputStream = null;
        InputStreamReader reader = null;
        Yaml yaml = new Yaml();
        try {
            inputStream = new FileInputStream(persistFile);
            reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            Map<String, Object> jsonMap = yaml.load(reader);
            ObjectMapper jsonWriter = new ObjectMapper();
            String jsonStr = jsonWriter.writeValueAsString(jsonMap);
            parser.merge(jsonStr, builder);
            return builder.build();
        } catch (IOException e) {
            LOG.debug("fail to read file {}", persistFile.getAbsoluteFile(), e);
            return null;
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.warn("fail to close reader for {}", persistFile.getAbsoluteFile(), e);
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOG.warn("fail to close stream for {}", persistFile.getAbsoluteFile(), e);
                }
            }
        }
    }

}