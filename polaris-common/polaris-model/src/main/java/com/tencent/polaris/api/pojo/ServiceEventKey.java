package com.tencent.polaris.api.pojo;

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
            return new ServiceEventKey(serviceKey, eventType);
        }
    }
}
