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

package com.tencent.polaris.client.pojo;

import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class Node {

    private static final Logger LOG = LoggerFactory.getLogger(Node.class);

    private final String host;

    private final int port;

    public Node(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Node(Node another) {
        this.host = another.host;
        this.port = another.port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getHostPort() {
        return host + ":" + port;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node) o;
        return port == node.port &&
                Objects.equals(host, node.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "Node{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

    public boolean isAnyAddress() {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            LOG.error("fail to parse host {}", host, e);
            return false;
        }
        return inetAddress.isAnyLocalAddress();
    }
}
