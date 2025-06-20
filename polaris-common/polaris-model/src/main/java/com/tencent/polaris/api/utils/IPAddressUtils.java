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

package com.tencent.polaris.api.utils;

import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

/**
 * @author Haotian Zhang
 */
public class IPAddressUtils {

    private static final Logger LOG = LoggerFactory.getLogger(IPAddressUtils.class);

    public static String getIpCompatible(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return ip;
        }
        if (ip.contains(":") && !ip.startsWith("[") && !ip.endsWith("]")) {
            return "[" + ip + "]";
        }
        return ip;
    }

    public static String getHostName() {
        try {
            String hostname = System.getenv("HOSTNAME");
            if (StringUtils.isBlank(hostname)) {
                hostname = System.getProperty("HOSTNAME");
            }
            if (StringUtils.isBlank(hostname)) {
                hostname = InetAddress.getLocalHost().getHostName();
            }
            return hostname;
        } catch (Exception e) {
            LOG.warn("get host name error", e);
            return "";
        }
    }

    /**
     * 检查主机地址是否为IPv6格式
     *
     * @param host
     * @return
     */
    public static boolean checkIpv6Host(String host) {
        try {
            if (StringUtils.isNotBlank(host)) {
                return InetAddress.getByName(host) instanceof Inet6Address;
            }
        } catch (UnknownHostException e) {
            // ignore
        }
        return false;
    }

    public static boolean detect(String host, int port, int timeout) {
        if (!ping(host)) {
            return connect(host, port, timeout);
        }
        return true;
    }

    public static boolean ping(String host) {
        try {
            // 根据操作系统构造不同的ping命令
            String command;
            if (System.getProperty("os.name").startsWith("Windows")) {
                command = "ping -n 1 " + host;
            } else {
                command = "ping -c 1 " + host;
            }
            // 执行ping命令
            Process process = Runtime.getRuntime().exec(command);
            // 读取命令输出
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                LOG.trace(line);
            }
            // 等待命令执行完成
            int exitCode = process.waitFor();
            // 0表示成功，其他值表示失败
            if (exitCode != 0) {
                LOG.warn("ping {} with exit code: {}", host, exitCode);
            } else {
                LOG.debug("ping {} successfully with exit code: {}", host, exitCode);
            }
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            LOG.warn("ping {} failed: {}", host, e.getMessage());
        }
        return false;
    }

    public static boolean connect(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            // 设置SO_REUSEADDR为false可以避免重用TIME_WAIT状态的端口
            socket.setReuseAddress(false);
            // 设置TCP_NODELAY禁用Nagle算法
            socket.setTcpNoDelay(true);
            // 绑定随机端口
            socket.bind(new InetSocketAddress(0));
            socket.connect(new InetSocketAddress(host, port), timeout); // 1秒超时
            LOG.debug("connect {}:{} successfully", host, port);
            return true;
        } catch (IOException e) {
            LOG.warn("connect {}:{} failed: {}", host, port, e.getMessage());
        }
        return false;
    }
}
