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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;

/**
 * Wrap for Instance.
 *
 * @author Haotian Zhang
 */
public class InstanceWrap implements Instance {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceWrap.class);

    private final Instance originalInstance;

    private final String host;

    public InstanceWrap(Instance originalInstance, boolean isPreferIpv6) {
        this.originalInstance = originalInstance;
        String host = "";
        if (isPreferIpv6 && MapUtils.isNotEmpty(originalInstance.getMetadata())) {
            host = originalInstance.getMetadata().get(MetadataConstants.ADDRESS_IPV6);
            if (StringUtils.isBlank(host)) {
                host = originalInstance.getMetadata().get(TsfMetadataConstants.TSF_ADDRESS_IPV6);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("get ipv6 address {} from metadata: {}", host, originalInstance.getMetadata());
            }
        } else if (MapUtils.isNotEmpty(originalInstance.getMetadata())) {
            host = originalInstance.getMetadata().get(MetadataConstants.ADDRESS_IPV4);
            if (StringUtils.isBlank(host)) {
                host = originalInstance.getMetadata().get(TsfMetadataConstants.TSF_ADDRESS_IPV4);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("get ipv4 address {} from metadata: {}", host, originalInstance.getMetadata());
            }
        }
        if (StringUtils.isBlank(host)) {
            host = originalInstance.getHost();
            if (LOG.isDebugEnabled()) {
                LOG.debug("get address {} from host", host);
            }
        }
        this.host = host;
    }

    @Override
    public String getRevision() {
        return originalInstance.getRevision();
    }

    @Override
    public CircuitBreakerStatus getCircuitBreakerStatus() {
        return originalInstance.getCircuitBreakerStatus();
    }

    @Override
    public Collection<StatusDimension> getStatusDimensions() {
        return originalInstance.getStatusDimensions();
    }

    @Override
    public CircuitBreakerStatus getCircuitBreakerStatus(StatusDimension statusDimension) {
        return originalInstance.getCircuitBreakerStatus(statusDimension);
    }

    @Override
    public RetStatus getDetectStatus() {
        return originalInstance.getDetectStatus();
    }

    @Override
    public boolean isHealthy() {
        return originalInstance.isHealthy();
    }

    @Override
    public boolean isIsolated() {
        return originalInstance.isIsolated();
    }

    @Override
    public String getProtocol() {
        return originalInstance.getProtocol();
    }

    @Override
    public String getId() {
        return originalInstance.getId();
    }

    @Override
    public String getVersion() {
        return originalInstance.getVersion();
    }

    @Override
    public Map<String, String> getMetadata() {
        return originalInstance.getMetadata();
    }

    @Override
    public boolean isEnableHealthCheck() {
        return originalInstance.isEnableHealthCheck();
    }

    @Override
    public String getRegion() {
        return originalInstance.getRegion();
    }

    @Override
    public String getZone() {
        return originalInstance.getZone();
    }

    @Override
    public String getCampus() {
        return originalInstance.getCampus();
    }

    @Override
    public int getPriority() {
        return originalInstance.getPriority();
    }

    @Override
    public int getWeight() {
        return originalInstance.getWeight();
    }

    @Override
    public String getLogicSet() {
        return originalInstance.getLogicSet();
    }

    @Override
    public Long getCreateTime() {
        return originalInstance.getCreateTime();
    }

    @Override
    public String getNamespace() {
        return originalInstance.getNamespace();
    }

    @Override
    public String getService() {
        return originalInstance.getService();
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return originalInstance.getPort();
    }

    @Override
    public int compareTo(Instance o) {
        return originalInstance.compareTo(o);
    }

    @Override
    public String toString() {
        return "InstanceWrap{" +
                "originalInstance=" + originalInstance +
                ", host='" + host + '\'' +
                '}';
    }
}
