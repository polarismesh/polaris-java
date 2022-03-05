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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class ServiceChangeEvent implements Serializable {

    public static class OneInstanceUpdate {

        private Instance before;
        private Instance after;

        public OneInstanceUpdate(Instance before, Instance after) {
            this.before = before;
            this.after = after;
        }

        public Instance getBefore() {
            return before;
        }

        public Instance getAfter() {
            return after;
        }
    }

    /**
     * serviceKey
     */
    private ServiceKey serviceKey;

    private List<Instance> addInstances = Collections.emptyList();

    private List<OneInstanceUpdate> updateInstances = Collections.emptyList();

    private List<Instance> deleteInstances = Collections.emptyList();

    public ServiceKey getServiceKey() {
        return serviceKey;
    }

    public List<Instance> getAddInstances() {
        return addInstances;
    }

    public List<OneInstanceUpdate> getUpdateInstances() {
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
        private List<Instance> addInstances = Collections.emptyList();
        private List<OneInstanceUpdate> updateInstances = Collections.emptyList();
        private List<Instance> deleteInstances = Collections.emptyList();

        private ServiceEventBuilder() {
        }

        public ServiceEventBuilder serviceKey(ServiceKey serviceKey) {
            this.serviceKey = serviceKey;
            return this;
        }

        public ServiceEventBuilder addInstances(List<Instance> addInstances) {
            this.addInstances = addInstances;
            return this;
        }

        public ServiceEventBuilder updateInstances(List<OneInstanceUpdate> updateInstances) {
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
            serviceEvent.serviceKey = this.serviceKey;
            serviceEvent.deleteInstances = this.deleteInstances;
            serviceEvent.updateInstances = this.updateInstances;
            return serviceEvent;
        }
    }

    @Override
    public String toString() {
        return "ServiceChangeEvent{" +
                "serviceKey=" + serviceKey +
                ", addInstances=" + addInstances +
                ", updateInstances=" + updateInstances +
                ", deleteInstances=" + deleteInstances +
                '}';
    }
}
