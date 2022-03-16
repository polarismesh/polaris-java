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
 * 服务实例变更事件
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class ServiceChangeEvent implements Serializable {

    /**
     * 针对单个实例变化的变化记录数据
     */
    public static class OneInstanceUpdate {

        /**
         * 更新前的实例
         */
        private Instance before;

        /**
         * 更新后的实例
         */
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

    /**
     * 当前服务下的最新实例列表
     */
    private List<Instance> allInstances = Collections.emptyList();

    /**
     * 本次服务实例变化中新增的实例
     */
    private List<Instance> addInstances = Collections.emptyList();

    /**
     * 本次服务实例变化中更新的实例
     */
    private List<OneInstanceUpdate> updateInstances = Collections.emptyList();

    /**
     * 本次服务实例变化中被删除的实例
     */
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

    public List<Instance> getAllInstances() {
        return allInstances;
    }

    public static ServiceEventBuilder builder() {
        return new ServiceEventBuilder();
    }

    @Override
    public String toString() {
        return "ServiceChangeEvent{" +
                "serviceKey=" + serviceKey +
                ", allInstances=" + allInstances +
                ", addInstances=" + addInstances +
                ", updateInstances=" + updateInstances +
                ", deleteInstances=" + deleteInstances +
                '}';
    }

    public static final class ServiceEventBuilder {
        private ServiceKey serviceKey;
        private List<Instance> allInstances = Collections.emptyList();
        private List<Instance> addInstances = Collections.emptyList();
        private List<OneInstanceUpdate> updateInstances = Collections.emptyList();
        private List<Instance> deleteInstances = Collections.emptyList();

        private ServiceEventBuilder() {
        }

        public ServiceEventBuilder serviceKey(ServiceKey serviceKey) {
            this.serviceKey = serviceKey;
            return this;
        }

        public ServiceEventBuilder allInstances(List<Instance> allInstances) {
            this.allInstances = allInstances;
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
            ServiceChangeEvent serviceChangeEvent = new ServiceChangeEvent();
            serviceChangeEvent.addInstances = this.addInstances;
            serviceChangeEvent.deleteInstances = this.deleteInstances;
            serviceChangeEvent.serviceKey = this.serviceKey;
            serviceChangeEvent.updateInstances = this.updateInstances;
            serviceChangeEvent.allInstances = this.allInstances;
            return serviceChangeEvent;
        }
    }
}
