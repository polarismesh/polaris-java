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

package com.tencent.polaris.discovery.client.flow;

import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.WatchInstancesRequest;
import com.tencent.polaris.api.rpc.WatchServiceRequest;

import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class CommonWatchServiceRequest  implements ServiceEventKeysProvider {

    private final WatchInstancesRequest watchServiceRequest;

    private final ServiceEventKey eventKey;

    private CommonInstancesRequest allRequest;

    public CommonWatchServiceRequest(WatchInstancesRequest watchServiceRequest, CommonInstancesRequest allRequest) {
        this.watchServiceRequest = watchServiceRequest;
        this.eventKey = ServiceEventKey
                .builder()
                .serviceKey(new ServiceKey(watchServiceRequest.getNamespace(), watchServiceRequest.getService()))
                .eventType(ServiceEventKey.EventType.INSTANCE).
                build();
        this.allRequest = allRequest;
    }

    @Override
    public boolean isUseCache() {
        return false;
    }

    @Override
    public Set<ServiceEventKey> getSvcEventKeys() {
        return Collections.emptySet();
    }

    @Override
    public ServiceEventKey getSvcEventKey() {
        return eventKey;
    }

    public WatchInstancesRequest getWatchServiceRequest() {
        return watchServiceRequest;
    }

    public CommonInstancesRequest getAllRequest() {
        return allRequest;
    }
}
