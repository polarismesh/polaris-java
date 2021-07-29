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

package com.tencent.polaris.router.api.rpc;

import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import java.util.List;

/**
 * 路由处理请求
 */
public class ProcessRoutersRequest extends RequestBaseEntity {

    private ServiceInfo sourceService;

    private RouterNamesGroup routers;

    private ServiceInstances dstInstances;

    private String method;

    public ServiceInfo getSourceService() {
        return sourceService;
    }

    public void setSourceService(ServiceInfo sourceService) {
        this.sourceService = sourceService;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public RouterNamesGroup getRouters() {
        return routers;
    }

    public void setRouters(RouterNamesGroup routers) {
        this.routers = routers;
    }

    public ServiceInstances getDstInstances() {
        return dstInstances;
    }

    public void setDstInstances(ServiceInstances dstInstances) {
        this.dstInstances = dstInstances;
    }

    public static class RouterNamesGroup {

        private List<String> beforeRouters;

        private List<String> coreRouters;

        private List<String> afterRouters;

        public List<String> getBeforeRouters() {
            return beforeRouters;
        }

        public void setBeforeRouters(List<String> beforeRouters) {
            this.beforeRouters = beforeRouters;
        }

        public List<String> getCoreRouters() {
            return coreRouters;
        }

        public void setCoreRouters(List<String> coreRouters) {
            this.coreRouters = coreRouters;
        }

        public List<String> getAfterRouters() {
            return afterRouters;
        }

        public void setAfterRouters(List<String> afterRouters) {
            this.afterRouters = afterRouters;
        }
    }
}
