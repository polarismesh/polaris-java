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

package com.tencent.polaris.client.remote;

import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.util.CommonValidator;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for service addresses.
 *
 * @author Haotian Zhang
 */
public class ServiceAddressRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceAddressRepository.class);

    private final List<Node> nodes;

    private int curIndex;

    private final String clientId;

    private final Extensions extensions;

    private final ServiceKey remoteCluster;

    private final List<String> routers;

    private final String lbPolicy;

    private final String protocol;

    public ServiceAddressRepository(List<String> addresses, String clientId, Extensions extensions,
                                    ServiceKey remoteCluster) {
        this(addresses, clientId, extensions, remoteCluster, null, null, null);
        this.routers.add(ServiceRouterConfig.DEFAULT_ROUTER_METADATA);
        this.routers.add(ServiceRouterConfig.DEFAULT_ROUTER_NEARBY);
    }

    public ServiceAddressRepository(List<String> addresses, String clientId, Extensions extensions,
                                    ServiceKey remoteCluster, List<String> routers, String lbPolicy, String protocol) {
        // to ip addresses.
        this.nodes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(addresses)) {
            for (String address : addresses) {
                if (StringUtils.isNotBlank(address)) {
                    int colonIdx = address.lastIndexOf(":");
                    if (colonIdx > 0 && colonIdx < address.length() - 1) {
                        String host = IPAddressUtils.getIpCompatible(address.substring(0, colonIdx));
                        try {
                            int port = Integer.parseInt(address.substring(colonIdx + 1));
                            nodes.add(new Node(host, port));
                        } catch (NumberFormatException e) {
                            LOG.warn("Invalid port number in address: {}", address);
                        }
                    } else {
                        LOG.warn("Invalid address format, expected 'host:port': {}", address);
                    }
                }
            }
        }
        this.curIndex = 0;

        // from discovery.
        this.clientId = clientId;
        this.extensions = extensions;
        CommonValidator.validateNamespaceService(remoteCluster.getNamespace(), remoteCluster.getService());
        this.remoteCluster = remoteCluster;
        if (CollectionUtils.isEmpty(routers)) {
            this.routers = new ArrayList<>();
        } else {
            this.routers = routers;
        }
        if (StringUtils.isBlank(lbPolicy)) {
            this.lbPolicy = DefaultValues.DEFAULT_LOADBALANCER;
        } else {
            this.lbPolicy = lbPolicy;
        }
        if (StringUtils.isBlank(protocol)) {
            this.protocol = "http";
        } else {
            this.protocol = protocol;
        }
    }

    public String getServiceAddress() throws PolarisException {
        Node node = getServiceAddressNode();
        return node.getHostPort();
    }

    public Node getServiceAddressNode() throws PolarisException {
        if (CollectionUtils.isNotEmpty(nodes)) {
            Node node = nodes.get(Math.abs(curIndex % nodes.size()));
            curIndex = (curIndex + 1) % Integer.MAX_VALUE;
            if (LOG.isDebugEnabled()) {
                LOG.debug("success to get instance, instance is {}:{}", node.getHost(), node.getPort());
            }
            return node;
        }
        Instance instance = getDiscoverInstance();
        String host = IPAddressUtils.getIpCompatible(instance.getHost());
        if (LOG.isDebugEnabled()) {
            LOG.debug("success to get instance for service {}, instance is {}:{}", remoteCluster, host, instance.getPort());
        }
        return new Node(IPAddressUtils.getIpCompatible(host), instance.getPort());
    }

    private Instance getDiscoverInstance() throws PolarisException {
        Instance instance = BaseFlow.commonGetOneInstance(extensions, remoteCluster, routers, lbPolicy, protocol, clientId);
        LOG.info("success to get instance for service {}, instance is {}:{}", remoteCluster, instance.getHost(), instance.getPort());
        return instance;
    }

    @JustForTest
    List<Node> getNodes() {
        return nodes;
    }

    @JustForTest
    List<String> getRouters() {
        return routers;
    }

    @JustForTest
    String getLbPolicy() {
        return lbPolicy;
    }

    @JustForTest
    String getProtocol() {
        return protocol;
    }
}
