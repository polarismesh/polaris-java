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

package com.tencent.polaris.configuration.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用日志捕获器，用于断言日志中不含配置正文与密钥。
 *
 * <p>用法：
 * <pre>
 * try (LogCapture capture = LogCapture.attach(TargetClass.class, Level.INFO)) {
 *     // 触发被测逻辑
 *     assertThat(capture.text()).doesNotContain(secret);
 * }
 * </pre>
 *
 * @author evelynwei
 */
public final class LogCapture implements AutoCloseable {

    private final Logger logger;

    private final ListAppender<ILoggingEvent> appender;

    private final Level originalLevel;

    private LogCapture(Logger logger, ListAppender<ILoggingEvent> appender, Level originalLevel) {
        this.logger = logger;
        this.appender = appender;
        this.originalLevel = originalLevel;
    }

    /**
     * 给目标类的 logger 挂上捕获器，并临时调整级别。
     *
     * @param target 目标类，须与被测代码 getLogger 的入参一致
     * @param level 捕获期间生效的日志级别
     * @return 捕获器，用完须 close 以摘除 appender 并还原级别
     */
    public static LogCapture attach(Class<?> target, Level level) {
        // 先经 polaris 的 LoggerFactory 取一次，确保其日志配置初始化早于挂载 appender
        com.tencent.polaris.logging.LoggerFactory.getLogger(target);
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(target);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(level);
        logger.addAppender(appender);
        return new LogCapture(logger, appender, originalLevel);
    }

    /**
     * 已捕获的日志消息（已完成占位符替换）。
     *
     * @return 消息列表
     */
    public List<String> messages() {
        List<String> messages = new ArrayList<>(appender.list.size());
        for (ILoggingEvent event : appender.list) {
            messages.add(event.getFormattedMessage());
        }
        return messages;
    }

    /**
     * 已捕获日志的合并文本，便于做 contains 断言。
     *
     * @return 以换行连接的全部消息
     */
    public String text() {
        return String.join("\n", messages());
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(originalLevel);
    }
}
