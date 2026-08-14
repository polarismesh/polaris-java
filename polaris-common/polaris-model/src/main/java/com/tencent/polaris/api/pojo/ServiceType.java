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

/**
 * Service type aligned with specification ServiceType.
 *
 * @author fishtailfu
 */
public enum ServiceType {

    /**
     * Traditional microservice (default).
     */
    MICROSERVICE("microservice"),

    /**
     * MCP Server that exposes AI tools via MCP.
     */
    MCP_SERVER("mcp_server"),

    /**
     * AI Agent with autonomous decision capability.
     */
    AI_AGENT("ai_agent"),

    ;

    private final String desc;

    ServiceType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * Convert specification ServiceType to SDK ServiceType.
     *
     * @param protoType protobuf service type
     * @return SDK service type, defaults to MICROSERVICE
     */
    public static ServiceType fromProto(ServiceProto.ServiceType protoType) {
        ServiceType serviceType = MICROSERVICE;
        if (null != protoType) {
            switch (protoType) {
                case SERVICE_TYPE_MCP_SERVER:
                    serviceType = MCP_SERVER;
                    break;
                case SERVICE_TYPE_AI_AGENT:
                    serviceType = AI_AGENT;
                    break;
                case SERVICE_TYPE_MICROSERVICE:
                case UNRECOGNIZED:
                default:
                    serviceType = MICROSERVICE;
                    break;
            }
        }
        return serviceType;
    }
}
