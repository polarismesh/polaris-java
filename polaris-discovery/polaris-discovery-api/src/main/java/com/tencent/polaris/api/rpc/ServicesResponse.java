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

package com.tencent.polaris.api.rpc;

import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.Services;

import java.util.Collections;
import java.util.List;

/**
 * 批量获取服务信息响应
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class ServicesResponse extends BaseEntity {

    private final Services services;

    public ServicesResponse(Services services) {
        this.services = services;
    }

    public List<ServiceInfo> getServices() {
        if (services == null) {
            return Collections.emptyList();
        }
        return services.getServices();
    }

}
