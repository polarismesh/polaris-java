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

package com.tencent.polaris.api.rpc;

import com.tencent.polaris.api.plugin.server.CommonProviderRequest;
import java.util.Map;

/**
 * 服务实例注册请求
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class InstanceRegisterRequest extends CommonProviderBaseEntity {

    public String getInstanceId() {
        return request.getInstanceID();
    }

    public void setInstanceId(String instanceId) {
        request.setInstanceID(instanceId);
    }

    public String getVersion() {
        return request.getVersion();
    }

    public void setVersion(String version) {
        request.setVersion(version);
    }

    public String getProtocol() {
        return request.getProtocol();
    }

    public void setProtocol(String protocol) {
        request.setProtocol(protocol);
    }

    public Integer getWeight() {
        return request.getWeight();
    }

    public void setWeight(Integer weight) {
        request.setWeight(weight);
    }

    public Integer getPriority() {
        return request.getPriority();
    }

    public void setPriority(Integer priority) {
        request.setPriority(priority);
    }

    public Map<String, String> getMetadata() {
        return request.getMetadata();
    }

    public void setMetadata(Map<String, String> metadata) {
        request.setMetadata(metadata);
    }

    public String getZone() {
        return request.getZone();
    }

    public String getRegion() {
        return request.getRegion();
    }

    public String getCampus() {
        return request.getCampus();
    }

    public void setZone(String zone) {
        request.setZone(zone);
    }

    public void setRegion(String region) {
        request.setRegion(region);
    }

    public void setCampus(String campus) {
        request.setCampus(campus);
    }

    /**
     * 心跳上报的TTL，单位秒
     *
     * @return ttl
     */
    public Integer getTtl() {
        return request.getTtl();
    }

    public void setTtl(Integer ttl) {
        request.setTtl(ttl);
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "InstanceRegisterRequest{" +
                "request=" + request +
                '}';
    }

    public CommonProviderRequest getRequest() {
        return request;
    }
}
