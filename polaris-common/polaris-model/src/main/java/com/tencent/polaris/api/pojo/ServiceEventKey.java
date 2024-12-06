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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.api.utils.StringUtils;

import java.util.Objects;

/**
 * 服务加规则的唯一标识KEY
 *
 * @author vickliu
 * @date
 */
public class ServiceEventKey implements Service {

    @Override
    public String getService() {
        return serviceKey.getService();
    }

    @Override
    public String getNamespace() {
        return serviceKey.getNamespace();
    }

    public enum EventType {
        UNKNOWN,
        INSTANCE,
        ROUTING,
        CIRCUIT_BREAKING,
        RATE_LIMITING,
        SERVICE,
        FAULT_DETECTING,
        SERVICE_CONTRACT,
        LANE_RULE,
        NEARBY_ROUTE_RULE,
        LOSSLESS,
        BLOCK_ALLOW_RULE,
    }

    private final ServiceKey serviceKey;

    private final EventType eventType;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceEventKey)) {
            return false;
        }
        ServiceEventKey that = (ServiceEventKey) o;
        return Objects.equals(serviceKey, that.serviceKey) && eventType == that.eventType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceKey, eventType);
    }

    public ServiceEventKey(ServiceKey serviceKey, EventType eventType) {
        this.serviceKey = serviceKey;
        this.eventType = eventType;
    }

    public ServiceKey getServiceKey() {
        return serviceKey;
    }

    public EventType getEventType() {
        return eventType;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ServiceEventKey{" +
                "serviceKey=" + serviceKey +
                ", eventType=" + eventType +
                '}';
    }

    public void verify() {
        if (Objects.equals(EventType.SERVICE, eventType)) {
            return;
        }
        if (StringUtils.isAnyEmpty(serviceKey.getNamespace(), serviceKey.getService())) {
            throw new IllegalArgumentException(String.format("invalid service key, namespace:%s service:%s",
                    serviceKey.getNamespace(), serviceKey.getService()));
        }
    }

    public static ServiceEventKeyBuilder builder() {
        return new ServiceEventKeyBuilder();
    }

    public static final class ServiceEventKeyBuilder {
        private ServiceKey serviceKey;
        private EventType eventType;

        private ServiceEventKeyBuilder() {
        }

        public ServiceEventKeyBuilder serviceKey(ServiceKey serviceKey) {
            this.serviceKey = serviceKey;
            return this;
        }

        public ServiceEventKeyBuilder eventType(EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public ServiceEventKey build() {
            ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, eventType);
            return serviceEventKey;
        }
    }
}
