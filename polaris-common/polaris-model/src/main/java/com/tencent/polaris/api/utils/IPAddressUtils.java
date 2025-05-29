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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
}
