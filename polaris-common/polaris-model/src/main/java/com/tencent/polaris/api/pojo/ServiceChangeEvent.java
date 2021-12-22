package com.tencent.polaris.api.pojo;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class ServiceChangeEvent implements Serializable {

    public static enum EventType {
        CREATE,
        UPDATE,
        DELETE,
    }

    /**
     * serviceKey
     */
    private ServiceKey serviceKey;

    private EventType eventType;

    private List<Instance> addInstances = Collections.emptyList();

    private List<Instance> updateInstances = Collections.emptyList();

    private List<Instance> deleteInstances = Collections.emptyList();

    public ServiceKey getServiceKey() {
        return serviceKey;
    }

    public EventType getEventType() {
        return eventType;
    }

    public List<Instance> getAddInstances() {
        return addInstances;
    }

    public List<Instance> getUpdateInstances() {
        return updateInstances;
    }

    public List<Instance> getDeleteInstances() {
        return deleteInstances;
    }

    public static ServiceEventBuilder Builder() {
        return new ServiceEventBuilder();
    }

    public static final class ServiceEventBuilder {

        private ServiceKey serviceKey;
        private EventType eventType;
        private List<Instance> addInstances = Collections.emptyList();
        private List<Instance> updateInstances = Collections.emptyList();
        private List<Instance> deleteInstances = Collections.emptyList();

        private ServiceEventBuilder() {
        }

        public ServiceEventBuilder serviceKey(ServiceKey serviceKey) {
            this.serviceKey = serviceKey;
            return this;
        }

        public ServiceEventBuilder eventType(EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public ServiceEventBuilder addInstances(List<Instance> addInstances) {
            this.addInstances = addInstances;
            return this;
        }

        public ServiceEventBuilder updateInstances(List<Instance> updateInstances) {
            this.updateInstances = updateInstances;
            return this;
        }

        public ServiceEventBuilder deleteInstances(List<Instance> deleteInstances) {
            this.deleteInstances = deleteInstances;
            return this;
        }

        public ServiceChangeEvent build() {
            ServiceChangeEvent serviceEvent = new ServiceChangeEvent();
            serviceEvent.addInstances = this.addInstances;
            serviceEvent.eventType = this.eventType;
            serviceEvent.serviceKey = this.serviceKey;
            serviceEvent.deleteInstances = this.deleteInstances;
            serviceEvent.updateInstances = this.updateInstances;
            return serviceEvent;
        }
    }
}
