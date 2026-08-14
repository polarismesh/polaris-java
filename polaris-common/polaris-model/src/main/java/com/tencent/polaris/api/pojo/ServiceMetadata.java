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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import java.util.Map;

/**
 * 服务元数据信息
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface ServiceMetadata extends Service {

    /**
     * 获取服务元数据信息
     *
     * @return metadata
     */
    Map<String, String> getMetadata();

    /**
     * Get service type, such as microservice, MCP server or AI agent.
     *
     * @return service type, defaults to {@link ServiceProto.ServiceType#SERVICE_TYPE_MICROSERVICE}
     */
    default ServiceProto.ServiceType getServiceType() {
        return ServiceProto.ServiceType.SERVICE_TYPE_MICROSERVICE;
    }

}
