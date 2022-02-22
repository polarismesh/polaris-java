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

package com.tencent.polaris.plugins.connector.grpc.codec;

import com.tencent.polaris.api.plugin.registry.CacheHandler;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse;
import com.tencent.polaris.client.pb.ServiceProto;
import com.tencent.polaris.client.pojo.ServicesByProto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.codec.digest.Sha2Crypt;

public class ServicesCacheHandler implements CacheHandler {

    @Override
    public EventType getTargetEventType() {
        return EventType.SERVICE;
    }

    @Override
    public CachedStatus compareMessage(RegistryCacheValue oldValue, Object newValue) {
        DiscoverResponse discoverResponse = (DiscoverResponse) newValue;
        return CommonHandler.compareMessage(getTargetEventType(), oldValue, discoverResponse,
                discoverResponse1 -> {
                    List<ServiceProto.Service> tmpServices = discoverResponse1.getServicesList();
                    List<String> revisions = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(tmpServices)) {
                        tmpServices.forEach(service -> revisions.add(service.getRevision().getValue()));
                    }
                    Collections.sort(revisions);
                    StringBuilder revisionAppender = new StringBuilder();
                    revisions.forEach(revisionAppender::append);
                    return Sha2Crypt.sha256Crypt(revisionAppender.toString().getBytes());
                });
    }

    @Override
    public RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded) {
        DiscoverResponse discoverResponse = (DiscoverResponse) newValue;
        return new ServicesByProto(discoverResponse, isCacheLoaded);
    }
}
