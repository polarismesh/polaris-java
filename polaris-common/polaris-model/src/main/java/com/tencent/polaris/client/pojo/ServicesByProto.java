/*
 *  Tencent is pleased to support the open source community by making Polaris available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.client.pojo;

import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.Services;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ServicesByProto implements Services, RegistryCacheValue {

    public static final ServicesByProto EMPTY_SERVICES = new ServicesByProto();

    private final List<ServiceInfo> services;

    private final boolean initialized;

    private final boolean loadedFromFile;

    private final int hashCode;

    private String revision;

    private ServiceKey svcKey;

    public ServicesByProto() {
        this.services = Collections.emptyList();
        this.initialized = false;
        this.loadedFromFile = false;
        this.hashCode = 0;
    }

    public ServicesByProto(List<ServiceInfo> services) {
        this.services = services;
        this.initialized = true;
        this.loadedFromFile = false;
        this.hashCode = 0;
    }

    public ServicesByProto(ResponseProto.DiscoverResponse response, boolean loadFromFile) {
        List<ServiceProto.Service> tmpServices = response.getServicesList();

        this.services = new ArrayList<>();
        this.svcKey = new ServiceKey("", "");
        if (Objects.nonNull(response.getService())) {
            this.revision = response.getService().getRevision().getValue();
        }

        if (CollectionUtils.isNotEmpty(tmpServices)) {
            ServiceProto.Service svc = tmpServices.get(0);
            this.svcKey = new ServiceKey(svc.getNamespace().getValue(), svc.getName().getValue());
            tmpServices.forEach(service -> {
                services.add(ServiceInfo.builder()
                        .namespace(service.getNamespace().getValue())
                        .service(service.getName().getValue())
                        .metadata(service.getMetadataMap())
                        .revision(service.getRevision().getValue())
                        .build());
            });
        }

        this.hashCode = Objects.hash(response.getServicesList());
        this.initialized = true;
        this.loadedFromFile = loadFromFile;
    }

    @Override
    public boolean isLoadedFromFile() {
        return this.loadedFromFile;
    }

    @Override
    public EventType getEventType() {
        return EventType.SERVICE;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public String getRevision() {
        return "";
    }

    public ServiceKey getSvcKey() {
        return svcKey;
    }

    @Override
    public ServiceKey getServiceKey() {
        return svcKey;
    }

    public List<ServiceInfo> getServices() {
        return services;
    }

    public int getHashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "ServicesByProto{" +
                "svcKey=" + svcKey +
                ", services=" + services +
                ", initialized=" + initialized +
                ", loadedFromFile=" + loadedFromFile +
                ", hashCode=" + hashCode +
                '}';
    }
}
