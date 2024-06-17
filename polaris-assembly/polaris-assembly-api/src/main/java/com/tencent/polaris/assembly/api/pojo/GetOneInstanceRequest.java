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

package com.tencent.polaris.assembly.api.pojo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.api.rpc.MetadataFailoverType;
import com.tencent.polaris.api.rpc.RequestBaseEntity;

public class GetOneInstanceRequest extends RequestBaseEntity {

    /**
     * 可选，服务元数据信息，用于服务路由过滤
     */
    private Map<String, String> metadata;

    /**
     * 所属的金丝雀集群
     */
    private String canary;

    /**
     * 可选，负载均衡辅助参数
     */
    private Criteria criteria;

    /**
     * 接口参数
     */
    private String method;

    /**
     * 可选, metadata失败降级策略
     */
    private MetadataFailoverType metadataFailoverType;


    /**
     * 主调方服务信息
     */
    private SourceService serviceInfo;

    /**
     * 北极星内部治理规则执行时，会识别规则中的参数来源类别，如果发现规则中的参数来源指定为外部数据源时，会调用本接口进行获取
     *
     * 可以实现该接口，实现规则中的参数来源于配置中心、数据库、环境变量等等
     */
    private Function<String, Optional<String>> externalParameterSupplier = s -> Optional.empty();

    public Criteria getCriteria() {
        return criteria;
    }

    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getCanary() {
        return canary;
    }

    public void setCanary(String canary) {
        this.canary = canary;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public SourceService getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(ServiceInfo serviceInfo) {
        if (serviceInfo instanceof SourceService) {
            this.serviceInfo = (SourceService) serviceInfo;
        } else {
            SourceService sourceService = new SourceService();
            sourceService.setNamespace(serviceInfo.getNamespace());
            sourceService.setService(serviceInfo.getService());
            Set<RouteArgument> argumentSet = new HashSet<>();
            Optional.ofNullable(serviceInfo.getMetadata()).orElse(Collections.emptyMap())
                    .forEach((key, value) -> argumentSet.add(RouteArgument.fromLabel(key, value)));
            sourceService.setArguments(argumentSet);
            this.serviceInfo = sourceService;
        }
    }

    public MetadataFailoverType getMetadataFailoverType() {
        return metadataFailoverType;
    }

    public void setMetadataFailoverType(MetadataFailoverType metadataFailoverType) {
        this.metadataFailoverType = metadataFailoverType;
    }

    public Function<String, Optional<String>> getExternalParameterSupplier() {
        return externalParameterSupplier;
    }

    public void setExternalParameterSupplier(Function<String, Optional<String>> externalParameterSupplier) {
        this.externalParameterSupplier = externalParameterSupplier;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "GetOneInstanceRequest{" +
                "metadata=" + metadata +
                ", canary='" + canary + '\'' +
                ", criteria=" + criteria +
                ", method='" + method + '\'' +
                ", metadataFailoverType=" + metadataFailoverType +
                ", serviceInfo=" + serviceInfo +
                "} " + super.toString();
    }
}
